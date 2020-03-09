package spyke.pcap4j.task;

import org.springframework.stereotype.Service;
import spyke.database.model.Download;

import java.util.List;

@Service
public class StoreDownload implements Runnable{

    private List<Download> downloadList;
    @Override
    public void run() {

    }
}
