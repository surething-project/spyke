package spyke.engine.pcap4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spyke.database.model.Download;
import spyke.database.model.Upload;
import spyke.engine.pcap4j.packet.PacketId;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Service
public class PacketManager {
    private final Logger logger = LoggerFactory.getLogger(PacketManager.class);
    private final HashMap<PacketId, List<Download>> downloadPacketManager = new HashMap<PacketId, List<Download>>();
    private final HashMap<PacketId, List<Upload>> uploadPacketManager = new HashMap<PacketId, List<Upload>>();

    public HashMap<PacketId, List<Download>> getDownloadPacketManager() {
        return this.downloadPacketManager;
    }

    public HashMap<PacketId, List<Upload>> getUploadPacketManager() {
        return this.uploadPacketManager;
    }

    public void addDownloadPacket(final String ip, final String hour, final Download download) {
        final PacketId packetId = new PacketId(ip, hour);
        if (!this.downloadPacketManager.containsKey(packetId)) {
            final List<Download> q = new LinkedList<>();
            if (q.add(download))
                this.downloadPacketManager.put(packetId, q);
            else
                this.logger.error("Download List add failed!");
        } else {
            final List<Download> q = this.downloadPacketManager.get(packetId);
            if (q.add(download))
                this.downloadPacketManager.put(packetId, q);
            else
                this.logger.error("Download List add failed!");
        }
    }

    public void addUploadPacket(final String ip, final String hour, final Upload upload) {
        final PacketId packetId = new PacketId(ip, hour);
        if (!this.uploadPacketManager.containsKey(packetId)) {
            final List<Upload> q = new LinkedList<>();
            if (q.add(upload))
                this.uploadPacketManager.put(packetId, q);
            else
                this.logger.error("Upload List add failed!");
        } else {
            final List<Upload> q = this.uploadPacketManager.get(packetId);
            if (q.add(upload))
                this.uploadPacketManager.put(packetId, q);
            else
                this.logger.error("Upload List add failed!");
        }
    }

    public List<Download> retrieveDownloadList(final String ip, final String hour) {
        final PacketId packetId = new PacketId(ip, hour);
        final List<Download> q = this.downloadPacketManager.get(packetId);
        this.downloadPacketManager.remove(packetId);
        return q;
    }

    public List<Upload> retrieveUploadList(final String ip, final String hour) {
        final PacketId packetId = new PacketId(ip, hour);
        final List<Upload> q = this.uploadPacketManager.get(packetId);
        this.uploadPacketManager.remove(packetId);
        return q;
    }
}
