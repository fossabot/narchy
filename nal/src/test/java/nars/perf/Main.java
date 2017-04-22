package nars.perf;

import org.openjdk.jmh.profile.HotspotMemoryProfiler;
import org.openjdk.jmh.profile.HotspotRuntimeProfiler;
import org.openjdk.jmh.profile.HotspotThreadProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

/**
 * Created by me on 12/11/15.
 */
public enum Main {
	;

	public static void perf(Class c, int iterations, int batchSize) throws RunnerException {
		perf(c.getSimpleName(), iterations, batchSize);
	}

	public static void perf(String include, int iterations, int batchSize) throws RunnerException {
		Options opt = new OptionsBuilder()
				// .include(".*" + YourClass.class.getSimpleName() + ".*")

				.include(include)

				 //.include(//c.getName())
				//.include(".*" + c.getSimpleName() + ".*")
				.warmupIterations(0)
				.measurementIterations(iterations)
				.measurementBatchSize(batchSize)
				.threads(1)
				.forks(1)
				.resultFormat(ResultFormatType.TEXT)
				.verbosity(VerboseMode.NORMAL) //VERBOSE OUTPUT

				//.addProfiler(StackProfiler.class)
				.addProfiler(StackProfiler.class,
						"lines=10;top=20;period=2;detailLine=true")
				 //.addProfiler(GCProfiler.class)

				//.addProfiler(LinuxPerfProfiler.class)
				//.addProfiler(LinuxPerfAsmProfiler.class)
				//.addProfiler(LinuxPerfNormProfiler.class)

				.addProfiler(HotspotRuntimeProfiler.class)
				.addProfiler(HotspotMemoryProfiler.class)
				.addProfiler(HotspotThreadProfiler.class)

				//.addProfiler(HotspotCompilationProfiler.class)
				// .addProfiler(HotspotClassloadingProfiler.class)
				/*.addProfiler(LinuxPerfProfiler.class)
				  .addProfiler(LinuxPerfAsmProfiler.class)
				  .addProfiler(LinuxPerfNormProfiler.class)*/

				//.addProfiler(CompilerProfiler.class)


				.timeout(TimeValue.seconds(100))

				.build();

		new Runner(opt).run();


	}

}