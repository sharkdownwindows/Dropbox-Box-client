# 06 — Upload/download bằng URI và stream

**Input:** Dropbox đã login, folder/file được chọn, URI do system picker cấp.

**Output:** upload xuất hiện trên cloud; download ghi thành công và có byte giống nguồn.

**Ràng buộc:** file lab ≤20 MiB, một transfer/lần, I/O trên worker; không load nguyên
file vào RAM; không xin `MANAGE_EXTERNAL_STORAGE`; không tự ghi đè mà chưa báo người dùng.

## Kiến thức dùng để dựng

SAF trả `content://` URI, là quyền truy cập một tài liệu do provider quản lý, **không phải**
đường dẫn filesystem. Dùng `ContentResolver` mở stream. Với buffer 64 KiB, bộ nhớ
copy không tăng tuyến tính theo kích thước file như `readAllBytes()`.

Đọc [Android: documents and files](https://developer.android.com/training/data-storage/shared/documents-files).
Không chuyển `uri.getPath()` thành `File`, không tìm cột `_data` để đoán đường dẫn.

## Bước 1 — Chọn file upload

Trong Fragment, đăng ký launcher trước khi Fragment tới trạng thái started:

```java
private final ActivityResultLauncher<String[]> pickUpload =
    registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
        if (uri == null) return; // Người dùng hủy: không tạo transfer.
        // Chuyển uri tới ViewModel/transfer controller; không upload trên callback UI.
    });
// Khi bấm Upload:
// pickUpload.launch(new String[]{"*/*"});
```

Imports từ `androidx.activity.result.*` và `androidx.activity.result.contract.*`.
Đọc tên/kích thước bằng query `OpenableColumns.DISPLAY_NAME`/`SIZE` và đóng Cursor.
SIZE có thể null hoặc không chính xác; không coi nó là bằng chứng duy nhất cho giới hạn.

Nếu cần sử dụng URI sau process restart, giữ persistable read permission khi provider
hỗ trợ. Nếu quyền không giữ được, yêu cầu chọn lại hoặc copy có giới hạn vào vùng private.
MVP immediate transfer không được hứa tự resume ở lần mở app sau.

## Bước 2 — Xác định đích và chính sách conflict

Lưu snapshot folder đích ngay lúc bắt đầu transfer; người dùng chuyển folder trong UI
không được làm file tải lên nhầm chỗ. Lấy tên từ picker để gợi ý, cho phép sửa tên,
validate tên (rỗng, dấu phân cách, `.`/`..`, control characters).

Dropbox upload dùng đường dẫn đích. Với folder đã chọn bằng ID, lấy metadata/path
của folder trước khi ghép tên; root dùng chuỗi rỗng. Không ghép `id:ABC/name.txt`
nếu endpoint/SDK không hỗ trợ dạng đó. Nếu không lấy được path hợp lệ, báo lỗi rõ.

Mặc định bài lab: **ADD + không auto-rename + strict conflict**. File trùng tên phải
được xử lý bằng đổi tên hoặc xác nhận rõ trước khi dùng chế độ overwrite/update.

## Bước 3 — Thêm transfer controller

Tạo `transfer/TransferState.java`, `TransferViewModel.java` và phương thức repository
ở bài 01. State: idle → running → succeeded/failed/cancelled. Giữ số byte đã truyền;
khi tổng byte không biết, dùng progress không xác định thay vì hiển thị phần trăm giả.

Gợi ý SDK chạy **trên worker**, imports `FileMetadata`, `WriteMode`, `InputStream`:

```java
try (InputStream input = resolver.openInputStream(sourceUri)) {
    if (input == null) throw new IOException("Cannot open selected document");
    FileMetadata uploaded = client.files().uploadBuilder(destinationPath)
        .withMode(WriteMode.ADD)
        .withAutorename(false)
        .withStrictConflict(true)
        .uploadAndFinish(input);
    // Chuyển metadata về state; chỉ success sau khi uploadAndFinish trả thành công.
}
```

Đoạn trên minh họa đường stream, **chưa** thực thi giới hạn 20 MiB hoặc báo progress.
Trước nghiệm thu, bọc input bằng counting/limited stream: mỗi lần đọc tăng counter,
vượt giới hạn thì ném IOException. Với tài liệu không rõ size và cần bảo đảm tuyệt đối
không gửi quá ngưỡng, copy có giới hạn vào file private trước rồi mới upload.

Giới hạn file lớn của endpoint phụ thuộc API hiện tại. File ngoài lab dùng upload session
theo tài liệu Dropbox; không suy ra 20 MiB là giới hạn của dịch vụ.

## Bước 4 — Chọn nơi lưu download

Đăng ký `ActivityResultContracts.CreateDocument("application/octet-stream")`, launch
bằng tên gợi ý. Lưu **pending file ID** vào SavedStateHandle trước khi mở picker;
callback có thể đến sau khi Activity bị tái tạo.

```java
// Worker; sinkUri do CreateDocument trả về, fileId đã chụp trước đó.
try (OutputStream output = resolver.openOutputStream(sinkUri, "w")) {
    if (output == null) throw new IOException("Cannot open download destination");
    client.files().download(fileId).download(output);
    output.flush();
}
```

Chỉ công bố success **sau khi** stream đã close thành công. Nếu lỗi giữa chừng, file đích
có thể tồn tại một phần: đánh dấu thất bại, đề nghị xóa bản dở (provider có thể không hỗ trợ
xóa), không hiển thị như file hoàn chỉnh. Nếu cần bảo đảm mạnh hơn, download vào file tạm
private trước, verify rồi copy ra URI; vẫn phải xử lý lỗi bước copy cuối.

File không downloadable/định dạng cloud đặc biệt: báo không hỗ trợ, không ghi trang HTML
lỗi thành file `.pdf`. Chỉ preview các loại nhóm chủ động hỗ trợ.

## Bước 5 — Hủy và tiến trình

- Không chỉ đổi text sang Cancelled: phải hủy request/đóng stream khi API hỗ trợ.
- Cancellation là best-effort; server có thể đã commit upload trước lúc app nhận hủy.
  Refresh remote để xác định kết quả, không tự upload lại ngay.
- Chặn hai lần nhấn Upload và double submit sau rotation bằng transfer ID/state.
- Dùng application context cho resolver nếu controller sống lâu hơn màn hình.
- Persistable URI permission + WorkManager là phần nâng cao ở bài 08, chưa có trong skeleton.

## Nghiệm thu

1. Chuẩn bị file 0 byte, 1 KiB, tên tiếng Việt, tên có dấu cách, file 20 MiB và vượt giới hạn.
2. Upload một file; kiểm tra tên/kích thước trên Dropbox web, không chỉ Toast trong app.
3. Download bản vừa upload; tính SHA-256 trên **hai file local** và so sánh.
4. Thử cancel picker, URI bị thu hồi quyền, hết dung lượng, mất mạng giữa luồng, tên trùng.
5. Quay màn hình khi transfer: không upload lần thứ hai, không giữ view cũ.

Dropbox `content_hash` có thuật toán riêng; không so nó trực tiếp với SHA-256 thông thường
của toàn bộ file. Trong bài lab, so hai hash local hoặc áp dụng đúng thuật toán được công bố.

**Checkpoint:** vì sao query SIZE thành công chưa bảo đảm stream chỉ có đúng số byte đó?
