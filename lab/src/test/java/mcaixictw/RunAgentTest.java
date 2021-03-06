package mcaixictw;

import mcaixictw.worldmodels.WorldModelSettings;
import mcaixictw.worldmodels.Worldmodel;
import org.junit.jupiter.api.*;

import java.util.logging.Logger;

abstract public class RunAgentTest {

	private static Logger log = Logger.getLogger(RunAgentTest.class
			.getName());

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	public void setUp() throws Exception {

		// set up the biased coin environment. The coin lands on one side with a
		// probability of 0.7.
		env = environment();

		WorldModelSettings modelSettings = new WorldModelSettings();
		modelSettings.setFacContextTree(true);
		modelSettings.setDepth(3);
		log.info("depth: " + modelSettings.getDepth());
		log.info("create new model");
		Worldmodel model = Worldmodel.getInstance(name(), modelSettings);
		controller = new AgentController(env, controllerSettings, uctSettings,
				model);

	}

	protected abstract String name();

	abstract public Environment environment();

	private AgentController controller;

	private Environment env;
	private ControllerSettings controllerSettings = new ControllerSettings();
	private UCTSettings uctSettings = new UCTSettings();

	@AfterEach
	public void tearDown() throws Exception {

	}

	@Test
	public final void test() {

		int n = 10000;
		log.info("Play " + n + " rounds against the biased coin environment");

		// A smart agent should learn to always choose the biased side and
		// should come close to an average reward of 0.7
		controller.play(n, false);
		
	}

}
