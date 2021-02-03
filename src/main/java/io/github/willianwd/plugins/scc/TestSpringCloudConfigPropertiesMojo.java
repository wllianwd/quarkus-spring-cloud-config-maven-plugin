package io.github.willianwd.plugins.scc;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "fetch-test-properties", defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES)
public class TestSpringCloudConfigPropertiesMojo extends AbstractSpringCloudConfigPropertiesMojo {

    @Parameter(property = "bootstrapTestDirectory", defaultValue = "src/test/resources/")
    private String bootstrapDirectory;

    @Override
    protected String getBootstrapPath() {
        return bootstrapDirectory + bootstrapFile;
    }
}
