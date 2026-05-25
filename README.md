# 📚 StudyPlan — Ứng dụng Quản lý Học tập Thông minh

> **Platform:** Android (Java)  
> **Database:** SQLite (local) + Firebase Firestore (cloud sync)  
> **Auth:** Email/Password + Google Sign-In (Firebase)

---

## 📁 Cấu trúc file Java

Tất cả file nằm trong package:  
`app/src/main/java/com/studyplan/app/`

---

### 🏠 Activity — Màn hình chính

| File | Chức năng |
|------|-----------|
| **`MainActivity.java`** | Activity chính chứa `BottomNavigationView` và `FrameLayout` để hiển thị các Fragment. Quản lý việc chuyển tab giữa Trang chủ, Lịch học, Bài tập, Môn học, Tài khoản. |
| **`LoginActivity.java`** | Màn hình đăng nhập / đăng ký. Xử lý 3 luồng xác thực: **(1)** Email + mật khẩu truyền thống, **(2)** Google Sign-In qua Firebase Auth, **(3)** Google fallback (nhập thủ công khi Firebase chưa liên kết). Quản lý validate form, toggle hiển thị mật khẩu, chuyển tab Đăng nhập ↔ Đăng ký, và lưu session vào `SharedPreferences`. |

---

### 📄 Fragment — Các màn hình con

| File | Tab | Chức năng |
|------|-----|-----------|
| **`HomeFragment.java`** | 🏠 Trang chủ | Dashboard tổng quan: hiển thị lời chào + avatar theo tên user, ngày hiện tại (tiếng Việt), thống kê bài tập (tổng/hoàn thành/đang làm/chưa làm), danh sách lịch học hôm nay và deadline sắp tới. Các card lịch/deadline được build bằng code Java (programmatic layout). Có nút Quick Action chuyển nhanh đến các tab khác. |
| **`SubjectFragment.java`** | 📗 Môn học | Quản lý danh sách môn học: thêm/sửa/xóa môn qua dialog. Hỗ trợ tìm kiếm theo tên/giảng viên. Hiển thị thống kê số môn và tổng tín chỉ. Chip chọn loại (Thực hành / Lý thuyết) với validate không cho dialog đóng khi thiếu dữ liệu. |
| **`ScheduleFragment.java`** | 📅 Lịch học | Quản lý thời khóa biểu: thêm/sửa/xóa lịch học qua dialog dùng chung (`bindAndShowScheduleDialog`). Lọc theo ngày (Tất cả, Hôm nay, Tuần, T2→T6). Tìm kiếm theo tên môn. Validate giờ kết thúc phải sau giờ bắt đầu. Hỗ trợ chọn học kỳ (HK1/HK2/Hè). |
| **`AssignmentFragment.java`** | 📝 Bài tập | Quản lý bài tập: thêm/sửa/xóa bài tập, chọn môn học từ Spinner, chọn deadline bằng DatePicker + TimePicker, chọn mức ưu tiên (Cao/TB/Thấp) và trạng thái (Chưa làm/Đang làm/Hoàn thành/Trễ). Tự động đánh dấu bài trễ hạn mỗi khi vào tab (`checkAndMarkLateAssignments`). Lọc theo trạng thái + tìm kiếm kết hợp. |
| **`AccountFragment.java`** | 👤 Tài khoản | Hiển thị thông tin profile (tên, email, phương thức đăng nhập), thống kê tổng quan (số môn/lịch/bài tập). Chỉnh sửa profile qua dialog với live preview avatar. Menu điều hướng nhanh, thông báo bài trễ, thông tin ứng dụng. Đăng xuất với xóa session Firebase + Google + SharedPreferences. |

---

### 🔌 Adapter — Kết nối dữ liệu với RecyclerView

| File | Chức năng |
|------|-----------|
| **`SubjectAdapter.java`** | Hiển thị danh sách môn học trong `RecyclerView`. Mỗi item gồm: tên môn, giảng viên, tín chỉ, học kỳ, chip loại môn (Thực hành/Lý thuyết), thanh màu theo `colorTag`, nút sửa/xóa. Hỗ trợ `filter()` tìm kiếm và `setData()` cập nhật toàn bộ. |
| **`ScheduleAdapter.java`** | Hiển thị timeline lịch học dạng thời gian biểu. Mỗi item gồm: giờ bắt đầu/kết thúc, tên môn, phòng học + giảng viên, badge trạng thái (Đã xong/Đang học/Sắp tới). Màu accent theo `colorTag` (blue/green/orange/purple). Click → sửa, long press → xóa. |
| **`AssignmentAdapter.java`** | Hiển thị danh sách bài tập. Mỗi item gồm: checkbox tick hoàn thành, tên bài, tên môn, deadline, chấm ưu tiên (đỏ/cam/xanh), badge trạng thái. Tick checkbox → cập nhật SQLite ngay lập tức. Bài đã hoàn thành hiển thị gạch ngang + đổi màu xám. |

---

### 📦 Model — Lớp dữ liệu

| File | Chức năng |
|------|-----------|
| **`Subject.java`** | Model môn học. Các trường: `id`, `name` (tên môn), `teacher` (giảng viên), `credits` (tín chỉ), `semester` (học kỳ), `type` (Thực hành/Lý thuyết), `colorTag` (mã màu: green/blue/orange/purple/red). |
| **`ScheduleItem.java`** | Model lịch học. Các trường: `id`, `subjectName`, `startTime`, `endTime`, `room` (phòng), `teacher`, `status` (done/in_progress/upcoming), `colorTag`, `dayOfWeek` (T2→CN). |
| **`Assignment.java`** | Model bài tập. Các trường: `id`, `title`, `subjectName`, `deadline`, `priority` (Cao/Trung bình/Thấp), `status` (not_started/in_progress/done/late), `isDone` (boolean). |

---

### 💾 DAO — Truy xuất cơ sở dữ liệu (SQLite)

| File | Chức năng |
|------|-----------|
| **`DatabaseHelper.java`** | Tạo và quản lý database SQLite (`studyplan.db`). Định nghĩa schema cho 4 bảng: `users`, `subjects`, `schedules`, `assignments`. Xử lý `onCreate()` và `onUpgrade()` khi thay đổi version. |
| **`SubjectDAO.java`** | CRUD môn học: `insert`, `update`, `delete`, `getAll`, `getById`. Hỗ trợ `search(query)` tìm theo tên/giảng viên, `getCount()` đếm tổng, `getTotalCredits()` tính tổng tín chỉ. |
| **`ScheduleDAO.java`** | CRUD lịch học: `insert`, `update`, `delete`, `getAll`. Hỗ trợ `getTodaySchedule()` lấy lịch hôm nay theo thứ, `searchWithFilter(keyword, dayFilter)` kết hợp tìm kiếm + lọc ngày, `getTodayDayLabel()` trả label thứ hiện tại (T2→CN). |
| **`AssignmentDAO.java`** | CRUD bài tập: `insert`, `update`, `delete`, `getAll`. Hỗ trợ `updateDoneStatus(id, isDone, status)` tick hoàn thành, `getCountByStatus(status)` đếm theo trạng thái, `getUpcomingDeadlines()` lấy deadline sắp tới, `searchWithFilter(keyword, statusFilter)` tìm + lọc kết hợp. |
| **`UserDAO.java`** | Quản lý tài khoản user trong SQLite: `register`, `login` (kiểm tra email + password hash SHA-256), `getUserByEmail`, `updateProfile`. Hỗ trợ lưu thông tin Google Sign-In (`google_uid`, `photo_url`), kiểm tra `isEmailExists`. |

---

### ☁️ Cloud Sync — Đồng bộ Firebase

| File | Chức năng |
|------|-----------|
| **`FirestoreHelper.java`** | Đồng bộ dữ liệu giữa SQLite local và Firebase Firestore. Hỗ trợ upload/download môn học, lịch học, bài tập theo `userId`. Xử lý conflict resolution và retry logic khi mất kết nối. |

---

## 🎨 Hệ thống màu theo module

| Module | Màu chủ đạo | Mã |
|--------|-------------|-----|
| 📝 Bài tập (Assignment) | 🟠 Cam | `orange_primary` |
| 📅 Lịch học (Schedule) | 🟣 Tím | `purple_primary` |
| 📗 Môn học (Subject) | 🟢 Xanh lá | `green_primary` |
| 👤 Tài khoản (Account) | 🔵 Xanh dương | `blue_primary` |

---

## 🔐 Xác thực & Bảo mật

- **Mật khẩu**: Hash SHA-256 trước khi lưu vào SQLite
- **Session**: Lưu trạng thái đăng nhập trong `SharedPreferences` (`StudyPlanPrefs`)
- **Google Auth**: Firebase Auth → Google Sign-In Client, với fallback nhập thủ công
- **Logout**: Xóa session Firebase + Google + SharedPreferences đồng thời

---

## 📐 Kiến trúc tổng quan

```
LoginActivity
    └── Xác thực → MainActivity
                        ├── HomeFragment (Dashboard)
                        ├── SubjectFragment + SubjectAdapter
                        ├── ScheduleFragment + ScheduleAdapter
                        ├── AssignmentFragment + AssignmentAdapter
                        └── AccountFragment
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
              SQLite (DAO)      Firebase Firestore
              ├── UserDAO         (FirestoreHelper)
              ├── SubjectDAO
              ├── ScheduleDAO
              └── AssignmentDAO
```

---

*StudyPlan v1.0 — Ứng dụng quản lý học tập thông minh cho sinh viên* 🎓
