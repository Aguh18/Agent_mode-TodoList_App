package com.example.demo.controller.api;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.TodoEntity;
import com.example.demo.service.AIOrchestatorService;
import com.example.demo.service.TodoService;
import com.example.demo.utils.JwtUtils;
import com.example.demo.utils.ResponseUtils;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/agent")
public class AIOrchestatorController {

    @Autowired
    private AIOrchestatorService aiOrchestatorService;

    @Autowired
    private TodoService todoService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Main endpoint for AI chat interaction
     */
    @PostMapping("/chat")
    public ResponseEntity<?> processChat(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            String userMessage = (String) request.get("message");
            String language = (String) request.getOrDefault("language", "id"); // Default to Indonesian
            Boolean autoExecute = (Boolean) request.getOrDefault("auto_execute", true); // Default to auto execute
            String username = getUsernameFromToken(httpRequest);
            
            if (username == null) {
                return ResponseUtils.fail("Pengguna tidak terautentikasi", HttpStatus.UNAUTHORIZED);
            }
            
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseUtils.fail("Pesan diperlukan", HttpStatus.BAD_REQUEST);
            }
            
            // Process the message through AI Orchestrator with language and auto_execute settings
            Map<String, Object> response = aiOrchestatorService.processUserMessage(userMessage, username, language, autoExecute);
            
            // Check if service determined action should be auto-executed
            Boolean shouldAutoExecute = (Boolean) response.get("auto_execute");
            if (Boolean.TRUE.equals(shouldAutoExecute) && response.containsKey("actions")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> actions = (Map<String, Object>) response.get("actions");
                
                if (Boolean.TRUE.equals(actions.get("actionable"))) {
                    String suggestedAction = (String) actions.get("suggested_action");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> actionParams = (Map<String, Object>) actions.get("parameters");
                    
                    try {
                        // Execute the suggested action
                        Object actionResult = aiOrchestatorService.executeAction(suggestedAction, actionParams, username);
                        
                        // Add execution result to response
                        response.put("execution_result", actionResult);
                        response.put("executed", true);
                        
                        // Update context after action execution for accurate count
                        List<TodoEntity> updatedTodos = todoService.getMyTodos(username);
                        response.put("context", Map.of(
                            "totalTodos", updatedTodos.size(),
                            "completedTodos", updatedTodos.stream().mapToInt(t -> t.isCompleted() ? 1 : 0).sum(),
                            "pendingTodos", updatedTodos.stream().mapToInt(t -> !t.isCompleted() ? 1 : 0).sum()
                        ));
                        
                    } catch (Exception actionException) {
                        System.err.println("Error executing action: " + actionException.getMessage());
                        actionException.printStackTrace();
                        response.put("execution_error", actionException.getMessage());
                        response.put("executed", false);
                    }
                } else {
                    response.put("executed", false);
                }
            } else {
                response.put("executed", false);
            }
            
            return ResponseUtils.ok("Respons AI berhasil dibuat", response);
            
        } catch (Exception e) {
            System.err.println("Error in AI chat: " + e.getMessage());
            return ResponseUtils.fail("Gagal memproses pesan: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all todos for AI context
     */
    @PostMapping("/context/todos")
    public ResponseEntity<?> getTodosContext(HttpServletRequest httpRequest) {
        try {
            String username = getUsernameFromToken(httpRequest);
            
            if (username == null) {
                return ResponseUtils.fail("Pengguna tidak terautentikasi", HttpStatus.UNAUTHORIZED);
            }
            
            List<TodoEntity> todos = todoService.getMyTodos(username);
            
            return ResponseUtils.ok("Konteks todo berhasil diambil", Map.of(
                "todos", todos,
                "total", todos.size(),
                "completed", todos.stream().mapToInt(t -> t.isCompleted() ? 1 : 0).sum(),
                "pending", todos.stream().mapToInt(t -> !t.isCompleted() ? 1 : 0).sum()
            ));
            
        } catch (Exception e) {
            return ResponseUtils.fail("Gagal mendapatkan konteks: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Execute AI-suggested actions
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executeAction(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            String username = getUsernameFromToken(httpRequest);
            String action = (String) request.get("action");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.get("parameters");
            
            if (username == null) {
                return ResponseUtils.fail("Pengguna tidak terautentikasi", HttpStatus.UNAUTHORIZED);
            }
            
            if (action == null) {
                return ResponseUtils.fail("Aksi diperlukan", HttpStatus.BAD_REQUEST);
            }
            
            Object result = aiOrchestatorService.executeAction(action, params, username);
            
            return ResponseUtils.ok("Aksi berhasil dijalankan", result);
            
        } catch (Exception e) {
            return ResponseUtils.fail("Gagal menjalankan aksi: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Test endpoint for AI integration
     */
    @PostMapping("/test")
    public ResponseEntity<?> testAI() {
        try {
            Map<String, Object> testResponse = Map.of(
                "status", "AI Orchestrator berfungsi dengan baik",
                "timestamp", System.currentTimeMillis(),
                "capabilities", List.of(
                    "Membuat todo",
                    "Menampilkan todo", 
                    "Memperbarui todo",
                    "Menghapus todo",
                    "Menandai todo sebagai selesai",
                    "Mencari todo",
                    "Mendapatkan statistik"
                )
            );
            
            return ResponseUtils.ok("Test AI Orchestrator berhasil", testResponse);
            
        } catch (Exception e) {
            return ResponseUtils.fail("Test AI gagal: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getUsernameFromToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtUtils.getUsername(token);
            }
        } catch (Exception e) {
            System.err.println("Error extracting username from token: " + e.getMessage());
        }
        return null;
    }
}
