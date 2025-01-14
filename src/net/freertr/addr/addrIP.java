package net.freertr.addr;

import net.freertr.util.bits;

/**
 * stores one IP (v4 or v6) address
 *
 * @author matecsaba
 */
public class addrIP extends addrType {

    /**
     * create instance
     */
    public addrIP() {
    }

    /**
     * size of address
     */
    public final static int size = 16;

    private final static byte[] IPv4addrBeg = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff};

    private final static byte[] IPv4maskBeg = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    private static int IPv4prefLen = addrIPv6.size - addrIPv4.size;

    /**
     * get size
     *
     * @return size
     */
    public int getSize() {
        return size;
    }

    /**
     * copy bytes
     *
     * @return copy
     */
    public addrIP copyBytes() {
        addrIP a = new addrIP();
        a.addr[0] = addr[0];
        a.addr[1] = addr[1];
        a.addr[2] = addr[2];
        a.addr[3] = addr[3];
        a.addr[4] = addr[4];
        a.addr[5] = addr[5];
        a.addr[6] = addr[6];
        a.addr[7] = addr[7];
        a.addr[8] = addr[8];
        a.addr[9] = addr[9];
        a.addr[10] = addr[10];
        a.addr[11] = addr[11];
        a.addr[12] = addr[12];
        a.addr[13] = addr[13];
        a.addr[14] = addr[14];
        a.addr[15] = addr[15];
        return a;
    }

    /**
     * compare two instances
     *
     * @param o1 first
     * @param o2 second
     * @return as usual
     */
    public int compare(addrType o1, addrType o2) {
        int v1 = bits.msbGetW(o1.addr, 0);
        int v2 = bits.msbGetW(o2.addr, 0);
        if (v1 < v2) {
            return -1;
        }
        if (v1 > v2) {
            return +1;
        }
        v1 = bits.msbGetW(o1.addr, 2);
        v2 = bits.msbGetW(o2.addr, 2);
        if (v1 < v2) {
            return -1;
        }
        if (v1 > v2) {
            return +1;
        }
        v1 = bits.msbGetW(o1.addr, 4);
        v2 = bits.msbGetW(o2.addr, 4);
        if (v1 < v2) {
            return -1;
        }
        if (v1 > v2) {
            return +1;
        }
        v1 = bits.msbGetW(o1.addr, 6);
        v2 = bits.msbGetW(o2.addr, 6);
        if (v1 < v2) {
            return -1;
        }
        if (v1 > v2) {
            return +1;
        }
        v1 = bits.msbGetW(o1.addr, 8);
        v2 = bits.msbGetW(o2.addr, 8);
        if (v1 < v2) {
            return -1;
        }
        if (v1 > v2) {
            return +1;
        }
        v1 = bits.msbGetW(o1.addr, 10);
        v2 = bits.msbGetW(o2.addr, 10);
        if (v1 < v2) {
            return -1;
        }
        if (v1 > v2) {
            return +1;
        }
        v1 = bits.msbGetW(o1.addr, 12);
        v2 = bits.msbGetW(o2.addr, 12);
        if (v1 < v2) {
            return -1;
        }
        if (v1 > v2) {
            return +1;
        }
        v1 = bits.msbGetW(o1.addr, 14);
        v2 = bits.msbGetW(o2.addr, 14);
        if (v1 < v2) {
            return -1;
        }
        if (v1 > v2) {
            return +1;
        }
        return 0;
    }

    /**
     * test if address is a v4 address
     *
     * @return true if it is a v4 address
     */
    public boolean isIPv4() {
        for (int i = 0; i < IPv4prefLen; i++) {
            if (addr[i] != IPv4addrBeg[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * convert address to ipv4 address does not check if v4 prefix exists at
     * beginning
     *
     * @return ipv4 address
     */
    public addrIPv4 toIPv4() {
        addrIPv4 a = new addrIPv4();
        a.fromBuf(addr, IPv4prefLen);
        return a;
    }

    /**
     * convert address to ipv6 address does not check if v4 prefix exists at
     * beginning
     *
     * @return ipv6 address
     */
    public addrIPv6 toIPv6() {
        addrIPv6 a = new addrIPv6();
        a.fromBuf(addr, 0);
        return a;
    }

    /**
     * convert from ipv4 address
     *
     * @param a ipv4 address
     */
    public void fromIPv4addr(addrIPv4 a) {
        bits.byteCopy(IPv4addrBeg, 0, addr, 0, IPv4prefLen);
        bits.byteCopy(a.addr, 0, addr, IPv4prefLen, addrIPv4.size);
    }

    /**
     * convert from ipv4 netmask
     *
     * @param a ipv4 netmask
     */
    public void fromIPv4mask(addrIPv4 a) {
        bits.byteCopy(IPv4maskBeg, 0, addr, 0, IPv4prefLen);
        bits.byteCopy(a.addr, 0, addr, IPv4prefLen, addrIPv4.size);
    }

    /**
     * convert from ipv6 address
     *
     * @param a ipv6 address
     */
    public void fromIPv6addr(addrIPv6 a) {
        bits.byteCopy(a.addr, 0, addr, 0, addrIPv6.size);
    }

    /**
     * convert from ipv6 netmask
     *
     * @param a ipv6 address
     */
    public void fromIPv6mask(addrIPv6 a) {
        bits.byteCopy(a.addr, 0, addr, 0, addrIPv6.size);
    }

    /**
     * convert multicast address to multicast mac address
     *
     * @return multicast mac address
     */
    public addrMac conv2multiMac() {
        if (isIPv4()) {
            return toIPv4().conv2multiMac();
        } else {
            return toIPv6().conv2multiMac();
        }
    }

    /**
     * test if this is a multicast address
     *
     * @return true if yes
     */
    public boolean isMulticast() {
        if (isIPv4()) {
            return toIPv4().isMulticast();
        } else {
            return toIPv6().isMulticast();
        }
    }

    /**
     * test if this is a routed multicast address
     *
     * @return true if yes
     */
    public boolean isRoutedMcast() {
        if (isIPv4()) {
            return toIPv4().isRoutedMcast();
        } else {
            return toIPv6().isRoutedMcast();
        }
    }

    /**
     * test if this is a unicast address
     *
     * @return tris if yes
     */
    public boolean isUnicast() {
        if (isIPv4()) {
            return toIPv4().isUnicast();
        } else {
            return toIPv6().isUnicast();
        }
    }

    /**
     * test if this is an empty address
     *
     * @return tris if yes
     */
    public boolean isEmpty() {
        if (isIPv4()) {
            return toIPv4().isEmpty();
        } else {
            return toIPv6().isEmpty();
        }
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        if (isIPv4()) {
            return "" + toIPv4();
        } else {
            return "" + toIPv6();
        }
    }

    /**
     * convert from string
     *
     * @param s string
     * @return false on success, true on error
     */
    public boolean fromString(String s) {
        addrIPv4 a4 = new addrIPv4();
        addrIPv6 a6 = new addrIPv6();
        if (!a4.fromString(s)) {
            fromIPv4addr(a4);
            return false;
        }
        if (!a6.fromString(s)) {
            fromIPv6addr(a6);
            return false;
        }
        return true;
    }

}
