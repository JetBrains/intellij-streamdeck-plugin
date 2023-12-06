package com.jetbrains.ide.streamdeck.util;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Local host util.
 *
 */
public class LocalHostUtil {

    public void foo() {

    }

    public static String getLocalHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    public static String getLocalIP() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    /**
     * Get all IP, skip loopback and local address and IPv6.
     * 
     * @return
     * @throws SocketException
     */
    public static String[] getLocalIPs() throws SocketException {
        List<String> list = new ArrayList<>();

        for(NetworkInterface intf : Collections.list( NetworkInterface.getNetworkInterfaces())) {
            if (intf.isLoopback() ) { // || intf.isVirtual()
                continue;
            }

            for(InetAddress addr : Collections.list(intf.getInetAddresses())) {
                if (addr.isLoopbackAddress() || !addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
                    continue;
                }

                if (addr instanceof Inet6Address && addr.getHostAddress().contains("%")) {
                    try {
                        addr = Inet6Address.getByAddress(addr.getAddress());
                    } catch (UnknownHostException e) {

                    }
                }
                list.add(addr.getHostAddress());
            }
        }

        return list.toArray(new String[0]);
    }

    public static void main(String[] args) {
        try {
            System.out.println("Host name：" + LocalHostUtil.getLocalHostName());
            System.out.println("Preferred IP：" + LocalHostUtil.getLocalIP());
            System.out.println("All IPs：" + String.join(",", LocalHostUtil.getLocalIPs()));
        } catch (UnknownHostException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}