package ucu.edu.aed.tda.grafo.model.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DijkstraResult<V> implements IDijkstraResult<V> {
    private final Map<V, Double> costs;
    private final Map<V, V> predecessors;

    public DijkstraResult(Map<V, Double> costs, Map<V, V> predecessors) {
        this.costs = costs;
        this.predecessors = predecessors;
    }

    @Override
    public double getCost(V otherVertex) {
        return costs.getOrDefault(otherVertex, Double.POSITIVE_INFINITY);
    }

    @Override
    public List<V> getPath(V otherVertex) {
        if (!costs.containsKey(otherVertex) || costs.get(otherVertex) == Double.POSITIVE_INFINITY) {
            return Collections.emptyList();
        }
        List<V> path = new ArrayList<>();
        V current = otherVertex;
        while (current != null) {
            path.add(current);
            current = predecessors.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}