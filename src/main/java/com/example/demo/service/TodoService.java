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

    // Get todos by userId (lebih efisien untuk JWT)
    public List<TodoEntity> getMyTodosByUserId(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
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

    // Create todo by userId (lebih efisien untuk JWT)
    @Transactional
    public TodoEntity createByUserId(Long userId, CreateTodoRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

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

    // Update todo by userId (lebih efisien untuk JWT)
    @Transactional
    public Optional<TodoEntity> updateByUserId(Long userId, Long todoId, UpdateTodoRequest request) {
        return todoRepository.findById(todoId).filter(t -> t.getUser().getId().equals(userId)).map(t -> {
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

    // Toggle todo by userId (lebih efisien untuk JWT)
    @Transactional
    public Optional<TodoEntity> toggleByUserId(Long userId, Long todoId) {
        return todoRepository.findById(todoId).filter(t -> t.getUser().getId().equals(userId)).map(t -> {
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

    @Transactional
    public boolean deleteByUserId(Long userId, Long id) {
        return todoRepository.findById(id).filter(t -> t.getUser().getId().equals(userId)).map(t -> {
            todoRepository.delete(t);
            return true;
        }).orElse(false);
    }

    private UserEntity getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
