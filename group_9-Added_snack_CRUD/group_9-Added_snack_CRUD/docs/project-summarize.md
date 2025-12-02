# Cinema HUB – Tóm tắt dự án

## 1. Tổng quan
Cinema HUB là hệ thống quản lý rạp chiếu phim (Spring Boot + Thymeleaf + JS) hỗ trợ đầy đủ các nghiệp vụ nội dung: phim, suất chiếu, banner trang chủ và phòng chiếu. Frontend thuần HTML/CSS/JS, no framework, giao tiếp REST nội bộ. Các trang chính (Trang chủ, Lịch chiếu, Khuyến mãi, Giá vé, Giới thiệu) dùng dữ liệu runtime từ backend. Admin dashboard (mục “Khu vực quản trị”) là trung tâm CRUD.

## 2. Chức năng chính
### 2.1 CRUD phim (“CRUD phim” tab)
- Thêm/sửa phim với thông tin chi tiết (tên, mô tả, thời lượng ≤ 900 phút, giới hạn tuổi, trailer, poster, embed YouTube).
- Gán genre (checkbox dropdown sắp xếp A‑Z), đạo diễn, diễn viên.
- Tự cân nhắc trạng thái dựa trên ngày khởi chiếu/ngừng chiếu: tự chuyển danh sách “Đang chiếu / Sắp chiếu / Ngừng chiếu” cho trang chủ.
- Upload poster lưu dưới `/uploads/movies/…`.
- Toast “Lưu thành công!” sau khi submit, tự cuộn lên đầu trang.

### 2.2 CRUD suất chiếu (“CRUD suất chiếu” tab)
- Tạo suất chiếu với phim + phòng chiếu + giờ bắt đầu.
- Tùy chọn “Tạo cho”: chỉ một ngày, cả tuần (7 ngày liên tiếp), ngày trong tuần (T2‑T6), cuối tuần (T7‑CN) hoặc tùy chỉnh bằng nút T2…CN.
- Có thể lặp đến một ngày cụ thể (`repeatUntil`), hệ thống tính danh sách ngày và chặn trùng giờ (thông báo tiếng Việt).
- Tự tính giá vé cơ bản dựa trên block thời gian (sáng/chiều/tối) & ngày (weekday/weekend) rồi nhân hệ số ghế khi clone seat snapshot.
- Tự tạo suất chiếu hàng loạt, trả về danh sách kết quả. Bảng filter theo phim, phòng, trạng thái, từ/đến ngày, có phân trang.

### 2.3 CRUD phòng chiếu (“CRUD phòng chiếu” tab)
- Tạo/sửa/xóa phòng chiếu với tên + số hàng ghế + số ghế/hàng (giới hạn 1‑999).
- Danh sách hiển thị tổng ghế, trạng thái (ON/OFF), filter theo tên & trạng thái.
- Xóa có dialog xác nhận; nếu phòng đang được dùng (ràng buộc FK) sẽ báo lỗi “Không thể xóa phòng chiếu đang được sử dụng.”
- Dữ liệu phòng chiếu được dùng cho dropdown của suất chiếu.

### 2.4 CRUD banner (“CRUD banner” tab)
- Quản lý banner trang chủ với ảnh upload, liên kết (Movie hoặc URL), thứ tự hiển thị, ngày hiệu lực.
- Khi chọn liên kết phim, preview card hiển thị poster + trạng thái (đã bỏ hiển thị ID).
- Form validate thứ tự ≥ 1 và <= 100, ngày bắt đầu/kết thúc; các banner quá hạn tự chuyển trạng thái OFF.
- Bảng danh sách hiển thị ON/OFF theo ngày, có nút sửa/xóa.

### 2.5 Giá vé & giới thiệu (trang public)
- `ticket-prices.html`: Hardcode bảng giá, văn bản quy định & ảnh. Nav highlight mục “Giá vé”.
- `about.html`: Tab “Giới thiệu” hiển thị mô tả tổ chức, thông tin liên hệ, ảnh, sơ đồ tổ chức; tabs “Dịch vụ/Phòng chiếu – Nhà hát” để trống cho tương lai.

### 2.6 Xử lý auth & UI chung
- Header/ Footer chung cho tất cả trang (component `fragments/header/footer`).
- Auth gồm đăng ký, đăng nhập, quên mật khẩu; số điện thoại bắt buộc unique, tối đa 11 ký tự và kiểm tra numeric.
- Các toast / confirm dialog dùng `admin-confirm.js`, `showSuccessToast` (định nghĩa ở `admin-movies.js`).

### 2.7 CRUD snack & quầy bắp nước
- Tab CRUD Snack trên admin-dashboard quản lý bắp rang, nước uống, snack (API `/api/admin/snacks`).
- Form: tên sản phẩm, slug (tự động), mô tả, category (POPCORN/DRINK/SNACK), giá, khẩu phần, calories, tồn kho, URL ảnh, thứ tự hiển thị, trạng thái.
- Upload ảnh snack qua `/api/admin/uploads/snack` (lưu trong `/uploads/snacks`) làm sẵn cho shop feature sau này.

## 3. Workflow tổng quan
1. **Khởi động**: `mvn spring-boot:run`. Database SQL Server (schema script `docs/sql.txt`, seed `docs/insert sql.txt`). Nhớ ALTER `Auditoriums` thêm `NumberOfRows/Columns`.
2. **Frontend**: truy cập `/` để xem trang khách. `/admin/dashboard` cho admin.
3. **Quy trình tạo nội dung**:
   - Tạo phòng chiếu → suất chiếu (tự clone seat snapshot) → banner → phim → danh mục snack.
   - Tính năng lặp suất chiếu tiết kiệm thao tác mass scheduling.
4. **Upload file**: Poster & banner lưu trong `/uploads/...` (đảm bảo cấu hình quyền ghi khi deploy).
5. **Quản lý dữ liệu**: Tất cả CRUD dùng REST `/api/admin/**`. JS fetch data, hiển thị, xử lý validation/client toast. Các bản ghi xóa yêu cầu confirm.

## 4. Điểm kỹ thuật nổi bật
- **Spring Boot + REST + Specification** cho filter/lazy paging.
- **Thymeleaf** render SSR + JS thuần, không framework nặng.
- **Seat snapshot**: tạo bản sao ghế theo suất chiếu với giá ghế = basePrice * multiplier.
- **Tự động trạng thái**: phim và banner cập nhật ON/OFF dựa vào ngày; suất chiếu check trùng giờ, lặp theo preset.
- **I18N**: thông báo lỗi/confirm tiếng Việt.

## 5. Hướng dẫn bàn giao cho team
1. Clone repo, cài Java 17+, SQL Server.
2. Chạy scripts trong `docs/sql.txt` rồi `docs/insert sql.txt`. Nếu đã có DB, nhớ ALTER bảng `Auditoriums`.
3. `mvn -q -DskipTests package` để chắc chắn build ok.
4. Deploy theo profile mặc định (`application.properties`).
5. Admin dashboard là công cụ duy nhất để team nhập dữ liệu demo/production.

Với file này, mọi người sẽ nắm được các module chính và cách vận hành khi nhận dự án. Chúc team merge suôn sẻ! :)
