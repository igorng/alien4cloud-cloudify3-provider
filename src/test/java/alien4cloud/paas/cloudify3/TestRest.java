package alien4cloud.paas.cloudify3;

import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.dao.BlueprintDAO;
import alien4cloud.paas.cloudify3.dao.DeploymentDAO;
import alien4cloud.paas.cloudify3.dao.EventDAO;
import alien4cloud.paas.cloudify3.dao.ExecutionDAO;
import alien4cloud.paas.cloudify3.dao.NodeDAO;
import alien4cloud.paas.cloudify3.dao.NodeInstanceDAO;
import alien4cloud.paas.cloudify3.model.Blueprint;
import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.ExecutionStatus;
import alien4cloud.paas.cloudify3.model.Node;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.model.Workflow;

import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
@Slf4j
public class TestRest {

    public static final String BLUEPRINTS_PATH = "./src/test/resources/blueprints/";

    public static final String BLUEPRINT_ID = "nodecellar";

    public static final String DEPLOYMENT_ID = "deploymentOfNodeCellar";

    public static final String BLUEPRINT_FILE = "singlehost-blueprint.yaml";

    public static final Map<String, Object> DEPLOYMENT_INPUTS = Maps.newHashMap();

    static {
        DEPLOYMENT_INPUTS.put("host_ip", "localhost");
        DEPLOYMENT_INPUTS.put("agent_user", "vagrant");
        DEPLOYMENT_INPUTS.put("agent_private_key_path", "/home/vagrant/.ssh/id_rsa");
    }

    @Resource
    private BlueprintDAO blueprintDAO;

    @Resource
    private DeploymentDAO deploymentDAO;

    @Resource
    private ExecutionDAO executionDAO;

    @Resource
    private EventDAO eventDAO;

    @Resource
    private NodeDAO nodeDAO;

    @Resource
    private NodeInstanceDAO nodeInstanceDAO;

    @Resource
    private CloudConfigurationHolder cloudConfigurationHolder;

    @Before
    public void before() throws InterruptedException {
        cloudConfigurationHolder.getConfiguration().setUrl("http://129.185.67.112:8100");
        if (deploymentDAO.list().length > 0) {
            deploymentDAO.delete(DEPLOYMENT_ID);
        }
        Thread.sleep(1000L);
        if (blueprintDAO.list().length > 0) {
            blueprintDAO.delete(BLUEPRINT_ID);
        }
        Thread.sleep(1000L);
    }

    @Test
    public void testBluePrint() throws InterruptedException {
        Blueprint[] blueprints = blueprintDAO.list();
        Assert.assertEquals(0, blueprints.length);
        blueprintDAO.create(BLUEPRINT_ID, BLUEPRINTS_PATH + BLUEPRINT_ID + "/" + BLUEPRINT_FILE);
        Thread.sleep(1000L);
        blueprints = blueprintDAO.list();
        Assert.assertEquals(1, blueprints.length);
        for (Blueprint blueprint : blueprints) {
            Blueprint readBlueprint = blueprintDAO.read(blueprint.getId());
            Assert.assertEquals(blueprint, readBlueprint);
        }
        blueprintDAO.delete(BLUEPRINT_ID);
        Thread.sleep(1000L);
        blueprints = blueprintDAO.list();
        Assert.assertEquals(0, blueprints.length);
    }

    @Test
    public void testDeployment() throws InterruptedException {
        blueprintDAO.create(BLUEPRINT_ID, BLUEPRINTS_PATH + BLUEPRINT_ID + "/" + BLUEPRINT_FILE);
        Deployment[] deployments = deploymentDAO.list();
        Assert.assertEquals(0, deployments.length);
        deploymentDAO.create(DEPLOYMENT_ID, BLUEPRINT_ID, Maps.<String, Object> newHashMap());
        Thread.sleep(1000L);
        deployments = deploymentDAO.list();
        Assert.assertEquals(1, deployments.length);
        for (Deployment deployment : deployments) {
            Deployment readDeployment = deploymentDAO.read(deployment.getId());
            Assert.assertEquals(deployment, readDeployment);
        }
        Thread.sleep(20000L);
        deploymentDAO.delete(DEPLOYMENT_ID);
        Thread.sleep(1000L);
        deployments = deploymentDAO.list();
        Assert.assertEquals(0, deployments.length);
        blueprintDAO.delete(BLUEPRINT_ID);
    }

    @Test
    public void testExecution() throws InterruptedException {
        blueprintDAO.create(BLUEPRINT_ID, "/home/vuminhkh/Projects/alien4cloud-cloudify3-provider/target/alien/cloudify3/testDeployApache/blueprint.yaml");
        deploymentDAO.create(DEPLOYMENT_ID, BLUEPRINT_ID, Maps.<String, Object> newHashMap());
        // The creation of a deployment automatically trigger a initialization execution
        waitForExecutionFinished(DEPLOYMENT_ID);
        Execution startExecution = executionDAO.start(DEPLOYMENT_ID, Workflow.INSTALL, null, false, false);
        Thread.sleep(1000L);
        waitForExecutionFinished(DEPLOYMENT_ID);
        Event[] events = eventDAO.getBatch(startExecution.getId(), null, 0, Integer.MAX_VALUE);
        Assert.assertEquals(39, events.length);
        Node[] nodes = nodeDAO.list(DEPLOYMENT_ID, null);
        Assert.assertEquals(4, nodes.length);
        for (Node node : nodes) {
            Assert.assertEquals(node, nodeDAO.list(DEPLOYMENT_ID, node.getId())[0]);
        }
        NodeInstance[] nodeInstances = nodeInstanceDAO.list(DEPLOYMENT_ID);
        for (NodeInstance nodeInstance : nodeInstances) {
            Assert.assertEquals(nodeInstance, nodeInstanceDAO.read(nodeInstance.getId()));
        }
        executionDAO.start(DEPLOYMENT_ID, Workflow.UNINSTALL, null, false, false);
        Thread.sleep(1000L);
        waitForExecutionFinished(DEPLOYMENT_ID);
        deploymentDAO.delete(DEPLOYMENT_ID);
        Thread.sleep(1000L);
        blueprintDAO.delete(BLUEPRINT_ID);
    }

    private void waitForExecutionFinished(String deploymentId) throws InterruptedException {
        while (true) {
            boolean executionFinished = true;
            Execution[] executions = executionDAO.list(deploymentId);
            for (Execution execution : executions) {
                executionFinished = executionFinished && ExecutionStatus.isTerminated(execution.getStatus());
                if (!ExecutionStatus.isTerminated(execution.getStatus())) {
                    log.info("Running Execution {} for workflow {} is in status {}", execution.getId(), execution.getWorkflowId(), execution.getStatus());
                }
            }
            if (!executionFinished) {
                Thread.sleep(2000L);
            } else {
                log.info("All execution has finished");
                break;
            }
        }
    }
}
