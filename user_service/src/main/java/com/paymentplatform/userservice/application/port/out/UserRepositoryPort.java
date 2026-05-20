package com.paymentplatform.userservice.application.port.out;

import com.paymentplatform.userservice.domain.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
}
