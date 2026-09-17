package com.ussdauto.api.repository;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.enums.DeviceStatus;
import com.ussdauto.api.domain.enums.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByApiKey(String apiKey);

    List<Device> findByStatutAndLastSeenAtBefore(DeviceStatus statut, Instant cutoff);

    @Query("""
            select d from Device d
            where d.statut = :statut
              and exists (
                  select 1 from DeviceSimSlot s
                  where s.deviceId = d.id and s.operator = :operator and s.actif = true
              )
            """)
    List<Device> findOnlineWithActiveSlotForOperator(@Param("statut") DeviceStatus statut, @Param("operator") Operator operator);
}
