# 12 — Trạng thái kiểm chứng của code mẫu 0.2.0

Ngày: **16/10/2026**, múi giờ Asia/Saigon. Phiên bản hiện tại có code OAuth PKCE,
lưu phiên mã hóa và browse Dropbox; báo cáo này phân biệt kiểm thử JVM với
OAuth/Keystore/API trên thiết bị. Chưa phải báo cáo hoàn thành toàn bộ MVP.

## Đã kiểm tra

| Kiểm tra | Kết quả | Giới hạn |
|---|---|---|
| Gradle wrapper | JAR SHA-256 khớp nguồn Gradle; distribution có checksum | Không chứng minh business logic |
| `:app:assembleDebug` | Thành công, có APK | Build trong bản sao đường dẫn ASCII |
| `:app:testDebugUnitTest` | **25 tests, 0 failures, 0 errors, 0 skipped** | JVM; HTTP và credential storage đều giả lập |
| `:app:lintDebug` | **0 errors, 0 warnings** | Kết quả lần chạy lại toàn bộ task, không phải kiểm thử runtime |
| Source đối chiếu | **40 input files** trong bản sao khớp byte/SHA-256 source gốc | Không đưa local.properties chứa cấu hình máy vào hash manifest |
| `adb devices` | Không có thiết bị/emulator kết nối | Chưa chạy UI/instrumentation |

Lần kiểm chứng cuối chạy `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --rerun-tasks`
với Gradle 8.7, AGP 8.5.2, Java 17, compileSdk 34, Dropbox SDK 7.0.0.
Không tắt lint error hoặc suppress toàn bộ warning. Báo cáo hiện tại thay báo cáo
skeleton trước đây (6 tests, 4 warning dependency); phiên bản AndroidX không đổi.

| Nhóm test | Số lượng | Bằng chứng giới hạn ở đâu |
|---|---|---|
| FakeCloudRepositoryTest | 6 | Pagination, cursor, empty/error, immutable page |
| CredentialVaultTest | 6 | Session generation, logout ordering, lỗi lưu; invalidate không chờ disk write bị chặn |
| FolderPageTest | 1 | Dedupe theo ID, cập nhật metadata, giữ thứ tự và snapshot cũ |
| DropboxCloudRepositoryTest | 12 | SDK serialize/parse qua HTTP giả lập, root/continue, refresh, mapping lỗi, logout giữa response |

Test refresh kiểm tra access token mới được persist và refresh token được giữ;
không chứng minh refresh token thật của Dropbox sẽ được cấp cho một app chưa cấu hình.
Test vault dùng memory store, không chạy Android Keystore hoặc mã hóa trên thiết bị.

Bằng chứng có thể mở trực tiếp:

- [Kết quả JUnit XML tổng hợp](evidence/unit-tests.xml); XML gốc từng suite ở `evidence/unit-tests/`.
- [Lint text](evidence/lint-results-debug.txt) và [Lint XML](evidence/lint-results-debug.xml).
- [SHA-256 các input build](evidence/source-sha256.json).
- [Thông tin build và checksum APK](evidence/build-summary.json).

APK cuối đã chép từ bản build kiểm chứng về
`app/build/outputs/apk/debug/app-debug.apk` trong repo chính. APK chứa code tích hợp
nhưng **app key đang để trống**: có thể chạy demo, cần cấu hình và rebuild theo
[bài 13](13-run-dropbox-sample.md) để đăng nhập Dropbox của bạn.
APK/build outputs được gitignore; không đưa vào source commit.

## Các lỗi môi trường đã gặp và phạm vi kết luận

1. Wrapper `.bat` tại đường dẫn có dấu báo không tìm thấy `GradleWrapperMain`.
   Gọi Java với classpath tương đối khởi chạy được wrapper.
2. Java NIO báo `Unable to establish loopback connection`; stack trace trỏ tới
   `PipeImpl`/`UnixDomainSockets.connect0`. Đặt `JAVA_TOOL_OPTIONS` với
   `jdk.net.unixdomain.tmpdir` vào thư mục ASCII riêng cho mọi JVM con thì build tiếp tục.
   Đổi IPv4 hoặc chỉ truyền property một phía không giải quyết đầy đủ lỗi.
3. Tại đường dẫn gốc, app biên dịch/đóng gói được nhưng JUnit runner báo
   `ClassNotFoundException` cho test class. Cùng source ở thư mục ASCII chạy được cả 6 test.
   Đây là bằng chứng cho workaround đường dẫn; chưa chứng minh đầy đủ nguyên nhân bên trong JVM.
4. Bản sao kiểm chứng được tạo tại `C:\Users\Public\codex-mobile-check-20261016`;
   không copy `.git` hoặc sửa repo con. Source/build files được đối chiếu byte trước khi lưu kết quả.

Xem [bài 02](02-setup.md) để chạy lại. Nếu báo cáo test cũ dưới `app/build/` của
đường dẫn gốc còn ghi failed, đó là lần JUnit gặp lỗi classpath nêu trên;
báo cáo kiểm chứng thành công được lưu rõ ở `evidence/`, không sửa lịch sử lỗi thành pass.

## Đã triển khai nhưng chưa kiểm chứng trên thiết bị

- UI trên emulator/điện thoại: rotation, Back, ngôn ngữ, TalkBack, process death.
- OAuth consent/callback qua Dropbox app hoặc trình duyệt; hủy và process death giữa OAuth.
- Android Keystore, AES-GCM file persistence, restart, refresh và logout thật.
- Dropbox list/list tiếp với tài khoản, scope và account policy thật.

## Chưa triển khai

- Upload/download, create/rename/delete, preview/share và Box.
- WorkManager, cache/offline sync, backoff/Retry-After tự động, performance benchmark hoặc phát hành store.

Các phần đã viết được mô tả ở bài 13; những phần còn lại tiếp tục là bài tập 06–10.
Build và unit test pass không thay thế bằng chứng API/thiết bị thật. Checklist D01–D12
trong bài 13 và T01–T18 trong bài 09 vẫn để chưa chạy cho tới khi có kết quả thực tế.

## Git và tài liệu

Repo chính trước thay đổi chỉ có README được track và repo con untracked.
Đã thêm source/config/docs trong repo chính, cập nhật README; không commit/push.
Repo con được giữ nguyên. Các PDF bài giảng không bị sửa; file yêu cầu Deep Learning
đã biến mất giữa các lần đọc, không bị công cụ của tác vụ này xóa hoặc di chuyển.
