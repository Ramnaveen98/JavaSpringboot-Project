package com.autobridge_api.auth.reset;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findByEmailAndPurposeAndUsedFalse(String email, String purpose);

    @Modifying
    @Query("update PasswordResetOtp p set p.used = true where p.email = :email and p.purpose = :purpose and p.used = false")
    int markActiveAsUsed(@Param("email") String email, @Param("purpose") String purpose);

    @Modifying
    @Query("delete from PasswordResetOtp p where p.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
