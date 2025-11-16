package com.Southern.book.service;

import com.Southern.book.entity.Role;
import com.Southern.book.entity.User;
import com.Southern.book.repository.RoleRepository;
import com.Southern.book.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 获取所有用户
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 分页获取用户
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    // 根据ID查找用户
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // 根据用户名查找用户
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // 添加用户
    @Transactional
    public User addUser(User user) {
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // 更新用户
    @Transactional
    public Optional<User> updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(user -> {
            try {
                // 保留原始用户名（不允许修改）
                // 更新基本信息
                user.setEmail(userDetails.getEmail());
                user.setFullName(userDetails.getFullName());
                user.setPhone(userDetails.getPhone());
                user.setEnabled(userDetails.isEnabled());
                // 角色处理 - 重要：只有在明确提供新角色信息时才更新角色
                // 检查是否明确提供了新的角色信息（不仅仅是空集合）
                boolean shouldUpdateRoles = false;
                if (userDetails.getRoles() != null && !userDetails.getRoles().isEmpty()) {
                    // 检查是否有至少一个有效的角色信息
                    for (Role role : userDetails.getRoles()) {
                        if (role != null && role.getName() != null && !role.getName().isEmpty()) {
                            shouldUpdateRoles = true;
                            break;
                        }
                    }
                }
                
                // 只有在明确需要更新角色时才执行角色更新操作
                if (shouldUpdateRoles) {
                    // 清空现有角色关联
                    user.getRoles().clear();
                    // 从数据库中查找并添加已存在的角色
                    userDetails.getRoles().forEach(role -> {
                        if (role != null && role.getName() != null && !role.getName().isEmpty()) {
                            Role existingRole = roleRepository.findByName(role.getName())
                                    .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + role.getName()));
                            user.getRoles().add(existingRole);
                        }
                    });
                }
                // 注意：当只切换用户状态（启用/禁用）时，不会执行任何角色更新操作，完全保留原有角色
                
                // 如果提供了新密码，则更新密码
                if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                    // 检查是否需要加密（如果前端已经加密过，这里可能需要调整）
                    // 如果密码长度明显不是BCrypt加密后的长度（通常是60个字符），则进行加密
                    if (userDetails.getPassword().length() < 50) {
                        user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                    } else {
                        // 假设已经是加密的密码，直接设置
                        user.setPassword(userDetails.getPassword());
                    }
                }
                
                // 保存并返回更新后的用户
                User updatedUser = userRepository.save(user);
                System.out.println("用户更新成功，ID: " + updatedUser.getId());
                return updatedUser;
            } catch (Exception e) {
                System.err.println("用户更新失败，ID: " + id + ", 错误: " + e.getMessage());
                e.printStackTrace();
                throw e; // 重新抛出异常以便上层捕获
            }
        });
    }

    // 删除用户
    @Transactional
    public boolean deleteUser(Long id) {
        return userRepository.findById(id).map(user -> {
            userRepository.delete(user);
            return true;
        }).orElse(false);
    }

    // 检查用户名是否存在
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // 检查邮箱是否存在
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    // 获取用户总数
    public long getTotalUserCount() {
        return userRepository.count();
    }
    
    // 根据关键词搜索用户（用户名、邮箱、全名）
    public List<User> searchUsers(String keyword) {
        // 简化实现，实际应该在Repository层提供更高效的查询方法
        List<User> allUsers = userRepository.findAll();
        String lowerKeyword = keyword.toLowerCase();
        return allUsers.stream()
                .filter(user -> user.getUsername().toLowerCase().contains(lowerKeyword) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerKeyword)) ||
                        (user.getFullName() != null && user.getFullName().toLowerCase().contains(lowerKeyword)))
                .toList();
    }
}