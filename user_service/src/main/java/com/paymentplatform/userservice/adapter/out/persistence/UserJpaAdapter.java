package com.paymentplatform.userservice.adapter.out.persistence;

import com.paymentplatform.userservice.application.port.out.UserRepositoryPort;
import com.paymentplatform.userservice.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .build();
        
        UserEntity savedEntity = repository.save(entity);
        
        return User.builder()
                .id(savedEntity.getId())
                .username(savedEntity.getUsername())
                .email(savedEntity.getEmail())
                .status(savedEntity.getStatus())
                .build();
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(entity -> User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .build());
    }
}
