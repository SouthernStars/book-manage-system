package com.Southern.book.controller;

import com.Southern.book.entity.Role;
import com.Southern.book.entity.User;
import com.Southern.book.repository.RoleRepository;
import com.Southern.book.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public String listUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "status", required = false) String status,
            Model model) {

        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        
        // 获取用户列表（这里简化处理，实际应该根据keyword、role和status进行过滤）
        Page<User> usersPage = userService.getAllUsers(pageable);
        
        // 如果有搜索条件，进行过滤（实际应该在Service层实现更复杂的过滤逻辑）
        List<User> filteredUsers = usersPage.getContent();
        if (keyword != null && !keyword.isEmpty()) {
            final String lowerKeyword = keyword.toLowerCase();
            filteredUsers = filteredUsers.stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(lowerKeyword) ||
                            user.getFullName().toLowerCase().contains(lowerKeyword) ||
                            (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerKeyword)))
                    .collect(Collectors.toList());
        }
        
        if (role != null && !role.isEmpty()) {
            final String roleUpper = role.toUpperCase();
            filteredUsers = filteredUsers.stream()
                    .filter(user -> user.getRoles().stream()
                            .anyMatch(r -> r.getName().equals(roleUpper)))
                    .collect(Collectors.toList());
        }
        
        if (status != null && !status.isEmpty()) {
            final boolean isEnabled = "ENABLED".equals(status.toUpperCase());
            filteredUsers = filteredUsers.stream()
                    .filter(user -> user.isEnabled() == isEnabled)
                    .collect(Collectors.toList());
        }
        
        // 设置分页信息
        int totalPages = usersPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        
        // 添加模型属性
        model.addAttribute("users", filteredUsers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", usersPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("startIndex", page * size);
        model.addAttribute("endIndex", Math.min((page + 1) * size, usersPage.getTotalElements()));
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        
        return "admin/User/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", List.of("USER", "ADMIN"));
        return "admin/User/add";
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addUser(@RequestBody Map<String, Object> requestData) {
        try {
            // 从请求数据中提取字段
            String username = (String) requestData.get("username");
            String password = (String) requestData.get("password");
            String fullName = (String) requestData.get("fullName");
            String email = (String) requestData.get("email");
            String phone = (String) requestData.get("phone");
            String roleStr = (String) requestData.get("role");
            Boolean enabled = (Boolean) requestData.get("enabled");
            
            // 验证必填字段
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "用户名不能为空"));
            }
            if (password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "密码不能为空"));
            }
            if (fullName == null || fullName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "姓名不能为空"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "邮箱不能为空"));
            }
            if (roleStr == null || roleStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "请选择角色"));
            }
            
            // 验证密码强度
            if (password.length() < 8 || 
                !password.matches("^.*[a-zA-Z].*$") || 
                !password.matches("^.*[0-9].*$")) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "密码长度至少8位，必须包含字母和数字"));
            }
            
            // 检查用户名是否已存在
            if (userService.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "用户名已存在，请选择其他用户名"));
            }
            
            // 检查邮箱是否已存在
            if (userService.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "邮箱已被使用，请选择其他邮箱"));
            }
            
            // 创建用户对象
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setEnabled(enabled != null ? enabled : true);
            
            // 设置角色 - 从数据库中查找现有的角色对象
            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByName(roleStr)
                    .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleStr));
            roles.add(userRole);
            user.setRoles(roles);
            
            // 添加用户
            userService.addUser(user);
            
            // 返回成功响应
            return ResponseEntity.ok(Collections.singletonMap("message", "用户添加成功"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("添加用户失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "添加用户失败: " + e.getMessage()));
        }
    }


    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id)
            .orElse(null);
        if (user == null) {
            model.addAttribute("errorMessage", "用户不存在");
            return "redirect:/admin/users";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("roles", List.of("USER", "ADMIN"));
        
        // 设置当前用户的角色
        String userRole = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("USER");
        model.addAttribute("userRole", userRole);
        
        return "admin/User/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable Long id,
            @ModelAttribute User user,
            @RequestParam("role") String role,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        
        // 获取原始用户信息
        User originalUser = userService.getUserById(id)
                .orElse(null);
        if (originalUser == null) {
            model.addAttribute("errorMessage", "用户不存在");
            return "redirect:/admin/users";
        }
        
        // 处理密码更新
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // 验证密码
            if (!user.getPassword().equals(confirmPassword)) {
                model.addAttribute("errorMessage", "两次输入的密码不一致");
                model.addAttribute("roles", List.of("USER", "ADMIN"));
                model.addAttribute("user", originalUser);
                
                String userRole = originalUser.getRoles().stream()
                        .map(Role::getName)
                        .findFirst()
                        .orElse("USER");
                model.addAttribute("userRole", userRole);
                
                return "admin/User/edit";
            }
            
            // 验证密码强度
            if (user.getPassword().length() < 8 || 
                !user.getPassword().matches("^.*[a-zA-Z].*$") || 
                !user.getPassword().matches("^.*[0-9].*$")) {
                model.addAttribute("errorMessage", "密码长度至少8位，必须包含字母和数字");
                model.addAttribute("roles", List.of("USER", "ADMIN"));
                model.addAttribute("user", originalUser);
                
                String userRole = originalUser.getRoles().stream()
                        .map(Role::getName)
                        .findFirst()
                        .orElse("USER");
                model.addAttribute("userRole", userRole);
                
                return "admin/User/edit";
            }
        } else {
            // 不更新密码，使用原始密码
            user.setPassword(originalUser.getPassword());
        }
        
        // 保留用户名（确保用户名不被修改）
        user.setUsername(originalUser.getUsername());
        
        // 修复角色设置 - 从数据库中查找现有的角色对象
        Set<Role> roles = new HashSet<>();
        // 从数据库中查找角色
        Role existingRole = roleRepository.findByName(role)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + role));
        roles.add(existingRole);
        user.setRoles(roles);
        
        // 更新用户
        try {
            userService.updateUser(id, user);
            model.addAttribute("successMessage", "用户更新成功");
        } catch (Exception e) {
            // 记录异常详情以帮助调试
            System.err.println("更新用户失败：" + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "更新用户失败：" + e.getMessage());
            return "redirect:/admin/users/edit/" + id;
        }
        
        return "redirect:/admin/users";
    }

    @GetMapping("/toggle/{id}")
    public String toggleUserStatus(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id)
                .orElse(null);
        if (user == null) {
            model.addAttribute("errorMessage", "用户不存在");
            return "redirect:/admin/users";
        }
        
        // 创建一个仅包含必要信息的用户对象进行状态更新
        // 关键：不修改原始用户对象，避免可能的角色信息干扰
        User userForUpdate = new User();
        userForUpdate.setId(user.getId());
        userForUpdate.setUsername(user.getUsername()); // 保留用户名
        userForUpdate.setEmail(user.getEmail());
        userForUpdate.setFullName(user.getFullName());
        userForUpdate.setPhone(user.getPhone());
        userForUpdate.setPassword(user.getPassword()); // 保留密码
        
        // 切换用户状态
        userForUpdate.setEnabled(!user.isEnabled());
        
        // 重要：不设置roles集合，确保不会触角色更新逻辑
        // 让UserService根据这个空集合判断不执行角色更新
        
        try {
            userService.updateUser(id, userForUpdate);
            model.addAttribute("successMessage", "用户状态更新成功");
        } catch (Exception e) {
            // 记录详细错误信息以帮助调试
            System.err.println("更新用户状态失败：" + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "更新用户状态失败：" + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, Model model) {
        try {
            boolean deleted = userService.deleteUser(id);
            if (deleted) {
                model.addAttribute("successMessage", "用户删除成功");
            } else {
                model.addAttribute("errorMessage", "删除用户失败：用户不存在");
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "删除用户失败：" + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
}