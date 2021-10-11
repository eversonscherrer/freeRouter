package net.freertr.rtr;

import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.addr.addrPrefix;
import net.freertr.ip.ipFwd;
import net.freertr.ip.ipFwdIface;
import net.freertr.ip.ipRtr;
import net.freertr.pack.packHolder;
import net.freertr.prt.prtGenConn;
import net.freertr.prt.prtServP;
import net.freertr.prt.prtUdp;
import net.freertr.tab.tabGen;
import net.freertr.tab.tabRoute;
import net.freertr.tab.tabRouteAttr;
import net.freertr.tab.tabRouteEntry;
import net.freertr.user.userFormat;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;
import net.freertr.util.counter;
import net.freertr.util.debugger;
import net.freertr.util.logger;
import net.freertr.util.state;

/**
 * routing information protocol (rfc2453) version 2
 *
 * @author matecsaba
 */
public class rtrRip4 extends ipRtr implements prtServP {

    /**
     * port number
     */
    public final static int port = 520;

    /**
     * version number
     */
    public final static int version = 2;

    /**
     * address family identifier
     */
    public final static int afiID = 2;

    /**
     * size of header
     */
    public final static int sizeHead = 4;

    /**
     * size of routing entry
     */
    public final static int sizeNtry = 20;

    /**
     * minimum metric
     */
    public final static int metricMin = 1;

    /**
     * maximum metric this is the inaccessibly one
     */
    public final static int metricMax = 16;

    /**
     * entries per update
     */
    public final static int entryPerUpdate = 23;

    /**
     * the udp protocol
     */
    protected prtUdp udpCore;

    /**
     * forwarding core
     */
    public final ipFwd fwdCore;

    private tabGen<rtrRip4iface> ifaces;

    private tabGen<rtrRip4neigh> neighs;

    /**
     * create one rip process
     *
     * @param forwarder the ip protocol
     * @param protocol the udp protocol
     * @param id process id
     */
    public rtrRip4(ipFwd forwarder, prtUdp protocol, int id) {
        if (debugger.rtrRip4evnt) {
            logger.debug("startup");
        }
        fwdCore = forwarder;
        udpCore = protocol;
        ifaces = new tabGen<rtrRip4iface>();
        neighs = new tabGen<rtrRip4neigh>();
        routerCreateComputed();
        fwdCore.routerAdd(this, tabRouteAttr.routeType.rip4, id);
    }

    /**
     * get neighbor count
     *
     * @return count
     */
    public int routerNeighCount() {
        return neighs.size();
    }

    /**
     * list neighbors
     *
     * @param tab list
     */
    public void routerNeighList(tabRoute<addrIP> tab) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrRip4neigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            if (nei.iface.iface.lower.getState() != state.states.up) {
                continue;
            }
            tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
            ntry.prefix = new addrPrefix<addrIP>(nei.conn.peerAddr, addrIP.size * 8);
            tabRoute.addUpdatedEntry(tabRoute.addType.better, tab, rtrBgpUtil.sfiUnicast, 0, ntry, true, null, null, routerAutoMesh);
        }
    }

    /**
     * get interface count
     *
     * @return count
     */
    public int routerIfaceCount() {
        return ifaces.size();
    }

    /**
     * get list of link states
     *
     * @param tab table to update
     * @param par parameter
     * @param asn asn
     * @param adv advertiser
     */
    public void routerLinkStates(tabRoute<addrIP> tab, int par, int asn, addrIPv4 adv) {
    }

    /**
     * convert to string
     *
     * @return string
     */
    public String toString() {
        return "rip on " + fwdCore;
    }

    /**
     * add one interface to work on
     *
     * @param ifc ip forwarder interface
     * @return false if successful, true if error happened
     */
    public rtrRip4iface addInterface(ipFwdIface ifc) {
        if (debugger.rtrRip4evnt) {
            logger.debug("add iface " + ifc);
        }
        if (ifc == null) {
            return null;
        }
        rtrRip4iface ntry = new rtrRip4iface(this, ifc);
        rtrRip4iface old = ifaces.add(ntry);
        if (old != null) {
            ntry = old;
        }
        ntry.register2udp();
        routerCreateComputed();
        return ntry;
    }

    /**
     * close interface
     *
     * @param iface interface
     */
    public void closedInterface(ipFwdIface iface) {
        rtrRip4iface ifc = new rtrRip4iface(this, iface);
        ifc = ifaces.del(ifc);
        if (ifc == null) {
            return;
        }
        ifc.unregister2udp();
        for (int i = neighs.size() - 1; i >= 0; i--) {
            rtrRip4neigh nei = neighs.get(i);
            if (nei.iface.iface.ifwNum != iface.ifwNum) {
                continue;
            }
            neighs.del(nei);
            nei.unregister2udp();
        }
        routerCreateComputed();
    }

    /**
     * start connection
     *
     * @param id connection
     * @return false on success, true on error
     */
    public boolean datagramAccept(prtGenConn id) {
        rtrRip4iface ifc = new rtrRip4iface(this, id.iface);
        ifc = ifaces.find(ifc);
        if (ifc == null) {
            logger.warn("no interface " + id);
            return true;
        }
        if ((ifc.connectedCheck) && (!ifc.iface.lower.checkConnected(id.peerAddr))) {
            logger.warn("got from out of subnet peer " + id);
            return true;
        }
        logger.warn("neighbor " + id.peerAddr + " up");
        rtrRip4neigh ntry = new rtrRip4neigh(id);
        rtrRip4neigh old = neighs.add(ntry);
        if (old != null) {
            ntry = old;
        }
        ntry.iface = ifc;
        ntry.conn = id;
        if (ifc.bfdTrigger) {
            ifc.iface.bfdAdd(id.peerAddr, ntry, "rip");
        }
        return false;
    }

    /**
     * connection ready
     *
     * @param id connection
     */
    public void datagramReady(prtGenConn id) {
    }

    /**
     * close connection
     *
     * @param id connection
     */
    public void datagramClosed(prtGenConn id) {
        rtrRip4neigh ntry = new rtrRip4neigh(id);
        ntry = neighs.del(ntry);
        if (ntry == null) {
            return;
        }
        logger.error("neighbor " + id.peerAddr + " down");
        id.iface.bfdDel(id.peerAddr, ntry);
        routerCreateComputed();
    }

    /**
     * work connection
     *
     * @param id connection
     */
    public void datagramWork(prtGenConn id) {
        rtrRip4neigh nei = new rtrRip4neigh(id);
        nei = neighs.find(nei);
        if (nei != null) {
            if (nei.doWork()) {
                routerCreateComputed();
            }
            return;
        }
        rtrRip4iface ifc = new rtrRip4iface(this, id.iface);
        ifc = ifaces.find(ifc);
        if (ifc != null) {
            ifc.doWork();
            return;
        }
        id.setClosing();
    }

    /**
     * received error
     *
     * @param id connection
     * @param pck packet
     * @param rtr reporting router
     * @param err error happened
     * @param lab error label
     * @return false on success, true on error
     */
    public boolean datagramError(prtGenConn id, packHolder pck, addrIP rtr, counter.reasons err, int lab) {
        return false;
    }

    /**
     * notified that state changed
     *
     * @param id id number to reference connection
     * @param stat state
     * @return return false if successful, true if error happened
     */
    public boolean datagramState(prtGenConn id, state.states stat) {
        if (stat == state.states.up) {
            return false;
        }
        rtrRip4neigh ntry = new rtrRip4neigh(id);
        ntry = neighs.find(ntry);
        if (ntry == null) {
            id.setClosing();
            return false;
        }
        ntry.bfdPeerDown();
        return false;
    }

    /**
     * received packet
     *
     * @param id connection
     * @param pck packet
     * @return false on success, true on error
     */
    public boolean datagramRecv(prtGenConn id, packHolder pck) {
        rtrRip4neigh ntry = new rtrRip4neigh(id);
        ntry = neighs.find(ntry);
        if (ntry == null) {
            id.setClosing();
            return false;
        }
        if (ntry.gotPack(pck)) {
            return false;
        }
        routerCreateComputed();
        return false;
    }

    /**
     * create computed
     */
    public synchronized void routerCreateComputed() {
        if (debugger.rtrRip4evnt) {
            logger.debug("create table");
        }
        tabRoute<addrIP> tab = new tabRoute<addrIP>("rip");
        tabRouteEntry<addrIP> ntry;
        for (int i = 0; i < ifaces.size(); i++) {
            rtrRip4iface ifc = ifaces.get(i);
            if (ifc == null) {
                continue;
            }
            if (ifc.iface.lower.getState() != state.states.up) {
                continue;
            }
            if (ifc.suppressAddr) {
                continue;
            }
            ntry = tab.add(tabRoute.addType.better, ifc.iface.network, null);
            ntry.best.rouTyp = tabRouteAttr.routeType.conn;
            ntry.best.iface = ifc.iface;
            ntry.best.distance = tabRouteAttr.distanIfc;
        }
        for (int i = 0; i < neighs.size(); i++) {
            rtrRip4neigh nei = neighs.get(i);
            if (nei == null) {
                continue;
            }
            if (!nei.iface.allowRx) {
                continue;
            }
            if (nei.iface.iface.lower.getState() != state.states.up) {
                continue;
            }
            tab.mergeFrom(tabRoute.addType.ecmp, nei.learned, null, true, tabRouteAttr.distanLim);
        }
        routerDoAggregates(rtrBgpUtil.sfiUnicast, tab, tab, fwdCore.commonLabel, null, 0);
        tab.setProto(routerProtoTyp, routerProcNum);
        tab.preserveTime(routerComputedU);
        routerComputedU = tab;
        routerComputedM = tab;
        fwdCore.routerChg(this);
    }

    /**
     * redistribution changed
     */
    public void routerRedistChanged() {
        routerCreateComputed();
    }

    /**
     * others changed
     */
    public void routerOthersChanged() {
    }

    /**
     * stop work
     */
    public void routerCloseNow() {
        rtrRip4iface ntryi = new rtrRip4iface(null, null);
        for (int i = ifaces.size() - 1; i >= 0; i--) {
            ntryi = ifaces.get(i);
            if (ntryi == null) {
                continue;
            }
            ifaces.del(ntryi);
            ntryi.unregister2udp();
        }
        rtrRip4neigh ntryn = new rtrRip4neigh(null);
        for (int i = neighs.size() - 1; i >= 0; i--) {
            ntryn = neighs.get(i);
            if (ntryn == null) {
                continue;
            }
            neighs.del(ntryn);
            ntryn.unregister2udp();
        }
        fwdCore.routerDel(this);
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
    public void routerGetConfig(List<String> l, String beg, int filter) {
    }

    /**
     * configure
     *
     * @param cmd command
     * @return false on success, true on error
     */
    public boolean routerConfigure(cmds cmd) {
        return true;
    }

    /**
     * list neighbors
     *
     * @return list of neighbors
     */
    public userFormat showNeighs() {
        userFormat l = new userFormat("|", "interface|learn|neighbor|uptime");
        for (int i = 0; i < neighs.size(); i++) {
            rtrRip4neigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            l.add(ntry.iface.iface + "|" + ntry.learned.size() + "|" + ntry.conn.peerAddr + "|" + bits.timePast(ntry.upTime));
        }
        return l;
    }

    /**
     * list interfaces
     *
     * @return list of interfaces
     */
    public userFormat showIfaces() {
        userFormat l = new userFormat("|", "interface|neighbors");
        for (int i = 0; i < ifaces.size(); i++) {
            rtrRip4iface ifc = ifaces.get(i);
            l.add(ifc.iface + "|" + countNeighs(ifc.iface.ifwNum));
        }
        return l;
    }

    private int countNeighs(int ifc) {
        int o = 0;
        for (int i = 0; i < neighs.size(); i++) {
            rtrRip4neigh nei = neighs.get(i);
            if (nei.iface.iface.ifwNum == ifc) {
                o++;
            }
        }
        return o;
    }

    /**
     * find peer
     *
     * @param addr address to find
     * @return neighbor, null if not found
     */
    public rtrRip4neigh findPeer(addrIP addr) {
        for (int i = 0; i < neighs.size(); i++) {
            rtrRip4neigh ntry = neighs.get(i);
            if (ntry == null) {
                continue;
            }
            if (addr.compare(addr, ntry.conn.peerAddr) != 0) {
                continue;
            }
            return ntry;
        }
        return null;
    }

}