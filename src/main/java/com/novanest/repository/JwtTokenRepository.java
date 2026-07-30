package com.novanest.repository;

import com.novanest.model.JwtToken;
import com.novanest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Integer> {

    Optional<JwtToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUser(User user);
}
