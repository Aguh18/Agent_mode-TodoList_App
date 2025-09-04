package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.request.CreateTodoRequest;
import com.example.demo.dto.request.UpdateTodoRequest;
import com.example.demo.model.TodoEntity;
import com.example.demo.model.UserEntity;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.UserRepository;

@Service
public class TodoService {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    // Database-based CRUD operations for TodoEntity
    public List<TodoEntity> getMyTodos(String username) {
        UserEntity user = getUserOrThrow(username);
        return todoRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public TodoEntity create(String username, CreateTodoRequest request) {
        UserEntity user = getUserOrThrow(username);

        TodoEntity todo = new TodoEntity();
        todo.setTitle(request.getTitle());
        todo.setDescription(request.getDescription());
        todo.setCompleted(false);
        todo.setUser(user);
        return todoRepository.save(todo);
    }

    @Transactional
    public Optional<TodoEntity> update(String username, Long id, UpdateTodoRequest request) {
        UserEntity user = getUserOrThrow(username);
        return todoRepository.findById(id).filter(t -> t.getUser().getId().equals(user.getId())).map(t -> {
            if (request.getTitle() != null) {
                t.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                t.setDescription(request.getDescription());
            }
            if (request.getCompleted() != null) {
                t.setCompleted(request.getCompleted());
            }
            return todoRepository.save(t);
        });
    }

    @Transactional
    public Optional<TodoEntity> toggle(String username, Long id) {
        UserEntity user = getUserOrThrow(username);
        return todoRepository.findById(id).filter(t -> t.getUser().getId().equals(user.getId())).map(t -> {
            t.setCompleted(!t.isCompleted());
            return todoRepository.save(t);
        });
    }

    @Transactional
    public boolean delete(String username, Long id) {
        UserEntity user = getUserOrThrow(username);
        return todoRepository.findById(id).filter(t -> t.getUser().getId().equals(user.getId())).map(t -> {
            todoRepository.delete(t);
            return true;
        }).orElse(false);
    }

    private UserEntity getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
