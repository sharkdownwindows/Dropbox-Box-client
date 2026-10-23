# 11 — Tài liệu tham khảo và cách đọc

Đối chiếu web ngày **16/10/2026**. Đây là nguồn chính thức hoặc repository do chủ sở hữu
công bố; API/version có thể thay đổi. Các hướng dẫn triển khai và tiêu chí lab là thiết kế
của bộ guide, không phải toàn bộ câu chữ từ các nguồn này.

## Android

| Nguồn | Đọc phần gì | Dùng ở đâu |
|---|---|---|
| [AGP 8.5 compatibility](https://developer.android.com/build/releases/agp-8-5-0-release-notes) | Gradle 8.7, JDK 17, API 34 | Bài 02: pin toolchain |
| [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) | Lifecycle, scope, saved state | Bài 03: state qua rotation |
| [Java asynchronous work](https://developer.android.com/develop/background-work/background-tasks/asynchronous/java-threads) | Thread pool, worker/main | Bài 03/05: I/O không chặn UI |
| [RecyclerView](https://developer.android.com/develop/ui/views/layout/recyclerview) | Adapter/ViewHolder/LayoutManager | Bài 03: list metadata |
| [AsyncTask API](https://developer.android.com/reference/android/os/AsyncTask) | Deprecated và lựa chọn thay thế | Hiểu vì sao không copy slide cũ |
| [Documents and files / SAF](https://developer.android.com/training/data-storage/shared/documents-files) | Open/CreateDocument, URI, permissions | Bài 06: đọc/ghi file |
| [Android Keystore](https://developer.android.com/privacy-and-security/keystore) | Key storage và crypto operations | Bài 04: token store |
| [Android 12 backup changes](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore) | Data extraction rules | Loại dữ liệu private khỏi backup/transfer |
| [Work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) | Constraints/retry/work policy | Bài 08: mở rộng transfer |

## Dropbox

| Nguồn | Đọc phần gì | Dùng ở đâu |
|---|---|---|
| [OAuth guide](https://developers.dropbox.com/oauth-guide) | Scopes, PKCE, refresh | Bài 04 |
| [Java/Android SDK repository](https://github.com/dropbox/dropbox-sdk-java) | Usage on Android, AuthActivity, queries, release/version | Bài 04: manifest và SDK |
| [Auth source 7.0.0](https://github.com/dropbox/dropbox-sdk-java/blob/v7.0.0/android/src/main/java/com/dropbox/core/android/Auth.kt) | `startOAuth2PKCE`, nhận credential | Đã đọc signature/flow theo đúng version của mẫu |
| [DbxCredential Javadoc 7.0.0](https://dropbox.github.io/dropbox-sdk-java/api-docs/v7.0.0/com/dropbox/core/oauth/DbxCredential.html) | Constructor PKCE, expiry, refresh | Bài 04: client không app secret |
| [File access guide](https://developers.dropbox.com/dbx-file-access-guide) | Metadata, ID/path, traversal, file đặc biệt | Bài 05–07 |
| [Files Javadoc 7.0.0](https://dropbox.github.io/dropbox-sdk-java/api-docs/v7.0.0/com/dropbox/core/v2/files/DbxUserFilesRequests.html) | listFolder/continue, transfer, mutation | Đối chiếu gợi ý code |
| [Error handling](https://developers.dropbox.com/error-handling-guide) | Status/error tags, retry | Bài 05/08 |
| [HTTP API reference](https://www.dropbox.com/developers/documentation/http/documentation) | Request/response/scopes từng endpoint | Mở bằng browser; bản text tự động không tải đủ nội dung JS |
| [Core SDK 7.0.0 POM](https://repo.maven.apache.org/maven2/com/dropbox/core/dropbox-core-sdk/7.0.0/dropbox-core-sdk-7.0.0.pom) | Artifact và dependency đã công bố | Kiểm tra version của ví dụ |
| [Android SDK 7.0.0 POM](https://repo.maven.apache.org/maven2/com/dropbox/core/dropbox-android-sdk/7.0.0/dropbox-android-sdk-7.0.0.pom) | Kotlin runtime dependency | Bài 04 |
| [Core SDK 7.0.0 source JAR](https://repo.maven.apache.org/maven2/com/dropbox/core/dropbox-core-sdk/7.0.0/dropbox-core-sdk-7.0.0-sources.jar) | Credential, HttpRequestor, metadata constructors và error types | Đã đối chiếu khi viết repository và test HTTP giả lập ở bài 13 |

Các URL `developers.dropbox.com` có thể chuyển tới `docs.dropboxapi.com`; redirect
đã được quan sát lúc đọc. Không dựa vào trang HTTP reference trả về chỉ navigation để
khẳng định đã đọc đầy đủ endpoint; guide dùng file-access, SDK/Javadoc và OAuth bổ sung.

## Box

| Nguồn | Đọc phần gì | Dùng ở đâu |
|---|---|---|
| [OAuth without SDK](https://developer.box.com/guides/authentication/oauth2/without-sdk/) | Code exchange và client secret | Chọn kiến trúc auth |
| [Folder items](https://developer.box.com/reference/get-folders-id-items/) | Root ID, fields, pagination | Adapter Box |
| [Upload file](https://developer.box.com/reference/post-files-content/) | Multipart và thứ tự attributes/file | Transfer Box |
| [Refresh token](https://developer.box.com/guides/authentication/tokens/refresh/) | Rotation, lưu token mới | Session backend |

## Nguồn môn học và cách dùng

PDF gốc ở thư mục cha repo; danh mục/trang đọc ở [bài 00](00-materials-and-scope.md).
Không copy nguyên PDF vào repo để tránh nhân đôi tài liệu. Khi clone repo ở máy khác,
sinh viên cần bộ PDF của lớp; các link web vẫn dùng độc lập.

Repo [SonTG/androiddev2022](https://github.com/SonTG/androiddev2022) được intro tr. 8
giới thiệu. Đã kiểm tra trang repository, **chưa audit source của toàn repo đó**;
không coi code năm 2022 là cấu hình toolchain bắt buộc cho bài hiện tại.

## Cách đọc một API trước khi code

1. Endpoint giải quyết thao tác nào, input có phải ID hay path?
2. Scope nào, loại app nào được hỗ trợ?
3. Body JSON, binary hay multipart? Response gồm metadata hay bytes?
4. Có pagination/redirect không? Khi nào kết thúc?
5. Lỗi nào retry được? Mutation timeout có thể đã commit chưa?
6. Viết một test thành công và hai test thất bại trước khi ghép vào UI.

Không học bằng cách copy cả sample rồi sửa tên. Chỉ mượn mẫu giải quyết vấn đề cụ thể,
đối chiếu version, license nếu sao chép code, và giải thích được trách nhiệm mỗi lớp.
