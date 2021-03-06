package alien4cloud.paas.cloudify3;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Resource;

import junitx.framework.FileAssert;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import alien4cloud.model.topology.Topology;
import alien4cloud.paas.cloudify3.service.BlueprintService;
import alien4cloud.paas.cloudify3.service.CloudifyDeploymentBuilderService;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.cloudify3.util.ApplicationUtil;
import alien4cloud.paas.cloudify3.util.DeploymentUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
@Slf4j
public class TestBlueprintService extends AbstractDeploymentTest {

    @Resource
    private BlueprintService blueprintService;

    @Resource
    private CloudifyDeploymentBuilderService cloudifyDeploymentBuilderService;

    @Resource
    private ApplicationUtil applicationUtil;

    @Resource
    private DeploymentUtil deploymentUtil;

    @Test
    public void testGenerateSingleCompute() {
        Topology topology = applicationUtil.createAlienApplication("testGenerateSingleCompute", SINGLE_COMPUTE_TOPOLOGY);
        CloudifyDeployment alienDeployment = deploymentUtil.buildAlienDeployment("testGenerateSingleCompute", "testGenerateSingleCompute", topology,
                generateDeploymentSetup(topology));
        Path generated = blueprintService.generateBlueprint(alienDeployment);
        FileAssert.assertEquals(new File("src/test/resources/outputs/blueprints/single_compute.yaml"), generated.toFile());
    }

    @Test
    public void testGenerateSingleComputeWithApache() {
        Path generated = blueprintService.generateBlueprint(cloudifyDeploymentBuilderService.buildCloudifyDeployment(buildPaaSDeploymentContext(
                "testGenerateSingleComputeWithApache", SINGLE_COMPUTE_TOPOLOGY_WITH_APACHE)));
        FileAssert.assertEquals(new File("src/test/resources/outputs/blueprints/single_compute_with_apache.yaml"), generated.toFile());
        Assert.isTrue(Files.exists(generated.getParent().resolve("apache-type/alien.nodes.Apache/scripts/start_apache.sh")));
        Assert.isTrue(Files.exists(generated.getParent().resolve("apache-type/alien.nodes.Apache/scripts/install_apache.sh")));
    }

    @Test
    public void testGenerateLamp() {
        blueprintService.generateBlueprint(cloudifyDeploymentBuilderService.buildCloudifyDeployment(buildPaaSDeploymentContext("testGenerateLamp",
                LAMP_TOPOLOGY)));
    }

    @Test
    public void testGenerateFloatingIP() {
        blueprintService.generateBlueprint(cloudifyDeploymentBuilderService.buildCloudifyDeployment(buildPaaSDeploymentContext("testGenerateFloatingIP",
                NETWORK_TOPOLOGY)));
    }
}
