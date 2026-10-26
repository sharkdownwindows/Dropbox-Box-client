# 14 — Dùng AI tiếp tục project trên máy khác

**Input:** GitHub repo, một máy có Git + Android Studio/JDK 17/SDK 34, AI coding agent có
quyền đọc checkout, và app key Dropbox do bạn tự cấu hình cục bộ.

**Output:** AI hiểu đúng trạng thái trong vài file nhỏ, tiếp tục đúng milestone, kiểm tra bằng
bằng chứng mới và để lại handoff cho phiên sau mà không cần lịch sử chat cũ.

**Ràng buộc:** Git là nguồn truyền ngữ cảnh; không gửi credential trong prompt; không coi mô tả
cũ là bằng chứng hiện tại; không yêu cầu AI đọc tất cả PDF/guide/source trước mỗi tác vụ.

## 1. Ngữ cảnh được chia theo trách nhiệm

| Nguồn | AI đọc khi nào | Nội dung sở hữu |
|---|---|---|
| `AGENTS.md` | Mọi phiên ở repo | Quy tắc kỹ thuật, bảo mật, cách kiểm tra và mức bằng chứng |
| `PROJECT-STATUS.md` | Đầu mọi phiên | Đã làm gì, đã kiểm chứng gì, blocker và việc kế tiếp |
| Guide 13 | Khi setup/login/browse | Cấu hình app key, luồng code và checklist device/live |
| Guide 06/07/10 | Chỉ khi làm đúng phần đó | Transfer, file actions hoặc Box |
| Code + test + output mới | Trước kết luận | Nguồn sự thật cao nhất về checkout hiện tại |

[OpenAI Docs](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex)
xác nhận Codex nhận `AGENTS.md` làm chỉ dẫn repository lâu dài. File trạng thái là quy ước
của project: nó giúp tải ngữ cảnh nhanh nhưng phải được đối chiếu với code và Git.

## 2. Chuẩn bị máy mới

Clone vào đường dẫn ASCII ngắn để tránh lỗi Java/Gradle đã từng gặp với Unicode:

```powershell
git clone https://github.com/sharkdownwindows/Dropbox-Box-client.git C:\dev\Dropbox-Box-client
Set-Location C:\dev\Dropbox-Box-client
git status --short --branch
git log -1 --oneline
```

Mở `C:\dev\Dropbox-Box-client` trong Android Studio. Chọn JDK 17, cài SDK Platform 34 và
Build Tools 34.0.0, rồi Sync. Android Studio thường tạo `local.properties` với `sdk.dir`.
Nếu phải tạo tay, copy nội dung cần thiết từ `local.properties.example`, nhưng dùng đường dẫn
SDK của máy mới.

Để chạy Dropbox thật, tự mở `local.properties` và thêm:

```properties
DROPBOX_APP_KEY=yourActualAppKey
```

Không paste client secret, token hoặc password vào chat. App key là identifier và được dùng để
tạo callback scheme, nhưng vẫn nên giữ cấu hình theo từng máy/app đăng ký. Build/cài lại sau khi
đổi key. Chi tiết nằm ở guide 13.

Trước khi sửa code, chạy baseline:

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Nếu baseline fail, gửi AI output lỗi đầy đủ từ task đầu tiên thất bại, phiên bản Java/Android SDK
và `git status`; không chỉ gửi ảnh dòng cuối `BUILD FAILED`.

## 3. Prompt mở phiên đầu tiên trên máy mới

Copy prompt này vào Codex hoặc một AI coding agent có thể đọc repo:

```text
Bạn là senior Android/Mobile Application Development và là engineering partner của project này.

Trước tiên chỉ làm read-only preflight:
1. Đọc AGENTS.md và docs/guide/PROJECT-STATUS.md.
2. Đọc docs/guide/14-ai-handoff.md và chỉ guide liên quan trực tiếp tới tác vụ hiện tại.
3. Kiểm tra git status, branch/HEAD, cấu trúc source/test và toolchain đang có.
4. Đối chiếu status note với code/test hiện tại; báo rõ điểm nào cũ hoặc chưa xác minh.

Không đọc toàn bộ PDF, toàn bộ docs hoặc toàn bộ repo nếu chưa cần. Không đọc/in/log
local.properties, token, credential JSON, password hoặc keystore.

Tác vụ hiện tại: thiết lập checkout mới, chạy baseline build/test/lint, sau đó hướng dẫn tôi
thực hiện checklist D01-D12 của guide 13 trên thiết bị. Không triển khai upload/download,
CRUD hoặc Box trong phiên này trừ khi tôi đổi phạm vi rõ ràng.

Trước khi sửa code, nêu: hành vi cần đạt, input, output, ràng buộc, acceptance criteria và
thay đổi tối thiểu dự kiến. Chỉ sửa khi có lỗi được quan sát hoặc tôi yêu cầu tính năng.
Phân biệt implemented, JVM-tested, device-tested và live Dropbox-tested.
Không commit/push nếu tôi chưa yêu cầu.
```

Prompt này không mang code hoặc lịch sử chat; nó chỉ chỉ cho AI đọc đúng nguồn đã version-control.
Nếu dùng AI không tự nhận `AGENTS.md`, thêm câu đầu: “Hãy coi `AGENTS.md` là instruction bắt buộc”.

## 4. Prompt cho từng phần tiếp theo

Sau khi D01–D12 có bằng chứng, chọn **một** phần. Ví dụ upload/download:

```text
Đọc AGENTS.md, docs/guide/PROJECT-STATUS.md và docs/guide/06-upload-download.md.
Kiểm tra code/test liên quan và git status; không đọc các guide không liên quan.

Hãy triển khai vertical slice nhỏ nhất: chọn một file bằng Android SAF, upload bằng stream
vào folder Dropbox hiện tại, rồi chứng minh metadata/bytes đúng. Trước khi code, ghi input,
output, giới hạn file, trạng thái UI, lỗi cần xử lý và acceptance criteria. Không thêm Box,
cache, share, database hoặc WorkManager. Viết test có ý nghĩa, chạy focused test rồi
assemble/test/lint. Không dùng token thật trong test/log. Cập nhật PROJECT-STATUS.md bằng
bằng chứng quan sát được; không commit/push nếu tôi chưa yêu cầu.
```

Thay câu “vertical slice” bằng một mục khác từ guide 07 hoặc 10 khi đến milestone đó.
Không giao nhiều milestone trong một prompt vì AI dễ tạo code rộng nhưng khó kiểm chứng.

## 5. Cách làm việc với AI trong một phiên

1. Cho AI làm read-only preflight và xác nhận checkout sạch/bẩn.
2. Yêu cầu AI giải thích ngắn luồng hiện tại trước khi sửa nếu bạn chưa hiểu nó.
3. Chốt một hành vi quan sát được, ví dụ “upload file 1 MiB và thấy đúng hash”, thay vì
   “làm hoàn chỉnh upload”.
4. Cho AI tự sửa và chạy test trong phạm vi đã chốt. Bạn thực hiện thao tác cần tài khoản/
   thiết bị; AI đọc kết quả đã khử dữ liệu nhạy cảm.
5. Khi có lỗi, gửi log text và bước tái hiện. Yêu cầu reproduce → root cause → minimal fix → verify.
6. Cuối phiên, yêu cầu cập nhật status, review diff và nêu giới hạn. Chỉ sau đó mới yêu cầu commit/push.

AI không thể tự chứng minh consent, account policy, Keystore hoặc file xuất hiện trên Dropbox web
nếu nó không có device/account evidence. Bạn là người cung cấp quan sát đó; AI phải ghi đúng mức.

## 6. Prompt bàn giao cuối phiên

```text
Kết thúc phiên này bằng handoff ngắn cho AI/máy tiếp theo.

1. Đối chiếu git diff/status, code/test và output vừa chạy.
2. Cập nhật docs/guide/PROJECT-STATUS.md chỉ với sự thật mới: branch/commit, hành vi đã làm,
   lệnh và kết quả, device/live evidence, blocker, việc nhỏ nhất tiếp theo.
3. Không chép log dài, credential hoặc diễn biến hội thoại. Không biến kế hoạch thành kết quả.
4. Chạy git diff --check và kiểm tra link Markdown thay đổi.
5. Tóm tắt file thay đổi và mức kiểm chứng. Không commit/push nếu tôi chưa yêu cầu riêng.
```

Sau khi review, commit status cùng code của milestone. Máy tiếp theo chỉ cần `git pull --ff-only`,
đọc `AGENTS.md` + `PROJECT-STATUS.md`, rồi dùng prompt mở phiên. Nếu status và code mâu thuẫn,
AI phải ưu tiên code/test/output hiện tại và sửa lại status.

## 7. Điều không nên làm

- Không paste toàn bộ cuộc chat cũ: dài, trùng và nhanh lỗi thời.
- Không dùng một “mega prompt” chứa mọi guide; prompt chỉ nên trỏ tới nguồn cần đọc.
- Không ghi “test pass” dựa trên status cũ sau khi đổi máy/dependency.
- Không đưa `local.properties`, generated access token, refresh token hay log credential lên Git.
- Không để AI vừa làm OAuth, transfer, CRUD và Box trong một phiên chưa có acceptance criteria.
- Không để status file tăng vô hạn; Git history giữ lịch sử, status chỉ giữ trạng thái hiện tại.

**Checkpoint:** nếu `PROJECT-STATUS.md` nói 25 tests pass nhưng checkout mới chạy 24 tests và
một test fail, nguồn nào được ưu tiên và AI phải cập nhật handoff ra sao?
