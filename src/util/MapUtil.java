package util;

import java.util.Map;

/**
 * User: david
 * Date: 7/11/13
 * Time: 10:40 PM
 * Description:
 *      Utils for numeric maps
 */
public class MapUtil {

    /**
     * Increment the val of a key, or add a new (k,v) pair if one isn't present
     * @param map
     * @param key
     * @param incBy
     * @param <K>
     */

    public static <K> void incDouble(Map<K,Double> map, K key, double incBy) {
        double toPut = incBy;
        if (map.containsKey(key))
           toPut += map.get(key);
        map.put(key, toPut);
    }

    public static <K> void incInt(Map<K,Integer> map, K key, int incBy) {
        int toPut = incBy;
        if (map.containsKey(key))
            toPut += map.get(key);
        map.put(key, toPut);
    }

}
