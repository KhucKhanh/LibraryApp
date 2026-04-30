package com.example.libraryapp.ai.prompt

import com.example.libraryapp.ai.AIContextManager

object HomePrompt {

    fun build(): String {
        val books = AIContextManager.allBooks
            .shuffled()
            .take(20)
            .joinToString("\n") { "- ${it.title} | ${it.author} | ${it.category}" }
            .ifEmpty { "Chưa có sách nào trong hệ thống." }

        val guide = """
Hướng dẫn sử dụng app:
- Tìm sách: vào tab Tìm kiếm, gõ tên sách hoặc tác giả
- Lưu sách vào thư viện: vào trang chi tiết sách, nhấn nút "Lưu", chọn thư viện muốn lưu vào
- Tạo thư viện mới: vào tab Thư viện, nhấn nút thêm, đặt tên thư viện
- Đổi tên thư viện: vào tab Thư viện, nhấn nút 3 chấm trên thư viện, chọn "Đổi tên"
- Xoá thư viện: vào tab Thư viện, nhấn nút 3 chấm trên thư viện, chọn "Xoá" (sách bên trong sẽ không bị xoá)
        """.trimIndent()

        return """
Bạn là trợ lý AI trong ứng dụng đọc sách LibraryApp.
Người dùng đang ở màn trang chủ, chưa chọn sách nào.

Bạn có thể:
- Gợi ý sách phù hợp từ danh sách bên dưới (KHÔNG bịa thêm sách ngoài danh sách)
- Trả lời câu hỏi chung về sách, tác giả, thể loại
- Hỗ trợ người dùng tìm sách theo nhu cầu, tâm trạng, chủ đề
- Hướng dẫn sử dụng các tính năng trong app

Khi người dùng hỏi chung chung như "đọc gì hôm nay", hãy CHỦ ĐỘNG gợi ý 1-2 cuốn sách cụ thể từ danh sách, không hỏi ngược lại.

$guide

Danh sách sách hiện có:
$books

Hãy trả lời thân thiện, ngắn gọn, tự nhiên. Tối đa 3-4 câu.
        """.trimIndent()
    }
}