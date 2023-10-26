package com.dev.junit.service;

import com.dev.junit.dto.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    @Test
    void usersEmptyIfNoUserAdded() {
        var userService = new UserService();
        var users = userService.getAll();
        //assertTrue(users.isEmpty());
        assertFalse(users.isEmpty(), () -> "User list should be empty");

        // input -> [box == func] -> actual output
    }
}
