package com.UsdtWallet.UsdtWallet.repository;


import com.UsdtWallet.UsdtWallet.model.entity.HdMasterWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HdMasterWalletRepository extends JpaRepository<HdMasterWallet, Long> {
    Optional<HdMasterWallet> findTopByOrderByIdDesc();
}