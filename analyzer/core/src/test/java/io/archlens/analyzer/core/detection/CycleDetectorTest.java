package io.archlens.analyzer.core.detection;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CycleDetectorTest {

    @Test
    void findsBillingOrdersCycle() {
        Map<String, Set<String>> adjacency = Map.of(
                "com.demo.billing", Set.of("com.demo.orders"),
                "com.demo.orders", Set.of("com.demo.billing")
        );

        List<List<String>> cycles = CycleDetector.findPackageCycles(adjacency);
        assertFalse(cycles.isEmpty());
        assertTrue(cycles.getFirst().contains("com.demo.billing"));
        assertTrue(cycles.getFirst().contains("com.demo.orders"));
    }
}
