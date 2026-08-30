## Summary

<!-- What does this PR do and why? Link any related issue: Closes #123 -->

Add **Borrow Renewal** feature that allows librarians to extend a borrowing period by N days, with a configurable maximum renewal count (default: 2 times). The new due date is calculated from today (if overdue) or from the current due date. The `renew_count` field is persisted directly in the database and displayed in the UI.

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
- **`DatabaseInitializer.java`** — Added `renew_count INTEGER DEFAULT 0` column to the `borrows` DDL; added automatic migration (`ALTER TABLE borrows ADD COLUMN renew_count ...`) on application startup.
- **`BorrowDAO.java`** — Updated `insert` and `mapRow` to handle `renew_count`; added `renewBorrow(borrowId, newDueDate, renewCount, status)` method.
- **`BorrowService.java`** — Added constants `MAX_RENEW_COUNT = 2` and `DEFAULT_RENEW_DAYS = 7`; added two `renewBorrow` overloads with full validation (already-returned check, renewal limit check, new due date calculation).
- **`BorrowPanel.java`** — Added **"Gia Hạn"** column showing `x/2` in the table; added **"⏳ Gia Hạn"** button on the toolbar; built a renewal dialog with a day spinner (1–60) and quick-select buttons (+7 / +14).
- **`BorrowRenewTest.java`** *(new)* — Unit tests covering: successful renewal, count increment, exceeding limit, already-returned borrow, and overdue borrow.
- **`data/library.db`**, **`demo/data/library.db`** — Schema updated directly: `renew_count` column added to the `borrows` table.

## Screenshots / recordings

<!-- For UI changes, drop before/after screenshots or a short clip. Delete if N/A. -->

> Select a borrow record → click **⏳ Gia Hạn** → choose number of days → Confirm.
> The "Gia Hạn" column updates immediately after a successful renewal (e.g. `1/2`).

## How to test

<!-- Steps for a reviewer to verify locally. Note any env vars (e.g. VITE_API_BASE) or seed data needed. -->

1. Run the application: `java -jar demo/target/QLThuVienNguyenHue.jar`
2. Log in as a **librarian** or **admin** account.
3. Navigate to the **Mượn / Trả Sách** tab — verify the **"Gia Hạn"** column shows `0/2` for existing records.
4. Select a record with status **Đang mượn** or **Quá hạn** → click **⏳ Gia Hạn**.
5. In the dialog, choose the number of days (default 7) → click **Xác Nhận Gia Hạn**.
6. Verify the "Gia Hạn" column increments to `1/2` and the "Hạn Trả" column shows the new due date.
7. Renew the same record again → column should show `2/2`.
8. Attempt a third renewal → app should show an error: renewal limit reached.
9. Attempt to renew an already-returned record → app should reject with an error.
10. Run unit tests: `mvn test -Dtest=BorrowRenewTest` — all 5 tests must PASS.