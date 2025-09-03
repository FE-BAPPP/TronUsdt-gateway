package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.TokenSweep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenSweepRepository extends JpaRepository<TokenSweep, Long> {

    List<TokenSweep> findByStatus(TokenSweep.SweepStatus status);

    Optional<TokenSweep> findBySweepTxHash(String sweepTxHash);

    List<TokenSweep> findByChildIndexAndStatus(Integer childIndex, TokenSweep.SweepStatus status);
}
