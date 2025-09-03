package com.UsdtWallet.UsdtWallet.repository;

import com.UsdtWallet.UsdtWallet.model.entity.ChildWalletPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChildWalletPoolRepository extends JpaRepository<ChildWalletPool, UUID> {

    Optional<ChildWalletPool> findByAddress(String address);

    Optional<ChildWalletPool> findByDerivationIndex(Integer derivationIndex);

    Optional<ChildWalletPool> findByUserId(UUID userId);

    Optional<ChildWalletPool> findFirstByStatusOrderByIdAsc(ChildWalletPool.WalletStatus status);

    long countByStatus(ChildWalletPool.WalletStatus status);

    @Query("SELECT MAX(c.derivationIndex) FROM ChildWalletPool c")
    Optional<Integer> findMaxDerivationIndex();

    @Query("SELECT c.address FROM ChildWalletPool c")
    List<String> findAllAddresses();
}