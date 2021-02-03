package io.github.willianwd.plugins.scc;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "fetch-test-properties", defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES)
public class TestSpringCloudConfigPropertiesMojo extends AbstractSpringCloudConfigPropertiesMojo
{

    @Override
    protected String getBootstrapPath()
    {
        return bootstrapTestDirectory + bootstrapFile;
    }


    @Override
    public String getTargetDirectory()
    {
        return targetTestDirectory;
    }


    @Override
    public String getTargetFile()
    {
        return targetTestFile;
    }

}
