package spyke.monitor.pcap4j.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spyke.database.model.Upload;
import spyke.database.repository.UploadRepository;

import java.util.List;

@Service
public class StoreUpload implements Runnable{

    @Autowired
    private UploadRepository uploadRepository;
    public List<Upload> uploadList;

    @Override
    public void run() {
        if(uploadList.size()!=0){
            Upload upload = uploadList.get(0);
            for(int i=1; i<uploadList.size(); i++) {
                upload.addData(uploadList.get(i).getData());
            }
            uploadRepository.save(upload);
        }
    }
}
