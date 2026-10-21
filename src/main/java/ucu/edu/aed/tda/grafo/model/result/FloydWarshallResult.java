package ucu.edu.aed.tda.grafo.model.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FloydWarshallResult<V> implements IFloydWarshallResult<V> {
    private final List<V> vertices;
    private final double[][] costMatrix;
    private final int[][] predecessorMatrix; // Matriz de índices

    public FloydWarshallResult(List<V> vertices, double[][] costMatrix, int[][] predecessorMatrix) {
        this.vertices = vertices;
        this.costMatrix = costMatrix;
        this.predecessorMatrix = predecessorMatrix;
    }

    private int getIndex(V vertex) {
        return vertices.indexOf(vertex);
    }

    @Override
    public double getCost(V source, V target) {
        int i = getIndex(source);
        int j = getIndex(target);
        if (i == -1 || j == -1) return Double.POSITIVE_INFINITY;
        return costMatrix[i][j];
    }

    @Override
    public boolean connected(V source, V target) {
        return getCost(source, target) < Double.POSITIVE_INFINITY;
    }

    @Override
    public List<V> getPath(V source, V target) {
        int i = getIndex(source);
        int j = getIndex(target);
        if (i == -1 || j == -1 || !connected(source, target)) return Collections.emptyList();

        List<V> path = new ArrayList<>();
        path.add(source);
        if (i == j) return path;

        int curr = i;
        while (curr != j) {
            curr = predecessorMatrix[curr][j];
            if (curr == -1) return Collections.emptyList(); // Seguridad
            path.add(vertices.get(curr));
        }
        return path;
    }
}