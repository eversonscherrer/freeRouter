package prt;

import addr.addrIP;
import addr.addrPrefix;
import ip.ipFwd;
import ip.ipFwdEcho;
import ip.ipFwdIface;
import ip.ipFwdTab;
import ip.ipPrt;
import pack.packHolder;
import pipe.pipeLine;
import pipe.pipeSide;
import tab.tabConnect;
import user.userFormat;
import util.bits;
import util.counter;
import util.debugger;
import util.logger;
import util.state;

/**
 * protocols have to use this to be able to serve servers
 *
 * @author matecsaba
 */
public abstract class prtGen implements ipPrt {

    /**
     * create instance
     */
    public prtGen() {
    }

    /**
     * counter
     */
    public counter cntr = new counter();

    /**
     * forwarding core to use
     */
    public ipFwd fwdCore;

    /**
     * connection table
     */
    public tabConnect<addrIP, prtGenConn> clnts = new tabConnect<addrIP, prtGenConn>(new addrIP(), "connections");

    /**
     * listener table
     */
    public tabConnect<addrIP, prtGenServ> srvrs = new tabConnect<addrIP, prtGenServ>(new addrIP(), "listeners");

    /**
     * list servers
     *
     * @param l list to update
     * @param b beginning to use
     */
    public void listServers(userFormat l, String b) {
        srvrs.dump(l, b);
    }

    /**
     * list connections
     *
     * @param l list to update
     * @param b beginning to use
     */
    public void listConnects(userFormat l, String b) {
        clnts.dump(l, b);
    }

    /**
     * test port number if valid
     *
     * @param i parameter to test
     * @return false if valid, true if not
     */
    protected abstract boolean testPortNumber(int i);

    /**
     * generate random port number
     *
     * @return random port number
     */
    protected abstract int getRandomPortNum();

    /**
     * test if valid first packet, create new connection state
     *
     * @param clnt client that is going to connect
     * @param pck packet that seems a new session, null if connect invoked
     * @return false if successful, true if not
     */
    protected abstract boolean connectionStart(prtGenConn clnt, packHolder pck);

    /**
     * close requested
     *
     * @param clnt client that is going to connect
     */
    protected abstract void connectionClose(prtGenConn clnt);

    /**
     * do work of one connection
     *
     * @param clnt client that is going to connect
     */
    protected abstract void connectionWork(prtGenConn clnt);

    /**
     * send one packet
     *
     * @param clnt connection to send on
     * @param pck packet to send
     * @return false if successful, true if not
     */
    protected abstract boolean connectionSend(prtGenConn clnt, packHolder pck);

    /**
     * check how much bytes free in tx buffer
     *
     * @param ntry entry to check
     * @return number of bytes free
     */
    protected abstract int connectionBytes(prtGenConn ntry);

    /**
     * got one packet check if good in the context
     *
     * @param clnt connection to send on
     * @param pck packet to send
     */
    protected abstract void connectionRcvd(prtGenConn clnt, packHolder pck);

    /**
     * got one error check if good in the context
     *
     * @param clnt connection to send on
     * @param pck packet to send
     * @param rtr reporting router
     * @param err error happened
     * @param lab error label
     */
    protected abstract void connectionError(prtGenConn clnt, packHolder pck, addrIP rtr, counter.reasons err, int lab);

    /**
     * refuse one connection
     *
     * @param ifc source interface
     * @param pck packet to refuse
     */
    protected abstract void connectionRefuse(ipFwdIface ifc, packHolder pck);

    /**
     * get counter
     *
     * @return counter
     */
    public counter getCounter() {
        return cntr;
    }

    /**
     * close all connections
     */
    public void closeConns() {
        for (int i = clnts.size() - 1; i >= 0; i--) {
            prtGenConn ntry = clnts.get(i);
            ntry.setClosing();
            ntry.notif.wakeup();
        }
    }

    /**
     * interface is going down
     *
     * @param ifc interface id
     */
    public void closeUp(ipFwdIface ifc) {
        for (;;) {
            prtGenConn cln = clnts.delNext(ifc, null, 0, 0);
            if (cln == null) {
                break;
            }
            cln.setClosing();
            cln.deleteImmediately();
        }
        for (;;) {
            prtGenServ prt = srvrs.delNext(ifc, null, 0, 0);
            if (prt == null) {
                break;
            }
            if (prt.stream) {
                prt.serverS.closedInterface(ifc);
            } else {
                prt.serverP.closedInterface(ifc);
            }
        }
    }

    /**
     * start listening on port
     *
     * @param srv server that will handle
     * @param locI local interface, 0 means all
     * @param locP local port, 0 means all
     * @param remA remote address, null means all
     * @param remP remote port, 0 means all
     * @param name name of server
     * @param pwd password
     * @param ttl time to live
     * @return false if successful, true if error
     */
    public boolean packetListen(prtServP srv, ipFwdIface locI, int locP, addrIP remA, int remP, String name, String pwd, int ttl) {
        if (srv == null) {
            return true;
        }
        return anybodyListen(srv, null, null, locI, locP, remA, remP, name, pwd, ttl);
    }

    /**
     * start listening on port
     *
     * @param srv server that will handle
     * @param pip sample pipeline
     * @param locI local interface, 0 means all
     * @param locP local port, 0 means all
     * @param remA remote address, null means all
     * @param remP remote port, 0 means all
     * @param name name of server
     * @param pwd session password
     * @param ttl time to live
     * @return false if successful, true if error
     */
    public boolean streamListen(prtServS srv, pipeLine pip, ipFwdIface locI, int locP, addrIP remA, int remP, String name, String pwd, int ttl) {
        if (srv == null) {
            return true;
        }
        if (pip == null) {
            return true;
        }
        return anybodyListen(null, srv, pip, locI, locP, remA, remP, name, pwd, ttl);
    }

    /**
     * stop listening
     *
     * @param locI local interface, 0 means all
     * @param locP local port, 0 means all
     * @param remA remote address, null means all
     * @param remP remote port, 0 means all
     * @return false if successful, true if already found
     */
    public boolean listenStop(ipFwdIface locI, int locP, addrIP remA, int remP) {
        if (debugger.prtGenTraf) {
            logger.debug("del ifc=" + locI + " prt=" + locP);
        }
        prtGenServ ntry = srvrs.del(locI, remA, locP, remP);
        if (ntry == null) {
            return true;
        }
        return false;
    }

    /**
     * start one connection after it, have to wait until pipeline gets ready
     *
     * @param srv server that will be used
     * @param locI local interface, null means pick up one
     * @param locP local port, 0 means pick up one
     * @param remA remote address
     * @param remP remote port
     * @param name name of connection
     * @param pwd session password
     * @param ttl time to live
     * @return id reference of connection, null means error
     */
    public prtGenConn packetConnect(prtServP srv, ipFwdIface locI, int locP, addrIP remA, int remP, String name, String pwd, int ttl) {
        return anybodyConnect(srv, null, null, locI, locP, remA, remP, name, pwd, ttl);
    }

    /**
     * start one connection after it, have to wait until pipeline gets ready
     *
     * @param sample pipeline to clone from
     * @param locI local interface, null means pick up one
     * @param locP local port, 0 means pick up one
     * @param remA remote address
     * @param remP remote port
     * @param name name of connection
     * @param pwd password
     * @param ttl time to live
     * @return pipeline to access the connection, null if error happened
     */
    public pipeSide streamConnect(pipeLine sample, ipFwdIface locI, int locP, addrIP remA, int remP, String name, String pwd, int ttl) {
        prtGenConn cln = anybodyConnect(null, null, sample, locI, locP, remA, remP, name, pwd, ttl);
        if (cln == null) {
            return null;
        }
        return cln.pipeClient;
    }

    /**
     * stop one connection
     *
     * @param locI interface
     * @param locP local port
     * @param remA remote address
     * @param remP remote port
     * @return false on success, true on error
     */
    public boolean connectStop(ipFwdIface locI, int locP, addrIP remA, int remP) {
        prtGenConn cln = clnts.get(locI, remA, locP, remP);
        if (cln == null) {
            return true;
        }
        cln.setClosing();
        return false;
    }

    private boolean anybodyListen(prtServP upP, prtServS upS, pipeLine pip, ipFwdIface locI, int locP, addrIP remA, int remP, String name, String pwd, int ttl) {
        if (locP != 0) {
            if (testPortNumber(locP)) {
                return true;
            }
        }
        if (remP != 0) {
            if (testPortNumber(remP)) {
                return true;
            }
        }
        prtGenServ ntry = new prtGenServ();
        if (pip == null) {
            ntry.stream = false;
        } else {
            ntry.stream = true;
        }
        ntry.serverP = upP;
        ntry.serverS = upS;
        ntry.sample = pip;
        ntry.locP = locP;
        ntry.name = name;
        ntry.iface = locI;
        ntry.passwd = pwd;
        ntry.ttl = ttl;
        if (debugger.prtGenTraf) {
            logger.debug("add " + ntry);
        }
        return srvrs.add(locI, remA, locP, remP, ntry, ntry.name);
    }

    private prtGenConn anybodyConnect(prtServP upP, prtServS upS, pipeLine pip, ipFwdIface locI, int locP, addrIP remA, int remP, String nam, String pwd, int ttl) {
        if (testPortNumber(remP)) {
            return null;
        }
        if (locI == null) {
            locI = ipFwdTab.findSendingIface(fwdCore, remA);
        }
        if (locI == null) {
            return null;
        }
        if (locP < 1) {
            for (;;) {
                locP = getRandomPortNum();
                if (clnts.get(locI, remA, locP, remP) == null) {
                    break;
                }
            }
        }
        if (testPortNumber(locP)) {
            return null;
        }
        prtGenConn cln = new prtGenConn(this, upP, upS, pip, false, locI, locP, remA, remP, nam, pwd, ttl);
        if (connectionStart(cln, null)) {
            return null;
        }
        if (debugger.prtGenTraf) {
            logger.debug("connect " + cln);
        }
        if (!cln.register2lower()) {
            cln.notif.wakeup();
            return cln;
        }
        cln.deleteImmediately();
        return null;
    }

    /**
     * find a connection by interface and filled packet data
     *
     * @param rxIfc source interface
     * @param pck packet to give
     * @return connection handle
     */
    protected prtGenConn findOneConn(ipFwdIface rxIfc, packHolder pck) {
        return clnts.get(rxIfc, pck.IPsrc, pck.UDPtrg, pck.UDPsrc);
    }

    /**
     * accept incoming connection
     *
     * @param rxIfc source interface
     * @param pck packet to give
     * @return new connection entry
     */
    protected prtGenConn connectionAccept(ipFwdIface rxIfc, packHolder pck) {
        prtGenServ srv = null;
        if (srv == null) {
            srv = srvrs.get(rxIfc, pck.IPsrc, pck.UDPtrg, pck.UDPsrc);
        }
        if (srv == null) {
            srv = srvrs.get(null, pck.IPsrc, pck.UDPtrg, pck.UDPsrc);
        }
        if (srv == null) {
            srv = srvrs.get(rxIfc, pck.IPsrc, pck.UDPtrg, 0);
        }
        if (srv == null) {
            srv = srvrs.get(null, pck.IPsrc, pck.UDPtrg, 0);
        }
        if (srv == null) {
            srv = srvrs.get(rxIfc, null, pck.UDPtrg, pck.UDPsrc);
        }
        if (srv == null) {
            srv = srvrs.get(null, null, pck.UDPtrg, pck.UDPsrc);
        }
        if (srv == null) {
            srv = srvrs.get(rxIfc, null, pck.UDPtrg, 0);
        }
        if (srv == null) {
            srv = srvrs.get(null, null, pck.UDPtrg, 0);
        }
        if (srv == null) {
            cntr.drop(pck, counter.reasons.badTrgPort);
            connectionRefuse(rxIfc, pck);
            if (debugger.prtGenTraf) {
                logger.debug("noserver " + pck.IPsrc + " -> " + pck.UDPtrg);
            }
            return null;
        }
        prtGenConn cln = new prtGenConn(this, srv.serverP, srv.serverS, srv.sample, true, rxIfc, pck.UDPtrg, pck.IPsrc, pck.UDPsrc, srv.name, srv.passwd, srv.ttl);
        if (connectionStart(cln, pck)) {
            cntr.drop(pck, counter.reasons.badHdr);
            cln.deleteImmediately();
            return null;
        }
        if (cln.newConnectAccept()) {
            cntr.drop(pck, counter.reasons.denied);
            cln.deleteImmediately();
            connectionRefuse(rxIfc, pck);
            return null;
        }
        if (!cln.register2lower()) {
            return cln;
        }
        cln.setClosing();
        cln.deleteImmediately();
        cntr.drop(pck, counter.reasons.badTim);
        return null;
    }

    /**
     * send a packet to a connection
     *
     * @param ntry connection entry
     * @param pck packet to give
     */
    protected void connectionGotPack(prtGenConn ntry, packHolder pck) {
        connectionRcvd(ntry, pck);
        ntry.lastActivity = bits.getTime();
        ntry.notif.wakeup();
    }

    /**
     * do simple packet processing work attach to an existing connection create
     * new one if no one found
     *
     * @param rxIfc source interface
     * @param pck packet to give
     */
    protected void connectionSimpleWork(ipFwdIface rxIfc, packHolder pck) {
        prtGenConn ntry = findOneConn(rxIfc, pck);
        if (ntry != null) {
            connectionGotPack(ntry, pck);
            return;
        }
        ntry = connectionAccept(rxIfc, pck);
        if (ntry != null) {
            connectionGotPack(ntry, pck);
            return;
        }
        cntr.drop(pck, counter.reasons.badTrgPort);
    }

    /**
     * do simple state processing work
     *
     * @param iface interface
     * @param stat state
     */
    protected void connectionSimpleState(ipFwdIface iface, state.states stat) {
        for (int i = 0; i < clnts.size(); i++) {
            prtGenConn ntry = clnts.get(i);
            if (ntry == null) {
                continue;
            }
            if (ntry.iface != iface) {
                continue;
            }
            ntry.state2server(stat);
        }
    }

    /**
     * do simple error processing work
     *
     * @param err error happened
     * @param rtr reporting router
     * @param rxIfc source interface
     * @param pck packet to give
     */
    protected void connectionSimpleError(counter.reasons err, addrIP rtr, ipFwdIface rxIfc, packHolder pck) {
        pck.IPsrc.setAddr(pck.IPtrg);
        int i = pck.UDPsrc;
        pck.UDPsrc = pck.UDPtrg;
        pck.UDPtrg = i;
        prtGenConn ntry = findOneConn(rxIfc, pck);
        if (ntry == null) {
            return;
        }
        int lab = ipFwdEcho.getMplsExt(pck);
        connectionError(ntry, pck, rtr.copyBytes(), err, lab);
    }

    /**
     * count clients
     *
     * @param ifc interface
     * @param prt local port
     * @param adr remote address
     * @return number of clients
     */
    public int countClients(ipFwdIface ifc, int prt, addrIP adr) {
        return clnts.countClients(ifc, prt, adr);
    }

    /**
     * count clients
     *
     * @param ifc interface
     * @param prt local port
     * @param prf remote prefix
     * @return number of clients
     */
    public int countSubnet(ipFwdIface ifc, int prt, addrPrefix<addrIP> prf) {
        return clnts.countSubnet(ifc, prt, prf);
    }

    /**
     * got counter update
     *
     * @param ifc interface
     * @param adr peer address
     * @param rem remote port
     * @param loc local port
     * @param cntr counter
     */
    public void counterUpdate(ipFwdIface ifc, addrIP adr, int rem, int loc, counter cntr) {
        prtGenConn ntry = clnts.get(ifc, adr, loc, rem);
        if (ntry == null) {
            return;
        }
        counter old = ntry.hwCntr;
        ntry.hwCntr = cntr;
        if (old == null) {
            old = new counter();
        }
        if (old.compare(old, ntry.hwCntr) == 0) {
            return;
        }
        ntry.lastActivity = bits.getTime();
    }

}
