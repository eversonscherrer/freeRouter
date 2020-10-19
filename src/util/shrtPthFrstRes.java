package util;

import addr.addrIP;
import addr.addrType;
import java.util.Comparator;
import tab.tabRouteIface;

/**
 * spf result
 *
 * @param <Ta> type of nodes
 * @author matecsaba
 */
public class shrtPthFrstRes<Ta extends addrType> implements Comparator<shrtPthFrstRes<Ta>> {

    /**
     * node handle
     */
    protected shrtPthFrstNode<Ta> nodeH;

    /**
     * node address
     */
    public Ta nodeA;

    /**
     * hop count
     */
    public int hops;

    /**
     * nexthop address
     */
    public addrIP nxtHop;

    /**
     * forwarding interface
     */
    public tabRouteIface iface;

    /**
     * segrou base
     */
    public int srBeg;

    /**
     * bier base
     */
    public int brBeg;

    /**
     * create instance
     *
     * @param nam node
     * @param hp hops
     */
    public shrtPthFrstRes(shrtPthFrstNode<Ta> nam, int hp) {
        nodeH = nam;
        nodeA = nam.name;
        hops = hp;
    }

    public int compare(shrtPthFrstRes<Ta> o1, shrtPthFrstRes<Ta> o2) {
        if (o1.hops < o2.hops) {
            return -1;
        }
        if (o1.hops > o2.hops) {
            return +1;
        }
        return o1.nodeH.compare(o1.nodeH, o2.nodeH);
    }

    public String toString() {
        return "" + nodeH;
    }

}
