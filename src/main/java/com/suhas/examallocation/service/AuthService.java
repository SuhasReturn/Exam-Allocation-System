package com.suhas.examallocation.service;

import com.suhas.examallocation.dto.LoginRequest;
import com.suhas.examallocation.dto.LoginResponse;
import com.suhas.examallocation.model.Role;
import com.suhas.examallocation.model.UserAccount;
import com.suhas.examallocation.repository.UserAccountRepository;
import com.suhas.examallocation.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates the user and returns a JWT token.
     *
     * The AuthenticationManager delegates to CustomUserDetailsService
     * to load the user, then compares the password using BCrypt.
     * If either the username doesn't exist or the password is wrong,
     * Spring throws BadCredentialsException.
     */
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));
        } catch (BadCredentialsException ex) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        UserAccount account = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        String token = jwtTokenProvider.generateToken(
                account.getUsername(),
                account.getRole().name());

        return new LoginResponse(token, account.getRole().name(), account.getUsername());
    }

    /**
     * Creates a new user account. Used internally to seed the admin
     * and can be extended for user registration later.
     */
    public UserAccount createAccount(String username, String rawPassword, Role role) {
        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username " + username + " already exists");
        }

        UserAccount account = new UserAccount(
                username,
                passwordEncoder.encode(rawPassword),
                role);

        return userAccountRepository.save(account);
    }
}
