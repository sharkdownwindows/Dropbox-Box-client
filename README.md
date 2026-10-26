# Dropbox / Box Client — Mobile Application Development

Project học tập Android **Java + XML**: làm Dropbox trước, mở rộng Box sau.
Bắt đầu tại [docs/guide/README.md](docs/guide/README.md).
Tiếp tục project bằng AI trên máy khác: [guide 14](docs/guide/14-ai-handoff.md).

## Trạng thái hiện tại

Đã triển khai **Dropbox OAuth PKCE**, lưu credential mã hóa bằng Android Keystore,
khôi phục phiên, logout local và duyệt thư mục thật qua SDK: phân trang, Back/Up,
tải lại, lỗi có phân loại. Có chế độ demo độc lập và tài nguyên Anh/Việt.
Code đã được build và kiểm thử với HTTP giả lập; **chưa xác nhận đăng nhập/API trên thiết bị thật**.
Upload/download, CRUD, share và Box vẫn là bài thực hành, chưa triển khai.

## Chạy khung

1. Mở **thư mục repo có `settings.gradle`** trong Android Studio.
2. Chọn Gradle JDK 17; cài Android SDK Platform 34 và Build Tools 34.0.0.
3. Sync Gradle, chọn emulator/điện thoại API 26 trở lên, Run `app`.
4. Chọn **Thử demo**; thử `Course`, `Empty folder`, `Simulated error`, `Load more`.
5. Để dùng Dropbox thật, cấu hình app key theo [13 — Chạy mẫu Dropbox](docs/guide/13-run-dropbox-sample.md),
   build lại rồi chọn **Đăng nhập Dropbox**. Không cần client secret hoặc token dán tay.

PowerShell tại thư mục repo:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Nếu đường dẫn tiếng Việt khiến batch báo `GradleWrapperMain`, dùng:

```powershell
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Máy mới cần tạo `local.properties` với `sdk.dir` của máy mình hoặc để Android Studio tạo.
Chi tiết và giới hạn môi trường: [02 — Setup](docs/guide/02-setup.md).
Kết quả kiểm tra thực tế: [12 — Verification](docs/guide/12-verification.md).

## Cấu trúc

```text
app/src/main/java/vn/edu/usth/cloudclient/
  model/           CloudItem, FolderPage
  data/            CloudRepository
    fake/          FakeCloudRepository (chế độ demo)
    dropbox/       DropboxCloudRepository (list/list tiếp, mapping lỗi)
    box/           Điểm mở rộng sau MVP
  auth/            SessionViewModel, CredentialVault, EncryptedCredentialStore
  transfer/        Bài upload/download
  ui/
    MainActivity
    files/         FilesFragment, FilesViewModel, FilesState, CloudItemAdapter
app/src/main/res/   Layout XML, theme, strings Anh/Việt
app/src/test/      Contract demo, vault, mapping/pagination và SDK với HTTP giả lập
docs/guide/        Lộ trình, bài thực hành, tham khảo, checklist
gradle/wrapper/    Gradle 8.7 wrapper có kiểm tra SHA-256
```

Các package `transfer` và `data/box` mới là vị trí dành cho bài tập.
Không cần app key/token để chạy demo. Không commit `local.properties`, token hay secret.

Luôn xem `git status` trước khi stage; không đưa `local.properties`, token,
keystore, build output hoặc repository Git lồng vào commit.
