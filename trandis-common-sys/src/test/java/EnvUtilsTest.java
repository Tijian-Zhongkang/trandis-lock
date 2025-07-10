import com.xm.sanvanfo.common.utils.EnvUtils;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Collections;

public class EnvUtilsTest {

    @Test
    public void testGetIp() {
        InetAddress address = EnvUtils.getIpAddressByRegex(Collections.singletonList("192.168.1."));
        System.out.println(address);
    }

    @Test
    public void testGetIpDefault() {
        InetAddress address = EnvUtils.getIpAddressByRegex(null);
        System.out.println(address);
    }
}
