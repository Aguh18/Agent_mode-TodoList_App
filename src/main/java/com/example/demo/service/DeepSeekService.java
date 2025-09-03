package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class DeepSeekService {
    
    @Value("${deepseek.api.key}")
    private String apiKey;
    
    @Value("${deepseek.api.url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public DeepSeekService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public String generateResponse(String prompt, String context) {
        // Check if API key is configured
        if (apiKey == null || apiKey.equals("sk-your-deepseek-api-key-here") || apiKey.equals("your-deepseek-api-key")) {
            System.out.println("DeepSeek API key not configured. Please set your API key in application.properties");
            return "Maaf, API DeepSeek belum dikonfigurasi dengan benar. Silakan tambahkan API key yang valid di file application.properties.";
        }
        
        try {
            return callDeepSeekAPI(prompt, context);
        } catch (Exception e) {
            System.err.println("Error calling DeepSeek API: " + e.getMessage());
            e.printStackTrace();
            return "Maaf, terjadi kesalahan saat menghubungi DeepSeek API. Silakan coba lagi nanti.";
        }
    }
    
    /**
     * Call the actual DeepSeek API
     */
    private String callDeepSeekAPI(String prompt, String context) throws Exception {
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        // Prepare request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("max_tokens", 1000);
        requestBody.put("temperature", 0.7);
        requestBody.put("stream", false);
        
        // Prepare messages
        List<Map<String, String>> messages = new ArrayList<>();
        
        // System message with Indonesian context
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", buildSystemPrompt());
        messages.add(systemMessage);
        
        // Add context if available
        if (context != null && !context.trim().isEmpty()) {
            Map<String, String> contextMessage = new HashMap<>();
            contextMessage.put("role", "assistant");
            contextMessage.put("content", "Konteks todo saat ini:\n" + context);
            messages.add(contextMessage);
        }
        
        // User message
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        
        // Create request entity
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Make API call
        ResponseEntity<String> response = restTemplate.exchange(
            apiUrl, 
            HttpMethod.POST, 
            entity, 
            String.class
        );
        
        // Parse response
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode choices = responseJson.get("choices");
        
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    return content.asText();
                }
            }
        }
        
        throw new RuntimeException("Invalid response format from DeepSeek API");
    }
    
    /**
     * Generate JSON analysis using DeepSeek API
     */
    public String generateJsonAnalysis(String prompt, String context) {
        // Check if API key is configured
        if (apiKey == null || apiKey.equals("sk-your-deepseek-api-key-here") || apiKey.equals("your-deepseek-api-key")) {
            return generateFallbackJsonAnalysis(prompt);
        }
        
        try {
            return callDeepSeekAPIForAnalysis(prompt, context);
        } catch (Exception e) {
            System.err.println("Error calling DeepSeek API for analysis: " + e.getMessage());
            return generateFallbackJsonAnalysis(prompt);
        }
    }
    
    /**
     * Call DeepSeek API specifically for JSON analysis
     */
    private String callDeepSeekAPIForAnalysis(String prompt, String context) throws Exception {
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        // Prepare request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("max_tokens", 500);
        requestBody.put("temperature", 0.1); // Lower temperature for more consistent JSON
        requestBody.put("stream", false);
        
        // Prepare messages for JSON analysis
        List<Map<String, String>> messages = new ArrayList<>();
        
        // System message for JSON analysis
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", buildJsonAnalysisPrompt());
        messages.add(systemMessage);
        
        // Add context if available
        if (context != null && !context.trim().isEmpty()) {
            Map<String, String> contextMessage = new HashMap<>();
            contextMessage.put("role", "assistant");
            contextMessage.put("content", "Konteks todo saat ini:\n" + context);
            messages.add(contextMessage);
        }
        
        // User message
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        
        // Create request entity
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Make API call
        ResponseEntity<String> response = restTemplate.exchange(
            apiUrl, 
            HttpMethod.POST, 
            entity, 
            String.class
        );
        
        // Parse response
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode choices = responseJson.get("choices");
        
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    String responseContent = content.asText().trim();
                    
                    // Extract JSON from response if it's wrapped in text
                    if (responseContent.contains("{") && responseContent.contains("}")) {
                        int startIndex = responseContent.indexOf("{");
                        int endIndex = responseContent.lastIndexOf("}") + 1;
                        return responseContent.substring(startIndex, endIndex);
                    }
                    
                    return responseContent;
                }
            }
        }
        
        throw new RuntimeException("Invalid response format from DeepSeek API");
    }
    
    /**
     * Build system prompt for general responses
     */
    private String buildSystemPrompt() {
        return "Anda adalah asisten AI cerdas untuk aplikasi todo list berbahasa Indonesia. " +
               "Tugas Anda adalah membantu pengguna mengelola todo mereka dengan cara yang ramah dan efektif.\n\n" +
               "Karakteristik Anda:\n" +
               "- Berbicara dalam bahasa Indonesia yang natural dan ramah\n" +
               "- Memberikan saran yang konstruktif tentang manajemen todo\n" +
               "- Memahami konteks dan memberikan respons yang relevan\n" +
               "- Dapat membantu dengan berbagai operasi todo: membuat, melihat, mengupdate, menghapus, dan menandai selesai\n" +
               "- Memberikan motivasi dan tips produktivitas\n\n" +
               "Selalu berikan respons yang helpful, informatif, dan mendorong produktivitas pengguna.";
    }
    
    /**
     * Build system prompt for JSON analysis
     */
    private String buildJsonAnalysisPrompt() {
        return "Anda adalah sistem analisis intent untuk aplikasi todo list. " +
               "Tugas Anda adalah menganalisis pesan pengguna dan mengidentifikasi aksi yang ingin dilakukan.\n\n" +
               "Berikan respons dalam format JSON dengan struktur berikut:\n" +
               "{\n" +
               "  \"suggested_action\": \"nama_aksi\",\n" +
               "  \"actionable\": true/false,\n" +
               "  \"parameters\": {\n" +
               "    \"title\": \"judul todo (jika ada)\",\n" +
               "    \"description\": \"deskripsi todo (jika ada)\",\n" +
               "    \"id\": \"id todo (jika ada)\"\n" +
               "  },\n" +
               "  \"auto_execute\": true/false\n" +
               "}\n\n" +
               "Aksi yang tersedia:\n" +
               "- create_todo: membuat todo baru\n" +
               "- list_todos: menampilkan daftar todo\n" +
               "- update_todo: mengupdate todo\n" +
               "- delete_todo: menghapus todo\n" +
               "- complete_todo: menandai todo selesai\n" +
               "- get_statistics: melihat statistik todo\n" +
               "- none: tidak ada aksi spesifik\n\n" +
               "Set auto_execute=true jika aksi dapat dieksekusi langsung tanpa parameter tambahan.";
    }
    
    /**
     * Fallback JSON analysis when API is not available
     */
    private String generateFallbackJsonAnalysis(String prompt) {
        String lowerMessage = prompt.toLowerCase();
        
        if (lowerMessage.contains("buat") || lowerMessage.contains("tambah") || lowerMessage.contains("create") || lowerMessage.contains("add")) {
            String title = prompt.replaceFirst("(?i)^(tolong\\s+)?(buat(kan)?|tambah(kan)?)\\s+(todo\\s+)?", "").trim();
            return String.format("{\"suggested_action\": \"create_todo\", \"actionable\": true, \"parameters\": {\"title\": \"%s\"}, \"auto_execute\": true}", title);
        } else if (lowerMessage.contains("list") || lowerMessage.contains("show") || lowerMessage.contains("tampilkan") || lowerMessage.contains("semua")) {
            return "{\"suggested_action\": \"list_todos\", \"actionable\": true, \"parameters\": {}, \"auto_execute\": true}";
        } else if (lowerMessage.contains("hapus") || lowerMessage.contains("delete") || lowerMessage.contains("remove")) {
            return "{\"suggested_action\": \"delete_todo\", \"actionable\": true, \"parameters\": {}, \"auto_execute\": false}";
        } else if (lowerMessage.contains("selesai") || lowerMessage.contains("complete") || lowerMessage.contains("done") || lowerMessage.contains("tandai")) {
            return "{\"suggested_action\": \"complete_todo\", \"actionable\": true, \"parameters\": {}, \"auto_execute\": false}";
        } else if (lowerMessage.contains("ubah") || lowerMessage.contains("update") || lowerMessage.contains("edit")) {
            return "{\"suggested_action\": \"update_todo\", \"actionable\": true, \"parameters\": {}, \"auto_execute\": false}";
        } else if (lowerMessage.contains("statistik") || lowerMessage.contains("stats") || lowerMessage.contains("summary")) {
            return "{\"suggested_action\": \"get_statistics\", \"actionable\": true, \"parameters\": {}, \"auto_execute\": true}";
        } else {
            return "{\"suggested_action\": \"none\", \"actionable\": false, \"parameters\": {}, \"auto_execute\": false}";
        }
    }
}
