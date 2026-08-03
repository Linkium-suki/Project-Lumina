package com.lumina.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByDeviceId(String deviceId);

    Optional<User> findByTokenHash(String tokenHash);
}
