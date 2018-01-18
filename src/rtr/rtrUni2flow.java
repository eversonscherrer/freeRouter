package rtr;

import addr.addrIP;
import ip.ipCor4;
import ip.ipCor6;
import ip.ipFwd;
import ip.ipRtr;
import java.util.ArrayList;
import java.util.List;
import tab.tabRoute;
import tab.tabRouteEntry;
import tab.tabRtrmapN;
import user.userHelping;
import util.bits;
import util.cmds;

/**
 * unicast to flowspec
 *
 * @author matecsaba
 */
public class rtrUni2flow extends ipRtr {

    /**
     * the forwarder protocol
     */
    public final ipFwd fwdCore;

    /**
     * route type
     */
    protected final tabRouteEntry.routeType rouTyp;

    /**
     * router number
     */
    protected final int rtrNum;

    /**
     * route afi
     */
    protected final boolean ipv6;

    /**
     * distance to give
     */
    protected int distance;

    /**
     * as to give
     */
    protected int trgAs;

    /**
     * rate to give
     */
    protected int trgRate;

    /**
     * create unicast to flowspec process
     *
     * @param forwarder forwarder to update
     * @param id process id
     */
    public rtrUni2flow(ipFwd forwarder, int id) {
        fwdCore = forwarder;
        rtrNum = id;
        switch (fwdCore.ipVersion) {
            case ipCor4.protocolVersion:
                rouTyp = tabRouteEntry.routeType.uni2flow4;
                ipv6 = false;
                break;
            case ipCor6.protocolVersion:
                rouTyp = tabRouteEntry.routeType.uni2flow6;
                ipv6 = true;
                break;
            default:
                ipv6 = false;
                rouTyp = null;
                break;
        }
        distance = 15;
        routerComputedU = new tabRoute<addrIP>("rx");
        routerComputedM = new tabRoute<addrIP>("rx");
        routerComputedF = new tabRoute<addrIP>("rx");
        routerCreateComputed();
        fwdCore.routerAdd(this, rouTyp, id);
    }

    public String toString() {
        return "uni2flow on " + fwdCore;
    }

    public synchronized void routerCreateComputed() {
        tabRoute<addrIP> res = new tabRoute<addrIP>("computed");
        for (int i = 0; i < routerRedistedU.size(); i++) {
            tabRouteEntry<addrIP> ntry = routerRedistedU.get(i);
            if (ntry == null) {
                continue;
            }
            ntry = ntry.copyBytes();
            tabRouteEntry<addrIP> attr = new tabRouteEntry<addrIP>();
            attr.rouTyp = rouTyp;
            attr.protoNum = rtrNum;
            attr.distance = distance;
            attr.nextHop = ntry.nextHop;
            if (attr.nextHop == null) {
                attr.nextHop = new addrIP();
            }
            attr.stdComm = ntry.stdComm;
            attr.extComm = ntry.extComm;
            attr.lrgComm = ntry.lrgComm;
            attr.metric = ntry.metric;
            attr.tag = ntry.tag;
            if (trgRate > 0) {
                if (attr.extComm == null) {
                    attr.extComm = new ArrayList<Long>();
                }
                attr.extComm.add(tabRtrmapN.rate2comm(trgAs, trgRate));
            }
            rtrBgpFlow.advertNetwork(res, ntry.prefix, ipv6, attr);
        }
        routerComputedF = res;
        fwdCore.routerChg(this);
    }

    public void routerRedistChanged() {
        routerCreateComputed();
    }

    public void routerOthersChanged() {
    }

    public void routerGetHelp(userHelping l) {
        l.add("1 2   distance                    specify default distance");
        l.add("2 .     <num>                     distance");
        l.add("1 2   as                          specify target as");
        l.add("2 .     <num>                     as");
        l.add("1 2   rate                        specify target rate");
        l.add("2 .     <num>                     bytes/sec");
    }

    public void routerGetConfig(List<String> l, String beg, boolean filter) {
        l.add(beg + "distance " + distance);
        l.add(beg + "as " + trgAs);
        l.add(beg + "rate " + trgRate);
    }

    public boolean routerConfigure(cmds cmd) {
        String s = cmd.word();
        boolean negated = false;
        if (s.equals("no")) {
            s = cmd.word();
            negated = true;
        }
        if (s.equals("distance")) {
            distance = bits.str2num(cmd.word());
            return false;
        }
        if (s.equals("as")) {
            trgAs = bits.str2num(cmd.word());
            return false;
        }
        if (s.equals("rate")) {
            trgRate = bits.str2num(cmd.word());
            return false;
        }
        return true;
    }

    public void routerCloseNow() {
    }

    public int routerNeighCount() {
        return 0;
    }

    public void routerNeighList(tabRoute<addrIP> tab) {
    }

    public int routerIfaceCount() {
        return 0;
    }

}
