# Cinema HUB – Tài liệu tóm tắt dự án

## 1. Kiến trúc & công nghệ
- **Back-end**: Spring Boot 3, Java 21, Spring MVC, Spring Security, Spring Data JPA (SQL Server), REST API.
- **Front-end**: Thymeleaf SSR + các bundle JS thuần (không dùng framework SPA). CSS tổng hợp trong `styles.css` và các file chuyên dụng như `seat-selection.css`, `staff-ui.css`, `admin.css`.
- **CSDL**: Microsoft SQL Server. Tập tin `docs/insert sql.txt`, `docs/sample-movies.sql`, `docs/query-new-staff.sql` dùng để khởi tạo dữ liệu và kiểm thử.
- **Tầng tích hợp**: PayOS VietQR (booking online & staff counter), SMTP gửi mail + PDF vé, lưu trữ tệp tĩnh tại thư mục `/uploads`.
- **Build/Test**: Maven (`mvn -q test` mặc định sau mỗi chỉnh sửa), cấu hình VS Code/IntelliJ thông qua `.vscode/launch.json`.

## 2. Các mô-đun chức năng

### 2.1 Trang web khách hàng
- **Trang chủ & landing**: lấy dữ liệu phim “Đang chiếu”, “Sắp chiếu”, banner promo, combo ưu đãi và render qua Thymeleaf.
- **Danh sách phim & chi tiết phim**: hiển thị poster, trailer YouTube, tóm tắt, rating, thời lượng, lịch chiếu; có bộ lọc theo ngày/phòng/định dạng.
- **Đặt vé online**:
  - `seat-selection.html` và `seat-selection.js` kết nối `/api/showtimes/{id}` để tải sơ đồ ghế snapshot theo từng suất, giữ ghế tối đa 10 phút (SeatHold).
  - Cho phép combo đồ ăn, mã giảm giá, PayOS VietQR, tiền mặt/quầy.
  - `ticket-view.html`, `ticket-email.html`, `ticket-pdf.html` dùng chung dữ liệu `Booking`.
- **Tài khoản người dùng**:
  - Đăng ký/đăng nhập bằng email, số điện thoại, mật khẩu.
  - Trang `profile.html` cung cấp tab thông tin, lịch sử vé có phân trang (mỗi trang 10 vé).
  - Cho phép đổi thông tin cá nhân, reset mật khẩu, quản lý thông báo.

### 2.2 Admin Workspace ( `/admin/*` )
- **Quản lý phim**: CRUD đầy đủ (tên, tựa quốc tế, trailer, thể loại, đạo diễn, diễn viên, mô tả, thời lượng, độ tuổi, poster). Hỗ trợ upload hình ảnh (`/uploads/movies`), preview ngay sau khi chọn file.
- **Quản lý suất chiếu**: lập lịch theo phim/phòng/khung giờ; các template tạo nhanh (từng ngày, cả tuần, ngày trong tuần, cuối tuần). Cho phép lặp đến ngày kết thúc và tự clone sơ đồ ghế với giá theo block (weekday/weekend + morning/afternoon/evening).
- **Quản lý phòng chiếu**: định nghĩa tên, số hàng/cột, trạng thái kích hoạt. Có cảnh báo nếu phòng đang chứa suất chiếu hoạt động.
- **Quản lý banner & khuyến mãi**: đặt thời gian hiệu lực, liên kết phim/URL, vị trí hiển thị. Danh sách hỗ trợ ON/OFF tự động theo ngày.
- **Quản lý tài khoản & phân quyền**: staff, admin, marketing. Tất cả thao tác CRUD dùng fetch API, confirm dialog tiếng Việt, toast báo thành công/thất bại.
- **Báo cáo**:
  - Dashboard doanh thu tổng, theo phim, theo phòng thông qua `cinema-backend-revenue`.
  - Xuất Excel (Apache POI `poi-ooxml`), biểu đồ hiển thị trên `admin-dashboard.html`.

### 2.3 Staff Portal ( `/staff/*` )
- **Dashboard**: tuần lịch chiếu (7 ngày hiện tại), lọc nhanh theo phim, xem booking đang chờ và booking hôm nay. Các card liên kết tới quầy đặt vé với `showtimeId`.
- **Counter Booking**:
  - Trang `counter-booking.html` + `staff-booking.js` cho phép chọn phim/suất đang mở, xem occupancy, chọn ghế bằng seat fragment chung, tính tổng tiền.
  - Panel “Thông tin khách hàng” tổng hợp số điện thoại, email, discount, phương thức thanh toán (Tiền mặt/Chuyển khoản).
  - Khi chọn chuyển khoản, hệ thống tạo booking pending, gọi PayOS để sinh VietQR, poll trạng thái và mở link chi tiết `/staff/bookings/{code}` ngay khi thanh toán thành công.
- **Pending/confirmation**:
  - `booking-confirmation.html`, `booking-qr.html`, `pending-tickets.html`, `sold-tickets.html` phục vụ việc in vé, xem QR, xác nhận thanh toán.
  - `staff-dashboard.js`, `staff-tickets.js`, `staff-scan.js` hỗ trợ tìm vé thủ công và tự động đọc QR (tự điền BookingCode vào trường nhập).
- **QR Scan**: `/staff/qr-scan` dùng camera/ảnh, giải mã và tự fetch booking chi tiết, đồng thời auto điền form “Nhập mã thủ công”.
- **Booking service phụ trợ**: `StaffBookingService`, `StaffShowtimeService`, `StaffBookingController`, `StaffPaymentController`, `StaffBookingVerificationController`.

### 2.4 Các dịch vụ chung
- **BookingService & SeatReservationService**: đảm bảo giữ ghế, giải phóng hold theo timer (task scheduled), kiểm tra trùng lặp, chuyển trạng thái booking (Pending → Paid → Confirmed → Cancelled).
- **Payment**: PayOS (VietQR) + tiền mặt tại quầy. `TicketEmailService` gửi mail có PDF vé (OpenHTML2PDF + font Roboto hỗ trợ tiếng Việt).
- **AuthService/AuthController**: xác thực người dùng, staff, admin với Spring Security, ghi log hoạt động.
- **ProfileService/ProfileController**: cập nhật thông tin cá nhân, đổi mật khẩu, lấy lịch sử vé (phân trang `Pageable`).
- **MovieService/ShowtimeService**: cung cấp dữ liệu public qua REST (đang chiếu, sắp chiếu, chi tiết theo ID, showtime theo ngày…).

## 3. Dữ liệu & sơ đồ CSDL
- **Thực thể chính**:
  - `Movie`, `Showtime`, `Auditorium`, `Seat`, `SeatSnapshot`, `Booking`, `BookingSeat`, `Promotion`, `Banner`, `User`, `Staff`, `PaymentLog`.
  - `BookingStatus` (`Pending`, `PendingVerification`, `Confirmed`, `Cancelled`, `Refunded`), `PaymentStatus`, `PaymentMethodNormalizer` giữ đồng bộ tên phương thức.
- **Seed & script**:
  - `docs/insert sql.txt`, `docs/sample-movies.sql` thêm dữ liệu demo phim/suất/suất chiếu.
  - `docs/query-new-staff.sql` hỗ trợ tạo staff nhanh.
  - `docs/insert sql.txt` còn chứa script cập nhật status phim quá hạn showtime → “Hết chiếu”.
- **Quy tắc dữ liệu**:
  - Poster/Banner được lưu theo UUID trong `uploads/movies` hoặc `uploads/banners`.
  - Seat snapshot lưu giá ghế, row/column label, cho phép đổi giá theo thời gian (không ảnh hưởng các booking cũ).
  - Tự động cập nhật trạng thái phim/banners dựa trên ngày chiếu/ngày hiệu lực.

## 4. Luồng nghiệp vụ tiêu biểu
1. **Đặt vé trực tuyến** (khách):
   - Người dùng chọn phim/suất → `seat-selection.js` tải sơ đồ ghế → chọn ghế + thông tin liên hệ → PayOS tạo VietQR → webhook xác nhận → gửi email + PDF.
2. **Đặt vé tại quầy** (staff):
   - Staff chọn suất từ dashboard hoặc filter → `counter-booking` hiển thị ghế, tính tổng, nhập dữ liệu khách → tạo booking.
   - Nếu chuyển khoản, hệ thống tạo VietQR và poll; nếu tiền mặt, staff nhận tiền và nhấn “Xác nhận tạo vé” → booking confirmed → in vé.
3. **Quản trị nội dung** (admin):
   - Admin đăng nhập `/admin/workspace`, sử dụng các tab CRUD để cập nhật phim/suất/banners/phòng.
   - Thao tác xóa đều có confirm dialog; khi dữ liệu đang được dùng, hệ thống báo rõ nguyên nhân.
4. **Báo cáo doanh thu**:
   - Module `cinema-backend-revenue` đọc dữ liệu booking và hiển thị biểu đồ, đồng thời có CRUD phụ để tổng hợp doanh thu theo phim, phòng, ngày.

## 5. Hướng dẫn chạy & triển khai
1. **Chuẩn bị**: cài Java 21, Maven 3.9+, SQL Server. Tạo DB và chạy script `docs/insert sql.txt` (bao gồm DDL/DML) + các file mẫu nếu cần thêm dữ liệu.
2. **Cấu hình**: chỉnh `application.properties` cho URL, user, password của SQL Server và thông số PayOS/SMTP. Đảm bảo thư mục `uploads` có quyền ghi.
3. **Chạy**:
   ```bash
   mvn -q test        # kiểm tra nhanh
   mvn spring-boot:run
   ```
   Ứng dụng lắng nghe cổng 8080. Truy cập `/` cho client, `/admin/workspace` cho admin, `/staff/dashboard` cho staff.
4. **Build & deploy**:
   ```bash
   mvn -q -DskipTests package
   java -jar target/cinema-backend-*.jar
   ```
   Sử dụng profile môi trường qua `--spring.profiles.active=prod`.

## 6. Quy ước phát triển
- Tất cả file mã nguồn lưu UTF-8 (không BOM). Các chuỗi hiển thị tiếng Việt phải qua i18n hoặc literal rõ ràng, tránh ký tự lỗi.
- Khi sửa code: `git status -sb` để theo dõi thay đổi; luôn chạy `mvn -q test`.
- JavaScript: ưu tiên `fetch` + async/await, đóng gói logic vào module tương ứng (`admin-*.js`, `staff-*.js`, `seat-selection.js`, `auth.js`).
- CSS: bố trí trong file tương ứng (không nhúng inline trừ layout đặc biệt). Assets font (`Roboto`) lưu trong `src/main/resources/fonts`.
- Không thay đổi dữ liệu seed trong `docs` nếu không cập nhật tài liệu này.

## 7. Công việc tương lai gợi ý
1. Chuẩn hóa bài toán giữ ghế & đồng bộ PayOS trên cả trang client và staff (giảm yêu cầu poll).
2. Bổ sung unit test/service test cho `BookingService`, `SeatReservationService` và layer PayOS.
3. Xây dựng trang báo cáo doanh thu liên hợp (backend chính + module revenue) để staff không phải chuyển hệ thống.
4. Tối ưu hoá các truy vấn showtime/movies bằng view hoặc stored procedure trong SQL Server khi số lượng dữ liệu lớn.

---

Tài liệu này giúp mọi thành viên nhanh chóng nắm bắt phạm vi chức năng, công nghệ và quy trình làm việc hiện tại của Cinema HUB. Hãy cập nhật file nếu có module mới hoặc thay đổi kiến trúc đáng kể.

**IMPORTANT* dự án chạy phải trên 1 server, vd thuê cloudflare dùng localhost thay server mình mới chạy được thanh toán payos