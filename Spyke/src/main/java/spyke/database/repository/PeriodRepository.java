package spyke.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spyke.database.model.Period;
import spyke.database.model.PeriodId;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Date;

@Repository
@Transactional
public interface PeriodRepository extends JpaRepository<Period, PeriodId> {
    @Query(value = "select p from Period p where p.id.device.mac = :device_mac")
    List<Period> findAllByMac(@Param("device_mac") String device_mac);
    @Query("select p from Period p where p.id.device.mac = :device_mac and p.id.startTime <= :paramtime")
    List<Period> findAllByTimeStartBefore(@Param("device_mac") String device_mac, @Param("paramtime") Date paramtime);
    @Query("select p from Period p where p.id.device.mac = :device_mac and p.id.startTime >= :paramtime")
    List<Period> findAllByTimeStartAfter(@Param("device_mac") String device_mac, @Param("paramtime") Date paramtime);
    @Query("select p from Period p where p.id.device.mac = :device_mac and p.id.startTime >= :paramtime and p.id.endTime <= :paramtime")
    List<Period> findAllByTime(@Param("device_mac") String device_mac, @Param("paramtime") Date paramtime);
    @Modifying
    @Query("delete from Period p where p.id.device.mac = :device_mac")
    void deleteByAllMac(@Param("device_mac") String device_mac);
}
