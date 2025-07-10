import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.javassist.MultiModulesJarParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Slf4j
public class JavassistForSpringTest {

    @Test
    public void springJarParseTest() throws IOException {
        URL url = new URL("jar:file:///D:/work/product/zk/new/src/zkbackconfig/target/zkbackconfig-1.0-SNAPSHOT.jar!/");
        MultiModulesJarParser parser = new MultiModulesJarParser(url);
        InputStream inputStream = parser.openSpringJarInputStream("org.springframework.util.Assert", true, "");
        Asserts.noNull(inputStream);
        inputStream.close();
    }
}
