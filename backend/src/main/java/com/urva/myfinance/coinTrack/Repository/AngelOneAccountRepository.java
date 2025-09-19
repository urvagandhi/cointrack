package com.urva.myfinance.coinTrack.Repository;

import com.urva.myfinance.coinTrack.Model.AngelOneAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface AngelOneAccountRepository extends MongoRepository<AngelOneAccount, String> {
    Optional<AngelOneAccount> findByAppUserId(String appUserId);

    Optional<AngelOneAccount> findByAngelClientId(String angelClientId);

    Optional<AngelOneAccount> findByUserId(String userId);
}