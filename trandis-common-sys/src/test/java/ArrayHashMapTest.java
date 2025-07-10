import com.xm.sanvanfo.common.hashmap.ArrayKeyHashMap;
import com.xm.sanvanfo.common.hashmap.ArrayKeyHashSet;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class ArrayHashMapTest {

    @Test
    public void ArrayHashMapTest() throws Exception {

        ArrayKeyHashMap<byte[], String> map = new ArrayKeyHashMap<>();
        map.put(new byte[]{1, 2, 3}, "aaaaaa");
        map.put(new byte[] {1, 2, 3}, "bbbbbbb");
        map.put(new byte[] {3, 4, 5}, "eee");
        System.out.println(map);
        String value = map.get(new byte[] {3, 4, 5});
        System.out.println(value);
        value = map.get(new byte[] {99,98});
        System.out.println(null == value ? "null" : value);
        boolean ret = map.containsKey(new byte[] {1, 2, 3});
        System.out.println(ret);
        map.putIfAbsent(new byte[] {1, 2, 3}, "cccccccc");
        map.putIfAbsent(new byte[] {8, 9}, "hahahah");
        value = map.getOrDefault(new byte[]{0}, "bala");
        System.out.println(value);
        value = map.computeIfAbsent(new byte[] {1, 2, 3}, o-> {
           return map.get(o) + "1111111";
        });
        System.out.println(value);
        value = map.compute(new byte[]{1, 2, 3}, (o, v)-> {
            return v + "1111111";
        });

        System.out.println(map.get(new byte[] {1, 2, 3}));
        System.out.println(value);
        map.remove(new byte[] {1, 2, 3});
        System.out.println(map);
        map.replace(new byte[] {3,4,5}, "eeee2h");
        map.replace(new byte[] {3,4,5}, "eeee2h", "eeee3h");
        System.out.println(map);
        Map<byte[], String> newMNap = new ArrayKeyHashMap<>();
        newMNap.put(new byte[] {5,6,7}, "dsgsdagsdag");
        newMNap.put(new byte[] {22}, "gregwe");
        map.putAll(newMNap);
        System.out.println(map);
    }

    @Test
    public void ArrayHashMapTestMulti() throws Exception {
        ArrayKeyHashMap<byte[][], String> map = new ArrayKeyHashMap<>();
        map.put(new byte[][]{{1,2,3}, {1,2,3}}, "aaaaaa");
        map.put(new byte[][]{{1,2}, {1,1}}, "bbbbb");
        map.put(new byte[][]{{1,2,3}, {1,2,3}}, "bbbbbccccc");
        System.out.println(map);
    }

    @Test
    public void ArrayHashMapIntTestMulti() throws Exception {
        ArrayKeyHashMap<Integer[][], String> map = new ArrayKeyHashMap<>();
        map.put(new Integer[][]{{1,2,3}, {1,2,3}}, "aaaaaa");
        map.put(new Integer[][]{{1,2}, {1,1}}, "bbbbb");
        map.put(new Integer[][]{{1,2,3}, {1,2,3}}, "bbbbbccccc");
        System.out.println(map);
    }

    @Test
    public void ArrayHashSetTest() throws Exception {
        ArrayKeyHashSet<String[]> set = new ArrayKeyHashSet<>();
        set.add(new String[] {"1", "2", "3"});
        set.add(new String[] {"1", "3"});
        set.add(new String[] {"1", "2"});
        set.add(new String[] {"1", "2", "3"});
        System.out.println(set);
    }
}
