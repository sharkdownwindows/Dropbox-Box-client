# 03 — UI, trạng thái và lifecycle trước khi nối mạng

**Input:** skeleton build được; model `CloudItem`, `FolderPage`; slide fundamentals/resources/tasks.

**Output:** màn hình danh sách dùng fake repository, xử lý đủ loading/content/empty/error,
đi vào thư mục, quay lại, tải trang tiếp, xoay màn hình.

**Ràng buộc:** Fragment chỉ xử lý UI; không giữ binding sau `onDestroyView()`;
không tạo request mới chỉ vì màn hình được dựng lại.

## Kiến thức dùng để dựng

- Activity là host; Fragment có vòng đời riêng cho object và cho view.
- ViewBinding tạo lớp truy cập view từ XML, giảm nhầm ID/ép kiểu.
- RecyclerView tái sử dụng hàng; ListAdapter/DiffUtil so sánh bằng ID và nội dung.
- ViewModel giữ state qua configuration change; `SavedStateHandle` giữ dữ liệu nhỏ
  để khôi phục khi hệ thống tái tạo. Nó không phải database hoặc kho lưu token.
- LiveData chỉ cung cấp state tới observer còn hoạt động; worker không sửa View trực tiếp.

## Bước 1 — Đọc XML và sửa một hàng

Mở `item_cloud_file.xml`: thêm trường thời gian sửa đổi sau khi mở rộng model.
Đặt label trong `strings.xml` và `values-vi/strings.xml`. Dùng `dp` cho kích thước,
`sp` cho chữ; tên file dài phải đọc được, không tràn lên nút thao tác.

**Input hàng:** metadata của một item. **Output:** tên, loại, kích thước/thời gian.
Không tải toàn bộ nội dung file để vẽ hàng. Folder không có kích thước file giả.

## Bước 2 — Hiểu một lần tải

```text
Refresh → state.loading=true → executor gọi listFolder
        → thành công: state có items + nextCursor
        → thất bại: state.failed=true, hiện Retry
```

Đọc `FilesViewModel.load()`. Biến `generation` tăng sau mỗi request.
Ví dụ request A đang chậm, người dùng chuyển sang B: A trả về sau B phải bị bỏ qua.
`Future.cancel(true)` chỉ yêu cầu interrupt; API/SDK có thể không dừng ngay.
Vì vậy vẫn cần kiểm tra generation trước khi công bố kết quả.

## Bước 3 — Observe theo vòng đời của view

Gợi ý trong `FilesFragment.onViewCreated()`:

```java
model.state().observe(getViewLifecycleOwner(), state -> {
    binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
    adapter.submitList(state.items);
});
```

Trong `onDestroyView()`, bỏ adapter và đặt binding về null. Không observe bằng
Fragment lifecycle nếu callback còn truy cập view đã bị hủy. Không truyền binding
vào ViewModel hoặc callback của network.

## Bước 4 — Navigation và restore

Fake root là chuỗi rỗng. Khi click folder, đẩy folder hiện tại vào stack rồi mở ID con.
Khi Back/Up, pop stack. Không đoán parent bằng `split("/")` trên ID.
Đối với Dropbox thật cần ID ổn định; đối với Box ID thường là chuỗi số.

Code lưu folder/parent ID và tên bằng `SavedStateHandle`, giữ vị trí khi rotation
trong cùng phiên. Sau process recreation, `SessionViewModel` tạo phiên mới và browser
về root sau khi khôi phục credential; không tái dùng stack/cursor của phiên cũ.
Nó không lưu file bytes hoặc token trong Bundle.
Activity chỉ thêm Fragment lần đầu (`savedInstanceState == null`) để không nhân đôi màn hình.

## Bước 5 — Bài tập của sinh viên

1. Đã hiển thị tên thư mục thay ID; hãy mở rộng thành breadcrumb đầy đủ có thể bấm.
2. Thêm icon file/folder có content description nếu icon truyền thông tin riêng.
3. Thêm popup menu bằng long press hoặc nút overflow; xóa chưa nối API ở bài này.
4. Tạo fake chậm để quan sát spinner; không `sleep` trên UI thread.
5. Giữ danh sách cũ khi refresh lỗi, đồng thời gắn nhãn dữ liệu cũ. Skeleton hiện
   xóa list khi refresh: đây là cải tiến bài tập, không phải tính năng đã có.

## Nghiệm thu

| Ca | Kỳ vọng |
|---|---|
| Root → Load more | Tổng 5 item, không lặp |
| Course → Back | Về root |
| Empty folder | Hiện thông báo trống, không lỗi |
| Simulated error | Hiện lỗi, Up còn hoạt động |
| Xoay trong Course | Vẫn ở Course, không thêm Fragment trùng |
| Chuyển B khi A còn chạy | Kết quả A không thay list B |
| Ngôn ngữ Việt, font lớn | Label dịch đúng, thao tác vẫn truy cập được |

**Checkpoint:** vì sao ViewModel sống qua rotation nhưng không đồng nghĩa transfer
sẽ sống qua việc hệ điều hành giết process?

Đọc thêm: [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel),
[RecyclerView](https://developer.android.com/develop/ui/views/layout/recyclerview).
