# Quarkus Spring Cloud Config Maven Plugin

![Maven Central](https://img.shields.io/maven-central/v/io.github.willianwd/quarkus-spring-cloud-config-maven-plugin?logo=apache-maven)
![Release CI](https://github.com/wllianwd/quarkus-spring-cloud-config-maven-plugin/actions/workflows/ci-release.yml/badge.svg)](https://github.com/wllianwd/quarkus-spring-cloud-config-maven-plugin/actions/workflows/ci-release.yml)

In a nutshell this maven plugin will download the properties from Spring Cloud Config and generate an `application.yml` file inside `src/main/resources`, so this file can be used during build phase.
It is recommended to use together with [spring cloud config client](https://quarkus.io/guides/spring-cloud-config-client).

## How to use it

### Quickstart

Add the plugin in your `pom.xml`:

```
<profiles>
    <profile>
        <id>quarkus-sccmp<id>
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
    <profile>
</profiles>

<!-- Spring Cloud Config quarkus extension: https://quarkus.io/guides/spring-cloud-config-client -->
<dependencies>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-spring-cloud-config-client</artifactId>
    </dependency>
</dependencies>

```
Create a `bootstrap.yml` in your `src/main/resources`, like below:
```
quarkus:
  application:
    name: myservice
  spring-cloud-config:
    # this property is not used by this plugin, but in the `spring-cloud-config-client` extension
    enabled: true
    url: "https://springcloudconfigserver.mycompany.com/springconfig"
```
> The properties above will be copied to the target `application.yml`, so you don't need to duplicate it.

Now you should be able to use it like bellow:

```
mvn compile quarkus:dev -Pquarkus-sccmp
```

OR: When building the native image you can do the same:
```
mvn package -Dnative,quarkus-sccmp -Dquarkus.native.container-build=true
```
That is it!

### How to configure it

Those are all the properties from the `src/main/resources/bootstrap.yml`:
```
quarkus:
  application:
    name: myservice
  spring-cloud-config:
    # this property is not used by this plugin, but in the `spring-cloud-config-client` extension
    enabled: true
    # this property is mandatory
    url: "https://springcloudconfigserver.mycompany.com/springconfig"
    # this property is optional and it represents the branch from where the properties will be retrieved, default is master
    label: master
    # this property is optional and overrides quarkus.application.name
    name: myservice
```

Those are all the properties from the plugin that can be defined in the `pom.xml`:
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
            <configuration>
                <!-- skip this plugin execution -->
                <skip>false</skip>
                <!-- the bootstrap.yml directory -->
                <bootstrapDirectory>src/main/resources/</bootstrapDirectory>
                <!-- the bootstrap.yml file -->
                <bootstrapFile>bootstrap.yml</bootstrapFile>
                <!-- the target directory -->
                <targetDirectory>target/classes/</targetDirectory>
                <!-- the target file -->
                <targetFile>application.yml</targetFile>
                <!-- the profile that will be used to call Spring Cloud Config -->
                <profile>all</profile>
                <!-- will apply pretty yaml options -->
                <prettyDumperOptions>true</prettyDumperOptions>
            </configuration>
        </plugin>
    </plugins>
</build> 
```

> It also supports properties if the `targetFile` is defined as `*.properties`

This plugin will NOT maintain the runtime properties up-to-date.

For that, please use the official [spring cloud config client](https://quarkus.io/guides/spring-cloud-config-client).

## How to skip the plugin

You can skip using the `skip` argument in the plugin configuration:
```
<build>
    <plugins>
        <plugin>
            <groupId>io.github.willianwd</groupId>
            <artifactId>quarkus-spring-cloud-config-maven-plugin</artifactId>
            <version>${version}</version>
            <configuration>
                <skip>true</skip>
            </configuration>
        </plugin>
    </plugins>
</build> 
```
Or you can use the property `SKIP_QUARKUS_SCCMP=true`:
```
mvn clean verify -DSKIP_QUARKUS_SCCMP=true
```
