package io.hexlet.spring.service;

import java.util.List;

import io.hexlet.spring.dto.UserRequest;
import io.hexlet.spring.dto.UserResponse;

public interface UserService {

    List<UserResponse> getAll();

    UserResponse create(UserRequest request);

    /** @throws io.hexlet.spring.exception.ResourceNotFoundException if there is no such user */
    UserResponse findById(String userId);
}
