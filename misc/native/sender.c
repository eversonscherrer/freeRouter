#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <pcap.h>

char *ifaceName;
pcap_t *ifacePcap;
FILE *fil;
int packLen;
unsigned char packBuf[16 * 1024];

void err(char*buf) {
    printf("%s\n", buf);
    exit(1);
}

void gotRawPack(unsigned char*dummyparameter, const struct pcap_pkthdr *hdr, unsigned char *dat) {
}

int main(int argc, char **argv) {
    char errbuf[PCAP_ERRBUF_SIZE + 1];

    if (argc < 2) err("using: <iface> <packet>");

    printf("pcap version: %s\n", pcap_lib_version());

    ifaceName = malloc(1024);
    strcpy(ifaceName, argv[1]);
    printf("opening interface %s\n", ifaceName);

    ifacePcap = pcap_create(ifaceName, errbuf);
    if (ifacePcap == NULL) err("unable to open interface");
    if (pcap_set_snaplen(ifacePcap, 65536) < 0) err("unable to set snaplen");
    if (pcap_set_promisc(ifacePcap, 1) < 0) err("unable to set promisc");
    if (pcap_set_timeout(ifacePcap, 2) < 0) err("unable to set timeout");
    if (pcap_activate(ifacePcap) < 0) err("activation failed");
    if (pcap_setdirection(ifacePcap, PCAP_D_IN) < 0) err("unable to set direction");

    fil = fopen(argv[2], "rb");
    if (!fil) err("error opening file");
    packLen = fread(packBuf, 1, sizeof packBuf, fil);
    if (packLen < 1) err("error reading file");
    fclose(fil);
    printf("sending packets with length %i\n", packLen);

    for (;;) {
        pcap_sendpacket(ifacePcap, packBuf, packLen);
    }

}
