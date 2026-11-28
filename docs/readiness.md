# Trạng thái tích hợp và kiểm chứng — 28/11/2026

Đây là mốc kiểm chứng có ngày, không phải tuyên bố mọi chức năng MVP đã hoàn tất.
[API](api.md) quy định hành vi; [quyết định](decisions.md) giữ các ranh giới kiến
trúc. Merge code, deployment, áp dụng SQL và cài APK là các bước riêng.

## Các phiên bản đã đối chiếu

| Phần | Bằng chứng đã xác nhận | Giới hạn |
| --- | --- | --- |
| `develop` | [PR #60](https://github.com/KhiemPham1512/Lost-Found/pull/60) đã merge thành `faedf614757499ceae2eb5e3491a2929cc88817e` | Chưa quảng bá bản demo sang `main` trong đợt này |
| Admin web trên Render | Deployment `dep-dc58i460tbcc73dvapdg` có trạng thái LIVE tại `faedf61` | LIVE không thay thế kiểm thử các flow trên web hosted |
| Backend trên Render | Deployment `dep-dc00vas9v7es73cdqmgg` vẫn LIVE tại `b82143e684c5af8bd63feea58e5f7213c5ba62eb` | PR #49–60 không đồng nghĩa backend đã rebuild ở cùng SHA |
| APK debug trên Xiaomi USB | Đã cài code được review `38ac8ae`, tương đương source Android của merge #60 | Chưa phát hành APK release hoặc kiểm tra mọi máy/ROM |
| Helper trigger | [PR #54](https://github.com/KhiemPham1512/Lost-Found/pull/54) merge `f57cbc8`; patch được duyệt và áp dụng riêng ngày 27/11, migration `20261127131521` | Merge không tự chạy SQL; không thay đổi RLS, thân hàm hoặc Auth settings |
| Cron hết hạn | Lần chạy tự nhiên `expire-posts` lúc 14:00 UTC ngày 27/11 thành công sau patch #54 | Không dùng kết quả job để suy ra có bài đủ điều kiện đã đổi trạng thái |

## Android: điều đã kiểm tra

- [PR #49](https://github.com/KhiemPham1512/Lost-Found/pull/49) đã merge tại
  `a3b5769`: theo dõi yêu cầu của claimant và đọc lại inbox qua JWT/RLS. Đây là
  cập nhật khi app mở, không cung cấp push hệ điều hành khi app đóng.
- PR #55–60 bổ sung retry đọc hồ sơ, cập nhật vòng đời bài, lỗi tải lại báo cáo,
  đối soát khi mất phản hồi gửi, bố cục chữ lớn và saved state form báo cáo.
  Không thay enum, endpoint hoặc cấp thêm quyền nghiệp vụ.
- Trên APK của PR #60: **127 instrumentation test đạt** trên máy thật, gồm
  10 ca khôi phục form, 15 ca báo cáo, 23 ca giao diện liên quan và 79 ca
  repository/phiên/realtime; build debug, test APK, một unit test mẫu và lint đạt.
  Lint có 0 lỗi và 21 warning sẵn có. Test mới và bằng chứng thiết bị giữ local
  theo chính sách repo; lỗi HTTP được tạo tại ranh giới ngoài, không phải sự cố live.
- Ca native riêng ngày 28/11: mở form báo cáo từ Home, chọn Khác và nhập draft
  trên một bài QA sẵn có, không bấm Gửi. Sau HOME và `am kill` chỉ cho app này,
  PID cũ đã mất. Chọn đúng thẻ LostFound trong Đa nhiệm tạo PID mới và khôi phục
  đúng đối tượng, lý do và nguyên nội dung. Không đổi font, bàn phím hoặc máy ảo
  của dự án khác; cuối ca đã đăng xuất QA về màn Đăng nhập.
- Lệnh thử mở một Activity mới phía trên Activity gốc từng cho kết quả Home;
  quay về Activity gốc vẫn thấy draft. Vì vậy kết luận khôi phục ở trên dựa vào
  **task cũ trong Đa nhiệm**, không dựa vào thao tác mở Activity mới. Đây là ca
  dừng tiến trình có chủ đích, chưa mô phỏng áp lực bộ nhớ tự nhiên của Android.

## Phần cần kiểm chứng tiếp trước bản demo

| Ưu tiên | Việc tiếp theo | Phạm vi còn thiếu |
| --- | --- | --- |
| 1 | Kiểm tra admin web hosted bằng dữ liệu QA có kiểm soát | Viewer ảnh chat riêng tư và thao tác gỡ bài/khóa tài khoản chưa được xác nhận qua trình duyệt hosted trong mốc này |
| 2 | Mở rộng lifecycle trên thiết bị | Khôi phục sau mất tiến trình cho draft chat/câu trả lời, xoay máy và mất mạng dài; ca report ở trên không chứng minh toàn app |
| 3 | Đối chiếu mốc release | Chốt phiên bản APK/backend/web/SQL đã test cùng nhau trước khi đề nghị promote `develop` sang `main` |

Không suy ra backup/restore cloud, giữ draft sau force-stop/xóa dữ liệu, mọi cỡ
chữ/màn hình hay mọi luồng live từ các test trên. Các ca kiểm chứng mới phải tránh
phát lại thao tác gửi chưa rõ kết quả và không dùng dữ liệu người dùng thật làm
fixture. Không có SQL, Auth settings hoặc dữ liệu nghiệp vụ mới được thay đổi
trong đợt cập nhật tài liệu này.
