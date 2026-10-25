package ucu.edu.aed.tda.grafo.Impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UnionFind<V> {
    private final Map<V, V> padre = new HashMap<>();
    private final Map<V, Integer> rango = new HashMap<>();

    public UnionFind(Collection<V> elementos) {
        for (V v : elementos) {
            padre.put(v, v);   
            rango.put(v, 0);
        }
    }

    public V find(V x) {
        V p = padre.get(x);
        if (!p.equals(x)) {
            p = find(p);
            padre.put(x, p); 
        }
        return p;
    }

    public boolean union(V a, V b) {
    V ra = find(a);
    V rb = find(b);

    if (ra.equals(rb)) {
        return false;
    }

    if (rango.get(ra) < rango.get(rb)) {
        padre.put(ra, rb);
    } else if (rango.get(ra) > rango.get(rb)) {
        padre.put(rb, ra);
    } else {
        padre.put(rb, ra);
        rango.put(ra, rango.get(ra) + 1);
    }

    return true;
}
}