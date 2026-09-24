package com.code.searchEngine.service;

import org.springframework.stereotype.Service;

import java.net.*;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class LocalSSRFChecker {
    private static final List<String> ALLOWED_SCHEMES = List.of( "https");;

    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost", "localhost.localdomain", "ip6-localhost", "ip6-loopback"
    );

    private static final Set<String> BLOCKED_LITERAL_IPS = Set.of(
            "169.254.169.254",      // AWS / GCP / Azure metadata
            "100.100.100.200",      // Alibaba Cloud metadata
            "metadata.google.internal"
    );

    private static final Pattern IPV6_ULA = Pattern.compile("^f[cd][0-9a-f]{2}:.*", Pattern.CASE_INSENSITIVE);

    public boolean isSafe(String url) {
        try{
            if (url == null || url.isBlank()) {
                return false;
            }

            URI uri = new URI(url);
            if(!ALLOWED_SCHEMES.contains(uri.getScheme())){
                return false;
            }

            if(uri.getHost() == null){
                return false;
            }
            String host = uri.getHost().toLowerCase();
            if (BLOCKED_HOSTNAMES.contains(host) || BLOCKED_LITERAL_IPS.contains(host)) {
                return false;
            }
            InetAddress[] addresses = InetAddress.getAllByName(host);

            for (InetAddress addr : addresses) {
                if (isBlockedAddress(addr)) {
                    return false;
                }
            }
            return true;
        }catch (Exception e){
            System.out.println(e.getMessage());
            return false;
        }
    }

    private boolean isBlockedAddress(InetAddress addr){
        if(BLOCKED_LITERAL_IPS.contains(addr.getHostAddress())){
            return false;
        }
        return addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()
                || isUniqueLocalIPv6(addr)
                || isCarrierGradeNat(addr);
    }

    private boolean isUniqueLocalIPv6(InetAddress addr) {
        if (!(addr instanceof Inet6Address)) return false;
        return IPV6_ULA.matcher(addr.getHostAddress()).matches();
    }

    private boolean isCarrierGradeNat(InetAddress addr) {
        if (!(addr instanceof Inet4Address)) return false;
        byte[] b = addr.getAddress();
        int first = b[0] & 0xFF;
        int second = b[1] & 0xFF;
        return first == 100 && (second >= 64 && second <= 127); // 100.64.0.0/10
    }
}
