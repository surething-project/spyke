package spyke.engine.pcap4j.packet;

import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.TcpPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TcpPacketHandler {
    //TODO analyze tcp packets
    private final Map<TcpPort, TcpSession> sessions = new HashMap<TcpPort, TcpSession>();

    // TODO code below are not used
    public synchronized void handleTcp(final org.pcap4j.packet.TcpPacket tcp) {
        try {
            if (tcp == null) {
                System.out.println("Tcp packet null!!!");
                return;
            }

            boolean isToServer = true;
            TcpPort port = tcp.getHeader().getSrcPort();
            if (port.value() == 443) {
                port = tcp.getHeader().getDstPort();
                isToServer = false;
            }

            final boolean syn = tcp.getHeader().getSyn();
            final boolean fin = tcp.getHeader().getFin();

            if (syn) {
                final TcpSession session;
                if (isToServer) {
                    session = new TcpSession();
                    this.sessions.put(port, session);
                } else {
                    session = this.sessions.get(port);
                }

                final long seq = tcp.getHeader().getSequenceNumberAsLong();
                session.setSeqNumOffset(isToServer, seq + 1L);

            } else if (fin) {
                final TcpSession session = this.sessions.get(port);
                session.getPackets(isToServer).add(tcp);

                final byte[] reassembledPayload
                        = doReassemble(
                        session.getPackets(isToServer),
                        session.getSeqNumOffset(isToServer),
                        tcp.getHeader().getSequenceNumberAsLong(),
                        tcp.getPayload().length()
                );

                final int len = reassembledPayload.length;
              /*
              for (int i = 0; i < len;) {
                try {
                  TlsPacket tls = TlsPacket.newPacket(reassembledPayload, i, len - i);
                  System.out.println(tls);
                  i += tls.length();
                } catch (IllegalRawDataException e) {
                  e.printStackTrace();
                }
              }
              */
            } else {
                if (tcp.getPayload() != null && tcp.getPayload().length() != 0) {
                    final TcpSession session = this.sessions.get(port);
                    session.getPackets(isToServer).add(tcp);
                }
            }
        } catch (final Exception e) {
            System.out.println("Exception -> " + e.getMessage());
        }
    }

    private static byte[] doReassemble(
            final List<TcpPacket> packets, final long seqNumOffset, final long lastSeqNum, final int lastDataLen
    ) {
        // This cast is not safe.
        // The sequence number is unsigned int and so
        // (int) (lastSeqNum - seqNumOffset) may be negative.
        final byte[] buffer = new byte[(int) (lastSeqNum - seqNumOffset) + lastDataLen];

        for (final TcpPacket p : packets) {
            final byte[] payload = p.getPayload().getRawData();
            final long seq = p.getHeader().getSequenceNumberAsLong();
            System.arraycopy(payload, 0, buffer, (int) (seq - seqNumOffset), payload.length);
        }

        return buffer;
    }

    public static final class TcpSession {

        private final List<TcpPacket> packetsToServer = new ArrayList<TcpPacket>();
        private final List<TcpPacket> packetsToClient = new ArrayList<TcpPacket>();
        private long serverSeqNumOffset;
        private long clientSeqNumOffset;

        public List<TcpPacket> getPackets(final boolean toServer) {
            if (toServer) {
                return this.packetsToServer;
            } else {
                return this.packetsToClient;
            }
        }

        public long getSeqNumOffset(final boolean toServer) {
            if (toServer) {
                return this.clientSeqNumOffset;
            } else {
                return this.serverSeqNumOffset;
            }
        }

        public void setSeqNumOffset(final boolean toServer, final long seqNumOffset) {
            if (toServer) {
                this.clientSeqNumOffset = seqNumOffset;
            } else {
                this.serverSeqNumOffset = seqNumOffset;
            }
        }

    }
}
