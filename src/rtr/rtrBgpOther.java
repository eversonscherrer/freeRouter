package rtr;

import addr.addrIP;
import addr.addrPrefix;
import cfg.cfgIfc;
import cfg.cfgRtr;
import ip.ipFwd;
import ip.ipMpls;
import ip.ipRtr;
import java.util.List;
import tab.tabGen;
import tab.tabRoute;
import tab.tabRouteEntry;
import user.userHelping;
import util.cmds;

/**
 * bgp4 other router
 *
 * @author matecsaba
 */
public class rtrBgpOther extends ipRtr {

    /**
     * import distance
     */
    public int distance;

    /**
     * srv6 advertisement source
     */
    public cfgIfc srv6;

    private final rtrBgp parent;

    private final ipFwd fwd;

    private tabGen<addrIP> peers = new tabGen<addrIP>();

    /**
     * unregister from ip
     */
    public void unregister2ip() {
        fwd.routerDel(this);
    }

    /**
     * register to ip
     */
    public void register2ip() {
        fwd.routerAdd(this, parent.rouTyp, parent.rtrNum);
    }

    /**
     * create new instance
     *
     * @param p parent to use
     * @param f vrf to use
     */
    public rtrBgpOther(rtrBgp p, ipFwd f) {
        fwd = f;
        parent = p;
        routerVpn = true;
        distance = -1;
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        return "bgp on " + parent.fwdCore;
    }

    /**
     * create computed table
     */
    public synchronized void routerCreateComputed() {
    }

    /**
     * redistribution changed
     */
    public void routerRedistChanged() {
        parent.routerRedistChanged();
        fwd.routerChg(this);
    }

    /**
     * others changed
     */
    public void routerOthersChanged() {
        if (fwd.prefixMode == ipFwd.labelMode.common) {
            return;
        }
        parent.routerRedistChanged();
        fwd.routerChg(this);
    }

    private void doExportRoute(tabRouteEntry<addrIP> ntry, tabRoute<addrIP> trg) {
        if (ntry == null) {
            return;
        }
        ntry = ntry.copyBytes();
        if (ntry.best.labelLoc == null) {
            ntry.best.labelLoc = fwd.commonLabel;
        }
        ntry.best.rouSrc = rtrBgpUtil.peerOriginate;
        ipMpls.putSrv6prefix(ntry, srv6, ntry.best.labelLoc);
        tabRoute.addUpdatedEntry(tabRoute.addType.better, trg, parent.afiOtr, ntry, true, null, null, null);
    }

    /**
     * merge routes to table
     *
     * @param nUni unicast table to update
     */
    public void doAdvertise(tabRoute<addrIP> nUni) {
        for (int i = 0; i < routerRedistedU.size(); i++) {
            doExportRoute(routerRedistedU.get(i), nUni);
        }
    }

    private void doImportRoute(tabRouteEntry<addrIP> ntry, tabRoute<addrIP> trg) {
        if (ntry.best.rouSrc == rtrBgpUtil.peerOriginate) {
            return;
        }
        ntry = ntry.copyBytes();
        ntry.best.rouTab = parent.fwdCore;
        if (ntry.best.segrouPrf != null) {
            ntry.best.rouTab = parent.vrfCore.fwd6;
        }
        if (distance > 0) {
            ntry.best.distance = distance;
        }
        tabRoute.addUpdatedEntry(tabRoute.addType.better, trg, rtrBgpUtil.safiUnicast, ntry, false, null, null, null);
        if (parent.routerAutoMesh == null) {
            return;
        }
        peers.add(ntry.best.nextHop);
    }

    /**
     * import routes from table
     *
     * @param cmpU unicast table to read
     * @return other changes trigger full recomputation
     */
    public boolean doPeers(tabRoute<addrIP> cmpU) {
        tabRoute<addrIP> tabU = new tabRoute<addrIP>("bgp");
        peers = new tabGen<addrIP>();
        for (int i = 0; i < cmpU.size(); i++) {
            doImportRoute(cmpU.get(i), tabU);
        }
        routerDoAggregates(parent.afiUni, tabU, null, fwd.commonLabel, rtrBgpUtil.peerOriginate, parent.routerID, parent.localAs);
        routerComputedU = tabU;
        fwd.routerChg(this);
        return fwd.prefixMode != ipFwd.labelMode.common;
    }

    /**
     * get help
     *
     * @param l list
     */
    public void routerGetHelp(userHelping l) {
    }

    /**
     * get config
     *
     * @param l list
     * @param beg beginning
     * @param filter filter
     */
    public void routerGetConfig(List<String> l, String beg, boolean filter) {
    }

    /**
     * configure
     *
     * @param cmd command
     * @return false if success, true if error
     */
    public boolean routerConfigure(cmds cmd) {
        return true;
    }

    /**
     * stop work
     */
    public void routerCloseNow() {
    }

    /**
     * get config
     *
     * @param l list to append
     * @param beg beginning
     */
    public void getConfig(List<String> l, String beg) {
        l.add(beg + "distance " + distance);
        if (srv6 != null) {
            l.add(beg + "srv6 " + srv6.name);
        }
        cfgRtr.getShRedist(l, beg, this);
    }

    /**
     * get neighbor count
     *
     * @return count
     */
    public int routerNeighCount() {
        return 0;
    }

    /**
     * get neighbor list
     *
     * @param tab list
     */
    public void routerNeighList(tabRoute<addrIP> tab) {
    }

    /**
     * get interface count
     *
     * @return count
     */
    public int routerIfaceCount() {
        return 0;
    }

    /**
     * get peer list
     *
     * @param tab list to append
     */
    public void getPeerList(tabRoute<addrIP> tab) {
        for (int i = 0; i < peers.size(); i++) {
            addrIP adr = peers.get(i);
            tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
            ntry.prefix = new addrPrefix<addrIP>(adr, addrIP.size * 8);
            tabRoute.addUpdatedEntry(tabRoute.addType.better, tab, parent.afiUni, ntry, true, null, null, parent.routerAutoMesh);
        }
    }

}
