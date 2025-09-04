package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.request.CreateTodoRequest;
import com.example.demo.dto.request.UpdateTodoRequest;
import com.example.demo.model.TodoEntity;

@Service
public class AIOrchestatorService {

    @Autowired
    private TodoService todoService;

    @Autowired
    private DeepSeekService deepSeekService;

    /**
     * Process user message and generate AI response with possible actions
     */
    public Map<String, Object> processUserMessage(String userMessage, String username) {
        try {
            // Use the enhanced AI analysis method
            return processUserMessage(userMessage, username, "id", false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process AI message: " + e.getMessage(), e);
        }
    }

    /**
     * Process user message with language and auto execute support using AI
     */
    public Map<String, Object> processUserMessage(String userMessage, String username, String language, Boolean autoExecute) {
        Map<String, Object> actions = new HashMap<>();
        try {
            // Create prompt for AI to analyze user intent and extract parameters
            String analysisPrompt = String.format(
                    "Analisis pesan user berikut dan tentukan aksi yang diinginkan beserta parameternya:\n\n"
                    + "Pesan: \"%s\"\n\n"
                    + "Tugas:\n"
                    + "1. Identifikasi jenis aksi yang diinginkan user\n"
                    + "2. Ekstrak parameter yang diperlukan untuk aksi tersebut\n"
                    + "3. Tentukan apakah ini actionable (dapat langsung dieksekusi)\n\n"
                    + "Jenis aksi yang tersedia:\n"
                    + "- create_todo: membuat todo baru (perlu: title, description opsional)\n"
                    + "- list_todos: menampilkan semua todo\n"
                    + "- update_todo: mengubah todo (perlu: id/title lama, title/description/completed baru)\n"
                    + "- complete_todo: menandai todo selesai (perlu: id/title)\n"
                    + "- delete_todo: menghapus todo (perlu: id/title)\n"
                    + "- get_statistics: menampilkan statistik todo\n"
                    + "- none: tidak ada aksi spesifik (chat biasa)\n\n"
                    + "Responmu harus dalam format JSON seperti ini:\n"
                    + "{\n"
                    + "  \"suggested_action\": \"jenis_aksi\",\n"
                    + "  \"actionable\": true/false,\n"
                    + "  \"parameters\": {\n"
                    + "    \"id\": \"id_todo (jika ada)\",\n"
                    + "    \"title\": \"judul_todo\",\n"
                    + "    \"description\": \"deskripsi_todo\",\n"
                    + "    \"completed\": true/false,\n"
                    + "    \"search_term\": \"kata_kunci_pencarian\"\n"
                    + "  }\n"
                    + "}\n\n"
                    + "Contoh:\n"
                    + "- \"buat todo belajar\" → {\"suggested_action\": \"create_todo\", \"actionable\": true, \"parameters\": {\"title\": \"belajar\"}}\n"
                    + "- \"hapus todo nomor 1\" → {\"suggested_action\": \"delete_todo\", \"actionable\": true, \"parameters\": {\"id\": \"1\"}}\n"
                    + "- \"tandai selesai todo belajar\" → {\"suggested_action\": \"complete_todo\", \"actionable\": true, \"parameters\": {\"title\": \"belajar\"}}\n"
                    + "- \"ubah judul todo 2 jadi belajar java\" → {\"suggested_action\": \"update_todo\", \"actionable\": true, \"parameters\": {\"id\": \"2\", \"title\": \"belajar java\"}}\n"
                    + "- \"tampilkan semua todo\" → {\"suggested_action\": \"list_todos\", \"actionable\": true, \"parameters\": {}}\n"
                    + "- \"halo apa kabar\" → {\"suggested_action\": \"none\", \"actionable\": false, \"parameters\": {}}\n\n"
                    + "Hanya berikan JSON response, tanpa penjelasan tambahan.",
                    userMessage
            );
            // Get AI response
            String aiResponse = deepSeekService.generateResponse(analysisPrompt, "");
            // Parse JSON response
            if (aiResponse != null && aiResponse.trim().startsWith("{")) {
                try {
                    String jsonContent = aiResponse.trim();
                    // Extract suggested_action
                    String suggestedAction = extractJsonValue(jsonContent, "suggested_action");
                    if (suggestedAction != null) {
                        actions.put("suggested_action", suggestedAction);
                    }
                    // Extract actionable
                    boolean actionable = jsonContent.contains("\"actionable\": true");
                    actions.put("actionable", actionable);
                    // Extract parameters
                    Map<String, Object> parameters = new HashMap<>();
                    String parametersSection = extractJsonSection(jsonContent, "parameters");
                    if (parametersSection != null) {
                        // Extract individual parameters
                        String id = extractJsonValue(parametersSection, "id");
                        String title = extractJsonValue(parametersSection, "title");
                        String description = extractJsonValue(parametersSection, "description");
                        String searchTerm = extractJsonValue(parametersSection, "search_term");
                        String completedStr = extractJsonValue(parametersSection, "completed");
                        if (id != null && !id.equals("null")) {
                            try {
                                parameters.put("id", Long.parseLong(id));
                            } catch (NumberFormatException e) {
                                // If not a number, might be a title reference
                                parameters.put("title_reference", id);
                            }
                        }
                        if (title != null && !title.equals("null")) {
                            parameters.put("title", title);
                        }
                        if (description != null && !description.equals("null")) {
                            parameters.put("description", description);
                        }
                        if (searchTerm != null && !searchTerm.equals("null")) {
                            parameters.put("search_term", searchTerm);
                        }
                        if (completedStr != null && !completedStr.equals("null")) {
                            parameters.put("completed", Boolean.parseBoolean(completedStr));
                        }
                    }
                    if (!parameters.isEmpty()) {
                        actions.put("parameters", parameters);
                    }
                } catch (Exception e) {
                    System.out.println("Error parsing AI response for action analysis: " + e.getMessage());
                    // Fallback to simple keyword detection
                    return analyzeUserMessageFallback(userMessage, username);
                }
            } else {
                // Fallback to simple keyword detection
                return analyzeUserMessageFallback(userMessage, username);
            }
        } catch (Exception e) {
            System.out.println("Error in AI action analysis: " + e.getMessage());
            // Fallback to simple keyword detection
            return analyzeUserMessageFallback(userMessage, username);
        }

        // Generate chat response in Indonesian
        List<TodoEntity> todos = todoService.getMyTodos(username);
        String context = buildContextForAI(todos, username);
        String chatResponse = deepSeekService.generateResponse(userMessage, context);

        // Build complete response
        Map<String, Object> response = new HashMap<>();
        response.put("message", chatResponse);
        response.put("actions", actions);

        // Determine auto_execute based on action type and parameters
        boolean shouldAutoExecute = shouldAutoExecuteAction(actions, autoExecute);
        response.put("auto_execute", shouldAutoExecute);

        response.put("context", Map.of(
                "totalTodos", todos.size(),
                "completedTodos", todos.stream().mapToInt(t -> t.isCompleted() ? 1 : 0).sum(),
                "pendingTodos", todos.stream().mapToInt(t -> !t.isCompleted() ? 1 : 0).sum()
        ));

        return response;
    }

    /**
     * Determine if an action should be auto-executed based on the action type
     * and parameters
     */
    private boolean shouldAutoExecuteAction(Map<String, Object> actions, Boolean userAutoExecute) {
        if (actions == null || !Boolean.TRUE.equals(actions.get("actionable"))) {
            System.out.println("DEBUG: Action not actionable");
            return false;
        }

        String suggestedAction = (String) actions.get("suggested_action");
        if (suggestedAction == null) {
            System.out.println("DEBUG: No suggested action");
            return false;
        }

        System.out.println("DEBUG: Checking auto-execute for action: " + suggestedAction);

        // Auto-execute rules based on action type
        switch (suggestedAction.toLowerCase()) {
            case "create_todo":
                // Auto-execute if title is provided
                @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) actions.get("parameters");
                if (params != null && params.get("title") != null) {
                    String title = (String) params.get("title");
                    boolean canExecute = title != null && !title.trim().isEmpty();
                    System.out.println("DEBUG: create_todo auto-execute: " + canExecute + " (title: " + title + ")");
                    return canExecute;
                }
                System.out.println("DEBUG: create_todo no title provided");
                return false;

            case "list_todos":
            case "get_statistics":
                // These are safe to auto-execute
                return true;

            case "delete_todo":
            case "complete_todo":
                // Auto-execute if title is provided and specific
                @SuppressWarnings("unchecked") Map<String, Object> actionParams = (Map<String, Object>) actions.get("parameters");
                if (actionParams != null) {
                    String actionTitle = (String) actionParams.get("title");
                    Long actionId = null;
                    try {
                        Object idObj = actionParams.get("id");
                        if (idObj instanceof Number) {
                            actionId = ((Number) idObj).longValue();
                        }
                    } catch (Exception e) {
                        // ID not provided
                    }

                    // Auto-execute if ID is provided or title is specific enough
                    if (actionId != null || (actionTitle != null && actionTitle.trim().length() > 2)) {
                        System.out.println("DEBUG: " + suggestedAction + " auto-execute: true (title: " + actionTitle + ", id: " + actionId + ")");
                        return true;
                    }
                }
                // Otherwise require confirmation
                System.out.println("DEBUG: " + suggestedAction + " requires confirmation");
                return Boolean.TRUE.equals(userAutoExecute);

            case "update_todo":
                // Auto-execute if sufficient parameters are provided
                @SuppressWarnings("unchecked") Map<String, Object> updateParams = (Map<String, Object>) actions.get("parameters");
                if (updateParams != null) {
                    String updateTitle = (String) updateParams.get("title");
                    String titleRef = (String) updateParams.get("title_reference");
                    Long updateId = null;
                    try {
                        Object idObj = updateParams.get("id");
                        if (idObj instanceof Number) {
                            updateId = ((Number) idObj).longValue();
                        }
                    } catch (Exception e) {
                        // ID not provided
                    }

                    // Auto-execute if we have clear identification
                    if (updateId != null || titleRef != null || (updateTitle != null && updateTitle.trim().length() > 2)) {
                        System.out.println("DEBUG: update_todo auto-execute: true");
                        return true;
                    }
                }
                System.out.println("DEBUG: update_todo requires confirmation");
                return Boolean.TRUE.equals(userAutoExecute);

            default:
                return Boolean.TRUE.equals(userAutoExecute);
        }
    }

    /**
     * Process user message and generate AI response with possible actions using
     * userId
     */
    public Map<String, Object> processUserMessageByUserId(String userMessage, Long userId) {
        try {
            // Use the enhanced AI analysis method
            return processUserMessageByUserId(userMessage, userId, "id", false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process AI message: " + e.getMessage(), e);
        }
    }

    /**
     * Process user message with language and auto execute support using AI with
     * userId
     */
    public Map<String, Object> processUserMessageByUserId(String userMessage, Long userId, String language, Boolean autoExecute) {
        Map<String, Object> actions = new HashMap<>();
        try {
            // Create prompt for AI to analyze user intent and extract parameters
            String analysisPrompt = String.format(
                    "Analisis pesan user berikut dan tentukan aksi yang diinginkan beserta parameternya:\n\n"
                    + "Pesan: \"%s\"\n\n"
                    + "Tugas:\n"
                    + "1. Identifikasi jenis aksi yang diinginkan user\n"
                    + "2. Ekstrak parameter yang diperlukan untuk aksi tersebut\n"
                    + "3. Tentukan apakah ini actionable (dapat langsung dieksekusi)\n\n"
                    + "Jenis aksi yang tersedia:\n"
                    + "- create_todo: membuat todo baru (perlu: title, description opsional)\n"
                    + "- list_todos: menampilkan semua todo\n"
                    + "- update_todo: mengubah todo (perlu: id/title lama, title/description/completed baru)\n"
                    + "- complete_todo: menandai todo selesai (perlu: id/title)\n"
                    + "- delete_todo: menghapus todo (perlu: id/title)\n"
                    + "- get_statistics: menampilkan statistik todo\n"
                    + "- none: tidak ada aksi spesifik (chat biasa)\n\n"
                    + "Responmu harus dalam format JSON seperti ini:\n"
                    + "{\n"
                    + "  \"suggested_action\": \"jenis_aksi\",\n"
                    + "  \"actionable\": true/false,\n"
                    + "  \"parameters\": {\n"
                    + "    \"id\": \"id_todo (jika ada)\",\n"
                    + "    \"title\": \"judul_todo\",\n"
                    + "    \"description\": \"deskripsi_todo\",\n"
                    + "    \"completed\": true/false,\n"
                    + "    \"search_term\": \"kata_kunci_pencarian\"\n"
                    + "  }\n"
                    + "}\n\n"
                    + "Contoh:\n"
                    + "- \"buat todo belajar\" → {\"suggested_action\": \"create_todo\", \"actionable\": true, \"parameters\": {\"title\": \"belajar\"}}\n"
                    + "- \"hapus todo nomor 1\" → {\"suggested_action\": \"delete_todo\", \"actionable\": true, \"parameters\": {\"id\": \"1\"}}\n"
                    + "- \"tandai selesai todo belajar\" → {\"suggested_action\": \"complete_todo\", \"actionable\": true, \"parameters\": {\"title\": \"belajar\"}}\n"
                    + "- \"ubah judul todo 2 jadi belajar java\" → {\"suggested_action\": \"update_todo\", \"actionable\": true, \"parameters\": {\"id\": \"2\", \"title\": \"belajar java\"}}\n"
                    + "- \"tampilkan semua todo\" → {\"suggested_action\": \"list_todos\", \"actionable\": true, \"parameters\": {}}\n"
                    + "- \"halo apa kabar\" → {\"suggested_action\": \"none\", \"actionable\": false, \"parameters\": {}}\n\n"
                    + "Hanya berikan JSON response, tanpa penjelasan tambahan.",
                    userMessage
            );
            // Get AI response
            String aiResponse = deepSeekService.generateResponse(analysisPrompt, "");
            // Parse JSON response
            if (aiResponse != null && aiResponse.trim().startsWith("{")) {
                try {
                    String jsonContent = aiResponse.trim();
                    // Extract suggested_action
                    String suggestedAction = extractJsonValue(jsonContent, "suggested_action");
                    if (suggestedAction != null) {
                        actions.put("suggested_action", suggestedAction);
                    }
                    // Extract actionable
                    boolean actionable = jsonContent.contains("\"actionable\": true");
                    actions.put("actionable", actionable);
                    // Extract parameters
                    Map<String, Object> parameters = new HashMap<>();
                    String parametersSection = extractJsonSection(jsonContent, "parameters");
                    if (parametersSection != null) {
                        // Extract individual parameters
                        String id = extractJsonValue(parametersSection, "id");
                        String title = extractJsonValue(parametersSection, "title");
                        String description = extractJsonValue(parametersSection, "description");
                        String searchTerm = extractJsonValue(parametersSection, "search_term");
                        String completedStr = extractJsonValue(parametersSection, "completed");
                        if (id != null && !id.equals("null")) {
                            try {
                                parameters.put("id", Long.parseLong(id));
                            } catch (NumberFormatException e) {
                                // If not a number, might be a title reference
                                parameters.put("title_reference", id);
                            }
                        }
                        if (title != null && !title.equals("null")) {
                            parameters.put("title", title);
                        }
                        if (description != null && !description.equals("null")) {
                            parameters.put("description", description);
                        }
                        if (searchTerm != null && !searchTerm.equals("null")) {
                            parameters.put("search_term", searchTerm);
                        }
                        if (completedStr != null && !completedStr.equals("null")) {
                            parameters.put("completed", Boolean.parseBoolean(completedStr));
                        }
                    }
                    if (!parameters.isEmpty()) {
                        actions.put("parameters", parameters);
                    }
                } catch (Exception e) {
                    System.out.println("Error parsing AI response for action analysis: " + e.getMessage());
                    // Fallback to simple keyword detection
                    return analyzeUserMessageFallbackByUserId(userMessage, userId);
                }
            } else {
                // Fallback to simple keyword detection
                return analyzeUserMessageFallbackByUserId(userMessage, userId);
            }
        } catch (Exception e) {
            System.out.println("Error in AI action analysis: " + e.getMessage());
            // Fallback to simple keyword detection
            return analyzeUserMessageFallbackByUserId(userMessage, userId);
        }

        // Generate chat response in Indonesian
        List<TodoEntity> todos = todoService.getMyTodosByUserId(userId);
        String context = buildContextForAIByUserId(todos, userId);
        String chatResponse = deepSeekService.generateResponse(userMessage, context);

        // Build complete response
        Map<String, Object> response = new HashMap<>();
        response.put("message", chatResponse);
        response.put("actions", actions);

        // Auto execute if requested and actionable
        if (autoExecute && (Boolean) actions.getOrDefault("actionable", false)) {
            try {
                String suggestedAction = (String) actions.get("suggested_action");
                @SuppressWarnings("unchecked")
                Map<String, Object> parameters = (Map<String, Object>) actions.get("parameters");
                Object actionResult = executeActionByUserId(suggestedAction, parameters, userId);
                response.put("action_result", actionResult);
                response.put("action_executed", true);
            } catch (Exception e) {
                response.put("action_error", "Failed to execute action: " + e.getMessage());
                response.put("action_executed", false);
            }
        } else {
            response.put("action_executed", false);
        }

        return response;
    }

    /**
     * Execute specific actions identified by AI using userId
     */
    public Object executeActionByUserId(String action, Map<String, Object> params, Long userId) {
        try {
            switch (action.toLowerCase()) {
                case "create_todo":
                    return createTodoByUserId(params, userId);

                case "list_todos":
                    return listTodosByUserId(params, userId);

                case "update_todo":
                    return updateTodoByUserId(params, userId);

                case "delete_todo":
                    return deleteTodoByUserId(params, userId);

                case "complete_todo":
                    return completeTodoByUserId(params, userId);

                case "search_todos":
                    return searchTodosByUserId(params, userId);

                case "get_statistics":
                    return getStatisticsByUserId(userId);

                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute action: " + e.getMessage(), e);
        }
    }

    /**
     * Execute specific actions identified by AI
     */
    public Object executeAction(String action, Map<String, Object> params, String username) {
        try {
            switch (action.toLowerCase()) {
                case "create_todo":
                    return createTodo(params, username);

                case "list_todos":
                    return listTodos(params, username);

                case "update_todo":
                    return updateTodo(params, username);

                case "delete_todo":
                    return deleteTodo(params, username);

                case "complete_todo":
                    return completeTodo(params, username);

                case "search_todos":
                    return searchTodos(params, username);

                case "get_statistics":
                    return getStatistics(username);

                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute action: " + e.getMessage(), e);
        }
    }

    /**
     * Build context string for AI with current todos and user info
     */
    private String buildContextForAI(List<TodoEntity> todos, String username) {
        StringBuilder context = new StringBuilder();
        context.append("User: ").append(username).append("\n");
        context.append("Current Todos:\n");

        if (todos.isEmpty()) {
            context.append("- No todos yet\n");
        } else {
            todos.forEach(todo -> {
                context.append("- ID: ").append(todo.getId())
                        .append(", Title: ").append(todo.getTitle())
                        .append(", Status: ").append(todo.isCompleted() ? "Completed" : "Pending")
                        .append(", Description: ").append(todo.getDescription() != null ? todo.getDescription() : "No description")
                        .append("\n");
            });
        }

        context.append("\nAnda dapat membantu user dengan:\n");
        context.append("- Membuat todo baru\n");
        context.append("- Menampilkan dan mencari todo\n");
        context.append("- Mengupdate judul dan deskripsi todo\n");
        context.append("- Menandai todo sebagai selesai atau pending\n");
        context.append("- Menghapus todo\n");
        context.append("- Mendapatkan statistik tentang todo\n");

        return context.toString();
    }

    /**
     * Build context string for AI with current todos and user info using userId
     */
    private String buildContextForAIByUserId(List<TodoEntity> todos, Long userId) {
        StringBuilder context = new StringBuilder();
        context.append("User ID: ").append(userId).append("\n");
        context.append("Current Todos:\n");

        if (todos.isEmpty()) {
            context.append("- No todos yet\n");
        } else {
            todos.forEach(todo -> {
                context.append("- ID: ").append(todo.getId())
                        .append(", Title: ").append(todo.getTitle())
                        .append(", Status: ").append(todo.isCompleted() ? "Completed" : "Pending")
                        .append(", Description: ").append(todo.getDescription() != null ? todo.getDescription() : "No description")
                        .append("\n");
            });
        }

        context.append("\nAnda dapat membantu user dengan:\n");
        context.append("- Membuat todo baru\n");
        context.append("- Menampilkan dan mencari todo\n");
        context.append("- Mengupdate judul dan deskripsi todo\n");
        context.append("- Menandai todo sebagai selesai atau pending\n");
        context.append("- Menghapus todo\n");
        context.append("- Mendapatkan statistik tentang todo\n");

        return context.toString();
    }

    /**
     * Extract JSON section (for nested objects)
     */
    private String extractJsonSection(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\{([^}]+)\\}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return "{" + m.group(1) + "}";
        }
        return null;
    }

    /**
     * Fallback analysis method using simple keyword detection
     */
    private Map<String, Object> analyzeUserMessageFallback(String userMessage, String username) {
        Map<String, Object> actions = new HashMap<>();
        String lowerMessage = userMessage.toLowerCase();

        // Indonesian keywords for creating todos
        if (lowerMessage.contains("buat") || lowerMessage.contains("tambah") || lowerMessage.contains("create") || lowerMessage.contains("add")) {
            actions.put("suggested_action", "create_todo");

            // Extract title and description from message
            Map<String, String> todoDetails = extractTodoDetailsFromMessage(userMessage);
            String title = todoDetails.get("title");
            String description = todoDetails.get("description");

            if (title != null && !title.trim().isEmpty()) {
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("title", title.trim());
                if (description != null && !description.trim().isEmpty()) {
                    parameters.put("description", description.trim());
                }
                actions.put("parameters", parameters);
                actions.put("actionable", true);
            } else {
                actions.put("actionable", false);
            }
        } else if (lowerMessage.contains("list") || lowerMessage.contains("show") || lowerMessage.contains("all todos") || lowerMessage.contains("tampilkan")) {
            actions.put("suggested_action", "list_todos");
            actions.put("actionable", true);
        } else if (lowerMessage.contains("complete") || lowerMessage.contains("finish") || lowerMessage.contains("done") || lowerMessage.contains("selesai")) {
            actions.put("suggested_action", "complete_todo");
            actions.put("actionable", true);
        } else if (lowerMessage.contains("delete") || lowerMessage.contains("remove") || lowerMessage.contains("hapus")) {
            actions.put("suggested_action", "delete_todo");
            actions.put("actionable", true);
        } else if (lowerMessage.contains("update") || lowerMessage.contains("edit") || lowerMessage.contains("change") || lowerMessage.contains("ubah")) {
            actions.put("suggested_action", "update_todo");
            actions.put("actionable", true);
        } else if (lowerMessage.contains("statistics") || lowerMessage.contains("stats") || lowerMessage.contains("summary") || lowerMessage.contains("statistik")) {
            actions.put("suggested_action", "get_statistics");
            actions.put("actionable", true);
        } else {
            actions.put("actionable", false);
        }

        // Generate chat response for fallback as well
        try {
            List<TodoEntity> todos = todoService.getMyTodos(username);
            String context = buildContextForAI(todos, username);
            String chatResponse = deepSeekService.generateResponse(userMessage, context);

            // Build complete response for fallback
            Map<String, Object> response = new HashMap<>();
            response.put("message", chatResponse);
            response.put("actions", actions);
            response.put("auto_execute", true); // Default to auto-execute
            response.put("context", Map.of(
                    "totalTodos", todos.size(),
                    "completedTodos", todos.stream().mapToInt(t -> t.isCompleted() ? 1 : 0).sum(),
                    "pendingTodos", todos.stream().mapToInt(t -> !t.isCompleted() ? 1 : 0).sum()
            ));

            return response;
        } catch (Exception e) {
            // If error generating chat response, still return actions
            return Map.of("actions", actions, "auto_execute", true);
        }
    }

    /**
     * Fallback analysis method using simple keyword detection with userId
     */
    private Map<String, Object> analyzeUserMessageFallbackByUserId(String userMessage, Long userId) {
        Map<String, Object> actions = new HashMap<>();
        String lowerMessage = userMessage.toLowerCase();

        // Indonesian keywords for creating todos
        if (lowerMessage.contains("buat") || lowerMessage.contains("tambah") || lowerMessage.contains("create") || lowerMessage.contains("add")) {
            actions.put("suggested_action", "create_todo");

            // Extract title and description from message
            Map<String, String> todoDetails = extractTodoDetailsFromMessage(userMessage);
            String title = todoDetails.get("title");
            String description = todoDetails.get("description");

            if (title != null && !title.trim().isEmpty()) {
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("title", title.trim());
                if (description != null && !description.trim().isEmpty()) {
                    parameters.put("description", description.trim());
                }
                actions.put("parameters", parameters);
                actions.put("actionable", true);
            } else {
                actions.put("actionable", false);
            }
        } else if (lowerMessage.contains("list") || lowerMessage.contains("show") || lowerMessage.contains("all todos") || lowerMessage.contains("tampilkan")) {
            actions.put("suggested_action", "list_todos");
            actions.put("actionable", true);
        } else if (lowerMessage.contains("complete") || lowerMessage.contains("finish") || lowerMessage.contains("done") || lowerMessage.contains("selesai")) {
            actions.put("suggested_action", "complete_todo");
            actions.put("actionable", true);
        } else if (lowerMessage.contains("delete") || lowerMessage.contains("remove") || lowerMessage.contains("hapus")) {
            actions.put("suggested_action", "delete_todo");
            actions.put("actionable", true);
        } else if (lowerMessage.contains("update") || lowerMessage.contains("edit") || lowerMessage.contains("change") || lowerMessage.contains("ubah")) {
            actions.put("suggested_action", "update_todo");
            actions.put("actionable", true);
        } else if (lowerMessage.contains("statistics") || lowerMessage.contains("stats") || lowerMessage.contains("summary") || lowerMessage.contains("statistik")) {
            actions.put("suggested_action", "get_statistics");
            actions.put("actionable", true);
        } else {
            actions.put("actionable", false);
        }

        // Generate chat response for fallback as well
        try {
            List<TodoEntity> todos = todoService.getMyTodosByUserId(userId);
            String context = buildContextForAIByUserId(todos, userId);
            String chatResponse = deepSeekService.generateResponse(userMessage, context);

            // Build complete response for fallback
            Map<String, Object> response = new HashMap<>();
            response.put("message", chatResponse);
            response.put("actions", actions);
            response.put("auto_execute", true); // Default to auto-execute
            response.put("context", Map.of(
                    "totalTodos", todos.size(),
                    "completedTodos", todos.stream().mapToInt(t -> t.isCompleted() ? 1 : 0).sum(),
                    "pendingTodos", todos.stream().mapToInt(t -> !t.isCompleted() ? 1 : 0).sum()
            ));

            return response;
        } catch (Exception e) {
            // If error generating chat response, still return actions
            return Map.of("actions", actions, "auto_execute", true);
        }
    }

    // Action implementation methods
    private TodoEntity createTodo(Map<String, Object> params, String username) {
        String title = (String) params.get("title");
        String description = (String) params.get("description");

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required for creating todo");
        }

        CreateTodoRequest request = new CreateTodoRequest(title, description);
        return todoService.create(username, request);
    }

    private List<TodoEntity> listTodos(Map<String, Object> params, String username) {
        return todoService.getMyTodos(username);
    }

    private Optional<TodoEntity> updateTodo(Map<String, Object> params, String username) {
        // Try to get ID first
        Long id = null;
        try {
            id = getLongParam(params, "id");
        } catch (Exception e) {
            // ID not provided or invalid, try to find by title reference
        }

        String title = (String) params.get("title");
        String description = (String) params.get("description");
        Boolean completed = (Boolean) params.get("completed");
        String titleReference = (String) params.get("title_reference");

        // If no ID, try to find by title reference or existing title
        if (id == null) {
            String searchTitle = titleReference != null ? titleReference : title;
            if (searchTitle != null && !searchTitle.trim().isEmpty()) {
                List<TodoEntity> todos = todoService.getMyTodos(username);
                Optional<TodoEntity> todoToUpdate = todos.stream()
                        .filter(todo -> todo.getTitle().equalsIgnoreCase(searchTitle.trim()))
                        .findFirst();

                if (todoToUpdate.isPresent()) {
                    id = todoToUpdate.get().getId();
                } else {
                    throw new IllegalArgumentException("Todo dengan judul '" + searchTitle + "' tidak ditemukan");
                }
            } else {
                throw new IllegalArgumentException("Parameter id atau title reference diperlukan untuk mengupdate todo");
            }
        }

        UpdateTodoRequest request = new UpdateTodoRequest(title, description, completed);
        return todoService.update(username, id, request);
    }

    private boolean deleteTodo(Map<String, Object> params, String username) {
        // Try to get ID first
        Long id = null;
        try {
            id = getLongParam(params, "id");
        } catch (Exception e) {
            // ID not provided or invalid, try to find by title
        }

        if (id != null) {
            return todoService.delete(username, id);
        }

        // If no ID, search by title
        String title = (String) params.get("title");
        if (title != null && !title.trim().isEmpty()) {
            List<TodoEntity> todos = todoService.getMyTodos(username);
            Optional<TodoEntity> todoToDelete = todos.stream()
                    .filter(todo -> todo.getTitle().equalsIgnoreCase(title.trim()))
                    .findFirst();

            if (todoToDelete.isPresent()) {
                return todoService.delete(username, todoToDelete.get().getId());
            } else {
                throw new IllegalArgumentException("Todo dengan judul '" + title + "' tidak ditemukan");
            }
        }

        throw new IllegalArgumentException("Parameter id atau title diperlukan untuk menghapus todo");
    }

    private Optional<TodoEntity> completeTodo(Map<String, Object> params, String username) {
        // Try to get ID first
        Long id = null;
        try {
            id = getLongParam(params, "id");
        } catch (Exception e) {
            // ID not provided or invalid, try to find by title
        }

        if (id != null) {
            return todoService.toggle(username, id);
        }

        // If no ID, search by title
        String title = (String) params.get("title");
        if (title != null && !title.trim().isEmpty()) {
            List<TodoEntity> todos = todoService.getMyTodos(username);
            Optional<TodoEntity> todoToComplete = todos.stream()
                    .filter(todo -> todo.getTitle().equalsIgnoreCase(title.trim()))
                    .findFirst();

            if (todoToComplete.isPresent()) {
                return todoService.toggle(username, todoToComplete.get().getId());
            } else {
                throw new IllegalArgumentException("Todo dengan judul '" + title + "' tidak ditemukan");
            }
        }

        throw new IllegalArgumentException("Parameter id atau title diperlukan untuk menandai todo selesai");
    }

    private List<TodoEntity> searchTodos(Map<String, Object> params, String username) {
        String query = (String) params.get("query");
        List<TodoEntity> allTodos = todoService.getMyTodos(username);

        if (query == null || query.trim().isEmpty()) {
            return allTodos;
        }

        return allTodos.stream()
                .filter(todo -> todo.getTitle().toLowerCase().contains(query.toLowerCase())
                || (todo.getDescription() != null && todo.getDescription().toLowerCase().contains(query.toLowerCase())))
                .toList();
    }

    private Map<String, Object> getStatistics(String username) {
        List<TodoEntity> todos = todoService.getMyTodos(username);

        int total = todos.size();
        int completed = (int) todos.stream().filter(TodoEntity::isCompleted).count();
        int pending = total - completed;
        double completionRate = total > 0 ? (double) completed / total * 100 : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("pending", pending);
        stats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        return stats;
    }

    /**
     * Extract title and description from user message for creating todo using
     * AI
     */
    private Map<String, String> extractTodoDetailsFromMessage(String userMessage) {
        Map<String, String> details = new HashMap<>();

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return details;
        }

        try {
            // Create prompt for AI to extract todo details
            String extractionPrompt = String.format(
                    "Analisis pesan berikut dan ekstrak informasi todo:\n\n"
                    + "Pesan: \"%s\"\n\n"
                    + "Tugas:\n"
                    + "1. Identifikasi apakah ini permintaan untuk membuat todo\n"
                    + "2. Ekstrak judul todo (wajib)\n"
                    + "3. Ekstrak deskripsi todo (opsional)\n\n"
                    + "Responmu harus dalam format JSON seperti ini:\n"
                    + "{\n"
                    + "  \"is_todo_request\": true/false,\n"
                    + "  \"title\": \"judul todo\",\n"
                    + "  \"description\": \"deskripsi todo atau null jika tidak ada\"\n"
                    + "}\n\n"
                    + "Contoh:\n"
                    + "- \"buat todo belajar\" → {\"is_todo_request\": true, \"title\": \"belajar\", \"description\": null}\n"
                    + "- \"tolong buatkan todo belajar renang dengan deskripsi belajar renang di surabaya\" → {\"is_todo_request\": true, \"title\": \"belajar renang\", \"description\": \"belajar renang di surabaya\"}\n\n"
                    + "Hanya berikan JSON response, tanpa penjelasan tambahan.",
                    userMessage
            );

            // Get AI response
            String aiResponse = deepSeekService.generateResponse(extractionPrompt, "");

            // Parse JSON response
            if (aiResponse != null && aiResponse.trim().startsWith("{")) {
                try {
                    // Simple JSON parsing for the specific format we expect
                    String jsonContent = aiResponse.trim();

                    // Extract is_todo_request
                    boolean isTodoRequest = jsonContent.contains("\"is_todo_request\": true");

                    if (isTodoRequest) {
                        // Extract title
                        String title = extractJsonValue(jsonContent, "title");
                        if (title != null && !title.trim().isEmpty() && !title.equals("null")) {
                            details.put("title", title.trim());
                        }

                        // Extract description
                        String description = extractJsonValue(jsonContent, "description");
                        if (description != null && !description.trim().isEmpty() && !description.equals("null")) {
                            details.put("description", description.trim());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error parsing AI response for todo extraction: " + e.getMessage());
                    // Fallback to simple extraction
                    return extractTodoDetailsFallback(userMessage);
                }
            } else {
                // Fallback to simple extraction
                return extractTodoDetailsFallback(userMessage);
            }

        } catch (Exception e) {
            System.out.println("Error in AI todo extraction: " + e.getMessage());
            // Fallback to simple extraction
            return extractTodoDetailsFallback(userMessage);
        }

        return details;
    }

    /**
     * Simple helper to extract JSON values
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }

        // Try without quotes for null values
        pattern = "\"" + key + "\"\\s*:\\s*(null)";
        p = java.util.regex.Pattern.compile(pattern);
        m = p.matcher(json);
        if (m.find()) {
            return null;
        }

        return null;
    }

    /**
     * Fallback extraction method using simple pattern matching
     */
    private Map<String, String> extractTodoDetailsFallback(String userMessage) {
        String message = userMessage.trim();
        String lowerMessage = message.toLowerCase();
        Map<String, String> details = new HashMap<>();

        String title = null;
        String description = null;

        // Pattern: "buat todo [title] dengan deskripsi [description]"
        if (lowerMessage.contains("dengan deskripsi")) {
            String[] parts = message.split("(?i)dengan deskripsi");
            if (parts.length >= 2) {
                String titlePart = parts[0].trim();
                description = parts[1].trim();

                // Extract title from first part - handle "tolong" prefix
                if (titlePart.toLowerCase().startsWith("tolong buat todo") || titlePart.toLowerCase().startsWith("tolong buatkan todo")) {
                    title = titlePart.replaceFirst("(?i)^tolong\\s+(buat(kan)?\\s+todo\\s*)", "").trim();
                } else if (titlePart.toLowerCase().startsWith("buat todo") || titlePart.toLowerCase().startsWith("buatkan todo")) {
                    title = titlePart.replaceFirst("(?i)(buat(kan)?\\s+todo\\s*)", "").trim();
                } else if (titlePart.toLowerCase().startsWith("tolong tambah todo")) {
                    title = titlePart.replaceFirst("(?i)^tolong\\s+(tambah\\s+todo\\s*)", "").trim();
                } else if (titlePart.toLowerCase().startsWith("tambah todo")) {
                    title = titlePart.replaceFirst("(?i)(tambah\\s+todo\\s*)", "").trim();
                }
            }
        } // Pattern: "tolong buat todo [title]" or "buat/buatkan todo [title]"
        else if (lowerMessage.startsWith("tolong buat todo ") || lowerMessage.startsWith("tolong buatkan todo ")) {
            if (lowerMessage.startsWith("tolong buat todo ")) {
                title = message.substring(17).trim(); // Remove "tolong buat todo "
            } else {
                title = message.substring(20).trim(); // Remove "tolong buatkan todo "
            }
        } else if (lowerMessage.startsWith("buat todo ") || lowerMessage.startsWith("buatkan todo ")) {
            if (lowerMessage.startsWith("buat todo ")) {
                title = message.substring(10).trim(); // Remove "buat todo "
            } else {
                title = message.substring(13).trim(); // Remove "buatkan todo "
            }
        } // Pattern: "tolong tambah todo [title]" or "tambah todo [title]"
        else if (lowerMessage.startsWith("tolong tambah todo ")) {
            title = message.substring(19).trim(); // Remove "tolong tambah todo "
        } else if (lowerMessage.startsWith("tambah todo ")) {
            title = message.substring(12).trim(); // Remove "tambah todo "
        } // Pattern: "tolong buat [title]" or "buat [title]" (without "todo" keyword)
        else if (lowerMessage.startsWith("tolong buat ") || lowerMessage.startsWith("tolong buatkan ")) {
            if (lowerMessage.startsWith("tolong buat ")) {
                title = message.substring(12).trim(); // Remove "tolong buat "
            } else {
                title = message.substring(15).trim(); // Remove "tolong buatkan "
            }
        } else if (lowerMessage.startsWith("buat ") || lowerMessage.startsWith("buatkan ")) {
            if (lowerMessage.startsWith("buat ")) {
                title = message.substring(5).trim(); // Remove "buat "
            } else {
                title = message.substring(8).trim(); // Remove "buatkan "
            }
        } // Pattern: "tolong tambah [title]" or "tambah [title]"
        else if (lowerMessage.startsWith("tolong tambah ")) {
            title = message.substring(14).trim(); // Remove "tolong tambah "
        } else if (lowerMessage.startsWith("tambah ")) {
            title = message.substring(7).trim(); // Remove "tambah "
        }

        // Clean up title if it contains "todo" at the beginning
        if (title != null && title.toLowerCase().startsWith("todo ")) {
            title = title.substring(5).trim();
        }

        // Handle null values properly
        if (title != null && !title.trim().isEmpty()) {
            details.put("title", title.trim());
        }
        if (description != null && !description.trim().isEmpty()) {
            details.put("description", description.trim());
        }

        return details;
    }

    private Long getLongParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException("Invalid " + key + " parameter");
    }

    // Action implementation methods with userId
    private TodoEntity createTodoByUserId(Map<String, Object> params, Long userId) {
        String title = (String) params.get("title");
        String description = (String) params.get("description");

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required for creating todo");
        }

        CreateTodoRequest request = new CreateTodoRequest(title, description);
        return todoService.createByUserId(userId, request);
    }

    private List<TodoEntity> listTodosByUserId(Map<String, Object> params, Long userId) {
        return todoService.getMyTodosByUserId(userId);
    }

    private Optional<TodoEntity> updateTodoByUserId(Map<String, Object> params, Long userId) {
        // Try to get ID first
        Long id = null;
        try {
            id = getLongParam(params, "id");
        } catch (Exception e) {
            // ID not provided or invalid, try to find by title reference
        }

        String title = (String) params.get("title");
        String description = (String) params.get("description");
        Boolean completed = (Boolean) params.get("completed");
        String titleReference = (String) params.get("title_reference");

        // If no ID, try to find by title reference or existing title
        if (id == null) {
            String searchTitle = titleReference != null ? titleReference : title;
            if (searchTitle != null && !searchTitle.trim().isEmpty()) {
                List<TodoEntity> todos = todoService.getMyTodosByUserId(userId);
                Optional<TodoEntity> todoToUpdate = todos.stream()
                        .filter(todo -> todo.getTitle().equalsIgnoreCase(searchTitle.trim()))
                        .findFirst();

                if (todoToUpdate.isPresent()) {
                    id = todoToUpdate.get().getId();
                } else {
                    throw new IllegalArgumentException("Todo dengan judul '" + searchTitle + "' tidak ditemukan");
                }
            } else {
                throw new IllegalArgumentException("Parameter id atau title reference diperlukan untuk mengupdate todo");
            }
        }

        UpdateTodoRequest request = new UpdateTodoRequest(title, description, completed);
        return todoService.updateByUserId(userId, id, request);
    }

    private boolean deleteTodoByUserId(Map<String, Object> params, Long userId) {
        // Try to get ID first
        Long id = null;
        try {
            id = getLongParam(params, "id");
        } catch (Exception e) {
            // ID not provided or invalid, try to find by title reference
        }

        String titleReference = (String) params.get("title_reference");
        String title = (String) params.get("title");

        // If no ID, try to find by title reference or title
        if (id == null) {
            String searchTitle = titleReference != null ? titleReference : title;
            if (searchTitle != null && !searchTitle.trim().isEmpty()) {
                List<TodoEntity> todos = todoService.getMyTodosByUserId(userId);
                Optional<TodoEntity> todoToDelete = todos.stream()
                        .filter(todo -> todo.getTitle().equalsIgnoreCase(searchTitle.trim()))
                        .findFirst();

                if (todoToDelete.isPresent()) {
                    id = todoToDelete.get().getId();
                } else {
                    throw new IllegalArgumentException("Todo dengan judul '" + searchTitle + "' tidak ditemukan");
                }
            } else {
                throw new IllegalArgumentException("Parameter id atau title reference diperlukan untuk menghapus todo");
            }
        }

        return todoService.deleteByUserId(userId, id);
    }

    private Optional<TodoEntity> completeTodoByUserId(Map<String, Object> params, Long userId) {
        // Try to get ID first
        Long id = null;
        try {
            id = getLongParam(params, "id");
        } catch (Exception e) {
            // ID not provided or invalid, try to find by title reference
        }

        String titleReference = (String) params.get("title_reference");
        String title = (String) params.get("title");

        // If no ID, try to find by title reference or title
        if (id == null) {
            String searchTitle = titleReference != null ? titleReference : title;
            if (searchTitle != null && !searchTitle.trim().isEmpty()) {
                List<TodoEntity> todos = todoService.getMyTodosByUserId(userId);
                Optional<TodoEntity> todoToComplete = todos.stream()
                        .filter(todo -> todo.getTitle().equalsIgnoreCase(searchTitle.trim()))
                        .findFirst();

                if (todoToComplete.isPresent()) {
                    id = todoToComplete.get().getId();
                } else {
                    throw new IllegalArgumentException("Todo dengan judul '" + searchTitle + "' tidak ditemukan");
                }
            } else {
                throw new IllegalArgumentException("Parameter id atau title reference diperlukan untuk menandai todo selesai");
            }
        }

        return todoService.toggleByUserId(userId, id);
    }

    private List<TodoEntity> searchTodosByUserId(Map<String, Object> params, Long userId) {
        String searchTerm = (String) params.get("search_term");
        String title = (String) params.get("title");

        // Use title as search term if search_term is not provided
        String actualSearchTerm = searchTerm != null ? searchTerm : title;

        if (actualSearchTerm == null || actualSearchTerm.trim().isEmpty()) {
            // If no search term, return all todos
            return todoService.getMyTodosByUserId(userId);
        }

        // Simple search implementation - filter todos by title or description containing search term
        List<TodoEntity> allTodos = todoService.getMyTodosByUserId(userId);
        return allTodos.stream()
                .filter(todo
                        -> todo.getTitle().toLowerCase().contains(actualSearchTerm.toLowerCase())
                || (todo.getDescription() != null && todo.getDescription().toLowerCase().contains(actualSearchTerm.toLowerCase()))
                )
                .toList();
    }

    private Map<String, Object> getStatisticsByUserId(Long userId) {
        List<TodoEntity> todos = todoService.getMyTodosByUserId(userId);

        long totalTodos = todos.size();
        long completedTodos = todos.stream().mapToLong(t -> t.isCompleted() ? 1 : 0).sum();
        long pendingTodos = totalTodos - completedTodos;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_todos", totalTodos);
        stats.put("completed_todos", completedTodos);
        stats.put("pending_todos", pendingTodos);
        stats.put("completion_rate", totalTodos > 0 ? (double) completedTodos / totalTodos * 100 : 0.0);

        return stats;
    }
}
