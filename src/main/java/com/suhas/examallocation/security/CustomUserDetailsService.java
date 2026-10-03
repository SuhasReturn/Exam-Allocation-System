package com.suhas.examallocation.security;

import com.suhas.examallocation.model.UserAccount;
import com.suhas.examallocation.repository.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security needs a UserDetailsService to load user info
 * during authentication. This class bridges our UserAccount entity
 * to Spring Security's UserDetails interface.
 *
 * The "ROLE_" prefix is a Spring Security convention — it expects
 * authorities to start with "ROLE_" when you use hasRole("ADMIN").
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found with username " + username));

        return new User(
                account.getUsername(),
                account.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
        );
    }
}
