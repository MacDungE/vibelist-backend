package org.example.vibelist.domain.auth.repository;

import org.example.vibelist.domain.auth.entity.DevAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevAuthTokenRepository extends JpaRepository <DevAuthToken, Long>{
}
