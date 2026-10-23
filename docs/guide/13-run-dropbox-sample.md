# 13 — Chạy và hiểu code mẫu Dropbox OAuth + duyệt file

**Input:** source hiện tại, Android Studio/JDK 17, SDK 34, thiết bị API 26+, mạng,
tài khoản Dropbox thử nghiệm và **app key của bạn**.

**Output:** đăng nhập qua Dropbox → lưu phiên mã hóa → danh sách metadata thật →
vào thư mục, tải thêm, tải lại → đăng xuất local. Không cần dán access token vào source.

**Ràng buộc:** Java/XML, một tài khoản, một provider; chỉ đọc metadata. Không có
upload/download, rename/delete hoặc Box trong mẫu này. Không đưa app secret/token vào APK,
log, ảnh báo cáo hay Git. App key là định danh công khai, không phải mật khẩu.

**Mức bằng chứng:** code đã triển khai; kết quả build/test ở [bài 12](12-verification.md).
Chưa có thiết bị kết nối hoặc phiên Dropbox của bạn để xác nhận end-to-end.
Các bước dưới đây là cách chạy thử, không phải kết quả đã quan sát trên tài khoản thật.

## Bước 1 — Chạy demo trước để tách lỗi môi trường

1. Mở thư mục repo chứa `settings.gradle` bằng Android Studio.
2. Chọn Gradle JDK 17, Sync, Run trên API 26 trở lên; xem [bài 02](02-setup.md) nếu build lỗi.
3. Bấm **Thử demo**. Vào Course, quay lại, mở Empty folder và Simulated error.
4. Bấm Tải thêm ở root: demo có tổng cộng 5 mục. Bấm Đăng xuất để về Login.

Key để trống vẫn build được. Nếu demo cũng không chạy, xử lý lỗi Android/build trước
khi sửa OAuth. Demo không đọc hoặc ghi tài khoản Dropbox.

## Bước 2 — Đăng ký app với quyền vừa đủ

1. Mở [Dropbox App Console](https://www.dropbox.com/developers/apps), tạo một app scoped access.
2. Với lab, chọn **App Folder**. Nếu mục tiêu là duyệt toàn bộ tài khoản, chọn
   **Full Dropbox** ngay khi thiết kế và ghi lựa chọn vào báo cáo.
3. Trong Permissions, bật **`files.metadata.read`** và lưu thay đổi.
   Mẫu hiện chỉ yêu cầu scope này trong lời gọi OAuth; chưa yêu cầu quyền nội dung hoặc share.
4. Trong Settings, lấy **App key**, không lấy App secret hoặc generated access token.
5. Sử dụng tài khoản được phép thử app trong trạng thái phát triển. Nếu consent bị từ chối
   bởi chính sách tài khoản/tổ chức, ghi rõ lỗi và kiểm tra cấu hình trong console.

App Folder giới hạn root vào vùng app. Một root rỗng có thể hoàn toàn đúng:
hãy thêm file/thư mục thử trong vùng app bằng Dropbox web. Mẫu Android chưa tạo file được.
Phân biệt phạm vi truy cập và quyền endpoint trong [OAuth guide chính thức](https://developers.dropbox.com/oauth-guide).

## Bước 3 — Cấu hình và build lại

Mở `local.properties` tại repo, giữ nguyên `sdk.dir` hiện có và thêm:

```properties
DROPBOX_APP_KEY=yourActualAppKey
```

Thay `yourActualAppKey` bằng app key, không thêm dấu nháy hoặc dấu `< >`.
Có mẫu tại [local.properties.example](../../local.properties.example).
Không ghi đè `local.properties` bằng file ví dụ vì có thể mất đường dẫn SDK.

`app/build.gradle` đã làm ba việc:

- Đọc key cục bộ để tạo `BuildConfig.DROPBOX_APP_KEY`.
- Dùng cùng giá trị tạo callback scheme `db-<app-key>` trong merged Manifest.
- Cho phép key trống để chạy demo; từ chối ký tự không hợp lệ ngay khi cấu hình Gradle.

Sync và build/cài lại sau khi thay key. Trong merged Manifest, kiểm tra AuthActivity
có scheme tương ứng với key vừa cấu hình, không còn `db-notconfigured`.
Mẫu dùng callback do Dropbox Android SDK quản lý; không ghép redirect localhost hoặc
luồng OAuth của thư viện khác. Đối chiếu [hướng dẫn Android của SDK](https://github.com/dropbox/dropbox-sdk-java/tree/v7.0.0#usage-on-android).

Không đặt app secret vào BuildConfig. `local.properties` được gitignore nhưng
giá trị đưa vào BuildConfig vẫn có thể đọc từ APK.

## Bước 4 — Login và duyệt dữ liệu thật

1. Từ màn hình Login, chọn **Đăng nhập Dropbox**.
2. Hoàn tất đăng nhập/consent trong Dropbox app hoặc trình duyệt do SDK mở.
3. Khi trở về, app kiểm tra kết quả đang chờ; credential chỉ được chấp nhận khi có
   refresh token và app key đúng với bản build. Hủy sẽ về Login với thông báo thử lại.
4. App lưu credential trên worker rồi tạo repository. Browser bắt đầu ở root `""`.
5. Chạm thư mục để xem nội dung; **Lên trên** hoặc Back quay lại parent.
   Tên thư mục được hiển thị; ID dùng nội bộ để gọi API.
6. **Tải thêm** dùng cursor server, **Tải lại** bắt đầu listing mới.
   File chỉ hiển thị tên/kích thước; bấm file hiện thông báo bài transfer chưa triển khai.

Limit request hiện là 100, không được giả định server luôn trả đúng 100 mục/trang.
Nếu trang rỗng nhưng `has_more=true`, app vẫn cho Tải thêm. Muốn thử phân trang,
tạo đủ dữ liệu test hoặc tạm giảm limit trong repository, rồi trả về cấu hình đã chọn.

Nếu đăng nhập thành công nhưng list thất bại, đọc loại lỗi trên UI. Thiếu scope cần
kiểm tra Permissions rồi quay lại Login và cấp quyền lại. Đăng nhập thành công
không bảo đảm mọi endpoint đều được cấp quyền.

## Bước 5 — Đọc code theo luồng input → output

| File / phương thức | Input | Output | Kiến thức và ràng buộc |
|---|---|---|---|
| `ui/MainActivity.login()` | App key, thao tác người dùng | Mở OAuth PKCE | Chỉ gọi từ nút; không gọi network trực tiếp trên UI |
| `MainActivity.onResume()` | Cờ `authPending`, kết quả SDK | Credential hoặc thông báo hủy | Chỉ tiêu thụ kết quả khi thực sự đang login |
| `auth/SessionViewModel.acceptCredential()` | Credential của lần login | Session state có repository | Kiểm tra key/refresh token; ghi disk ngoài main thread |
| `auth/EncryptedCredentialStore` | JSON credential | Ciphertext/IV/version trong no-backup storage | AES-GCM, IV mới, khóa Android Keystore, AtomicFile |
| `auth/CredentialVault` | Generation + thao tác read/write/clear | Chấp nhận hoặc loại thao tác cũ | Không để request cũ ghi lại phiên sau logout |
| `data/dropbox/DropboxCloudRepository.listFolder()` | Folder ID, cursor | `FolderPage` hoặc `CloudException` | SDK HTTP, mapping metadata, lưu credential sau refresh |
| `model/FolderPage.appendTo()` | Trang trước và trang mới | List theo ID không trùng | Giữ thứ tự; metadata mới thay metadata cũ cùng ID |
| `ui/files/FilesViewModel` | Repository, thao tác điều hướng | `FilesState` trên main thread | Executor, generation request, SavedStateHandle |
| `ui/files/FilesFragment` | Session state + FilesState | UI list/loading/empty/error | Observe theo view lifecycle, không parse SDK JSON |

Ví dụ luồng đã ghép, đọc cùng [bài 04](04-dropbox-auth.md) và [bài 05](05-browse-and-networking.md):

```text
Login → SDK PKCE → onResume → acceptCredential
      → sessionIo: vault.save → encrypted file
      → main: session state → FilesViewModel.connect
      → file worker: list_folder / list_folder/continue
      → SDK có thể refresh access token → vault.save
      → main: nếu generation còn đúng → render list
```

Hai generation có vai trò khác nhau. Generation của FilesViewModel ngăn kết quả
folder A ghi đè folder B; generation của vault ngăn credential thuộc phiên A ghi
lại sau logout/phiên B. Ví dụ request dùng epoch 5; logout tăng epoch thành 6;
request hoàn tất sau đó gọi `save(5, ...)` sẽ bị từ chối. Đây là loại bỏ kết quả cũ,
không phải bảo đảm HTTP socket bị đóng ngay.

Gợi ý đọc sâu: đặt breakpoint trước `vault.save()` và trước `state.setValue()`;
quan sát số generation, không in credential. Tự giải thích vì sao chỉ hủy Future
là chưa đủ cho logout an toàn.

## Bước 6 — Phiên, refresh và logout

Credential chứa access token, expiry, refresh token và app key. SDK nhận cả credential
để refresh khi cần; repository serialize trạng thái sau request thành công.
Store dùng khóa AES trong [Android Keystore](https://developer.android.com/privacy-and-security/keystore),
không lưu plaintext vào SharedPreferences. Ciphertext ở `getNoBackupFilesDir()`;
Manifest cũng tắt backup. Chưa kiểm chứng behavior Keystore trên thiết bị cụ thể.

Logout vô hiệu hóa epoch, gỡ repository và list, rồi xóa khóa/ciphertext trên hàng
đợi I/O của Application. Hàng đợi tồn tại qua Activity recreation để thao tác xóa
không bị hủy chỉ vì Activity đóng. Nếu xóa thất bại, UI yêu cầu xóa dữ liệu app trong
Cài đặt Android và khóa nút Login/Demo; không báo logout thành công.

Đây là **logout local**; không gọi revoke quyền trên server và không đăng xuất tài khoản
khỏi Dropbox app/trình duyệt. Khi cần thu hồi quyền, thao tác trong phần quản lý ứng dụng
kết nối của tài khoản Dropbox. Đăng nhập lần sau có thể dùng phiên trình duyệt còn tồn tại.

Rotation giữ ViewModel và vị trí hiện tại. Sau process restart, app đọc phiên đã mã hóa
rồi tải root; không khôi phục cursor/stack của phiên cũ. Nếu process chết giữa OAuth
và mất trạng thái cần thiết của SDK, người dùng cần thử Login lại. Không tự chạy login lặp.

## Bước 7 — Nghiệm thu trên thiết bị của bạn

Ghi model thiết bị, Android API, version app/SDK, loại app access, scope và kết quả.
Không ghi token. Để trạng thái **Chưa chạy** cho tới khi thực hiện:

| Ca | Thao tác | Output cần quan sát |
|---|---|---|
| D01 | Thiếu key → Login, rồi Demo | Có hướng dẫn cấu hình; demo vẫn dùng được |
| D02 | Consent thành công; thử khi có/không có Dropbox app | Quay về đúng app, thấy metadata đúng trên web |
| D03 | Hủy hoặc Back từ login | Login dùng lại được, không tự kết nối bằng kết quả cũ |
| D04 | Vào nhánh có tên Unicode, folder trống, nhiều trang | Tên đúng, empty đúng, đủ ID không trùng |
| D05 | Tắt mạng → Refresh; bật mạng → Refresh | Error khác empty; có thể phục hồi |
| D06 | Xoay khi đang tải, Back/Up nhanh | Không nhân đôi Fragment hoặc hiện folder cũ |
| D07 | Đóng/mở process sau login | Phiên khôi phục; browser về root |
| D08 | Logout khi đang tải → mở lại app | Không hiện dữ liệu hoặc phiên cũ |
| D09 | Access token hết hạn | SDK refresh thành công; credential cập nhật còn dùng sau restart |
| D10 | Thu hồi quyền ở Dropbox rồi Refresh | Yêu cầu login lại, không retry vô hạn |
| D11 | Xóa dữ liệu app rồi mở lại | Login, không khôi phục credential cũ |
| D12 | Chuyển tài khoản qua logout/login | Chỉ thấy dữ liệu tài khoản mới, browser về root |

Để kiểm tra token expiry, chờ expiry thực hoặc dùng bản test có clock/credential giả;
không chép refresh token thật vào unit test. Test JVM hiện kiểm tra refresh qua HTTP
giả lập; D09 vẫn cần bằng chứng thật riêng.

## Giới hạn và bài tiếp theo

- Retry hiện do người dùng bấm, chưa có backoff hoặc đếm ngược Retry-After.
- Hủy Future không chắc hủy transport; request cũ có thể còn chạy tới timeout.
- Refresh xóa list hiện tại; chưa cache offline hoặc giữ dữ liệu cũ có nhãn stale.
- Mapping bỏ deleted metadata vì đây là browse theo trang, chưa phải delta sync.
- Chưa có test Android UI, Keystore/instrumentation hoặc kiểm thử process death.
- SDK được ghim 7.0.0 để đối chiếu source; xem compatibility/release notes trước khi nâng.

Sau khi D01–D12 đạt, làm [06 — Upload/download](06-upload-download.md), sau đó
[07 — File actions](07-file-actions.md). Chỉ mở rộng Box sau khi Dropbox MVP được nghiệm thu.

**Checkpoint:** vì sao mã hóa token trên disk, kiểm tra epoch và PKCE giải quyết ba
vấn đề khác nhau? Hãy chỉ ra một ca test tương ứng với mỗi cơ chế.
