package com.xm.sanvanfo.common.utils;


import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class EnvUtils {
   private static final ConcurrentHashMap<String, Object> mapCache =  new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

   public static InetAddress getIpAddress(String name, Integer type) {
       //InetAddress.getLocalhost() is wrong ,because there are multiple network cards
       String key = makeCacheKey("networkAddressByName", name, type);
       if (mapCache.containsKey(key)) {
           return (InetAddress) mapCache.get(key);
       }
       synchronized (lockMap.computeIfAbsent(key, o->new Object())) {
           if (mapCache.containsKey(key)) {
               return (InetAddress) mapCache.get(key);
           }
           try {
               InetAddress o;
               NetworkInterface networkInterface = NetworkInterface.getByName(name);
               if (null == networkInterface) {
                   return null;
               }
               o = getNetAddress(networkInterface, type);
               if(null != o) {
                   mapCache.put(key, o);
               }
               else {
                   Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
                   while (interfaceEnumeration.hasMoreElements()) {
                       NetworkInterface inter = interfaceEnumeration.nextElement();
                       o = getNetAddress(inter, type);
                       if(null != o && (o.getHostAddress().contains("192.") || o.getHostAddress().contains("172.") ||
                               o.getHostAddress().contains("196.") || o.getHostAddress().contains("10."))) {
                           mapCache.put(key, o);
                           break;
                       }
                   }
               }
               return o;

           } catch (SocketException ex) {
               return null;
           }
       }
   }

   public static InetAddress getIpAddressByRegex(List<String> regex) {
       if(null == regex) {
           regex = new ArrayList<>();
       }
       String key = makeCacheKey("networkAddressByRegex", String.join(":", regex));
       if (mapCache.containsKey(key)) {
           return (InetAddress) mapCache.get(key);
       }
       synchronized (lockMap.computeIfAbsent(key, o->new Object())) {
           if (mapCache.containsKey(key)) {
               return (InetAddress) mapCache.get(key);
           }
           try {
               Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
               while (interfaceEnumeration.hasMoreElements()) {
                   NetworkInterface inter = interfaceEnumeration.nextElement();
                   Enumeration<InetAddress> enumeration = inter.getInetAddresses();
                   while (enumeration.hasMoreElements()) {
                       InetAddress address = enumeration.nextElement();
                       String host = address.getHostAddress();
                       if(regex.size() == 0){
                           if(!host.equals("127.0.0.1") && address instanceof Inet4Address) {
                               return address;
                           }
                       }
                       else {
                           boolean match = true;
                           for (String pattern : regex
                           ) {
                               //和spring cloud判断一样，这样可以使用InetUtilsProperties的配置
                               if (!host.matches(pattern) && !host.startsWith(pattern)) {
                                   match = false;
                                   break;
                               }
                           }
                           if (match) {
                               return address;
                           }
                       }

                   }
               }
               return null;
           }
           catch (SocketException ignore) {
               return null;
           }
       }
   }

   public static String getClientId(String name, Integer type, int port, String applicationId) {
       String address = Objects.requireNonNull(getIpAddress(name, type)).getHostAddress();
       return String.format("%s:%d:%s", address, port, applicationId);
   }

   private static InetAddress getNetAddress(NetworkInterface networkInterface, Integer type) {
       Enumeration<InetAddress> enumeration = networkInterface.getInetAddresses();
       InetAddress o = null;
       while (enumeration.hasMoreElements()) {
           InetAddress address = enumeration.nextElement();
           if (address instanceof Inet4Address && type.equals(4)) {
               o = address;
               break;
           }
           else if(address instanceof Inet6Address && type.equals(6)) {
               o = address;
               break;
           }
       }
       return o;
   }

   private static String makeCacheKey(Object... objects) {
       List<Object> os = Arrays.asList(objects);
       List<String> joins = os.stream().map(Object::toString).collect(Collectors.toList());
       return String.join("-", joins);
   }
}
