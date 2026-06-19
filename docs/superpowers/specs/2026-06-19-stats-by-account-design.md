# Spec: "Theo tài khoản" trong màn Thống kê (Stats by account)

Date: 2026-06-19
Status: Approved

## Vấn đề
App cho tạo nhiều account (mặc định 🏦 Ngân hàng, 💵 Tiền mặt + custom), mỗi transaction gắn `accountId`,
nhưng **không có chỗ nào so sánh thu/chi theo từng account**. Stats hiện chỉ group theo category + theo kỳ,
gộp toàn bộ account.

## Scope (đã chốt)
- Mở rộng **màn Thống kê (Stats) hiện tại** — KHÔNG làm khái niệm số dư (balance), KHÔNG đổi DB schema, KHÔNG đụng backend.
- Cung cấp cả **so sánh** (tất cả account) lẫn **filter** (1 account) trong cùng màn Stats. Layout = phương án A.

## UX / Behavior (phương án A — chips lọc + thẻ so sánh)
- **Chip row** ngay dưới month selector: `[Tất cả] [🏦 Ngân hàng] [💵 Tiền mặt] [💳 …]` — cuộn ngang, liệt kê **mọi** account; "Tất cả" mặc định, account đang chọn được highlight.
- **Mode "Tất cả":** hiện thẻ **"💜 Theo tài khoản"**:
  - Mỗi account có phát sinh trong kỳ = 1 dòng: emoji + tên, **bar thu (xanh)** + **bar chi (đỏ)** scale theo giá trị lớn nhất giữa các account (để so sánh trực quan), **net** (xanh/đỏ) góc phải.
  - Chỉ liệt kê account **có phát sinh** trong kỳ; sort theo **tổng phát sinh (income+expense) giảm dần**.
  - Bấm 1 dòng = chọn account đó (chuyển sang mode filter).
- **Mode filter (1 account):** thẻ "Theo tài khoản" ẩn; **summary card + pie (danh mục) + trend 6 tháng + top entries** đều lọc riêng theo `accountId`. Bấm "Tất cả" để quay lại.

## Dữ liệu
- **Transfer bị loại** khỏi mọi tính toán thu/chi (đồng nhất với pie hiện tại). Per-account chỉ đếm INCOME/EXPENSE có `accountId` = account đó.
- Kỳ thời gian: dùng lại month đang chọn (summary/pie/breakdown) + 6 tháng (trend).

## Thay đổi code
- **`StatsViewModel.kt`**:
  - Thêm `accountFilter: MutableStateFlow<Long?>` (null = Tất cả) + `selectAccount(id: Long?)`.
  - Observe danh sách accounts (đã có `accountRepo.observeAll()` pattern ở các VM khác).
  - Tính `accountBreakdown: List<AccountStat>` (id, name, icon, color, incomeMinor, expenseMinor, netMinor) — transfer excluded, chỉ account có phát sinh, sort tổng phát sinh desc.
  - Khi `accountFilter != null`: filter transactions feeding summary/pie/trend/top theo `accountId == filter`.
  - Expose `accounts` (cho chip row) + `accountBreakdown` (cho thẻ) + `accountFilter` trong UI state.
- **`StatsScreen.kt`**:
  - Composable `AccountChipRow` (Tất cả + mỗi account 1 chip, highlight selected).
  - Composable `AccountBreakdownCard` (chỉ hiện khi filter==null) — rows với dual bar + net, tappable.
  - Wire tap chip/row → `vm.selectAccount(...)`.
- **Không** đổi `AccountEntity` / Room / DAO / migration. **Không** đụng backend.

## Edge cases
- Account không phát sinh trong kỳ → không vào thẻ so sánh; vẫn có chip, chọn xong hiện empty state "Kỳ này chưa có giao dịch".
- Mọi account đều rỗng kỳ này → thẻ "Theo tài khoản" hiện empty state nhẹ.
- 1 account → hiển thị 1 dòng bình thường.

## Out of scope
- Số dư (balance) từng account, initial balance.
- Filter theo account ở Home / Calendar.
- Backend / sync thay đổi.

## Verify
- Build `:app:assembleDebug` xanh; manual trên emulator: tạo vài transaction ở 2+ account → mở Stats → thấy thẻ so sánh; bấm 1 account → pie/trend lọc đúng; bấm "Tất cả" quay lại. (Không viết unit test — theo preference.)
