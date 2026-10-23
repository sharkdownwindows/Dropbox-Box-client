# 10 — Mở rộng Box sau khi Dropbox MVP đạt

**Input:** Dropbox đã qua checklist, `CloudRepository` có contract rõ, nhu cầu Box được xác nhận.

**Output:** provider Box thật cho cùng luồng UI, có auth và bộ test riêng.

**Ràng buộc:** không nhúng Box client secret vào APK; không coi ID/path/cursor hai dịch vụ
là tương đương; không triển khai backend nếu chưa chọn phương án auth.

## Kiến thức và khác biệt phải hiểu

| Điểm | Dropbox | Box |
|---|---|---|
| Gốc | `""` theo access scope | Folder ID `"0"` |
| Định danh | Stable ID và path có vai trò riêng | Chủ yếu file/folder ID |
| List | POST list_folder/continue, cursor | GET folder items, marker hoặc offset |
| Upload | Binary content + metadata qua SDK | Multipart, thuộc tính + stream file |
| Rename | Move tới path mới | Update resource name theo ID |
| Auth trong guide | SDK PKCE cho public client | Tài liệu OAuth đã đọc yêu cầu client secret lúc đổi code |

Nguồn: [Box OAuth](https://developer.box.com/guides/authentication/oauth2/without-sdk/),
[folder items](https://developer.box.com/reference/get-folders-id-items/),
[upload](https://developer.box.com/reference/post-files-content/).
Không suy luận rằng Box không bao giờ hỗ trợ PKCE: điều được xác minh là flow trong
trang đã đọc có `client_secret`. Nếu Box có flow native mới phù hợp, kiểm tra tài liệu
chính thức và thử riêng trước khi thay thiết kế.

## Bước 1 — Chọn auth trước khi viết adapter

Khuyến nghị khi dùng flow OAuth confidential-client của Box: backend giữ client secret,
đổi code và refresh token. Android mở URL đăng nhập của backend; backend lưu state
ràng buộc phiên, nhận callback HTTPS từ Box, đổi code, rồi cấp phiên ứng dụng cho Android.
Không nhét Box token/secret vào deep link. Nếu dùng code một lần để quay về app, code
phải ngắn hạn, dùng một lần và ràng buộc phiên/PKCE hoặc App Link đã xác minh.

Backend proxy API là phương án dễ phân định trách nhiệm: Android gửi session app,
backend áp dụng quyền theo người dùng và giữ token Box. Chi phí: phải deploy/vận hành
backend, thêm latency, bảo vệ session. Đây là phần mở rộng ngoài skeleton hiện có.

Developer token chỉ dùng thử API cục bộ trong thời gian ngắn; không phải cơ chế login
cho ứng dụng nhiều người dùng và không commit token vào app.

## Bước 2 — Adapter Box

Tạo `BoxCloudRepository`, nhận client của backend hoặc transport đã xác thực.
`rootId()` trả `"0"`. Gợi ý contract HTTP (phía nào gọi Box tùy kiến trúc auth đã chọn):

```http
GET /2.0/folders/0/items?usemarker=true&limit=100&fields=id,type,name,size HTTP/1.1
Host: api.box.com
Authorization: Bearer <runtime-box-token>
```

Map `type=file/folder` vào model; skip hoặc biểu diễn rõ loại khác như web_link.
Marker phải coi là opaque; URL-encode parameter. `next_marker` rỗng/null thì hết trang.
Không vừa dùng offset vừa dùng marker trong cùng một thuật toán. Dedupe theo ID trong provider.

```java
// Gợi ý composition, chưa có trong app.
CloudRepository repository = selectedProvider == Provider.BOX
    ? new BoxCloudRepository(authenticatedBoxTransport)
    : new DropboxCloudRepository(dropboxClient);
```

Khi thêm provider picker, thêm provider/account vào khóa cache và namespace item ID;
cùng string ID ở hai dịch vụ không được coi là cùng file. Reset folder stack/cursor
khi đổi provider. Chỉ tạo abstraction mới khi có khác biệt thật cần biểu diễn.

## Bước 3 — Upload/download/actions

Upload Box gửi multipart tới `upload.box.com/api/2.0/files/content`, với `attributes`
chứa tên và parent ID; phần attributes phải đứng trước file. Không copy nguyên request
upload Dropbox. Truyền stream qua backend cần giới hạn body/timeout phù hợp.

Download có thể redirect tới URL nội dung. Dùng SDK/HTTP client theo tài liệu và không
tự chuyển Authorization tới host bất kỳ. Delete/rename theo đúng resource type file/folder.
Quyền và quota của user/enterprise có thể giới hạn chức năng dù token còn hợp lệ.

## Bước 4 — Refresh và phiên

Box refresh-token rotation yêu cầu lưu token thay thế atomically và tránh hai request
cùng dùng refresh token cũ. Đọc [refresh token](https://developer.box.com/guides/authentication/tokens/refresh/).
Nếu backend giữ token, việc refresh single-flight ở backend; Android không tự refresh Box token.
Logout xóa session local và vô hiệu hóa session backend theo thiết kế.

## Nghiệm thu

Chạy lại test contract của repository với fake Box, rồi integration thật trên Box:
root, nhiều trang, empty, file/folder, token hết hạn, quota/conflict, upload/download hash,
logout, đổi provider. Không sửa UI theo dạng `if Dropbox` rải khắp các màn hình.

**Checkpoint:** vì sao thêm `BoxRepository` không đủ để tuyên bố ứng dụng hỗ trợ Box?
