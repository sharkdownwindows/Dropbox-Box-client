# Lộ trình tự build Dropbox client, sau đó mở rộng Box

Đọc theo thứ tự. Mỗi bài xác định input, output, ràng buộc, kiến thức,
các bước làm, gợi ý code và cách nghiệm thu. Đoạn code ghi **gợi ý** cần được
ghép vào đúng lớp và hoàn thiện xử lý lỗi; không phải mọi đoạn đều có sẵn trong app.

## Đường đi

| Bài | Việc làm | Kết quả cần chứng minh |
|---|---|---|
| [00 — Đọc tài liệu](00-materials-and-scope.md) | Hiểu đề, đối chiếu slide cũ với API mới | Phạm vi và kiến thức nền rõ ràng |
| [01 — Thiết kế](01-requirements-and-architecture.md) | User story, màn hình, contract | Sơ đồ và tiêu chí MVP |
| [02 — Setup](02-setup.md) | JDK, SDK, Gradle, Git | Build và chạy demo giả |
| [03 — UI và lifecycle](03-ui-and-lifecycle.md) | List, điều hướng, trạng thái, xoay màn hình | UI hoạt động khi chưa có mạng |
| [04 — OAuth](04-dropbox-auth.md) | App Console, PKCE, lưu phiên | Đăng nhập/đăng xuất thật |
| [05 — Networking](05-browse-and-networking.md) | Repository Dropbox, phân trang, lỗi | Duyệt file thật đúng và đủ |
| [06 — Truyền file](06-upload-download.md) | SAF, stream, upload/download | File đi và về không đổi byte |
| [07 — Quản lý file](07-file-actions.md) | Tạo thư mục, rename, delete, share tùy chọn | Thao tác đúng, có xác nhận |
| [08 — Độ bền và UX](08-reliability-and-ux.md) | Lifecycle, retry, cache, touch | Không treo, không ghi đè kết quả mới |
| [09 — Test và demo](09-testing-and-demo.md) | Unit/integration/manual, bài trình bày | Bằng chứng hoàn thành MVP |
| [10 — Box mở rộng](10-box-extension.md) | Auth riêng, mapping ID/API | Thay provider mà giữ UI |
| [11 — Tham khảo](11-references.md) | Nguồn chính thức và bài đọc | Biết đọc trang nào, để giải quyết gì |
| [12 — Kiểm chứng](12-verification.md) | Xem phần đã/chưa kiểm tra của khung | Không nhầm skeleton với sản phẩm |
| [13 — Chạy mẫu Dropbox](13-run-dropbox-sample.md) | Cấu hình và đọc code OAuth/browse đã triển khai | Chạy thử với tài khoản của bạn |
| [14 — AI handoff](14-ai-handoff.md) | Chuyển máy/phiên AI bằng ngữ cảnh trong Git | Tiếp tục mà không đọc lại toàn bộ lịch sử |

## Cách học mỗi bài

1. Đọc input/output và phần slide được chỉ định; tự giải thích lại bằng lời.
2. Chạy phần trước đã ổn; chỉ thêm một luồng mới trong một lần làm.
3. Viết code tại vị trí được gợi ý, chạy ca thành công **và** ca thất bại.
4. Ghi bằng chứng: phiên bản, thiết bị, bước tái hiện, kỳ vọng, kết quả thực tế.
5. Trả lời câu hỏi checkpoint ở cuối bài; chưa giải thích được thì chưa chuyển bài.

Ước lượng để lập kế hoạch, không phải thời lượng môn học: 8–10 buổi,
mỗi buổi 2–3 giờ cho nhóm đã biết Java/OOP. OAuth và môi trường có thể cần thêm thời gian.
Chỉ làm Box khi các ca nghiệm thu Dropbox ở bài 09 đã đạt.

## Ba mức hoàn thành

- **Code mẫu hiện tại:** demo + OAuth PKCE, lưu phiên mã hóa và browse Dropbox.
  Build/unit test đã chạy; OAuth/Keystore/API trên thiết bị chưa được kiểm chứng (bài 12–13).
- **MVP mục tiêu:** Dropbox thật; login/logout, browse + pagination, upload/download,
  tạo thư mục, đổi tên, xóa có xác nhận, thông báo lỗi.
- **Mở rộng:** preview, share, cache metadata, transfer bền qua process death, Box.

MVP và mức mở rộng là đề xuất kỹ thuật của bộ guide. Slide được cung cấp không
đưa ra một rubric chi tiết riêng cho Dropbox/Box; cần đối chiếu yêu cầu cập nhật của giảng viên.
