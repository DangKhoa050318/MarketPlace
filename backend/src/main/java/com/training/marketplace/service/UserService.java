package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateUserRequest;
import com.training.marketplace.dto.request.UpdateUserRequest;
import com.training.marketplace.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponse> getAll(Pageable pageable);

    UserResponse getById(Long id);

    UserResponse create(CreateUserRequest request);

    UserResponse update(Long id, UpdateUserRequest request);

    void delete(Long id);
}
