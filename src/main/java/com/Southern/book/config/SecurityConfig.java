package com.Southern.book.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.Southern.book.entity.User;
import com.Southern.book.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            System.out.println("=== 登录调试 ===");
            System.out.println("尝试登录用户: " + username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        System.out.println("用户不存在: " + username);
                        return new UsernameNotFoundException("用户不存在: " + username);
                    });

            System.out.println("数据库中找到用户: " + user.getUsername());
            System.out.println("用户启用状态: " + user.isEnabled());
            System.out.println("密码哈希: " + user.getPassword());
            System.out.println("用户角色: " + user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toList()));

            // 关键：测试密码验证
            String testPassword = "admin123"; // 您输入的密码
            boolean passwordMatch = passwordEncoder().matches(testPassword, user.getPassword());
            System.out.println("密码验证结果: " + passwordMatch);
            System.out.println("测试密码: " + testPassword);

            if (!passwordMatch) {
                System.out.println("❌ 密码不匹配！");
                System.out.println("存储的哈希: " + user.getPassword());
                System.out.println("输入密码的哈希: " + passwordEncoder().encode(testPassword));
            }

            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    user.isEnabled(),
                    true, true, true,
                    user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                            .collect(Collectors.toList())
            );

            System.out.println("UserDetails创建成功: " + userDetails.isEnabled());
            return userDetails;
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

                // 修改后
                .authorizeHttpRequests(authz -> authz
                        // 公开访问的路径
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/uploads/**", "/api/public/**").permitAll()
                        // 图书删除操作需要ADMIN角色
                        .requestMatchers("/books/delete/**").hasRole("ADMIN")
                        // 图书编辑操作需要ADMIN角色
                        .requestMatchers("/books/edit/**", "/books/update/**").hasRole("ADMIN")
                        // 图书添加操作需要ADMIN角色
                        .requestMatchers("/books/add").hasRole("ADMIN")
                        // 需要 USER 或 ADMIN 角色的其他图书路径
                        .requestMatchers("/books/**", "/api/books/**").hasAnyRole("USER", "ADMIN")
                        // 需要 ADMIN 角色的路径
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true) // 添加true参数确保总是重定向到首页
                        .failureUrl("/login?error=true") // 明确指定失败URL
                        .usernameParameter("username") // 明确指定用户名参数
                        .passwordParameter("password") // 明确指定密码参数
                        .permitAll()
                )
                .logout(logout -> logout
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf.disable()); // 在生产环境中应启用CSRF保护

        return http.build();
    }
}