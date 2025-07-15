package org.example.vibelist.domain.integration.repository;

import org.example.vibelist.domain.integration.entity.DevAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevAuthTokenRepository extends JpaRepository <DevAuthToken, Long>{
    DevAuthToken findByName(String name);
}
