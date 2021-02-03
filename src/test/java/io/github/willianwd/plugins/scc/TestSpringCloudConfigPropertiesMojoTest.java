package io.github.willianwd.plugins.scc;

import java.io.File;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class TestSpringCloudConfigPropertiesMojoTest extends AbstractMojoTestCase
{

    protected void setUp() throws Exception
    {
        super.setUp();
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
    }


    public void testGivenPom_whenGenerateTestResourcesIsExecuted_thenSpringCloudConfigPropertiesMojoShouldExecute() throws Exception
    {
        File pom = getTestFile("src/test/resources/test-spring-cloud-config-properties-mojo/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        TestSpringCloudConfigPropertiesMojo myMojo = (TestSpringCloudConfigPropertiesMojo) lookupMojo("fetch-test-properties", pom);
        assertNotNull(myMojo);
        myMojo.execute();

    }

}
