package org.example.vibelist.domain.integration.repository;

import org.example.vibelist.domain.integration.entity.DevIntegrationTokenInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevIntegrationTokenInfoRepository extends JpaRepository <DevIntegrationTokenInfo, Long>{
    DevIntegrationTokenInfo findByName(String name);
}
