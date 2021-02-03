# Quarkus Spring Cloud Config Maven Plugin

![Maven Central](https://img.shields.io/maven-central/v/io.github.willianwd/quarkus-spring-cloud-config-maven-plugin?logo=apache-maven&style=for-the-badge)
![Release CI](https://img.shields.io/github/workflow/status/wllianwd/quarkus-spring-cloud-config-maven-plugin/ci-release?style=for-the-badge)

In a nutshell this maven plugin will download the properties from Spring Cloud Config and generate an `application.properties` file
 inside `src/main/resources`, so this file can be used during build phase.

## How to use it

Add the maven plugin in your build, like below:
```
<build>
    <plugins>
        <plugin>
            <groupId>io.github.willianwd</groupId>
            <artifactId>quarkus-spring-cloud-config-maven-plugin</artifactId>
            <version>${version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>fetch-properties</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build> 
```
Create a `bootstrap.yml` in your `src/main/resources`, like below:
```
quarkus:
  application:
    name: myservice
  spring-cloud-config:
    enabled: true
    # this property is mandatory
    url: "https://springcloudconfigserver.mycompany.com/springconfig"
    # this property is optional and it represents the branch from where the properties will be retrieved, default is master
    label: master
    # this property is optional and overrides quarkus.application.name
    name: myservice
```
> The properties above will be copied to the target `application.properties`, so you don't need to duplicate it.

The plugin will read a file named `bootstrap.yml` in the `src/main/resources` directory.

You can change the default file locations by specifying the properties, as shown below:
```
<build>
    <plugins>
        <plugin>
            <groupId>io.quarkus.willianwd</groupId>
            <artifactId>quarkus-spring-cloud-config-maven-plugin</artifactId>
            <version>${version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>fetch-properties</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <bootstrapDirectory>src/main/resources/</bootstrapDirectory>
                <bootstrapTestDirectory>src/test/resources/</bootstrapTestDirectory>
                <bootstrapFile>bootstrap.yml</bootstrapFile>
                <targetDirectory>target/classes/</targetDirectory>
                <targetFile>application.properties</targetFile>
            </configuration>
        </plugin>
    </plugins>
</build> 
```

This plugin will NOT maintain the runtime properties up-to-date.

For that, please use the official [spring cloud config client](https://quarkus.io/guides/spring-cloud-config-client).

## How to skip the plugin

If you want to skip this plugin from the command line, you can use the property `SKIP_QUARKUS_SCCMP=true`:
```
mvn clean verify -DSKIP_QUARKUS_SCCMP=true
```