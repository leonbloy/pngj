package ar.com.hjg.pngj.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MappedCounterInt {

    private Map<String, Integer> map = new HashMap<String, Integer>();

    public void add(String k, int v) {
	if (!map.containsKey(k))
	    map.put(k, v);
	else
	    map.put(k, map.get(k) + v);
    }

    public int get(String k) {
	return map.containsKey(k) ? map.get(k) : 0;
    }

    public Set<String> getKeys() {
	return map.keySet();
    }
}
