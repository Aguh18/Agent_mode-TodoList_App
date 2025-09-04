package com.example.demo.controller.api;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.request.CreateTodoRequest;
import com.example.demo.dto.request.UpdateTodoRequest;
import com.example.demo.model.TodoEntity;
import com.example.demo.service.TodoService;
import com.example.demo.utils.JwtUtils;
import com.example.demo.utils.ResponseUtils;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/todo/")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private JwtUtils jwtUtils;

    // Create Todo
    @PostMapping("add")
    public ResponseEntity<?> addTodo(@RequestBody CreateTodoRequest request, HttpServletRequest httpRequest) {
        try {
            // Ambil userId dari JWT token
            Long userId = jwtUtils.getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseUtils.fail("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            TodoEntity todo = todoService.createByUserId(userId, request);
            return ResponseUtils.ok("Todo created successfully", todo);

        } catch (Exception e) {
            System.err.println("❌ Error in addTodo: " + e.getMessage());
            return ResponseUtils.fail("Failed to create todo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Read All Todos
    @GetMapping("my-todos")
    public ResponseEntity<?> getMyTodos(HttpServletRequest request) {
        try {
            // Ambil userId dari JWT token
            Long userId = jwtUtils.getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseUtils.fail("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            List<TodoEntity> todos = todoService.getMyTodosByUserId(userId);
            return ResponseUtils.ok("Todos retrieved successfully", todos);

        } catch (Exception e) {
            System.err.println("❌ Error in getMyTodos: " + e.getMessage());
            return ResponseUtils.fail("Failed to retrieve todos: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Update Todo
    @PutMapping("update/{id}")
    public ResponseEntity<?> updateTodo(@PathVariable Long id, @RequestBody UpdateTodoRequest request, HttpServletRequest httpRequest) {
        try {
            // Ambil userId dari JWT token
            Long userId = jwtUtils.getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseUtils.fail("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            Optional<TodoEntity> updatedTodo = todoService.updateByUserId(userId, id, request);
            if (updatedTodo.isPresent()) {
                return ResponseUtils.ok("Todo updated successfully", updatedTodo.get());
            } else {
                return ResponseUtils.fail("Todo not found or access denied", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in updateTodo: " + e.getMessage());
            return ResponseUtils.fail("Failed to update todo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Toggle Todo Status
    @PutMapping("toggle/{id}")
    public ResponseEntity<?> toggleTodo(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            // Ambil userId dari JWT token
            Long userId = jwtUtils.getUserIdFromRequest(httpRequest);
            if (userId == null) {
                return ResponseUtils.fail("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            Optional<TodoEntity> toggledTodo = todoService.toggleByUserId(userId, id);
            if (toggledTodo.isPresent()) {
                return ResponseUtils.ok("Todo status toggled successfully", toggledTodo.get());
            } else {
                return ResponseUtils.fail("Todo not found or access denied", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in toggleTodo: " + e.getMessage());
            return ResponseUtils.fail("Failed to toggle todo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Mark Todo as Completed
    @PutMapping("complete/{id}")
    public ResponseEntity<?> completeTodo(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String username = getUsernameFromToken(httpRequest);
            if (username == null) {
                return ResponseUtils.fail("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            UpdateTodoRequest request = new UpdateTodoRequest();
            request.setCompleted(true);

            Optional<TodoEntity> completedTodo = todoService.update(username, id, request);
            if (completedTodo.isPresent()) {
                return ResponseUtils.ok("Todo marked as completed successfully", completedTodo.get());
            } else {
                return ResponseUtils.fail("Todo not found or access denied", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in completeTodo: " + e.getMessage());
            return ResponseUtils.fail("Failed to complete todo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Mark Todo as Not Completed
    @PutMapping("uncomplete/{id}")
    public ResponseEntity<?> uncompleteTodo(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String username = getUsernameFromToken(httpRequest);
            if (username == null) {
                return ResponseUtils.fail("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            UpdateTodoRequest request = new UpdateTodoRequest();
            request.setCompleted(false);

            Optional<TodoEntity> uncompletedTodo = todoService.update(username, id, request);
            if (uncompletedTodo.isPresent()) {
                return ResponseUtils.ok("Todo marked as not completed successfully", uncompletedTodo.get());
            } else {
                return ResponseUtils.fail("Todo not found or access denied", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in uncompleteTodo: " + e.getMessage());
            return ResponseUtils.fail("Failed to mark todo as not completed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get Completed Todos
    @GetMapping("completed")
    public ResponseEntity<?> getCompletedTodos(HttpServletRequest request) {
        try {
            String username = getUsernameFromToken(request);
            if (username == null) {
                return ResponseUtils.fail("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            List<TodoEntity> todos = todoService.getMyTodos(username);
            List<TodoEntity> completedTodos = todos.stream()
                    .filter(TodoEntity::isCompleted)
                    .toList();

            return ResponseUtils.ok("Completed todos retrieved successfully", completedTodos);

        } catch (Exception e) {
            System.err.println("❌ Error in getCompletedTodos: " + e.getMessage());
            return ResponseUtils.fail("Failed to retrieve completed todos: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get Pending/Incomplete Todos
    @GetMapping("pending")
    public ResponseEntity<?> getPendingTodos(HttpServletRequest request) {
        try {
            String username = getUsernameFromToken(request);
            if (username == null) {
                return ResponseUtils.fail("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            List<TodoEntity> todos = todoService.getMyTodos(username);
            List<TodoEntity> pendingTodos = todos.stream()
                    .filter(todo -> !todo.isCompleted())
                    .toList();

            return ResponseUtils.ok("Pending todos retrieved successfully", pendingTodos);

        } catch (Exception e) {
            System.err.println("❌ Error in getPendingTodos: " + e.getMessage());
            return ResponseUtils.fail("Failed to retrieve pending todos: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Delete Todo
    @DeleteMapping("delete/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String username = getUsernameFromToken(httpRequest);
            if (username == null) {
                return ResponseUtils.fail("User not authenticated", HttpStatus.UNAUTHORIZED);
            }

            boolean deleted = todoService.delete(username, id);
            if (deleted) {
                return ResponseUtils.ok("Todo deleted successfully", null);
            } else {
                return ResponseUtils.fail("Todo not found or access denied", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in deleteTodo: " + e.getMessage());
            return ResponseUtils.fail("Failed to delete todo: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper method to extract username from JWT token
    private String getUsernameFromToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }

            String token = authHeader.substring(7);

            if (!jwtUtils.validateToken(token)) {
                return null;
            }

            return jwtUtils.getUsername(token);

        } catch (io.jsonwebtoken.ExpiredJwtException | io.jsonwebtoken.MalformedJwtException e) {
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error extracting username from token: " + e.getMessage());
            return null;
        }
    }
}
