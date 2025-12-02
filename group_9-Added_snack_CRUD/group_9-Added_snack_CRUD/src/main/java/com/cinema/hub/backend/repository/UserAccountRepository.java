package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Integer>, JpaSpecificationExecutor<UserAccount> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByPhone(String phone);

    long countByRole_NameIgnoreCaseAndActiveTrue(String roleName);

    boolean existsByIdAndActiveTrue(Integer id);
}
