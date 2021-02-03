package io.github.willianwd.plugins.scc;

import java.io.File;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class SpringCloudConfigPropertiesMojoTest extends AbstractMojoTestCase
{

    protected void setUp() throws Exception
    {
        super.setUp();
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
    }


    public void testGivenPom_whenGenerateResourcesIsExecuted_thenSpringCloudConfigPropertiesMojoShouldExecute() throws Exception
    {
        File pom = getTestFile("src/test/resources/spring-cloud-config-properties-mojo/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        SpringCloudConfigPropertiesMojo myMojo = (SpringCloudConfigPropertiesMojo) lookupMojo("fetch-properties", pom);
        assertNotNull(myMojo);
        myMojo.execute();

    }

}
