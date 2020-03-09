package spyke.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spyke.database.model.Download;

@Repository
public interface DownloadRepository extends JpaRepository<Download, String> {
}
