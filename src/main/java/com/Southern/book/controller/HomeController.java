package com.Southern.book.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String index() {
        try {
            // 获取当前认证的用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // 安全检查：确保authentication不为null
            if (authentication != null) {
                // 检查用户是否已认证并且不是匿名用户
                if (authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String && 
                        ((String)authentication.getPrincipal()).equals("anonymousUser"))) {
                    try {
                        // 安全地检查角色
                        boolean isAdmin = false;
                        boolean isUser = false;
                        
                        if (authentication.getAuthorities() != null) {
                            isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(role -> role != null && 
                                            "ROLE_ADMIN".equals(role.getAuthority()));
                            isUser = authentication.getAuthorities().stream()
                                    .anyMatch(role -> role != null && 
                                            "ROLE_USER".equals(role.getAuthority()));
                        }
                        
                        // 根据用户角色动态跳转到不同页面
                        if (isAdmin) {
                            // 如果是管理员，跳转到管理员仪表盘
                            return "redirect:/admin/dashboard";
                        } else if (isUser) {
                            // 如果是普通用户，跳转到个人借阅页面
                            return "redirect:/borrow/my-records";
                        }
                    } catch (Exception e) {
                        // 角色检查失败，记录日志并降级到图书列表
                        System.err.println("角色检查失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // 捕获所有异常，确保不会抛出到上层导致500错误
            System.err.println("首页重定向异常: " + e.getMessage());
        }
        
        // 安全默认值：无论什么情况都能返回一个有效的页面
        return "redirect:/books";
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}