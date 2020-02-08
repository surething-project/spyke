package spyke.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spyke.database.model.Device;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    @Query(value = "select d from Device d where d.ip = :ip")
    List<Device> findByIp(@Param("ip") String ip);
}
