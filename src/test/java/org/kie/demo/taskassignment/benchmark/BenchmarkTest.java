package org.kie.demo.taskassignment.benchmark;

import org.junit.Test;
import org.optaplanner.benchmark.api.PlannerBenchmark;
import org.optaplanner.benchmark.api.PlannerBenchmarkFactory;

public class BenchmarkTest {

    private static final String BENCHMARK_CONFIG_FILE = "org/kie/demo/taskassignment/benchmark/TaskAssigningBenchmarkConfig.xml";

    @Test
    public void runBenchmark() {
        PlannerBenchmarkFactory plannerBenchmarkFactory = PlannerBenchmarkFactory.createFromXmlResource(BENCHMARK_CONFIG_FILE);
        PlannerBenchmark plannerBenchmark = plannerBenchmarkFactory.buildPlannerBenchmark();
        plannerBenchmark.benchmark();
    }
}
