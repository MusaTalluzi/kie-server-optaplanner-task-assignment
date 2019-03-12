package org.kie.server.examples.optaplanner.taskassignment.client;

import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.instance.SolverInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.SolverServicesClient;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.examples.optaplanner.taskassignment.kjar.domain.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class TaskAssignmentClient {

    public static String CONTAINER_ID = "org.kie.server.examples.optaplanner:task-assignment-kjar:1.0-SNAPSHOT";

    public static String SERVER_URL = "serverURL";
    public static String USERNAME = "username";
    public static String PASSWORD = "password";

    public static String SOLVER_ID = "solver1";

    SolverServicesClient solverClient;
    public KieServicesConfiguration configuration;

    public TaskAssignmentClient() {
        String url = System.getProperty(SERVER_URL, "http://localhost:8080/kie-server/services/rest/server");
        String username = System.getProperty(USERNAME, "planner");
        String password = System.getProperty(PASSWORD, "Planner123_");
        configuration = KieServicesFactory.newRestConfiguration(url, username, password);
        configuration.setMarshallingFormat(MarshallingFormat.XSTREAM);
    }


    protected void setupClients(KieServicesClient kieServicesClient) {
        this.solverClient = kieServicesClient.getServicesClient(SolverServicesClient.class);
    }

    protected KieServicesClient createDefaultClient() throws Exception {

        configuration.setTimeout(30000);

        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(configuration);

        setupClients(kieServicesClient);
        return kieServicesClient;
    }

    public SolverServicesClient getClient() {
        return solverClient;
    }

    private TaskAssigningSolution buildProblem() {
        TaskAssigningSolution solution = new TaskAssigningSolution();
        solution.setId(0L);
        solution.setSkillList(new ArrayList<>());
        solution.setCustomerList(new ArrayList<>());
        solution.setEmployeeList(new ArrayList<>());
        solution.setTaskTypeList(new ArrayList<>());
        solution.setTaskList(new ArrayList<>());
        solution.setFrozenCutoff(0);

        Skill skill = new Skill();
        skill.setId(0L);
        solution.getSkillList().add(skill);

        TaskType taskType = new TaskType();
        taskType.setId(0L);
        taskType.setCode("type0");
        taskType.setRequiredSkillList(new ArrayList<>());
        solution.getTaskTypeList().add(taskType);

        Customer customer = new Customer();
        customer.setId(0L);
        customer.setName("customer0");
        solution.getCustomerList().add(customer);

        Employee employee = new Employee();
        employee.setId(0L);
        employee.setFullName("employee0");
        employee.setSkillSet(new HashSet<>());
        employee.setAffinityMap(new HashMap<>());
        solution.getEmployeeList().add(employee);

        Task task = new Task();
        task.setId(0L);
        task.setTaskType(taskType);
        task.setIndexInTaskType(0);
        task.setCustomer(customer);
        solution.getTaskList().add(task);

        return solution;
    }

    public static void main(String[] args) throws Exception {


        TaskAssignmentClient applicationClient = new TaskAssignmentClient();

        KieServicesClient client = applicationClient.createDefaultClient();

        ReleaseId kjar1 = new ReleaseId(
                "org.kie.server.examples.optaplanner", "task-assignment-kjar",
                "1.0-SNAPSHOT");

        KieContainerResource containerResource = new KieContainerResource(CONTAINER_ID, kjar1);

        client.deactivateContainer(CONTAINER_ID);
        client.disposeContainer(CONTAINER_ID);

        ServiceResponse<KieContainerResource> reply = client.createContainer(CONTAINER_ID, containerResource);


        SolverServicesClient solverClient = applicationClient.getClient();

        solverClient.createSolver(CONTAINER_ID,
                SOLVER_ID,
                "org/kie/server/examples/optaplanner/taskassignment/kjar/solver/taskAssigningSolverConfig.xml");

        solverClient.solvePlanningProblem(CONTAINER_ID, SOLVER_ID, applicationClient.buildProblem());
        boolean keepSolving = true;
        while (keepSolving) {
            TimeUnit.SECONDS.sleep(5);
            SolverInstance solver = solverClient.getSolver(CONTAINER_ID, SOLVER_ID);
            if (solver.getStatus() == SolverInstance.SolverStatus.SOLVING) {
                // continue
                System.out.println("Still solving");
            } else {
                TaskAssigningSolution taSolution = (TaskAssigningSolution) solverClient.getSolverWithBestSolution(CONTAINER_ID, SOLVER_ID).getBestSolution();
                // process the solution
                System.out.println("solution score:" + taSolution.getScore());
            }

        }

        solverClient.terminateSolverEarly(CONTAINER_ID, SOLVER_ID);
        solverClient.disposeSolver(CONTAINER_ID, SOLVER_ID);
    }
}