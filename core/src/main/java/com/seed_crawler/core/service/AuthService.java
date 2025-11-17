package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.Auth;

public interface AuthService {
    Auth.LoginResult login(String loginId, String password);
}
