package com.urva.myfinance.coinTrack.Repository;

import com.urva.myfinance.coinTrack.Model.UpstoxAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UpstoxAccountRepository extends MongoRepository<UpstoxAccount, String> {
    Optional<UpstoxAccount> findByAppUserId(String appUserId);

    Optional<UpstoxAccount> findByUserId(String userId);

    Optional<UpstoxAccount> findByAccessToken(String accessToken);
}