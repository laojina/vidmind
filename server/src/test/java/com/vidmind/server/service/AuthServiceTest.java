package com.vidmind.server.service;

import com.vidmind.server.dto.AuthRequest;
import com.vidmind.server.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AuthServiceTest {
    @Test
    void rejectsOversizedLoginPasswordBeforeCacheOrDatabaseAccess() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        UserMapper users = mock(UserMapper.class);
        AuthService service = new AuthService(redis, users);
        assertEquals(400, service.login(new AuthRequest("valid_user", "x".repeat(129), "")).code());
        verifyNoInteractions(redis, users);
    }
}
