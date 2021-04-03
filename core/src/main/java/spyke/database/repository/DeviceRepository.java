package spyke.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spyke.database.model.Device;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    /**
     * Gets list of requested device by given ip.
     *
     * @param ip The ip of given device.
     * @return the device within list.
     */
    @Query(value = "select d from Device d where d.ip = :ip")
    Optional<Device> findByIp(@Param("ip") String ip);
}
