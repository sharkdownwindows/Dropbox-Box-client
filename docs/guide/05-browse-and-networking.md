# 05 — Duyệt Dropbox và hiểu networking

**Input:** `DbxClientV2` của phiên hợp lệ; folder ID; cursor tùy chọn.

**Output:** `FolderPage` thật và UI đầy đủ các trạng thái, không bỏ sót trang.

**Ràng buộc:** network trên worker; cursor thuộc một listing; folder/name/ID không
đồng nhất; không biến lỗi API thành danh sách rỗng thành công.

## Kiến thức: HTTP → metadata → model → View

Một lệnh `client.files().listFolder(...)` vẫn là HTTP ở bên dưới. Hãy đọc method,
host/path, auth header, JSON request, JSON response và error của endpoint tương ứng.
SDK giảm công việc đóng gói/parse; không thay thế việc hiểu contract.

Ví dụ request mô tả, **không chứa token thật**:

```http
POST /2/files/list_folder HTTP/1.1
Host: api.dropboxapi.com
Authorization: Bearer <runtime-access-token>
Content-Type: application/json

{"path":"","recursive":false,"limit":100}
```

Metadata có discriminator `.tag`; folder và file không có cùng trường.
Chỉ đọc `size` từ file. `path_display` dùng hiển thị; ID phù hợp làm khóa ổn định.
Đọc [File access](https://developers.dropbox.com/dbx-file-access-guide) và
[Javadoc files](https://dropbox.github.io/dropbox-sdk-java/api-docs/v7.0.0/com/dropbox/core/v2/files/DbxUserFilesRequests.html).

## Bước 1 — Viết adapter dữ liệu

Đọc `data/dropbox/DropboxCloudRepository.java`, đã implement interface hiện có.
Constructor nhận `DbxClientV2`, credential, vault và generation phiên; không lấy Activity.
Phần chính của mapping:

```java
// Bên trong listFolder; imports từ com.dropbox.core.v2.files.
ListFolderResult result = cursor == null
    ? client.files().listFolderBuilder(folderId).withLimit(100L).start()
    : client.files().listFolderContinue(cursor);
List<CloudItem> mapped = new ArrayList<>();
for (Metadata metadata : result.getEntries()) {
    if (metadata instanceof FolderMetadata) {
        FolderMetadata dir = (FolderMetadata) metadata;
        mapped.add(new CloudItem(dir.getId(), dir.getName(), true, 0));
    } else if (metadata instanceof FileMetadata) {
        FileMetadata file = (FileMetadata) metadata;
        mapped.add(new CloudItem(file.getId(), file.getName(), false, file.getSize()));
    }
}
return new FolderPage(mapped, result.getHasMore() ? result.getCursor() : null);
```

`rootId()` trả `""`. Mẫu hiện có đoạn mapping này, lưu credential sau request thành công
và bắt `DbxException`. `CloudException extends IOException` chứa enum loại lỗi;
`retryAfter`/`requestId` chưa được giữ, là phần mở rộng ở bài 08.
Không truyền raw response/token ra UI.

## Bước 2 — Nối repository vào ViewModel

`SessionViewModel` chọn fake hoặc Dropbox. `FilesFragment` observe session rồi gọi
`FilesViewModel.connect(repository, sessionId)`. FilesViewModel giữ constructor
`SavedStateHandle` để Android tạo/khôi phục; chỉ dùng interface `CloudRepository`.
Nhận cùng repository thì không tải lại vì rotation. Phiên mới reset root/parent/cursor;
logout nối repository null, vô hiệu hóa request cũ và xóa danh sách trên UI.
Xem [bài 13](13-run-dropbox-sample.md) để lần theo đường đi cụ thể.

## Bước 3 — Pagination đúng

1. Lần đầu gửi folder ID, `recursive=false`, cursor null.
2. Append entries và lưu cursor nếu server báo còn trang, kể cả trang hiện tại rỗng.
3. Nút Load more gửi cursor, không tạo request root mới.
4. Khóa nút khi đang tải; chỉ cập nhật cursor sau khi nhận thành công.
5. Refresh/đổi folder phải reset cursor. Response muộn phải qua generation check.
6. Dedupe theo item ID khi ghép dữ liệu thật; listing có thể thay đổi giữa các request.

Nếu dùng cursor để đồng bộ lâu dài, phải xử lý cả deleted metadata và reset cursor;
đó là bài sync riêng. Không suy luận rằng skeleton load-more đã là delta sync hoàn chỉnh.

## Bước 4 — Xử lý lỗi có phân loại

| Tình huống | Hành vi |
|---|---|
| Không có mạng/timeout | Hiện Retry, giữ điều hướng, không báo folder rỗng |
| 401/credential hết hạn | Cho SDK refresh; nếu không khôi phục được thì login lại |
| Thiếu scope/quyền | Hướng dẫn cấp lại quyền; không refresh vô hạn |
| 409 | Đọc error tag; folder thiếu/conflict không cùng một nguyên nhân |
| 429 | Tôn trọng Retry-After, backoff có giới hạn |
| 5xx | Retry có giới hạn cho thao tác đọc; giữ khả năng hủy |

Đọc [Dropbox Error Handling](https://developers.dropbox.com/error-handling-guide).
Mẫu hiện phân loại các lỗi trên nhưng **chỉ retry bằng thao tác người dùng**, không
tự chờ `Retry-After` hay backoff. Hàng 429/5xx là mục tiêu cải tiến, chưa phải tính năng đã có.
Không phân loại lỗi bằng so sánh nguyên văn thông báo tiếng Anh. Log chỉ operation,
status/type, request ID và thời gian; không log auth header hay dữ liệu file cá nhân.

## Bài thực hành và nghiệm thu

Tạo folder test với file UTF-8/tiếng Việt, thư mục rỗng, hai nhánh cùng có tên file
`notes.txt`. Tạm đặt limit nhỏ để ép nhiều trang; so tập ID sau cùng với server.
Thử refresh nhanh, chuyển folder khi mạng chậm, revoke quyền, tắt mạng rồi thử lại.
Fake test pass không thay thế các ca này: ghi kết quả test thật riêng.

**Checkpoint:** server trả 0 entries nhưng `has_more=true`; UI có được kết luận folder trống không?
