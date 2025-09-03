package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailAndPassword(String email, String password);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmailAndPassword(String email, String password);
    
    @Query("SELECT u FROM User u WHERE u.username LIKE %:username%")
    Page<User> searchByUsername(@Param("username") String username, Pageable pageable);
}
