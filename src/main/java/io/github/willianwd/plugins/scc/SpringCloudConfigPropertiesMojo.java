package io.github.willianwd.plugins.scc;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "fetch-properties", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class SpringCloudConfigPropertiesMojo extends AbstractSpringCloudConfigPropertiesMojo
{

    @Override
    protected String getBootstrapPath()
    {
        return bootstrapDirectory + bootstrapFile;
    }


    @Override
    protected String getTargetDirectory()
    {
        return targetDirectory;
    }


    @Override
    protected String getTargetFile()
    {
        return targetFile;
    }

}
