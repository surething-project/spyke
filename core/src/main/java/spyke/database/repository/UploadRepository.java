package spyke.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spyke.database.model.Upload;

@Repository
public interface UploadRepository extends JpaRepository<Upload, String> {
}
