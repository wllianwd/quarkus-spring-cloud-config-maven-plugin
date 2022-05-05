package io.github.willianwd.plugins.scc;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.willianwd.plugins.scc.dto.PropertySource;
import io.github.willianwd.plugins.scc.dto.ServiceProperties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Mojo(name = "fetch-properties", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class SpringCloudConfigPropertiesMojo extends AbstractMojo {

    @Parameter(property = "skip")
    protected boolean skip = false;

    @Parameter(property = "prettyDumperOptions")
    protected boolean prettyDumperOptions = true;

    @Parameter(property = "bootstrapDirectory", defaultValue = "src/main/resources/")
    protected String bootstrapDirectory;

    @Parameter(property = "targetDirectory", defaultValue = "target/classes/")
    protected String targetDirectory;

    @Parameter(property = "targetFile", defaultValue = "application.yml")
    protected String targetFile;

    @Parameter(property = "bootstrapFile", defaultValue = "bootstrap.yml")
    protected String bootstrapFile;

    @Parameter(property = "profile", defaultValue = "all")
    protected String profile;

    private static final String SKIP_QUARKUS_SCCMP = "SKIP_QUARKUS_SCCMP";
    private static final String URL_TEMPLATE_YAML = "%s/%s/%s-%s.yaml";
    private static final String URL_TEMPLATE_PROPERTIES = "%s/%s/%s/%s";
    private static final String PROPERTY_DELIMITER = ".";
    private static final String APPLICATION_NAME_KEY_1 = "quarkus.spring-cloud-config.name";
    private static final String APPLICATION_NAME_KEY_2 = "quarkus.application.name";
    private static final String SPRING_CLOUD_CONFIG_LABEL_KEY = "quarkus.spring-cloud-config.label";
    private static final String SPRING_CLOUD_CONFIG_URL_KEY = "quarkus.spring-cloud-config.url";
    private static final String SPRING_CLOUD_CONFIG_ENABLED_KEY = "quarkus.spring-cloud-config.enabled";
    private static final String SPRING_CLOUD_CONFIG_DEFAULT_LABEL = "master";
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!shouldSkip()) {
            try {
                final File projectYamlBootstrap = getBootstrapFile();
                if (projectYamlBootstrap.exists()) {
                    final Yaml yaml = new Yaml();
                    final ObjectMapper mapper = new ObjectMapper();
                    final InputStream inputStream = new FileInputStream(projectYamlBootstrap);
                    final Map<String, Object> bootstrapYamlProperties = yaml.load(inputStream);

                    final boolean sccEnabled = (Boolean) getPropertyOrNull(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_ENABLED_KEY);

                    if (sccEnabled) {
                        final File targetClasses = new File(targetDirectory);
                        if (!targetClasses.exists()) {
                            final boolean targetGenerated = targetClasses.mkdirs();
                            getLog().info("Default target/classes directory generated [" + targetGenerated + "]");
                        }

                        String sccUrl = "";
                        if (targetFile.endsWith(".properties")) {
                            sccUrl = String.format(
                                    URL_TEMPLATE_PROPERTIES,
                                    getPropertyOrError(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_URL_KEY),
                                    getApplicationName(bootstrapYamlProperties),
                                    profile,
                                    getPropertyOrDefault(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_LABEL_KEY, SPRING_CLOUD_CONFIG_DEFAULT_LABEL)
                            );
                        } else {
                            sccUrl = String.format(
                                    URL_TEMPLATE_YAML,
                                    getPropertyOrError(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_URL_KEY),
                                    getPropertyOrDefault(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_LABEL_KEY, SPRING_CLOUD_CONFIG_DEFAULT_LABEL),
                                    getApplicationName(bootstrapYamlProperties),
                                    profile
                            );
                        }

                        getLog().info("Getting properties from Spring Cloud Config under URL [" + sccUrl + "]");
                        final HttpRequest request = HttpRequest.newBuilder()
                                .GET()
                                .uri(URI.create(sccUrl))
                                .build();
                        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() != 200) {
                            getLog().error("Spring Cloud Config returned status [" + response.statusCode() + "]");
                        }

                        getLog().info("Writing properties from Spring Cloud Config into local file [" + targetDirectory + targetFile + "]");

                        if (targetFile.endsWith(".properties")) {
                            final ServiceProperties serviceProperties = mapper.readValue(response.body(), ServiceProperties.class);
                            final Map<String, Object> appProps = new HashMap<>();
                            final Map<String, Object> tempProps = new HashMap<>();
                            serviceProperties.getPropertySources().forEach(propertySource -> tempProps.putAll(propertySource.getSource()));
                            appProps.putIfAbsent(APPLICATION_NAME_KEY_2, getRawPropertyOrDefault(bootstrapYamlProperties, APPLICATION_NAME_KEY_2, ""));
                            appProps.putIfAbsent(SPRING_CLOUD_CONFIG_URL_KEY, getRawPropertyOrDefault(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_URL_KEY, ""));
                            appProps.putIfAbsent(SPRING_CLOUD_CONFIG_ENABLED_KEY, sccEnabled);
                            if (getPropertyOrNull(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_LABEL_KEY) != null) {
                                appProps.putIfAbsent(SPRING_CLOUD_CONFIG_LABEL_KEY, getPropertyOrNull(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_LABEL_KEY));
                            }
                            tempProps.forEach((key, value) -> {
                                if (!key.contains("\\${") && key.contains("${") && key.contains("}")) {
                                    getLog().info("Key with placeholder found [" + key + "]");
                                    final String rawPlaceholderOnKey = key.substring(key.indexOf("${"), key.indexOf("}") + 1);
                                    final String placeholderOnKey = rawPlaceholderOnKey.replace("${", "").replace("}", "");
                                    getLog().info("Placeholder key [" + placeholderOnKey + "] key [" + key + "]");
                                    final Object placeholderOnKeyValue = tempProps.get(placeholderOnKey);
                                    if (placeholderOnKeyValue instanceof String) {
                                        appProps.put(key.replace(rawPlaceholderOnKey, (String) placeholderOnKeyValue), value);
                                    } else {
                                        appProps.put(key, value);
                                    }
                                    getLog().info("Found key [" + placeholderOnKey + "] value [" + placeholderOnKeyValue + "]");
                                } else {
                                    appProps.put(key, value);
                                }
                            });
                            final PrintWriter pw = new PrintWriter(new FileWriter(targetDirectory + targetFile));
                            appProps.forEach((key, value) -> pw.write(key + "=" + value + "\n"));
                            pw.close();
                        } else if (targetFile.endsWith(".yml") || targetFile.endsWith(".yaml")) {
                            final Map<String, Object> appProps = yaml.load(response.body());
                            putPropertyIfAbsent(appProps, APPLICATION_NAME_KEY_2, getPropertyOrDefault(bootstrapYamlProperties, APPLICATION_NAME_KEY_2, ""));
                            putPropertyIfAbsent(appProps, SPRING_CLOUD_CONFIG_URL_KEY, getPropertyOrDefault(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_URL_KEY, ""));
                            putPropertyIfAbsent(appProps, SPRING_CLOUD_CONFIG_ENABLED_KEY, sccEnabled);
                            if (getPropertyOrNull(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_LABEL_KEY) != null) {
                                putPropertyIfAbsent(appProps, SPRING_CLOUD_CONFIG_LABEL_KEY, getPropertyOrNull(bootstrapYamlProperties, SPRING_CLOUD_CONFIG_LABEL_KEY));
                            }

                            final Yaml yamlOutput = new Yaml(getDumperOptions());
                            yamlOutput.dump(appProps, new FileWriter(targetDirectory + targetFile));
                        } else {
                            getLog().warn("Invalid target file extension [" + targetFile + "]. Allowed [.properties, .yml, .yaml]");
                        }
                    } else {
                        getLog().info("Ignoring properties from Spring Cloud Config properties as property [" + SPRING_CLOUD_CONFIG_ENABLED_KEY + "] is not set to TRUE");
                    }
                } else {
                    getLog().info("Ignoring properties from Spring Cloud Config properties as config file [" + projectYamlBootstrap.getPath() + "] was not found");
                }

            } catch (InterruptedException | IOException ex) {
                getLog().error(ex);
            }
        }
    }


    private File getBootstrapFile() {
        return new File(bootstrapDirectory + bootstrapFile);
    }


    private Boolean shouldSkip() {
        if (skip) {
            getLog().info("Ignoring plugin as property 'skip' is set to true in your plugin configuration");
            return true;
        } else {
            final String skipArgument = System.getProperty(SKIP_QUARKUS_SCCMP);
            if (skipArgument != null) {
                final boolean shouldSkip = Boolean.parseBoolean(skipArgument);
                if (shouldSkip) {
                    getLog().info("Ignoring plugin as property '" + SKIP_QUARKUS_SCCMP + "' is set to true in your plugin configuration");
                    return true;
                }
            }
        }
        return false;
    }


    private String getApplicationName(final Map<String, Object> map) {
        final String property1 = (String) getPropertyOrNull(map, APPLICATION_NAME_KEY_1);
        return (Objects.nonNull(property1)) ? property1 : getPropertyOrError(map, APPLICATION_NAME_KEY_2);
    }


    private String getPropertyOrDefault(final Map<String, Object> map, final String key, final String defaultValue) {
        final Object property = getPropertyOrNull(map, key);
        if (Objects.nonNull(property)) {
            return (String) property;
        } else {
            return defaultValue;
        }
    }


    private Object getRawPropertyOrDefault(final Map<String, Object> map, final String key, final Object defaultValue) {
        final Object property = getPropertyOrNull(map, key);
        if (Objects.nonNull(property)) {
            return property;
        } else {
            return defaultValue;
        }
    }


    private String getPropertyOrError(final Map<String, Object> map, final String key) {
        final Object property = getPropertyOrNull(map, key);
        if (Objects.nonNull(property)) {
            return property.toString();
        } else {
            throw new RuntimeException("Required property " + key + " was not found in you bootstrap.yml");
        }
    }


    @SuppressWarnings("unchecked")
    private Object getPropertyOrNull(final Map<String, Object> map, final String key) {
        if (key.contains(PROPERTY_DELIMITER)) {
            final String firstKey = key.substring(0, key.indexOf(PROPERTY_DELIMITER));
            final String nextKey = key.substring(key.indexOf(PROPERTY_DELIMITER) + 1);
            final Map<String, Object> nextValue = (Map<String, Object>) map.get(firstKey);
            return getPropertyOrNull(nextValue, nextKey);
        } else {
            return map.get(key);
        }
    }


    public void putPropertyIfAbsent(final Map<String, Object> map, final String key, final Object value) {
        if (key.contains(PROPERTY_DELIMITER)) {
            final String firstKey = key.substring(0, key.indexOf(PROPERTY_DELIMITER));
            final String nextKey = key.substring(key.indexOf(PROPERTY_DELIMITER) + 1);
            map.computeIfAbsent(firstKey, k -> new HashMap<String, Object>());
            final Map<String, Object> nextValue = (Map<String, Object>) map.get(firstKey);
            putPropertyIfAbsent(nextValue, nextKey, value);
        } else {
            map.putIfAbsent(key, value);
        }
    }


    private DumperOptions getDumperOptions() {
        final DumperOptions options = new DumperOptions();
        if (prettyDumperOptions) {
            options.setIndent(2);
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        }
        return options;
    }

}