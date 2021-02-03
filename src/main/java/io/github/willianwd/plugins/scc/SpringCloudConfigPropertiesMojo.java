package io.github.willianwd.plugins.scc;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "fetch-properties", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class SpringCloudConfigPropertiesMojo extends AbstractSpringCloudConfigPropertiesMojo {

    @Parameter(property = "bootstrapDirectory", defaultValue = "src/main/resources/")
    private String bootstrapDirectory;

    @Override
    protected String getBootstrapPath() {
        return bootstrapDirectory + bootstrapFile;
    }
}
