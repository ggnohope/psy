import SwiftUI
import PhotosUI
import PsyCore

/// Add / edit a transaction. Presented as a sheet from Home. Ports AddEditTransactionScreen.kt.
/// HostGuardIQ re-skin: Lucide chrome icons, PsyFont typography, psyColors tokens. VM untouched.
struct AddEditView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.psyColors) private var psyColors
    @State private var vm: AddEditViewModel
    @State private var photoItem: PhotosPickerItem?
    @State private var showDatePicker = false
    @State private var showTimePicker = false

    init(container: AppContainer, txId: Int64) {
        _vm = State(initialValue: AddEditViewModel(container: container, txId: txId))
    }

    // Segmented control order: income, expense, transfer.
    private let segmentTypes: [TxType] = [.income, .expense, .transfer]
    private let segmentLabels = ["Thu nhập", "Chi tiêu", "Chuyển khoản"]

    var body: some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                VStack(spacing: 18) {
                    SegmentedControl(
                        options: segmentLabels,
                        selectedIndex: segmentTypes.firstIndex(of: vm.type) ?? 1
                    ) { idx in vm.onTypeChange(segmentTypes[idx]) }

                    amountSection

                    if vm.type != .transfer && !vm.groups.isEmpty {
                        categorySection
                    }

                    accountSections
                    dateTimeSection
                    noteSection
                    photoSection
                    saveButton

                    Spacer(minLength: 16)
                }
                .padding(.horizontal, 22)
                .padding(.top, 8)
                .padding(.bottom, 16)
            }
        }
        .background(psyColors.bg.ignoresSafeArea())
        .onChange(of: photoItem) { _, newItem in
            guard let newItem else { return }
            Task {
                if let data = try? await newItem.loadTransferable(type: Data.self) {
                    vm.attachPhoto(data: data)
                } else {
                    vm.photoErrorMessage = "Không thể đọc ảnh đã chọn."
                }
            }
        }
        .sheet(isPresented: $showDatePicker) {
            datePickerSheet(components: .date, title: "Chọn ngày")
        }
        .sheet(isPresented: $showTimePicker) {
            datePickerSheet(components: .hourAndMinute, title: "Chọn giờ")
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 12) {
            Button { dismiss() } label: {
                LucideIcon(name: "arrow-left", size: 22, tint: psyColors.text)
                    .frame(width: 40, height: 40)
                    .contentShape(Rectangle())
            }
            Text(vm.isEdit ? "Sửa giao dịch" : "Thêm giao dịch")
                .font(PsyFont.titleLarge)
                .foregroundStyle(psyColors.text)
            Spacer()
            if vm.isEdit {
                Button { vm.delete { dismiss() } } label: {
                    LucideIcon(name: "trash-2", size: 22, tint: psyColors.red)
                        .frame(width: 40, height: 40)
                        .contentShape(Rectangle())
                }
            }
        }
        .padding(.horizontal, 22)
        .padding(.vertical, 10)
        .background(psyColors.bg)
    }

    // MARK: - Amount

    private var amountSection: some View {
        VStack(spacing: 6) {
            MoneyText(amountMinor: displayAmountMinor, currency: vm.currency)
                .font(PsyFont.display(42))
                .foregroundStyle(psyColors.text)
            Text("Nhập số tiền")
                .font(PsyFont.mono(12))
                .foregroundStyle(psyColors.text3)

            TextField("0", text: Binding(
                get: { vm.amountText },
                set: { vm.amountText = $0.filter(\.isNumber) }
            ))
            .keyboardType(.numberPad)
            .multilineTextAlignment(.center)
            .font(PsyFont.mono(18))
            .foregroundStyle(psyColors.text)
            .padding(.vertical, 12)
            .padding(.horizontal, 14)
            .background(psyColors.surface)
            .overlay(
                RoundedRectangle(cornerRadius: PsyRadius.chip)
                    .stroke(amountInvalid ? psyColors.red : psyColors.hair, lineWidth: 1.5)
            )
            .clipShape(RoundedRectangle(cornerRadius: PsyRadius.chip))
            .padding(.top, 6)

            if amountInvalid {
                Text("Số tiền phải lớn hơn 0")
                    .font(PsyFont.labelSmall)
                    .foregroundStyle(psyColors.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var displayAmountMinor: Int64 {
        AddEditLogic.amountMinor(typed: vm.amountText, fractionDigits: vm.currency.fractionDigits)
    }

    /// True when the user typed something but it does not resolve to a positive amount.
    private var amountInvalid: Bool {
        !vm.amountText.isEmpty && displayAmountMinor <= 0
    }

    // MARK: - Category picker

    private let parentColumns = Array(repeating: GridItem(.flexible(), spacing: 7), count: 4)

    private var categorySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Header row: label + live breadcrumb pill (Parent › Sub).
            HStack {
                EyebrowLabel(text: "Danh mục")
                Spacer()
                if let g = vm.selectedGroup {
                    HStack(spacing: 6) {
                        LucideIcon(name: vm.selectedLeaf?.icon ?? g.icon, size: 14, tint: psyColors.blue)
                        Text(vm.selectedLeaf.map { "\(g.name) › \($0.name)" } ?? g.name)
                            .font(PsyFont.bodyMedium.weight(.semibold))
                            .foregroundStyle(psyColors.blue)
                            .lineLimit(1)
                    }
                    .padding(.horizontal, 10).padding(.vertical, 5)
                    .background(psyColors.blueSoft)
                    .clipShape(Capsule())
                }
            }

            // Picker card: parent grid + divider + subcategory pills.
            VStack(alignment: .leading, spacing: 12) {
                LazyVGrid(columns: parentColumns, spacing: 7) {
                    ForEach(vm.groups) { group in
                        parentTile(group)
                    }
                }
                Rectangle().fill(psyColors.hair).frame(height: 1)
                WrapLayout(spacing: 8) {
                    ForEach(vm.leaves) { leaf in
                        subPill(leaf)
                    }
                }
            }
            .padding(12)
            .background(psyColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(psyColors.hair, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func parentTile(_ group: CategoryGroup) -> some View {
        let isSelected = group.id == vm.selectedGroupId
        return VStack(spacing: 5) {
            LucideIcon(name: group.icon, size: 21, tint: isSelected ? psyColors.blue : psyColors.text2)
            Text(group.name)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(isSelected ? psyColors.blue : psyColors.text2)
                .lineLimit(1).minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(isSelected ? psyColors.blueSoft : psyColors.sunken)
        .overlay(RoundedRectangle(cornerRadius: 11).stroke(isSelected ? psyColors.blue : .clear, lineWidth: 1.5))
        .clipShape(RoundedRectangle(cornerRadius: 11))
        .contentShape(Rectangle())
        .onTapGesture { vm.selectGroup(group.id) }
    }

    private func subPill(_ leaf: PsyCore.Category) -> some View {
        let isSelected = leaf.id == vm.selectedCategoryId
        return HStack(spacing: 6) {
            LucideIcon(name: leaf.icon, size: 17, tint: isSelected ? .white : psyColors.text2)
            Text(leaf.name)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(isSelected ? Color.white : psyColors.text2)
                .lineLimit(1)
        }
        .padding(.horizontal, 12).padding(.vertical, 9)
        .background(isSelected ? psyColors.blue : psyColors.sunken)
        .clipShape(Capsule())
        .contentShape(Rectangle())
        .onTapGesture { vm.selectCategory(leaf.id) }
    }

    // MARK: - Account picker

    @ViewBuilder
    private var accountSections: some View {
        if !vm.accounts.isEmpty {
            if vm.type == .transfer {
                accountSection(label: "Từ tài khoản", selectedId: vm.selectedAccountId) { vm.selectedAccountId = $0 }
                accountSection(label: "Đến tài khoản", selectedId: vm.toAccountId) { vm.toAccountId = $0 }
                if let from = vm.selectedAccountId, let to = vm.toAccountId, from == to {
                    Text("Hai tài khoản phải khác nhau")
                        .font(PsyFont.labelSmall)
                        .foregroundStyle(psyColors.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else {
                accountSection(label: "Tài khoản", selectedId: vm.selectedAccountId) { vm.selectedAccountId = $0 }
            }
        }
    }

    private func accountSection(label: String, selectedId: Int64?, onSelect: @escaping (Int64) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            EyebrowLabel(text: label)
            WrapLayout(spacing: 8) {
                ForEach(vm.accounts) { account in
                    accountChip(account, isSelected: account.id == selectedId) { onSelect(account.id) }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func accountChip(_ account: Account, isSelected: Bool, onSelect: @escaping () -> Void) -> some View {
        HStack(spacing: 6) {
            LucideIcon(name: account.icon, size: 17, tint: isSelected ? psyColors.blue : psyColors.text2)
            Text(account.name)
                .font(PsyFont.bodyMedium.weight(isSelected ? .semibold : .regular))
                .foregroundStyle(isSelected ? psyColors.blue : psyColors.text2)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .background(isSelected ? psyColors.blueSoft : psyColors.sunken)
        .clipShape(Capsule())
        .overlay(Capsule().stroke(isSelected ? psyColors.blue : .clear, lineWidth: 1.5))
        .contentShape(Rectangle())
        .onTapGesture(perform: onSelect)
    }

    // MARK: - Date + time

    private var dateTimeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            EyebrowLabel(text: "Thời gian")
            HStack(spacing: 10) {
                readOnlyField(text: dateLabel, icon: "calendar") { showDatePicker = true }
                readOnlyField(text: timeLabel, icon: nil) { showTimePicker = true }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func readOnlyField(text: String, icon: String?, onTap: @escaping () -> Void) -> some View {
        HStack(spacing: 8) {
            if let icon {
                LucideIcon(name: icon, size: 16, tint: psyColors.text3)
            }
            Text(text)
                .font(PsyFont.mono(14))
                .foregroundStyle(psyColors.text)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(psyColors.surface)
        .overlay(
            RoundedRectangle(cornerRadius: PsyRadius.chip)
                .stroke(psyColors.hair, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: PsyRadius.chip))
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
    }

    private var dateLabel: String {
        let f = DateFormatter()
        f.dateFormat = "dd/MM/yyyy"
        return f.string(from: vm.date)
    }

    private var timeLabel: String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm"
        return f.string(from: vm.date)
    }

    private func datePickerSheet(components: DatePickerComponents, title: String) -> some View {
        VStack(spacing: 16) {
            Text(title)
                .font(PsyFont.titleMedium)
                .foregroundStyle(psyColors.text)
                .padding(.top, 20)
            DatePicker("", selection: $vm.date, displayedComponents: components)
                .labelsHidden()
                .datePickerStyle(.wheel)
            Spacer()
        }
        .padding(.horizontal, 22)
        .background(psyColors.bg.ignoresSafeArea())
        .presentationDetents([.medium])
    }

    // MARK: - Note

    private var noteSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            EyebrowLabel(text: "Ghi chú")
            TextField("Ghi chú", text: $vm.note, axis: .vertical)
                .lineLimit(1...3)
                .font(PsyFont.bodyMedium)
                .foregroundStyle(psyColors.text)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(psyColors.surface)
                .overlay(
                    RoundedRectangle(cornerRadius: PsyRadius.chip)
                        .stroke(psyColors.hair, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: PsyRadius.chip))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Photo

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            EyebrowLabel(text: "Đính kèm ảnh")
            if let uri = vm.photoUri {
                ZStack(alignment: .topTrailing) {
                    if let img = UIImage(contentsOfFile: uri) {
                        Image(uiImage: img)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 96, height: 96)
                            .clipShape(RoundedRectangle(cornerRadius: PsyRadius.chip))
                    } else {
                        RoundedRectangle(cornerRadius: PsyRadius.chip)
                            .fill(psyColors.sunken)
                            .frame(width: 96, height: 96)
                    }
                    Button {
                        vm.removePhoto()
                        photoItem = nil
                    } label: {
                        LucideIcon(name: "x", size: 13, tint: .white)
                            .frame(width: 24, height: 24)
                            .background(Color.black.opacity(0.55))
                            .clipShape(Circle())
                    }
                    .padding(4)
                }
            } else {
                PhotosPicker(selection: $photoItem, matching: .images) {
                    HStack(spacing: 8) {
                        LucideIcon(name: "plus", size: 17, tint: psyColors.blue)
                        Text("Đính kèm ảnh")
                            .font(PsyFont.bodyMedium.weight(.semibold))
                            .foregroundStyle(psyColors.blue)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 11)
                    .overlay(
                        RoundedRectangle(cornerRadius: PsyRadius.chip)
                            .stroke(psyColors.blue, lineWidth: 1)
                    )
                }
            }

            if let error = vm.photoErrorMessage {
                HStack {
                    Text(error)
                        .font(PsyFont.labelSmall)
                        .foregroundStyle(psyColors.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        vm.photoErrorMessage = nil
                    } label: {
                        LucideIcon(name: "x", size: 13, tint: psyColors.red)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(psyColors.redSoft)
                .clipShape(RoundedRectangle(cornerRadius: PsyRadius.chip))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Save

    private var saveButton: some View {
        Button {
            vm.save { dismiss() }
        } label: {
            Text("Lưu giao dịch")
                .font(PsyFont.labelLarge)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(vm.canSave ? psyColors.blue : psyColors.blue.opacity(0.4))
                .clipShape(RoundedRectangle(cornerRadius: PsyRadius.button))
        }
        .disabled(!vm.canSave)
    }
}
