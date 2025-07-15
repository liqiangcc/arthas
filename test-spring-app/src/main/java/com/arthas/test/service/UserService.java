package com.arthas.test.service;

import com.arthas.test.entity.User;
import com.arthas.test.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务类 - 包含各种数据库操作，用于测试链路跟踪
 */
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 创建用户（包含多个数据库操作）
     */
    public User createUser(String name, String email, String phone) {
        System.out.println("UserService.createUser() - Creating user: " + name);
        
        // 1. 检查邮箱是否已存在
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new RuntimeException("Email already exists: " + email);
        }
        
        // 2. 检查手机号是否已存在
        if (phone != null) {
            Optional<User> existingPhone = userRepository.findByPhone(phone);
            if (existingPhone.isPresent()) {
                throw new RuntimeException("Phone already exists: " + phone);
            }
        }
        
        // 3. 创建并保存用户
        User user = new User(name, email, phone);
        User savedUser = userRepository.save(user);
        
        // 4. 记录用户创建日志
        logUserCreation(savedUser);
        
        System.out.println("UserService.createUser() - User created successfully: " + savedUser.getId());
        return savedUser;
    }
    
    /**
     * 获取所有用户
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        System.out.println("UserService.getAllUsers() - Fetching all users");
        
        // 1. 获取用户总数
        long totalCount = userRepository.countAllUsers();
        System.out.println("Total users in database: " + totalCount);
        
        // 2. 获取所有用户
        List<User> users = userRepository.findAll();
        
        System.out.println("UserService.getAllUsers() - Found " + users.size() + " users");
        return users;
    }
    
    /**
     * 根据ID获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        System.out.println("UserService.getUserById() - Fetching user with ID: " + id);
        
        Optional<User> user = userRepository.findById(id);
        
        if (user.isPresent()) {
            System.out.println("UserService.getUserById() - User found: " + user.get().getName());
        } else {
            System.out.println("UserService.getUserById() - User not found with ID: " + id);
        }
        
        return user;
    }
    
    /**
     * 搜索用户（包含多种查询）
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword) {
        System.out.println("UserService.searchUsers() - Searching users with keyword: " + keyword);
        
        // 1. 按姓名搜索
        List<User> usersByName = userRepository.findByNameContaining(keyword);
        System.out.println("Found " + usersByName.size() + " users by name");
        
        // 2. 按邮箱域名搜索
        List<User> usersByDomain = userRepository.findByEmailDomain(keyword);
        System.out.println("Found " + usersByDomain.size() + " users by email domain");
        
        // 3. 获取最近用户（用于比较）
        List<User> recentUsers = userRepository.findRecentUsers();
        System.out.println("Recent users count: " + recentUsers.size());
        
        return usersByName;
    }
    
    /**
     * 更新用户
     */
    public User updateUser(Long id, String name, String email, String phone) {
        System.out.println("UserService.updateUser() - Updating user with ID: " + id);
        
        // 1. 查找用户
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found with ID: " + id);
        }
        
        User user = userOpt.get();
        
        // 2. 更新字段
        if (name != null) user.setName(name);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        
        // 3. 保存更新
        User updatedUser = userRepository.save(user);
        
        System.out.println("UserService.updateUser() - User updated successfully");
        return updatedUser;
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        System.out.println("UserService.deleteUser() - Deleting user with ID: " + id);
        
        // 1. 检查用户是否存在
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with ID: " + id);
        }
        
        // 2. 删除用户
        userRepository.deleteById(id);
        
        System.out.println("UserService.deleteUser() - User deleted successfully");
    }
    
    /**
     * 记录用户创建日志（私有方法，用于测试方法调用链）
     */
    private void logUserCreation(User user) {
        System.out.println("UserService.logUserCreation() - Logging user creation: " + user.getId());
        // 这里可以添加更多的日志记录逻辑
    }
}
