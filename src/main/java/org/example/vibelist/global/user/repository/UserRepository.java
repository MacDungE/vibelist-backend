package org.example.vibelist.global.user.repository;

import org.example.vibelist.global.constants.Role;
import org.example.vibelist.global.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 사용자명으로 사용자 조회 (Profile 포함) - EntityGraph 사용
     */
    @EntityGraph(attributePaths = {"userProfile"})
    Optional<User> findByUsername(String username);
    
    /**
     * 사용자명으로 사용자 조회 (Profile 포함) - JPQL 사용
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfile WHERE u.username = :username")
    Optional<User> findByUsernameWithProfile(@Param("username") String username);
    
    /**
     * 사용자명 존재 여부 확인
     */
    boolean existsByUsername(String username);
    
    /**
     * 역할별 사용자 조회 (Profile 포함) - EntityGraph 사용
     */
    @EntityGraph(attributePaths = {"userProfile"})
    List<User> findByRole(Role role);
    
    /**
     * 역할별 사용자 조회 (Profile 포함) - JPQL 사용
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfile WHERE u.role = :role")
    List<User> findByRoleWithProfile(@Param("role") Role role);
    
    /**
     * 모든 사용자 조회 (Profile 포함) - EntityGraph 사용
     */
    @EntityGraph(attributePaths = {"userProfile"})
    List<User> findAll();
    
    /**
     * 모든 사용자 조회 (Profile 포함) - JPQL 사용
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfile")
    List<User> findAllWithProfile();
    
    /**
     * ID로 사용자 조회 (Profile 포함) - EntityGraph 사용
     */
    @EntityGraph(attributePaths = {"userProfile"})
    Optional<User> findById(Long id);
    
    /**
     * ID로 사용자 조회 (Profile 포함) - JPQL 사용
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfile WHERE u.id = :id")
    Optional<User> findByIdWithProfile(@Param("id") Long id);
} 