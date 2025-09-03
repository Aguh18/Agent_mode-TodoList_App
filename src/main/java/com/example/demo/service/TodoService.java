package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Todo;
import com.example.demo.model.TodoEntity;
import com.example.demo.model.UserEntity;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.UserRepository;
import com.example.dto.request.CreateTodoRequest;
import com.example.dto.request.UpdateTodoRequest;

@Service
public class TodoService {
    private final Map<Long, Todo> todoStorage = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Autowired
    private TodoRepository todoRepository;
    
    @Autowired
    private UserRepository userRepository;

    // Basic in-memory CRUD operations for Todo model
    public Todo createTodo(String title, String description) {
        Todo todo = new Todo(title, description);
        Long id = idGenerator.getAndIncrement();
        todo.setId(id);
        todoStorage.put(id, todo);
        return todo;
    }

    public List<Todo> getAllTodos() {
        return new ArrayList<>(todoStorage.values());
    }

    public Optional<Todo> getTodoById(Long id) {
        return Optional.ofNullable(todoStorage.get(id));
    }

    public Optional<Todo> updateTodo(Long id, String title, String description, Boolean completed) {
        Todo todo = todoStorage.get(id);
        if (todo == null) {
            return Optional.empty();
        }

        if (title != null) todo.setTitle(title);
        if (description != null) todo.setDescription(description);
        if (completed != null) todo.setCompleted(completed);

        return Optional.of(todo);
    }

    public boolean deleteTodo(Long id) {
        return todoStorage.remove(id) != null;
    }

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
