# 08 — Lifecycle, retry, cache và UX có mục đích

**Input:** MVP Dropbox cơ bản; log các ca chậm/lỗi từ bài 05–07.

**Output:** app không treo, trạng thái nhất quán và cải tiến có bằng chứng.

**Ràng buộc:** đo trước khi tối ưu; không thêm sync/offline edit chỉ để tăng số tính năng;
không quảng cáo transfer bền vững nếu chưa kiểm tra process death.

## Kiến thức và quyết định

| Vấn đề | Cách tối thiểu | Khi nào cần thêm |
|---|---|---|
| List loading | Executor + ViewModel | Đủ cho request gắn với màn hình |
| Rotation | ViewModel + SavedStateHandle | Kiểm tra cả view lifecycle |
| Process chết | Mẫu hiện tại khôi phục session rồi về root | Restore sâu cần gắn state với đúng account; transfer cần scheduler |
| Transfer dài | Giới hạn lab, thể hiện trạng thái | Xem API background phù hợp với target Android |
| Offline list | Chưa cache thì hiện lỗi | Chỉ thêm cache metadata nếu cần |
| 429/5xx | Backoff có giới hạn | Không retry toàn bộ mutation một cách tự động |

## Bước 1 — Kiểm tra race và memory

1. Fake/repository test cho request A chậm, request B nhanh; kết quả B phải được giữ.
2. Logout trong lúc tải: invalidate generation và dọn state trước khi điều hướng.
3. Xoay 10 lần, theo dõi số request và màn hình; không giữ reference Activity trong singleton.
4. Quan sát memory khi tải file lớn: dùng stream, không `ByteArrayOutputStream` toàn file.
5. Thử Back khi loading; ứng dụng phải phản hồi thao tác.

Mẫu hiện có generation guard cho list và credential, lỗi có enum và logout local.
`Future.cancel(true)` không bảo đảm HTTP socket dừng ngay: kết quả muộn bị loại bỏ,
nhưng request kế tiếp có thể phải đợi request cũ kết thúc/timeout. Transport cancellation,
backoff tự động và transfer controller vẫn là bài tập; xem giới hạn tại bài 13.

## Bước 2 — Retry hữu hạn

Ví dụ policy lab cho thao tác đọc: tối đa 3 lần, exponential backoff cộng jitter;
`Retry-After` của server được ưu tiên. Không `Thread.sleep()` trên main thread.
Không retry 400/permission/conflict như lỗi tạm thời.

```java
// Gợi ý tính delay; dùng scheduler/worker, không sleep UI.
long baseMs = Math.min(8000L, 1000L << attempt); // attempt: 0..2
long delayMs = baseMs + random.nextInt(250);
```

Một nguồn quản lý refresh token, tránh “refresh storm” khi nhiều request đồng thời 401.
Sau lỗi invalid grant/revoked, dừng retry và yêu cầu login.

## Bước 3 — Cache metadata, tùy chọn

Chỉ làm sau khi list API đúng. Cache key gồm provider + account + folder; ghi fetchedAt
và hiển thị “dữ liệu đã lưu” khi offline. Logout dọn cache tương ứng. Cache metadata
không có nghĩa là file đã tải offline. Không lưu token chung với bảng metadata.

Có thể bắt đầu bằng file JSON private nhỏ; dùng Room khi truy vấn/schema đủ phức tạp.
Nếu dùng cache, test account A logout rồi B login không thấy dữ liệu của A.

## Bước 4 — Background transfer, mở rộng

Đọc [WorkManager work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
Đây là bài riêng: persist URI permission hoặc copy vào private storage; lưu transfer ID,
remote destination, tiến độ; đặt network constraints và backoff; thiết kế retry có idempotency.

Worker có thể chạy lại. Unique work giúp hạn chế job trùng nhưng không làm remote upload
tự động idempotent. Upload session/checkpoint cần được lưu và đối chiếu server.
Transfer do người dùng khởi tạo và rất dài cần kiểm tra hướng dẫn foreground/user-initiated
data transfer theo target Android hiện tại. Không mặc định Worker sống mãi hoặc luôn chạy ngay.

## Bước 5 — Touch và accessibility

List: tap folder để mở, tap file để xem details, long press/overflow để mở action menu.
Không swipe-delete trực tiếp. Preview ảnh là mở rộng: dùng GestureDetector/ScaleGestureDetector,
giới hạn scale và hỗ trợ ACTION_CANCEL; không chặn scroll của parent vô cớ.

Từ slide touch.p2 tr. 33: dx tác động leftMargin, dy tác động topMargin.
Nếu kéo view, ưu tiên translation khi phù hợp để tránh relayout mỗi pixel.
Cung cấp nút/menu tương đương gesture để TalkBack và bàn phím sử dụng được.

## Bước 6 — Đo trước/sau

Ghi thời gian từ bấm Refresh tới list sẵn sàng, peak memory khi transfer, số request
cho một thao tác, trạng thái overdraw của màn hình. Dùng cùng thiết bị, dữ liệu và mạng;
báo median và điều kiện đo, không bịa một con số FPS hoặc % cải thiện.
Không cần benchmark phức tạp nếu chưa tìm thấy bottleneck.

**Nghiệm thu:** checklist race/lifecycle/network đạt; có ít nhất một lỗi đã tái hiện
và một thay đổi được đo hoặc giải thích bằng bằng chứng.

**Checkpoint:** “WorkManager chạy lại job” có đồng nghĩa “upload lại file sẽ an toàn” không?
