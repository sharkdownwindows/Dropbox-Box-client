# 04 — Dropbox OAuth: đăng nhập và quản lý phiên

**Input:** UI giả chạy ổn, tài khoản Dropbox thử nghiệm, app đăng ký ở App Console.

**Output:** Login → consent của Dropbox → phiên hợp lệ → Files; hủy và logout rõ ràng.

**Ràng buộc:** không nhúng client secret/mật khẩu/access token cố định vào APK;
không log credential; dùng SDK PKCE. OAuth và lưu phiên đã có code mẫu;
xem [bài 13](13-run-dropbox-sample.md) để chạy và [bài 12](12-verification.md) để biết mức kiểm chứng.

## Kiến thức cần hiểu

OAuth cho phép app truy cập tài nguyên theo scope sau khi người dùng đồng ý.
App key/client ID nhận diện ứng dụng; access token cho phép gọi API; refresh token
xin access token mới. Chúng không có cùng vai trò. APK có thể bị giải nén nên
`BuildConfig` không phải nơi giữ bí mật.

PKCE ràng buộc lần nhận authorization code với bên đã khởi tạo luồng bằng một
verifier ngẫu nhiên. Dùng trình duyệt hoặc Dropbox app qua SDK, không tự dựng form
nhập mật khẩu Dropbox. Đọc [OAuth guide](https://developers.dropbox.com/oauth-guide)
và [SDK Android](https://github.com/dropbox/dropbox-sdk-java#usage-on-android).

## Bước 1 — Tạo app và chọn access

1. Mở [App Console](https://www.dropbox.com/developers/apps), tạo app có scoped access.
2. Khuyến nghị lab dùng **App Folder**: chỉ thấy nội dung thuộc vùng của app.
   Nếu yêu cầu duyệt tài khoản đầy đủ, chọn **Full Dropbox** và giải thích lý do.
3. Mẫu hiện tại chỉ yêu cầu `files.metadata.read`. Khi làm transfer, bật thêm
   `files.content.read`, `files.content.write`; thêm `account_info.read` nếu gọi thông tin tài khoản.
4. Chỉ thêm `sharing.read`/`sharing.write` khi làm bài share tùy chọn và kiểm tra
   endpoint đó có hỗ trợ loại access của app hay không.
5. Chép app key; không đưa app secret vào Android. Sau khi thay scopes, cấp quyền lại.

Trong App Folder, root `""` là gốc **vùng app**, không phải toàn bộ Dropbox của người dùng.
Tạo dữ liệu thử tại đúng vùng đó; folder rỗng không tự động nghĩa là API sai.

## Bước 2 — Đọc dependency SDK đã thêm

Ví dụ được ghim theo Javadoc 7.0.0 để dễ đối chiếu; đã kiểm tra artifact 7.0.0
có trên Maven Central và biên dịch trong app. Đây không phải tuyên bố SDK mới nhất;
test HTTP giả lập cũng không thay thế integration thật trên thiết bị.
README upstream tại thời điểm đọc nêu 8.0.2; khi chọn bản mới, đọc release notes và
kiểm tra tương thích toolchain. Hai artifact phải cùng version.

```groovy
// app/build.gradle, dependencies
implementation 'com.dropbox.core:dropbox-core-sdk:7.0.0'
implementation 'com.dropbox.core:dropbox-android-sdk:7.0.0'
```

Android SDK này dùng Kotlin nội bộ; app của nhóm vẫn viết Java. POM 7.0.0 khai báo
Kotlin runtime transitively. Kiểm tra dependency graph khi nâng version thay vì bỏ
dependency chỉ vì code của nhóm không có `.kt`.

## Bước 3 — Truyền app key và cấu hình callback

Thêm `DROPBOX_APP_KEY=...` vào `local.properties` (đã gitignore). Trong `app/build.gradle`,
đọc properties trước `android {}`; không hardcode đường dẫn máy:

```groovy
def localConfig = new Properties()
def localFile = rootProject.file('local.properties')
if (localFile.exists()) localFile.withInputStream { localConfig.load(it) }
def dropboxKey = localConfig.getProperty('DROPBOX_APP_KEY', '').trim()
if (!(dropboxKey ==~ /[A-Za-z0-9]*/)) {
    throw new GradleException('DROPBOX_APP_KEY must contain only letters and digits')
}
// Bên trong android:
// buildFeatures { viewBinding true; buildConfig true }
// defaultConfig {
//   buildConfigField 'String', 'DROPBOX_APP_KEY', '"' + dropboxKey + '"'
//   manifestPlaceholders = [dropboxKey: dropboxKey ?: 'notconfigured']
// }
```

Đoạn này đã có trong `app/build.gradle`, không thêm lần nữa. Key rỗng vẫn build demo;
nút Login sẽ hướng dẫn cấu hình. Thay key cần build/cài lại vì callback nằm trong Manifest.

Trong `<application>` của Manifest, thêm AuthActivity theo cấu hình SDK:

```xml
<activity android:name="com.dropbox.core.android.AuthActivity"
    android:exported="true" android:launchMode="singleTask"
    android:configChanges="orientation|keyboard">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="db-${dropboxKey}" />
    </intent-filter>
</activity>
```

Thêm `<queries><package android:name="com.dropbox.android" /></queries>` trực tiếp
dưới `<manifest>` (ngoài `<application>`) để SDK kiểm tra được Dropbox app.
Activity khởi tạo OAuth cần `android:launchMode="singleTask"` theo hướng dẫn SDK;
nếu tùy chỉnh taskAffinity thì AuthActivity phải tương ứng. Đọc ghi chú app-to-app
cho targetSdk 33 trong README SDK và kiểm tra phiên bản Dropbox app của thiết bị;
không chép workaround lịch sử sang mọi target mà không thử nghiệm.
Không thay scheme bằng tên tự nghĩ và không trộn callback
của SDK với redirect của một thư viện OAuth khác. Xem merged manifest sau build.

## Bước 4 — Khởi tạo và nhận kết quả

Mẫu tối thiểu đặt nút Login trong `ui/MainActivity.java` và quản lý phiên trong
`auth/SessionViewModel.java`; chưa cần Fragment riêng. Lời gọi đã triển khai:

```java
DbxRequestConfig config = SessionViewModel.requestConfig();
Auth.startOAuth2PKCE(this, BuildConfig.DROPBOX_APP_KEY, config,
    Collections.singletonList("files.metadata.read"));
```

Trong đường xử lý sau khi Activity trở lại foreground, lấy `Auth.getDbxCredential()`
(kiểu `com.dropbox.core.oauth.DbxCredential`). Chỉ xử lý khi có phiên đăng nhập đang
chờ và kết quả chưa được tiêu thụ. Không chạy login lại vô hạn trong `onResume()`.

```java
// Gợi ý luồng; code đầy đủ nằm trong MainActivity.onResume().
DbxCredential credential = Auth.getDbxCredential();
if (credential != null && loginPending) {
    loginPending = false;
    // Lưu an toàn trên worker, sau đó mới điều hướng trên main thread.
}
```

Nếu quay về mà chưa có credential: cho phép hủy/thử lại, không báo success.
Lưu trạng thái loginPending qua recreation; khi process chết giữa luồng, quay lại
Login và yêu cầu thử lại nếu không còn verifier/session hợp lệ. Logout phải ngăn
kết quả cũ từ SDK `onResume()` khôi phục phiên ngoài ý muốn.

## Bước 5 — Token store

Đọc `EncryptedCredentialStore`, `CredentialVault`, `SessionViewModel` để đối chiếu
implementation với các bước dưới đây. Kiểm thử Keystore trên thiết bị trước khi
đánh dấu A1/A6 hoàn chỉnh:

1. Tạo khóa AES trong Android Keystore; mã hóa bằng AES/GCM với IV mới mỗi lần ghi.
2. Lưu ciphertext + IV + format version trong app-private storage, ghi atomically.
3. Lưu đủ access token, expiry, refresh token, app key; không chỉ token ngắn hạn.
4. Decode lỗi/key invalidated → xóa phiên không đọc được và login lại; không fallback plaintext.
5. Logout: đánh dấu generation phiên mới, hủy request, xóa credential/cache/UI nhạy cảm.
6. Phân biệt logout local với revoke quyền trên Dropbox; offline logout vẫn phải xóa local.

Keystore giữ **khóa mã hóa**, không tự động mã hóa một file JSON. Không dùng Base64
như mã hóa. Skeleton có `allowBackup=false` và `data_extraction_rules.xml` loại dữ liệu
khỏi cloud backup/device transfer; các cấu hình này không thay token encryption.
Không đưa `credential.toString()` vào Logcat vì có thể chứa toàn bộ token.

Khởi tạo client bằng credential có refresh support: `new DbxClientV2(config, credential)`.
Serialize trạng thái cập nhật sau refresh; không tạo nhiều client cùng refresh token rồi
để chúng ghi đè nhau. Giới hạn retry để lỗi revoked/invalid grant dẫn về login.

## Nghiệm thu

- Login thật, hủy consent, trở lại bằng Back: không crash hoặc tự login lặp.
- Token hết hạn: refresh hợp lệ; token revoked: yêu cầu đăng nhập lại.
- Restart app: khôi phục phiên nếu đã làm persistent store.
- Logout giữa request: không có dữ liệu/phiên cũ xuất hiện trở lại.
- Kiểm tra APK/repo/log không có app secret, token hoặc mật khẩu hardcode.

**Checkpoint:** cất token trong `local.properties` rồi đưa vào `BuildConfig` có bảo vệ nó không?
