# Panduan Menggunakan DeepSeek API

## Langkah 1: Dapatkan API Key DeepSeek

1. Buka website DeepSeek di: https://platform.deepseek.com/
2. Daftar akun atau login jika sudah punya akun
3. Buka halaman API Keys atau Developer Console
4. Buat API key baru
5. Salin API key yang diberikan (format: sk-xxxxxxxxxxxxxxxx)

## Langkah 2: Konfigurasi API Key

1. Buka file `src/main/resources/application.properties`
2. Ganti baris berikut:
   ```
   deepseek.api.key=sk-your-deepseek-api-key-here
   ```
   Dengan API key yang sesungguhnya:
   ```
   deepseek.api.key=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```

## Langkah 3: Test Aplikasi

Setelah mengkonfigurasi API key:

1. Restart aplikasi Spring Boot
2. Buka aplikasi di browser
3. Coba kirim pesan ke AI assistant
4. AI akan menggunakan DeepSeek API yang sesungguhnya

## Fitur yang Tersedia dengan DeepSeek API:

### 1. Analisis Intent yang Cerdas

- AI dapat memahami konteks percakapan yang kompleks
- Deteksi aksi yang lebih akurat
- Pemahaman bahasa Indonesia yang natural

### 2. Respons Conversational yang Natural

- AI memberikan respons yang lebih manusiawi
- Dapat memberikan saran dan tips produktivitas
- Konteks percakapan yang lebih baik

### 3. Auto-Execute yang Pintar

- AI dapat menentukan kapan aksi harus dijalankan otomatis
- Parameter extraction yang lebih akurat
- Konfirmasi yang tepat untuk aksi sensitif

## Contoh Percakapan:

**User:** "Tolong buatkan todo untuk meeting dengan client besok jam 2 siang"

**AI Response:** "Baik! Saya telah membuat todo untuk meeting dengan client besok jam 2 siang. Todo ini penting untuk persiapan bisnis Anda. Apakah ada hal khusus yang perlu Anda persiapkan untuk meeting tersebut?"

**User:** "Tampilkan semua todo saya"

**AI Response:** "Berikut adalah daftar todo Anda saat ini:

- Meeting dengan client (besok 14:00) - Pending
- Review laporan bulanan - Pending
- Belanja groceries - Completed

Anda memiliki 2 todo yang masih pending. Mana yang ingin Anda selesaikan terlebih dahulu?"

## Troubleshooting:

### Jika API Key tidak valid:

- Pastikan API key dimulai dengan "sk-"
- Cek apakah API key masih aktif di dashboard DeepSeek
- Pastikan tidak ada spasi tambahan di API key

### Jika koneksi gagal:

- Cek koneksi internet
- Pastikan tidak ada firewall yang memblokir
- Coba restart aplikasi

### Fallback Mode:

Jika API tidak tersedia, aplikasi akan otomatis menggunakan mode fallback dengan respons yang sudah diprogrammed sebelumnya.
