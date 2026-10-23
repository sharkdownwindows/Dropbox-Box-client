# 00 — Đọc tài liệu và chốt phạm vi

**Input:** PDF bài giảng trong thư mục cha repo; kiến thức Java/OOP cơ bản.

**Output:** bảng liên hệ kiến thức → tính năng và backlog MVP của nhóm.

**Ràng buộc:** ứng dụng Android native; Dropbox trước, Box mở rộng; không coi
slide lịch sử là hướng dẫn phiên bản thư viện hiện tại.

## Tài liệu đã đọc

Các số trang dưới đây là **trang vật lý của PDF**, bắt đầu từ 1. Slide có animation
lặp nên số trang in ở chân slide có thể khác. Đã đọc phần văn bản trích xuất của
9 PDF Mobile; kiểm tra hình trực tiếp ở 6 trang chọn lọc. Không tuyên bố đã xem
từng hình ở mọi trang.

| Tài liệu trong thư mục cha | Số trang | Đọc để dựng gì |
|---|---:|---|
| `1. intro.md.pdf` | 19 | Tr. 3–8: mục tiêu, đánh giá, nhóm 5 người, đề tài số 5; tr. 10–18: bài trình bày |
| `3. intro_android.md.pdf` | 37 | Tr. 21, 31–33: bất đồng bộ, OOP/Java, HTTP, XML/JSON, Android Studio |
| `4. fundamentals.md.pdf` | 65 | Tr. 19–24: Context/Application/Manifest; 29–35: lifecycle/Intent; 43–52: Fragment; 56–64: View |
| `5. resources.md.pdf` | 101 | Tr. 10–24: XML/layout; 48–60: resources/i18n; 67–74: streams; 75–101: menu |
| `6. tasks.md.pdf` | 42 | Tr. 9–18: main/worker/Handler; 21–42: AsyncTask lịch sử |
| `7. networking.p1.md.pdf` | 41 | Tr. 12–19: permissions; 21–30: HTTP, response, stream |
| `7. networking.p2.md.pdf` | 72 | 41 trang đầu trùng phần 1; tr. 42–54: Volley; 55–72: JSON |
| `8. touch.p1.md.pdf` | 15 | Tr. 11–15: LayoutParams |
| `8. touch.p2.md.pdf` | 48 | Tr. 1–15 trùng phần 1; 21–34: MotionEvent; 36–48: GestureDetector |

Các trang đã kiểm tra hình: intro 7 và 11; fundamentals 29; resources 9;
tasks 9; touch.p2 33. Riêng trang touch.p2 33 có `topMargin = startViewY + dx`:
theo phép dịch chuyển theo hai trục phải dùng **dy** cho topMargin.

Ở lần kiểm kê đầu có `4_Exam Requirements.pdf`, nội dung thuộc **Deep Learning**,
không phải Mobile. File không còn trong thư mục ở lần kiểm tra tiếp theo.
Không dùng deadline, số trang báo cáo hay tiêu chí của file đó cho project này.

## Những gì slide thực sự yêu cầu

Slide intro nêu project Dropbox/Box client và các phần trình bày: introduction,
architecture, activities, networking, optimization, demo, conclusion. Slide còn
nêu demo bằng emulator, chuẩn bị kết nối mạng và trình bày phần chưa làm được.
Đây là bằng chứng trong bộ tài liệu; chưa xác nhận các thời lượng/quy định này
còn áp dụng cho lớp hiện tại.

Không có file rubric Dropbox riêng trong thư mục. Upload/download, CRUD và tiêu chí
test trong guide là **phạm vi MVP đề xuất**, không phải trích nguyên yêu cầu thi.

## Kiến thức cần dùng và cách cập nhật

| Trong slide | Áp dụng vào client | Lựa chọn trong guide |
|---|---|---|
| OOP, interface | Tách UI khỏi nguồn file | `CloudRepository` + model Java |
| Activity/Fragment | Màn hình và lifecycle | AndroidX, ViewBinding |
| XML/resources | List, nút, thông báo Anh/Việt | `res/layout`, `values`, `values-vi` |
| Main thread/Handler | Tránh treo khi gọi API | ExecutorService + ViewModel/LiveData |
| AsyncTask | Hiểu lịch sử callback | Không dùng mới; API đã deprecated |
| HTTP/JSON | Auth header, body, error, metadata | SDK Dropbox; vẫn học request bên dưới |
| External storage cũ | Chọn nơi đọc/ghi file | Storage Access Framework (SAF) |
| Touch/GestureDetector | Click/long press, preview | Ưu tiên gesture có ích, không trang trí |

Không hạ target SDK xuống 22 để né permission như ví dụ lịch sử. Không copy
`android.support.*`; dùng AndroidX. `INTERNET` là normal permission, không bật
dialog xin quyền runtime. Không coi `onDestroy()` là nơi chắc chắn được gọi để lưu dữ liệu.

## Bài làm

1. Viết ba user story: duyệt tài liệu, upload bài tập, download để mở bằng app khác.
2. Với mỗi story, nêu dữ liệu nào local và dữ liệu nào ở cloud.
3. Vẽ một request: click → ViewModel → worker → API → UI state → render.
4. Đọc [nguồn Android và Dropbox](11-references.md), đánh dấu khái niệm chưa biết.

**Gợi ý code:** lần theo `FilesFragment` → `FilesViewModel.load()` →
`FakeCloudRepository.listFolder()` → `FolderPage`. Chỉ ra đoạn chạy trên main
và đoạn chạy trên executor; chưa cần sửa code.

**Nghiệm thu:** mỗi thành viên giải thích được vì sao file list chỉ chứa metadata,
vì sao network không chạy trên main thread và vì sao demo giả chưa phải Dropbox client.

**Checkpoint:** thay `FakeCloudRepository` bằng Dropbox, những lớp nào nên giữ nguyên?
