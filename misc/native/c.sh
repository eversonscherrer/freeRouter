#!/bin/sh
CC="clang"                              #clang
CC="gcc"                                #gcc

compileFile()
{
echo compiling $1.
$CC $4 -o../../binTmp/$1.bin $2 $1.c $3
touch -d "2010-01-01 00:00:00" ../../binTmp/$1.bin
}

compileFile p4dpdk "-I /usr/include/dpdk/ -I /usr/include/x86_64-linux-gnu/dpdk" "-lpthread -lcrypto -lrte_eal -lrte_mempool -lrte_mbuf -lrte_ring -lrte_ethdev" "-march=corei7 -O3"

for fn in p4emu p4pkt; do
  compileFile $fn "" "-lpthread -lpcap -lcrypto" "-O3"
  done

for fn in pcapInt pcap2pcap sender; do
  compileFile $fn "" "-lpthread -lpcap" "-O3"
  done

for fn in mapInt rawInt tapInt bundle vlan hdlcInt stdLin ttyLin modem; do
  compileFile $fn "" "-lpthread" "-O3"
  done

