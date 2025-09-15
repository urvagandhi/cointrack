package com.urva.myfinance.coinTrack.Repository;

import com.urva.myfinance.coinTrack.Model.ZerodhaAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ZerodhaAccountRepository extends MongoRepository<ZerodhaAccount, String> {
    Optional<ZerodhaAccount> findByAppUserId(String appUserId);

    Optional<ZerodhaAccount> findByKiteUserId(String kiteUserId);
}