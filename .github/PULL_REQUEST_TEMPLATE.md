## Summary

<!-- What does this PR do and why? Link any related issue: Closes #123 -->

Implemented key library management enhancements:
1. **Borrow Renewal**: Allows librarians to extend a borrowing period by N days with a configurable maximum renewal count (default: 2 times). The `renew_count` field is persisted directly in the database and tracked in the UI.
2. **Max Concurrent Borrow Limit**: Restricts each reader to borrowing a maximum of N books simultaneously (default: 5 books). Validates active borrow count prior to creating new borrow records.
3. **Advanced Multi-Criteria Search & Filtering**:
   - **Books**: Filter by keyword, category, publication year, and availability status.
   - **Borrows**: Filter by keyword, borrow date range (`dd/MM/yyyy`), and status (Borrowing, Overdue, Returned).

## Type of change

- [x] Feature
- [ ] Bug fix
- [ ] Refactor / cleanup
- [x] Styling / UI
- [ ] Documentation
- [ ] Chore / tooling

## Changes

<!-- Bullet the notable changes. Mention any new modules, endpoints, or domain types. -->

- **`Borrow.java`** — Added `renewCount` field, full constructor, getter/setter, and `canRenew(maxRenewCount)` helper method.
- **`DatabaseInitializer.java`** — Added `renew_count INTEGER DEFAULT 0` column to `borrows` DDL; added automatic migration (`ALTER TABLE borrows ADD COLUMN renew_count ...`) on application startup.
- **`BorrowDAO.java`** — Added support for `renew_count`, `renewBorrow(...)`, `countActiveBorrowsByReader(...)`, and `advancedSearch(...)` with SQL date range comparison using `SUBSTR`.
- **`BorrowService.java`** — Added constants `MAX_RENEW_COUNT = 2` and `MAX_CONCURRENT_BORROWS = 5`; implemented validation logic in `borrowBook(...)` and `advancedSearchBorrows(...)`.
- **`BorrowPanel.java`** — Added **"Gia Hạn"** column showing `x/2`, **"⏳ Gia Hạn"** button with renewal modal dialog, and **"🔍 Lọc Nâng Cao"** button with date-range and status filter dialog.
- **`BookDAO.java` & `BookService.java`** — Added `advancedSearch(...)` supporting combined keyword, category, publication year, and availability status filters.
- **`BookPanel.java`** — Added **"🔍 Lọc Nâng Cao"** button and modal dialog for multi-criteria book filtering.
- **`BorrowRenewTest.java`** — Unit tests covering borrow renewal logic, renewal limit enforcement, and return status checks.
- **`data/library.db`**, **`demo/data/library.db`** — Database schema updated directly with `renew_count` column added to `borrows` table.

## Screenshots / recordings

<!-- For UI changes, drop before/after screenshots or a short clip. Delete if N/A. -->
![img.png](img.png)
![img_1.png](img_1.png)
> - Select a borrow record → click **⏳ Gia Hạn** to extend due date (updates column to `1/2`).
> - Click **🔍 Lọc Nâng Cao** on Books or Borrows panel to apply multi-criteria search filters.
> - Attempting to borrow > 5 books for a single reader triggers concurrent limit validation error.

## How to test

<!-- Steps for a reviewer to verify locally. Note any env vars (e.g. VITE_API_BASE) or seed data needed. -->

1. Run the application: `java -jar demo/target/QLThuVienNguyenHue.jar`
2. Log in as a **librarian** or **admin** account.
3. **Test Borrow Renewal**:
   - Navigate to **Mượn / Trả Sách** tab → select an active record → click **⏳ Gia Hạn**.
   - Verify renewal increments count (`1/2`) and extends due date. Exceeding 2 renewals displays an error limit reached.
4. **Test Max Concurrent Borrow Limit**:
   - Create new borrows for a reader who currently has 5 active borrows.
   - Verify system prevents borrowing a 6th book and displays a warning that max borrow limit (5 books) is reached.
5. **Test Advanced Search (Books)**:
   - Go to **Quản Lý Sách** → click **🔍 Lọc Nâng Cao**.
   - Filter by Category, Publish Year, and Availability status → click **Lọc Kết Quả** to verify correct results.
6. **Test Advanced Search (Borrows)**:
   - Go to **Mượn / Trả Sách** → click **🔍 Lọc Nâng Cao**.
   - Filter by Date Range (`dd/MM/yyyy`) and Status (Đang mượn / Quá hạn / Đã trả) → verify filtered records.
7. Run unit tests: `mvn test -Dtest=BorrowRenewTest` — all 5 tests must PASS.