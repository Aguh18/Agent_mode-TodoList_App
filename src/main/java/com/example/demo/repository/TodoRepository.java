package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.TodoEntity;
import com.example.demo.model.UserEntity;

@Repository
public interface TodoRepository extends JpaRepository<TodoEntity, Long> {

    List<TodoEntity> findByUser(UserEntity user);
    List<TodoEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
}
