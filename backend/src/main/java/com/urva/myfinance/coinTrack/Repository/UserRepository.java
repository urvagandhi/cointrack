package com.urva.myfinance.coinTrack.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.urva.myfinance.coinTrack.Model.User;

public interface UserRepository extends MongoRepository<User, String> {

    public User findByUsername(String username);

    public User findByEmail(String email);
}
