# 07 — Tạo thư mục, đổi tên, xóa và share tùy chọn

**Input:** phiên hợp lệ, item đang chọn, parent folder, tên hoặc xác nhận người dùng.

**Output:** thay đổi thật được đọc lại từ Dropbox, UI và server thống nhất.

**Ràng buộc:** dùng file test; mutation không retry mù; không dùng tên hiển thị làm ID;
không xóa ngay khi long press hoặc swipe.

## Kiến thức

Một request timeout không chứng minh server chưa thực hiện. Thao tác ghi cần xác minh
trạng thái trước khi retry. UI phải phân biệt pending, success, failure và kết quả chưa rõ.
DialogFragment phù hợp để xác nhận và không giữ callback vào view đã hủy.

## Bước 1 — Chuẩn bị action menu

Trong row menu: Details, Download, Rename, Delete. Nút New folder ở màn hình thư mục.
Chuyển item ID + thông tin cần thiết, không chuyển `position` rồi tra lại list sau khi
người dùng xác nhận vì list có thể đã thay đổi.

Tạo enum/action state riêng nếu cần; không thêm một repository cho từng nút.

## Bước 2 — Validate tên

Gợi ý hàm Java thuần để unit-test, đây là quy tắc lab tối thiểu, không phải toàn bộ
quy tắc tên hợp lệ của mọi provider:

```java
static boolean isValidName(String value) {
    if (value == null || value.trim().isEmpty()) return false;
    if (!value.equals(value.trim()) || value.equals(".") || value.equals("..")) return false;
    for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        if (c == '/' || c == '\\' || Character.isISOControl(c)) return false;
    }
    return true;
}
```

Giữ nguyên Unicode hợp lệ; không tự bỏ dấu tiếng Việt. Server là nơi quyết định cuối;
hiển thị lỗi validation/conflict đúng loại thay vì catch rồi báo “mất mạng”.

## Bước 3 — Mapping thao tác

| Thao tác | Input cụ thể | Dropbox SDK / endpoint cần đọc | Sau thành công |
|---|---|---|---|
| New folder | Parent path + tên | `createFolderV2`, `/files/create_folder_v2` | Reload parent, thấy folder mới |
| Rename | Item ID/path + destination path cùng parent | `moveV2`, `/files/move_v2` | Reload; cập nhật path cũ trong cache |
| Delete | Item ID + confirmation | `deleteV2`, `/files/delete_v2` | Reload; item không còn trong listing |
| Share, tùy chọn | Item + chính sách truy cập | `create_shared_link_with_settings`, `list_shared_links` | Link đúng phạm vi, không tự public |

Rename trong Dropbox dùng cơ chế move; giữ nguyên parent trong đường dẫn đích.
Tách `parentPath` và `newName`, không replace chuỗi tên ở mọi vị trí của path.
Không rename root. Không cho xóa root. Folder delete có thể ảnh hưởng nhiều file con:
dialog phải nói rõ tên và tác động; MVP có thể chỉ cho xóa file/folder rỗng.

## Bước 4 — Luồng mutation an toàn

```text
Chọn item → dialog nhập/xác nhận → validate → khóa nút xác nhận
          → worker gọi API → refresh parent → thông báo kết quả
          → nếu timeout: trạng thái chưa rõ, kiểm tra metadata/list trước retry
```

Gợi ý trong ViewModel: chụp `selectedItemId`, parent và generation phiên vào request.
Nếu logout/chuyển account trước khi response tới, không publish kết quả vào phiên mới.
Có thể bỏ optimistic update ở MVP để tránh phải viết rollback phức tạp.

## Bước 5 — Share chỉ khi có thời gian

App Folder có hạn chế theo endpoint; kiểm tra loại access, scopes và policy trước khi
hứa tính năng. Link có thể đã tồn tại: xử lý lỗi tương ứng, truy vấn link thay vì cứ tạo mới.
Cho người dùng chủ động bấm Share; mô tả ai có thể mở. Không mặc định biến file private
thành link công khai. Share không nằm trong acceptance MVP bắt buộc của guide này.

## Nghiệm thu

- Create folder hợp lệ; tên rỗng/space-only/`../x` không được gửi.
- Rename thành công, rename trùng tên không ghi đè file khác.
- Delete → Cancel: server không đổi. Delete → Confirm: đúng item bị xóa.
- Thử timeout sau server commit: không tạo thư mục hoặc file trùng do retry.
- Sau rename, download dùng ID/path mới hợp lệ; không giữ tham chiếu cũ trong UI.

**Checkpoint:** khi rename request timeout, vì sao không thể kết luận rename thất bại?
