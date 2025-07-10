package com.xm.sanvanfo.common.utils;


import lombok.extern.slf4j.Slf4j;
import java.net.*;
import java.util.Enumeration;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "unused"})
@Slf4j
public class NetUtils {
    private static final String LOCALHOST = "127.0.0.1";

    private static final String ANY_HOST = "0.0.0.0";

    private static volatile InetAddress LOCAL_ADDRESS = null;

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    /**
     * To string address string.
     *
     * @param address the address
     * @return the string
     */
    public static String toStringAddress(SocketAddress address) {
        if (address == null) {
            return "";
        }
        return toStringAddress((InetSocketAddress) address);
    }

    /**
     * To ip address string.
     *
     * @param address the address
     * @return the string
     */
    public static String toIpAddress(SocketAddress address) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
        return inetSocketAddress.getAddress().getHostAddress();
    }

    /**
     * To string address string.
     *
     * @param address the address
     * @return the string
     */
    public static String toStringAddress(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    /**
     * To inet socket address inet socket address.
     *
     * @param address the address
     * @return the inet socket address
     */
    public static InetSocketAddress toInetSocketAddress(String address) {
        int i = address.indexOf(':');
        String host;
        int port;
        if (i > -1) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
            port = 0;
        }
        return new InetSocketAddress(host, port);
    }

    /**
     * To long long.
     *
     * @param address the address
     * @return the long
     */
    public static long toLong(String address) {
        InetSocketAddress ad = toInetSocketAddress(address);
        String[] ip = ad.getAddress().getHostAddress().split("\\.");
        long r = 0;
        r = r | (Long.parseLong(ip[0]) << 40);
        r = r | (Long.parseLong(ip[1]) << 32);
        r = r | (Long.parseLong(ip[2]) << 24);
        r = r | (Long.parseLong(ip[3]) << 16);
        r = r | ad.getPort();
        return r;
    }

    /**
     * Gets local ip.
     *
     * @return the local ip
     */
    public static String getLocalIp() {
        InetAddress address = getLocalAddress();
        return address == null ? LOCALHOST : address.getHostAddress();
    }

    /**
     * Gets local host.
     *
     * @return the local host
     */
    public static String getLocalHost() {
        InetAddress address = getLocalAddress();
        return address == null ? "localhost" : address.getHostName();
    }

    /**
     * Gets local address.
     *
     * @return the local address
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = getLocalAddress0();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            log.warn("Failed to retrieving ip address, {}", e.getMessage(), e);
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            try {
                                InetAddress address = addresses.nextElement();
                                if (isValidAddress(address)) {
                                    return address;
                                }
                            } catch (Throwable e) {
                                log.warn("Failed to retrieving ip address, {}", e.getMessage(), e);
                            }
                        }
                    } catch (Throwable e) {
                        log.warn("Failed to retrieving ip address, {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            log.warn("Failed to retrieving ip address, {}", e.getMessage(), e);
        }
        log.error("Could not get local host ip address, will use 127.0.0.1 instead.");
        return localAddress;
    }

    /**
     * Valid address.
     *
     * @param address the address
     */
    public static void validAddress(InetSocketAddress address) {
        if (address.getHostName() == null || 0 == address.getPort()) {
            throw new IllegalArgumentException("invalid address:" + address);
        }
    }


    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        return isValidIp(address.getHostAddress(), false);
    }


    public static boolean isValidIp(String ip, boolean validLocalAndAny) {
        if (ip == null) {
            return false;
        }
        ip = convertIpIfNecessary(ip);
        if (validLocalAndAny) {
            return IP_PATTERN.matcher(ip).matches();
        } else {
            return !ANY_HOST.equals(ip) && !LOCALHOST.equals(ip) && IP_PATTERN.matcher(ip).matches();
        }

    }


    private static String convertIpIfNecessary(String ip) {
        if (IP_PATTERN.matcher(ip).matches()) {
            return ip;
        } else {
            try {
                return InetAddress.getByName(ip).getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
