package com.Southern.book.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String index() {
        // 获取当前认证的用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 检查用户是否已认证并且不是匿名用户
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            // 根据用户角色动态跳转到不同页面
            if (authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))) {
                // 如果是管理员，跳转到管理员仪表盘
                return "redirect:/admin/dashboard";
            } else if (authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_USER"))) {
                // 如果是普通用户，跳转到个人借阅页面
                return "redirect:/borrow/my-records";
            }
        }
        
        // 未认证用户或其他情况也直接跳转到图书列表，避免显示无用的首页
        return "redirect:/books";
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}