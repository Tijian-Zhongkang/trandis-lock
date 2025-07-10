import com.xm.sanvanfo.common.utils.PropertiesFileUtils;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class PropertiesTest {

    @Test
    public void listPropertiesTest() throws Exception {

        File file = new File("src/test/properties/testlist.properties");
        Properties properties = new Properties();
        try(InputStream stream = new FileInputStream(file)) {
            properties.load(stream);
            Set<String> set = properties.stringPropertyNames();
            System.out.println(String.join(",\n", set));
            TestList testList = PropertiesFileUtils.toConfigurationClass(file, "", TestList.class);
            System.out.println(testList);
            TestArray testArray = PropertiesFileUtils.toConfigurationClass(file, "", TestArray.class);
            System.out.println(testArray);
            Properties convert = PropertiesFileUtils.toProperties(testList, "");
            Set<String> convertSet = convert.stringPropertyNames();
            System.out.println(String.join(",\n", convertSet));
            testArray = PropertiesFileUtils.toConfigurationClass(convert, "", TestArray.class);
            System.out.println(testArray);
        }
    }



    @Test
    public void simplePropertiesTest() throws Exception {

        File file = new File("src/test/properties/test.properties");
        Properties properties = new Properties();
        try(InputStream stream = new FileInputStream(file)) {
            properties.load(stream);
            Set<String> set = properties.stringPropertyNames();
            System.out.println(String.join(",\n", set));
            TestA t = PropertiesFileUtils.toConfigurationClass(file, "test", TestA.class);
            System.out.println(t);
            Properties convert = PropertiesFileUtils.toProperties(t, "test");
            Set<String> convertSet = convert.stringPropertyNames();
            System.out.println(String.join(",\n", convertSet));
        }
    }

    @Test
    public void mapPropertiesTest() throws Exception {
        File file = new File("src/test/properties/testmap.properties");
        Properties properties = new Properties();
        try(InputStream stream = new FileInputStream(file)) {
            properties.load(stream);
            Set<String> set = properties.stringPropertyNames();
            System.out.println(String.join(",\n", set));
            TestMap t = PropertiesFileUtils.toConfigurationClass(file, "", TestMap.class);
            System.out.println(t);
            Properties convert = PropertiesFileUtils.toProperties(t, "");
            Set<String> convertSet = convert.stringPropertyNames();
            System.out.println(String.join(",\n", convertSet));
        }
    }

    @Data
    public static class TestA {
        private Integer abc;
        private Integer bbc;
        private String hello;
        private Bbb bbb = new Bbb();
        private List<Integer> yy;

        @Data
        private static class Bbb {
            private boolean cc;
        }
    }

    @Data
    public static class TestList {
        private Integer a;
        private Integer b;
        private C c;
        private D d;

        @Data
        private static class C {
            private List<Integer> a;
        }

        @Data
        private static class D {
            private List<E> e;
        }

        @Data
        private static class E {
            private String c;
            private String a;
        }
    }

    @Data
    public static class TestArray {
        private Integer a;
        private Integer b;
        private C c;
        private D d;

        @Data
        private static class C {
            private Integer[] a;
        }

        @Data
        private static class D {
            private TestList.E[] e;
        }
    }

    @Data
    public static class TestMap {
        private String a;
        private Integer v;
        private E e;
        private C c;

        @Data
        private static class E {
            private Map<String, String> t;
        }

        @Data
        private static class C {
            private Map<String, Integer> d;
            private Map<String, Point> f;
            private Map<String, Point> g;
        }

        @Data
        private static class Point {
            private Integer x;
            private Integer y;
        }
    }


}
