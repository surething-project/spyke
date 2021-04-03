package spyke.monitor.pcap4j.manage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spyke.database.model.Download;
import spyke.database.model.Upload;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Service
public class PacketManager {
    private Logger logger = LoggerFactory.getLogger(PacketManager.class);
    private HashMap<PacketId, List<Download>> downloadPacketManager = new HashMap<PacketId, List<Download>>();
    private HashMap<PacketId, List<Upload>> uploadPacketManager = new HashMap<PacketId, List<Upload>>();
    public HashMap<PacketId, List<Download>> getDownloadPacketManager(){
        return downloadPacketManager;
    }
    public HashMap<PacketId, List<Upload>> getUploadPacketManager(){
        return uploadPacketManager;
    }
    public void addDownloadPacket(String ip, String hour, Download download){
        PacketId packetId = new PacketId(ip, hour);
        if(!downloadPacketManager.containsKey(packetId)){
            List<Download> q = new LinkedList<>();
            if(q.add(download))
                downloadPacketManager.put(packetId, q);
            else
                logger.error("Download List add failed!");
        } else {
            List<Download> q = downloadPacketManager.get(packetId);
            if(q.add(download))
                downloadPacketManager.put(packetId, q);
            else
                logger.error("Download List add failed!");
        }
    }
    public void addUploadPacket(String ip, String hour, Upload upload){
        PacketId packetId = new PacketId(ip, hour);
        if(!uploadPacketManager.containsKey(packetId)){
            List<Upload> q = new LinkedList<>();
            if(q.add(upload))
                uploadPacketManager.put(packetId, q);
            else
                logger.error("Upload List add failed!");
        } else {
            List<Upload> q = uploadPacketManager.get(packetId);
            if(q.add(upload))
                uploadPacketManager.put(packetId, q);
            else
                logger.error("Upload List add failed!");
        }
    }
    public List<Download> retrieveDownloadList(String ip, String hour){
        PacketId packetId = new PacketId(ip, hour);
        List<Download> q = downloadPacketManager.get(packetId);
        downloadPacketManager.remove(packetId);
        return q;
    }
    public List<Upload> retrieveUploadList(String ip, String hour){
        PacketId packetId = new PacketId(ip, hour);
        List<Upload> q = uploadPacketManager.get(packetId);
        uploadPacketManager.remove(packetId);
        return q;
    }
}
