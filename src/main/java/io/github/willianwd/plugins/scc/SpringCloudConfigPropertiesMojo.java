package io.github.willianwd.plugins.scc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.yaml.snakeyaml.Yaml;

@Mojo(name = "fetch-properties", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class SpringCloudConfigPropertiesMojo extends AbstractMojo
{

    @Parameter(property = "skip")
    protected boolean skip = false;

    @Parameter(property = "bootstrapDirectory", defaultValue = "src/main/resources/")
    protected String bootstrapDirectory;

    @Parameter(property = "targetDirectory", defaultValue = "target/classes/")
    protected String targetDirectory;

    @Parameter(property = "targetFile", defaultValue = "application.yml")
    protected String targetFile;

    @Parameter(property = "bootstrapFile", defaultValue = "bootstrap.yml")
    protected String bootstrapFile;

    private static final String SKIP_QUARKUS_SCCMP = "SKIP_QUARKUS_SCCMP";
    private static final String URL_TEMPLATE = "%s/%s/%s-all.yaml";
    private static final String PROPERTY_DELIMITER = ".";
    private static final String APPLICATION_NAME_KEY_1 = "quarkus.spring-cloud-config.name";
    private static final String APPLICATION_NAME_KEY_2 = "quarkus.application.name";
    private static final String SPRING_CLOUD_CONFIG_LABEL_KEY = "quarkus.spring-cloud-config.label";
    private static final String SPRING_CLOUD_CONFIG_URL_KEY = "quarkus.spring-cloud-config.url";
    private static final String SPRING_CLOUD_CONFIG_ENABLED_KEY = "quarkus.spring-cloud-config.enabled";
    private static final String SPRING_CLOUD_CONFIG_DEFAULT_LABEL = "master";
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (!shouldSkip())
        {
            try
            {
                final File projectYamlBootstrap = getBootstrapFile();
                if (projectYamlBootstrap.exists())
                {
                    final Yaml yaml = new Yaml();
                    final InputStream inputStream = new FileInputStream(projectYamlBootstrap);
                    final Map<String, Object> yamlProperties = yaml.load(inputStream);

                    final boolean sccEnabled = (Boolean) getPropertyOrNull(yamlProperties, SPRING_CLOUD_CONFIG_ENABLED_KEY);

                    if (sccEnabled)
                    {
                        final File targetClasses = new File(targetDirectory);
                        if (!targetClasses.exists())
                        {
                            final boolean targetGenerated = targetClasses.mkdirs();
                            getLog().info("Default target/classes directory generated [" + targetGenerated + "]");
                        }

                        final String sccUrl = String.format(
                            URL_TEMPLATE,
                            getPropertyOrError(yamlProperties, SPRING_CLOUD_CONFIG_URL_KEY),
                            getPropertyOrDefault(yamlProperties, SPRING_CLOUD_CONFIG_LABEL_KEY, SPRING_CLOUD_CONFIG_DEFAULT_LABEL),
                            getApplicationName(yamlProperties)
                        );

                        getLog().info("Getting properties from Spring Cloud Config under URL [" + sccUrl + "]");
                        final HttpRequest request = HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(sccUrl))
                            .build();
                        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() != 200)
                        {
                            getLog().error("Spring Cloud Config returned status [" + response.statusCode() + "]");
                        }

                        final Map<String, Object> appProps = yaml.load(response.body());
                        appProps.putIfAbsent(APPLICATION_NAME_KEY_2, getPropertyOrDefault(yamlProperties, APPLICATION_NAME_KEY_2, ""));
                        appProps.putIfAbsent(SPRING_CLOUD_CONFIG_URL_KEY, getPropertyOrDefault(yamlProperties, SPRING_CLOUD_CONFIG_URL_KEY, ""));
                        appProps.putIfAbsent(SPRING_CLOUD_CONFIG_ENABLED_KEY, String.valueOf(sccEnabled));

                        getLog().info("Writing properties from Spring Cloud Config into local file [" + targetDirectory + targetFile + "]");

                        if (targetFile.endsWith(".properties"))
                        {
                            final Properties propertiesOutput = new Properties();
                            propertiesOutput.putAll(appProps);
                            propertiesOutput.store(new FileWriter(targetDirectory + targetFile), "Properties read from Spring Cloud Config [" + sccUrl + "] and written locally");
                        }
                        else if (targetFile.endsWith(".yml") || targetFile.endsWith(".yaml"))
                        {
                            final Yaml yamlOutput = new Yaml();
                            yamlOutput.dump(appProps, new FileWriter(targetDirectory + targetFile));
                        }
                        else
                        {
                            getLog().warn("Invalid target file extension [" + targetFile + "]. Allowed [.properties, .yml, .yaml]");
                        }
                    }
                    else
                    {
                        getLog().info("Ignoring properties from Spring Cloud Config properties as property [" + SPRING_CLOUD_CONFIG_ENABLED_KEY + "] is not set to TRUE");
                    }
                }
                else
                {
                    getLog().info("Ignoring properties from Spring Cloud Config properties as config file [" + projectYamlBootstrap.getPath() + "] was not found");
                }

            }
            catch (InterruptedException | IOException ex)
            {
                getLog().error(ex);
            }
        }
    }


    private File getBootstrapFile()
    {
        return new File(bootstrapDirectory + bootstrapFile);
    }


    private Boolean shouldSkip()
    {
        if (skip)
        {
            getLog().info("Ignoring plugin as property 'skip' is set to true in your plugin configuration");
            return true;
        }
        else
        {
            final String skipArgument = System.getProperty(SKIP_QUARKUS_SCCMP);
            if (skipArgument != null)
            {
                final boolean shouldSkip = Boolean.parseBoolean(skipArgument);
                if (shouldSkip)
                {
                    getLog().info("Ignoring plugin as property '" + SKIP_QUARKUS_SCCMP + "' is set to true in your plugin configuration");
                    return true;
                }
            }
        }
        return false;
    }


    private String getApplicationName(final Map<String, Object> map)
    {
        final String property1 = (String) getPropertyOrNull(map, APPLICATION_NAME_KEY_1);
        return (Objects.nonNull(property1)) ? property1 : getPropertyOrError(map, APPLICATION_NAME_KEY_2);
    }


    private String getPropertyOrDefault(final Map<String, Object> map, final String key, final String defaultValue)
    {
        final Object property = getPropertyOrNull(map, key);
        if (Objects.nonNull(property))
        {
            return (String) property;
        }
        else
        {
            return defaultValue;
        }
    }


    private String getPropertyOrError(final Map<String, Object> map, final String key)
    {
        final Object property = getPropertyOrNull(map, key);
        if (Objects.nonNull(property))
        {
            return property.toString();
        }
        else
        {
            throw new RuntimeException("Required property " + key + " was not found in you bootstrap.yml");
        }
    }


    @SuppressWarnings("unchecked")
    private Object getPropertyOrNull(final Map<String, Object> map, final String key)
    {
        if (key.contains(PROPERTY_DELIMITER))
        {
            final String firstKey = key.substring(0, key.indexOf(PROPERTY_DELIMITER));
            final String nextKey = key.substring(key.indexOf(PROPERTY_DELIMITER) + 1);
            final Map<String, Object> nextValue = (Map<String, Object>) map.get(firstKey);
            return getPropertyOrNull(nextValue, nextKey);
        }
        else
        {
            return map.get(key);
        }
    }

}