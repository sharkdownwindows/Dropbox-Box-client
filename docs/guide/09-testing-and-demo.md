# 09 — Kiểm thử, phối hợp nhóm và demo

**Input:** từng tính năng MVP đã được ghép, tài khoản/file test, thiết bị có ghi version.

**Output:** test report, bug list và kịch bản demo có thể lặp lại.

**Ràng buộc:** không ghi “pass” khi chưa chạy; test fake không chứng minh API thật;
không dùng dữ liệu cá nhân quan trọng cho delete/overwrite.

## 1. Kiểm thử theo tầng

| Tầng | Kiểm tra gì | Hiện trạng khung |
|---|---|---|
| Unit Java | Contract demo, vault generation, dedupe | Có test; kết quả ở bài 12 |
| SDK + HTTP giả lập | Root/cursor, JSON errors, refresh, logout giữa response | Có test; không gửi request tới Dropbox thật |
| ViewModel | Loading/state, stale response, logout race | Sinh viên bổ sung với executor/repository tiêm vào |
| API integration | Auth/scopes, refresh, list, transfer, mutation | Chưa có; cần tài khoản test |
| Device/manual | Rotation, picker, Back, mất mạng, font lớn | Chưa chạy trong lần dựng khung |

Lệnh: `./gradlew` trên Unix hoặc `.\gradlew.bat` trên Windows, với
`:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
Xem bài 02 và 12 nếu Windows gặp lỗi classpath/loopback.

## 2. Mẫu test có ý nghĩa

Gợi ý test sau khi đã viết name validator ở bài 07:

```java
@Test public void rejectsTraversalButAcceptsVietnameseName() {
    assertFalse(NameValidator.isValidName("../secret"));
    assertTrue(NameValidator.isValidName("Bài tập 01.pdf"));
}
```

Đối với ViewModel, tiêm fake có khả năng giữ callback A và B, hoàn tất B trước A,
rồi kiểm tra state cuối là B. Đừng dựa vào `Thread.sleep(1000)` để đoán thread đã xong.
Dùng test dispatcher/executor có kiểm soát; LiveData cần executor rule tương ứng.
Test HTTP phải giả lập cả body lỗi và status; status 409 đơn độc chưa đủ để xác định nguyên nhân.

## 3. Checklist nghiệm thu MVP Dropbox

Để trống kết quả cho tới khi chạy thật.

| ID | Thao tác | Kỳ vọng | Kết quả / bằng chứng |
|---|---|---|---|
| T01 | Login và consent | Mở Files bằng phiên thật | Chưa chạy |
| T02 | Hủy login | Vẫn ở Login, có thể thử lại | Chưa chạy |
| T03 | Token expired/revoked | Refresh hoặc login lại hữu hạn | Chưa chạy |
| T04 | Folder hơn một trang | Đủ ID, không trùng, hết trang dừng | Chưa chạy |
| T05 | Folder rỗng | Empty, không error | Chưa chạy |
| T06 | Mất mạng/timeout | Lỗi hữu ích, Retry/Back hoạt động | Chưa chạy |
| T07 | Chuyển folder khi request chậm | Không hiện kết quả folder cũ | Chưa chạy |
| T08 | Upload file hợp lệ | Có trên Dropbox web, đúng bytes | Chưa chạy |
| T09 | Upload tên trùng/vượt giới hạn | Conflict/limit rõ; không overwrite âm thầm | Chưa chạy |
| T10 | Download rồi hash local | Hai SHA-256 local giống nhau | Chưa chạy |
| T11 | Hủy picker/mất URI permission | Không crash; không báo success | Chưa chạy |
| T12 | Mất mạng giữa download | Không nhận file dở là hoàn chỉnh | Chưa chạy |
| T13 | Create/rename/delete | Đúng item và server state | Chưa chạy |
| T14 | Cancel delete | Không có request delete | Chưa chạy |
| T15 | Xoay màn hình/Home/Back | Không request/transfer trùng | Chưa chạy |
| T16 | Logout trong request | Credential/cache/UI cũ bị vô hiệu hóa | Chưa chạy |
| T17 | Đóng/mở process | Session/folder được restore theo thiết kế | Chưa chạy |
| T18 | Việt/Anh, font lớn, TalkBack | Nội dung và thao tác truy cập được | Chưa chạy |

Nhấn Home không phải process death. “Don't keep activities” chủ yếu kiểm tra tái tạo
Activity, không thay toàn bộ bài test process. Ghi đúng cách tái hiện; force-stop cũng
có ngữ nghĩa khác việc OS thu hồi process. Không nhận tất cả là cùng một loại lỗi.

## 4. Mẫu ghi bug/evidence

```text
Build/commit:
Thiết bị, Android API, mạng:
Dữ liệu test (không token):
Các bước tái hiện:
Kết quả mong đợi:
Kết quả thực tế:
Log đã bỏ dữ liệu nhạy cảm / ảnh / video:
Nguyên nhân đã xác nhận hoặc giả thuyết cần kiểm tra:
Fix và kết quả chạy lại:
```

Debug theo thứ tự reproduce → isolate → root cause → minimal fix → verify.
Ví dụ “list trống” có thể do App Folder scope, folder sai, parse sai hoặc UI không render;
đừng đổi API trước khi xem response metadata và state.

## 5. Phân công nhóm 5 người, có thể điều chỉnh

| Vai trò | Deliverable | Review chéo |
|---|---|---|
| 1 | Login, session, logout | Người 2 thử expired/revoked |
| 2 | List, paging, mapping lỗi | Người 3 thử cursor/race |
| 3 | Upload/download, URI | Người 4 thử byte/hash/cancel |
| 4 | UI, actions, resources | Người 5 thử lifecycle/accessibility |
| 5 | Test integration, báo cáo, demo | Người 1 review bằng chứng |

Mỗi người phải giải thích được một luồng từ UI tới HTTP; không chia để một người
chỉ làm slides mà không hiểu ứng dụng. Ghép một vertical slice sớm: login → list thật.

## 6. Demo và bài trình bày

Bám khung trong intro tr. 10–18: giới thiệu → kiến trúc → Activity/Fragment →
networking → tối ưu → demo → kết luận. Thời lượng nêu trong slide là 15 phút +
3 phút Q/A, cần xác nhận với giảng viên nếu thông báo lớp đã thay đổi.

Kịch bản đề xuất: login → vào folder → upload file nhỏ → kiểm tra trên web → download
và so hash → rename → cancel delete rồi confirm → tắt mạng → retry → logout.
Chuẩn bị kết nối riêng và video dự phòng; nói rõ khi chiếu video, không giả là live demo.

Trong kết luận, tách: đã implement, đã unit-test, đã test thiết bị, đã thử API thật,
chưa làm. Không trình bày Box là hoàn thành nếu mới tạo package/fake.

**Checkpoint:** test dùng SDK thật nhưng HTTP giả lập pass cho phép khẳng định điều gì, và chưa chứng minh được điều gì về OAuth trên thiết bị?
