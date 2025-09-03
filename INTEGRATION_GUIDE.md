# Panduan Integrasi Dashboard dengan API

## Overview

Dashboard telah berhasil terintegrasi dengan API yang sudah ada dan menambahkan fitur Agent Mode yang memungkinkan pengguna berinteraksi dengan AI untuk mengelola todo mereka.

## Fitur yang Terintegrasi

### 1. Todo Management API
- **GET** `/api/todo/my-todos` - Mengambil semua todo pengguna
- **POST** `/api/todo/add` - Membuat todo baru
- **PUT** `/api/todo/update/{id}` - Mengupdate todo
- **PUT** `/api/todo/toggle/{id}` - Toggle status todo
- **PUT** `/api/todo/complete/{id}` - Menandai todo sebagai selesai
- **PUT** `/api/todo/uncomplete/{id}` - Menandai todo sebagai pending
- **DELETE** `/api/todo/delete/{id}` - Menghapus todo

### 2. AI Agent API
- **POST** `/api/agent/chat` - Chat dengan AI agent
- **POST** `/api/agent/execute` - Eksekusi perintah AI
- **POST** `/api/agent/context/todos` - Mendapatkan konteks todo untuk AI

### 3. Authentication API
- **POST** `/api/auth/login` - Login pengguna
- **POST** `/api/auth/register` - Registrasi pengguna

## Fitur Dashboard

### 1. Dashboard Utama
- **Statistik Todo**: Menampilkan total, selesai, pending, dan tingkat penyelesaian
- **Quick Actions**: Tombol untuk menambah todo dan refresh data
- **Navigation**: Menu navigasi ke berbagai section

### 2. Todo Management
- **CRUD Operations**: Create, Read, Update, Delete todo
- **Bulk Operations**: Operasi massal untuk multiple todo
- **Search & Filter**: Pencarian dan filter berdasarkan status
- **Sorting**: Pengurutan berdasarkan tanggal dan judul

### 3. Agent Mode
- **AI Chat Interface**: Chat dengan AI dalam bahasa Indonesia
- **Auto Execute**: Eksekusi otomatis perintah AI
- **Quick Actions**: Tombol aksi cepat untuk operasi umum
- **Context Awareness**: AI memahami konteks todo pengguna

## Cara Kerja Integrasi

### 1. Authentication
```javascript
// Check authentication on page load
function checkAuth() {
  const token = localStorage.getItem("authToken");
  if (!token) {
    window.location.href = "/login";
    return false;
  }
  return token;
}
```

### 2. API Request Helper
```javascript
// Centralized API request function
async function apiRequest(url, options = {}) {
  const token = localStorage.getItem("authToken");
  const defaultOptions = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  };

  const response = await fetch(url, { ...defaultOptions, ...options });
  
  if (response.status === 401) {
    localStorage.removeItem("authToken");
    window.location.href = "/login";
    return;
  }

  return response.json();
}
```

### 3. Agent Mode Integration
```javascript
// Send message to AI agent
async function sendAgentMessage() {
  const response = await apiRequest("/api/agent/chat", {
    method: "POST",
    body: JSON.stringify({
      message: message,
      language: "id",
      auto_execute: true,
    }),
  });

  if (response.success) {
    // Handle AI response
    addAgentMessage("ai", response.data.message);
    
    // Check for actionable commands
    if (response.data.actions && response.data.actions.actionable) {
      await autoExecuteAction(response.data.actions);
    }
  }
}
```

## Error Handling

### 1. Enhanced Error Handling
```javascript
function handleApiError(error, context = "operation") {
  let errorMessage = "Terjadi kesalahan. Silakan coba lagi.";
  
  if (error.message) {
    if (error.message.includes("401")) {
      errorMessage = "Sesi Anda telah berakhir. Silakan login kembali.";
    } else if (error.message.includes("403")) {
      errorMessage = "Anda tidak memiliki izin untuk melakukan operasi ini.";
    } else if (error.message.includes("404")) {
      errorMessage = "Data tidak ditemukan.";
    } else if (error.message.includes("500")) {
      errorMessage = "Terjadi kesalahan server. Silakan coba lagi nanti.";
    }
  }
  
  showError(errorMessage);
  return errorMessage;
}
```

### 2. Notification System
```javascript
function showNotification(message, type = "success", duration = 5000) {
  const notification = document.createElement("div");
  notification.className = `alert alert-${type} notification alert-dismissible fade show`;
  notification.innerHTML = `
    ${message}
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
  `;
  
  // Auto remove after specified duration
  setTimeout(() => {
    if (notification.parentNode) {
      notification.remove();
    }
  }, duration);
}
```

## Fitur Agent Mode

### 1. Auto Execute Actions
Agent mode dapat secara otomatis mengeksekusi perintah seperti:
- **create_todo**: Membuat todo baru
- **complete_todo**: Menandai todo sebagai selesai
- **delete_todo**: Menghapus todo
- **list_todos**: Menampilkan daftar todo
- **get_statistics**: Menampilkan statistik

### 2. Context Awareness
AI agent memahami konteks todo pengguna dan dapat memberikan saran yang relevan berdasarkan:
- Jumlah todo total
- Todo yang sudah selesai
- Todo yang masih pending
- Tingkat penyelesaian

### 3. Language Support
Agent mode mendukung bahasa Indonesia dan dapat:
- Memahami perintah dalam bahasa Indonesia
- Memberikan respons dalam bahasa Indonesia
- Menangani error dalam bahasa Indonesia

## Cara Penggunaan

### 1. Login
1. Akses halaman login
2. Masukkan username dan password
3. Token JWT akan disimpan di localStorage

### 2. Dashboard
1. Dashboard akan menampilkan statistik todo
2. Gunakan menu navigasi untuk berpindah section
3. Gunakan tombol "Add Todo" untuk menambah todo baru

### 3. Agent Mode
1. Klik menu "Agent Mode" di sidebar
2. Ketik pesan dalam bahasa Indonesia
3. AI akan merespons dan mengeksekusi perintah jika memungkinkan
4. Gunakan tombol "Quick Actions" untuk aksi cepat

### 4. Todo Management
1. Gunakan search dan filter untuk menemukan todo
2. Gunakan bulk actions untuk operasi massal
3. Klik dropdown menu pada todo untuk aksi individual

## Troubleshooting

### 1. Authentication Issues
- Pastikan token JWT valid
- Refresh halaman jika token expired
- Login ulang jika diperlukan

### 2. API Connection Issues
- Periksa koneksi internet
- Pastikan server API berjalan
- Periksa console browser untuk error details

### 3. Agent Mode Issues
- Pastikan AI service berjalan
- Periksa response dari `/api/agent/chat`
- Gunakan browser console untuk debugging

## Security Considerations

1. **JWT Token**: Token disimpan di localStorage dengan expiration
2. **Authorization**: Semua request menggunakan Bearer token
3. **Input Validation**: Input divalidasi di frontend dan backend
4. **Error Handling**: Error tidak menampilkan informasi sensitif

## Performance Optimizations

1. **Lazy Loading**: Data dimuat sesuai kebutuhan
2. **Caching**: Todo data di-cache di frontend
3. **Debouncing**: Search input menggunakan debouncing
4. **Optimistic Updates**: UI update sebelum API response

## Future Enhancements

1. **Real-time Updates**: WebSocket untuk update real-time
2. **Offline Support**: Service worker untuk offline mode
3. **Advanced AI**: Machine learning untuk prediksi todo
4. **Multi-language**: Support untuk bahasa lain
5. **Mobile App**: Native mobile application
