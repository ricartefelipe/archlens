package io.archlens.analyzer.core.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CycleDetector {

    private CycleDetector() {
    }

    public static List<List<String>> findPackageCycles(Map<String, Set<String>> adjacency) {
        Set<String> nodes = new HashSet<>(adjacency.keySet());
        for (Set<String> edges : adjacency.values()) {
            nodes.addAll(edges);
        }

        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();
        List<String> path = new ArrayList<>();
        Set<String> reported = new HashSet<>();
        List<List<String>> cycles = new ArrayList<>();

        for (String node : sorted(nodes)) {
            if (!visited.contains(node)) {
                dfs(node, adjacency, visited, stack, path, reported, cycles);
            }
        }

        cycles.sort(CycleDetector::compareCycles);
        return cycles;
    }

    private static void dfs(
            String node,
            Map<String, Set<String>> adjacency,
            Set<String> visited,
            Set<String> stack,
            List<String> path,
            Set<String> reported,
            List<List<String>> cycles
    ) {
        visited.add(node);
        stack.add(node);
        path.add(node);

        for (String neighbor : sorted(adjacency.getOrDefault(node, Set.of()))) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, adjacency, visited, stack, path, reported, cycles);
            } else if (stack.contains(neighbor)) {
                int start = path.indexOf(neighbor);
                if (start >= 0) {
                    List<String> cycle = new ArrayList<>(path.subList(start, path.size()));
                    String key = canonicalCycleKey(cycle);
                    if (!reported.contains(key)) {
                        reported.add(key);
                        cycles.add(cycle);
                    }
                }
            }
        }

        stack.remove(node);
        path.remove(path.size() - 1);
    }

    static String canonicalCycleKey(List<String> cycle) {
        if (cycle.isEmpty()) {
            return "";
        }
        List<String> doubled = new ArrayList<>(cycle);
        doubled.addAll(cycle);
        String best = null;
        for (int i = 0; i < cycle.size(); i++) {
            List<String> rotation = doubled.subList(i, i + cycle.size());
            String candidate = String.join("->", rotation);
            if (best == null || candidate.compareTo(best) < 0) {
                best = candidate;
            }
        }
        return best;
    }

    private static int compareCycles(List<String> left, List<String> right) {
        return canonicalCycleKey(left).compareTo(canonicalCycleKey(right));
    }

    private static List<String> sorted(Set<String> values) {
        List<String> list = new ArrayList<>(values);
        Collections.sort(list);
        return list;
    }

    public static Map<String, Set<String>> buildAdjacency(Map<String, Set<String>> packageImports) {
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : packageImports.entrySet()) {
            adjacency.computeIfAbsent(entry.getKey(), key -> new LinkedHashSet<>());
            for (String target : entry.getValue()) {
                if (!entry.getKey().equals(target)) {
                    adjacency.computeIfAbsent(entry.getKey(), key -> new LinkedHashSet<>()).add(target);
                }
            }
        }
        return adjacency;
    }
}
