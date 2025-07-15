package com.arthas.test.repository;

import com.arthas.test.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户Repository - 用于测试数据库操作
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据姓名查找用户（模糊匹配）
     */
    List<User> findByNameContaining(String name);
    
    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhone(String phone);
    
    /**
     * 自定义查询：查找最近创建的用户
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findRecentUsers();
    
    /**
     * 自定义查询：根据邮箱域名查找用户
     */
    @Query("SELECT u FROM User u WHERE u.email LIKE %:domain%")
    List<User> findByEmailDomain(@Param("domain") String domain);
    
    /**
     * 统计用户总数
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();
}
