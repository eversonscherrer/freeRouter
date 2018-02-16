package clnt;

import addr.addrEmpty;
import addr.addrIP;
import addr.addrType;
import ifc.ifcDn;
import ifc.ifcEther;
import ifc.ifcNull;
import ifc.ifcUp;
import ip.ipFwdIface;
import java.util.Comparator;
import pack.packHolder;
import prt.prtGenConn;
import prt.prtServP;
import prt.prtUdp;
import util.bits;
import util.counter;
import util.logger;
import util.state;

/**
 * locator id separation protocol (rfc6830) client
 *
 * @author matecsaba
 */
public class clntLisp implements Comparator<clntLisp>, Runnable, prtServP, ifcDn {

    /**
     * port number
     */
    public static final int portNum = 4341;

    /**
     * upper layer
     */
    public ifcUp upper = new ifcNull();

    /**
     * target of tunnel
     */
    public addrIP target;

    /**
     * remote port number
     */
    public int prtR;

    /**
     * local port number
     */
    public int prtL;

    /**
     * udp to use
     */
    public prtUdp udp;

    /**
     * source interface
     */
    public ipFwdIface fwdIfc = null;

    /**
     * sending ttl value, -1 means maps out
     */
    public int sendingTTL = 255;

    /**
     * sending tos value, -1 means maps out
     */
    public int sendingTOS = -1;

    /**
     * counter
     */
    public counter cntr = new counter();

    private prtGenConn conn;

    private boolean working = true;

    public String toString() {
        return "lisp to " + target;
    }

    public int compare(clntLisp o1, clntLisp o2) {
        return o1.target.compare(o1.target, o2.target);
    }

    public addrType getHwAddr() {
        return new addrEmpty();
    }

    public void setFilter(boolean promisc) {
    }

    public state.states getState() {
        return state.states.up;
    }

    public void closeDn() {
        clearState();
    }

    public void flapped() {
        clearState();
    }

    public void setUpper(ifcUp server) {
        upper = server;
        upper.setParent(this);
    }

    public counter getCounter() {
        return cntr;
    }

    public int getMTUsize() {
        return 1400;
    }

    public long getBandwidth() {
        return 4000000;
    }

    public void sendPack(packHolder pck) {
        if (conn == null) {
            return;
        }
        cntr.tx(pck);
        pck.merge2beg();
        if (ifcEther.stripEtherType(pck)) {
            cntr.drop(pck, counter.reasons.badProto);
            return;
        }
        pck.msbPutD(0, 0); // flags + nonce
        pck.msbPutD(4, 0); // istance + lsb
        pck.putSkip(8);
        pck.merge2beg();
        pck.putDefaults();
        conn.send2net(pck);
    }

    /**
     * start connection
     */
    public void workStart() {
        new Thread(this).start();
    }

    /**
     * stop connection
     */
    public void workStop() {
        working = false;
        clearState();
    }

    public void run() {
        for (;;) {
            if (!working) {
                break;
            }
            try {
                clearState();
                workDoer();
            } catch (Exception e) {
                logger.traceback(e);
            }
            clearState();
            bits.sleep(1000);
        }
    }

    private void workDoer() {
        if (prtR == 0) {
            prtR = portNum;
        }
        if (prtL == 0) {
            prtL = portNum;
        }
        conn = udp.packetConnect(this, fwdIfc, prtL, target, prtR, "lisp", null, -1);
        if (conn == null) {
            return;
        }
        conn.timeout = 120000;
        conn.sendTOS = sendingTOS;
        conn.sendTTL = sendingTTL;
        for (;;) {
            bits.sleep(1000);
            if (!working) {
                return;
            }
            if (conn.txBytesFree() < 0) {
                return;
            }
        }
    }

    private void clearState() {
        if (conn != null) {
            conn.setClosing();
        }
    }

    public void closedInterface(ipFwdIface ifc) {
    }

    public boolean datagramAccept(prtGenConn id) {
        return true;
    }

    public void datagramReady(prtGenConn id) {
    }

    public void datagramClosed(prtGenConn id) {
    }

    public void datagramWork(prtGenConn id) {
    }

    public boolean datagramRecv(prtGenConn id, packHolder pck) {
        pck.getSkip(8);
        cntr.rx(pck);
        int i = ifcEther.guessEtherType(pck);
        if (i < 0) {
            cntr.drop(pck, counter.reasons.badVer);
            return true;
        }
        pck.msbPutW(0, i);
        i = pck.headSize();
        pck.putSkip(2);
        pck.mergeHeader(-1, i);
        upper.recvPack(pck);
        return false;
    }

}
