package org.example.vibelist.domain.user.repository;

import org.example.vibelist.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    /**
     * 이메일로 사용자 프로필 조회
     */
    Optional<UserProfile> findByEmail(String email);
    
    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
    
    /**
     * 전화번호로 사용자 프로필 조회
     */
    Optional<UserProfile> findByPhone(String phone);
    
    /**
     * 전화번호 존재 여부 확인
     */
    boolean existsByPhone(String phone);
    
    /**
     * 이름으로 사용자 프로필 조회
     */
    List<UserProfile> findByNameContaining(String name);
    
    /**
     * 이름으로 사용자 프로필 조회 (User fetch join)
     */
    @Query("SELECT p FROM UserProfile p JOIN FETCH p.user WHERE p.name LIKE %:name%")
    List<UserProfile> findByNameContainingWithUser(@Param("name") String name);


    @Query("SELECT p FROM UserProfile p JOIN FETCH p.user WHERE p.user.username LIKE %:username%")
    UserProfile findByUsernameContainingWithUser(@Param("username") String username);
}