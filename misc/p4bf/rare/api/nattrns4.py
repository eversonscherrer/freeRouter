from ..bf_gbl_env.var_env import *


def writeNatTrnsRules4(
    self, op_type, vrf, proto, osa, osp, ota, otp, nsa, nsp, nta, ntp
):
    if self.nat == False:
        return
    tbl_global_path = "ig_ctl.ig_ctl_nat"
    tbl_name = "%s.tbl_ipv4_nat_trns" % (tbl_global_path)
    tbl_action_name = "%s.act_rewrite_ipv4prt%s" % (tbl_global_path, str(proto))
    key_field_list = [
        gc.KeyTuple("ig_md.vrf", vrf),
        gc.KeyTuple("hdr.ipv4.protocol", proto),
        gc.KeyTuple("hdr.ipv4.src_addr", osa),
        gc.KeyTuple("hdr.ipv4.dst_addr", ota),
        gc.KeyTuple("ig_md.layer4_srcprt", osp),
        gc.KeyTuple("ig_md.layer4_dstprt", otp),
    ]
    data_field_list = [
        gc.DataTuple("srcadr", nsa),
        gc.DataTuple("trgadr", nta),
        gc.DataTuple("srcprt", nsp),
        gc.DataTuple("trgprt", ntp),
    ]
    key_annotation_fields = {
        "hdr.ipv4.src_addr": "ipv4",
        "hdr.ipv4.dst_addr": "ipv4",
    }
    data_annotation_fields = {
        "srcadr": "ipv4",
        "trgadr": "ipv4",
    }
    self._processEntryFromControlPlane(
        op_type,
        tbl_name,
        key_field_list,
        data_field_list,
        tbl_action_name,
        key_annotation_fields,
        data_annotation_fields,
    )
