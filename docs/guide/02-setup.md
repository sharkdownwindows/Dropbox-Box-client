# 02 — Môi trường, build và Git

**Input:** repo chính `Repo Project`, Android Studio, JDK 17, SDK, emulator/điện thoại.

**Output:** APK debug và demo dữ liệu giả mở được; biết lệnh chạy lại kiểm tra.

**Ràng buộc:** không commit đường dẫn SDK/token; không sửa repo con có sẵn;
không dùng phiên bản dependency động như `+`.

## Bộ phiên bản của skeleton

| Thành phần | Giá trị | Lý do |
|---|---|---|
| Java | 17 | Tương thích AGP đã chọn |
| Android Gradle Plugin | 8.5.2 | Baseline được ghim, không tuyên bố là mới nhất |
| Gradle | 8.7 | Theo compatibility table AGP 8.5 |
| compileSdk / targetSdk | 34 / 34 | Baseline lab, SDK 34 có trên máy lúc dựng |
| minSdk | 26 | Giảm số nhánh tương thích trong bài thực hành |
| AppCompat / RecyclerView | 1.7.0 / 1.3.2 | UI XML và danh sách |
| Lifecycle | 2.8.7 | ViewModel, SavedStateHandle, LiveData |

Đây là baseline học tập. Trước khi phát hành lên store hoặc nâng SDK Dropbox,
đối chiếu yêu cầu target API và compatibility mới; nâng AGP/Gradle/JDK/AndroidX
như một thay đổi có kiểm tra. Không hạ target để né quyền.

## Bước 1 — Mở đúng project

Chọn Open → `Repo Project`, nơi có `settings.gradle` và `app/`.
Repo con `Dropbox-Box-client/` rỗng đã có `.git` riêng và cùng remote; nó không phải
module Android. Không xóa hoặc biến nó thành submodule trong bài này.

## Bước 2 — Chuẩn bị công cụ

1. SDK Manager: cài Platform 34, Build Tools 34.0.0 và Platform Tools.
2. Gradle settings: chọn JDK 17. `java -version` trong terminal cũng cần đúng JDK.
3. Tạo emulator API 34 hoặc dùng thiết bị API ≥26 có USB debugging.
4. Sync Gradle; lần đầu tải Gradle/dependency nên cần Internet.

`local.properties` (ví dụ, đổi theo máy):

```properties
sdk.dir=C:/Users/YOUR_NAME/AppData/Local/Android/Sdk
```

## Bước 3 — Build và chạy

PowerShell, đang ở thư mục repo:

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`; báo cáo test ở
`app/build/reports/tests/testDebugUnitTest/`, lint ở `app/build/reports/`.
Trên macOS/Linux: `chmod +x gradlew`, sau đó `./gradlew` với các task tương tự.

Đường dẫn workspace hiện có dấu và khoảng trắng. Nếu Windows batch làm hỏng classpath:

```powershell
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

`android.overridePathCheck=true` chỉ bỏ kiểm tra đường dẫn của AGP, không bảo đảm
mọi công cụ native xử lý Unicode. Nếu còn lỗi đường dẫn, dùng một checkout riêng
ở đường dẫn ASCII như `C:\dev\cloud-client`; giữ bản gốc và thay đổi chưa commit.
Không đổi Gradle wrapper ngẫu nhiên để chữa lỗi encoding.

Nếu gặp `Unable to establish loopback connection` từ Java `PipeImpl` trước khi
Gradle biên dịch, thử terminal PowerShell bình thường ngoài trình chạy bị lỗi.
Trong lần kiểm chứng này, đặt thư mục Unix-domain socket riêng cho **mọi JVM con**
bằng biến môi trường của phiên terminal đã giải quyết lỗi:

```powershell
New-Item -ItemType Directory -Force 'C:\Users\Public\codex-mobile-jvm-tmp' | Out-Null
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/Users/Public/codex-mobile-jvm-tmp'
# Chạy Gradle ở đường dẫn ASCII như hướng dẫn trên.
```

Đây là workaround đã quan sát cho môi trường hiện tại, không phải thiết lập bắt buộc
cho mọi máy. Nếu `JAVA_TOOL_OPTIONS` đã có giá trị, giữ lại các tùy chọn cần thiết.
Biến chỉ tồn tại trong phiên terminal; không tắt firewall/antivirus hoặc đổi cấu hình
toàn hệ thống để xử lý lỗi này. Chi tiết kiểm chứng ở bài 12.

## Bước 4 — Đọc structure bằng một luồng nhỏ

1. Manifest khai báo launcher `MainActivity` và quyền `INTERNET`.
2. `activity_main.xml` giữ Login và container Fragment; chọn **Thử demo** để vào dữ liệu giả.
3. `FilesFragment` gắn layout/listener và observe `FilesState` theo view lifecycle.
4. `FilesViewModel` gọi repository bằng executor; Handler trả state về main.
5. Fake repository trả trang đầu ba thư mục; `Load more` thêm hai file.
6. `values-vi` được chọn khi ngôn ngữ thiết bị là tiếng Việt.

## Bước 5 — Làm việc nhóm

```powershell
git status --short --branch
git diff --check
```

Review file cụ thể trước khi stage. Không `git add .` khi còn embedded repo chưa rõ mục đích.
Mỗi nhánh hoàn thành một chức năng nhỏ, ví dụ `feature/dropbox-login`; commit giải thích
hành vi thay đổi. Không commit build output, SDK path hoặc token. App key là public identifier,
nhưng vẫn nên cấu hình cục bộ để các nhóm không vô tình dùng chung app đăng ký.

**Nghiệm thu:** build, toàn bộ unit test và lint; mở app, chọn demo, vào Course, trở về,
load thêm, xem thư mục trống và lỗi giả. Xem bài 12 để biết kiểm tra nào đã chạy thực tế.

**Checkpoint:** `compileSdk`, `targetSdk`, `minSdk` trả lời ba câu hỏi khác nhau nào?
