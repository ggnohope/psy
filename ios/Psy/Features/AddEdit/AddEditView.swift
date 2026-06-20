import SwiftUI
import PhotosUI
import PsyCore

/// Add / edit a transaction. Presented as a sheet from Home. Ports AddEditTransactionScreen.kt.
struct AddEditView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.psyColors) private var psyColors
    @State private var vm: AddEditViewModel
    @State private var photoItem: PhotosPickerItem?

    init(container: AppContainer, txId: Int64) {
        _vm = State(initialValue: AddEditViewModel(container: container, txId: txId))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    typeToggle
                    amountSection
                    if vm.type != .transfer && !vm.categories.isEmpty {
                        categorySection
                    }
                    accountSections
                    dateSection
                    noteSection
                    photoSection
                    saveButton
                    Spacer(minLength: 16)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
            .background(psyColors.background)
            .navigationTitle(vm.isEdit ? "Sửa giao dịch" : "Thêm giao dịch")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Quay lại") { dismiss() }
                }
                if vm.isEdit {
                    ToolbarItem(placement: .destructiveAction) {
                        Button {
                            vm.delete { dismiss() }
                        } label: {
                            Image(systemName: "trash")
                        }
                        .tint(CandyColor.pinkDeep)
                    }
                }
            }
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
        }
    }

    // MARK: - Type segmented toggle

    private var typeToggle: some View {
        HStack(spacing: 0) {
            ForEach(TxType.allCases, id: \.self) { type in
                let isSelected = type == vm.type
                Text(typeLabel(type))
                    .font(PsyFont.bodyMedium)
                    .fontWeight(isSelected ? .bold : .regular)
                    .foregroundStyle(isSelected ? Color.white : psyColors.onSurface)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(isSelected ? psyColors.primary : Color.clear)
                    .clipShape(Capsule())
                    .contentShape(Rectangle())
                    .onTapGesture { vm.onTypeChange(type) }
            }
        }
        .background(psyColors.onSurface.opacity(0.06))
        .clipShape(Capsule())
    }

    private func typeLabel(_ type: TxType) -> String {
        switch type {
        case .expense: return "Chi tiêu"
        case .income: return "Thu nhập"
        case .transfer: return "Chuyển khoản"
        }
    }

    // MARK: - Amount

    private var amountSection: some View {
        VStack(spacing: 8) {
            MoneyText(amountMinor: displayAmountMinor, currency: vm.currency)
                .font(.system(size: 36, weight: .bold))
                .foregroundStyle(psyColors.onSurface)
            TextField("Số tiền", text: Binding(
                get: { vm.amountText },
                set: { vm.amountText = $0.filter(\.isNumber) }
            ))
            .keyboardType(.numberPad)
            .textFieldStyle(.roundedBorder)
        }
        .frame(maxWidth: .infinity)
    }

    private var displayAmountMinor: Int64 {
        AddEditLogic.amountMinor(typed: vm.amountText, fractionDigits: vm.currency.fractionDigits)
    }

    // MARK: - Category grid

    private var categorySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionLabel("Danh mục")
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                ForEach(vm.categories) { category in
                    categoryChip(category)
                }
            }
        }
    }

    private func categoryChip(_ category: PsyCore.Category) -> some View {
        let isSelected = category.id == vm.selectedCategoryId
        return VStack(spacing: 2) {
            Text(category.icon).font(.system(size: 22))
            Text(category.name)
                .font(.system(size: 10))
                .fontWeight(isSelected ? .bold : .regular)
                .foregroundStyle(isSelected ? psyColors.primary : psyColors.onSurface)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .padding(.horizontal, 4)
        .background(isSelected ? psyColors.primary.opacity(0.15) : psyColors.onSurface.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: CandyShape.small))
        .overlay(
            RoundedRectangle(cornerRadius: CandyShape.small)
                .stroke(isSelected ? psyColors.primary : .clear, lineWidth: 2)
        )
        .contentShape(Rectangle())
        .onTapGesture { vm.selectedCategoryId = category.id }
    }

    // MARK: - Account chips

    @ViewBuilder
    private var accountSections: some View {
        if !vm.accounts.isEmpty {
            if vm.type == .transfer {
                accountSection(label: "Từ tài khoản", selectedId: vm.selectedAccountId) { vm.selectedAccountId = $0 }
                accountSection(label: "Đến tài khoản", selectedId: vm.toAccountId) { vm.toAccountId = $0 }
                if let from = vm.selectedAccountId, let to = vm.toAccountId, from == to {
                    Text("Hai tài khoản phải khác nhau")
                        .font(PsyFont.labelSmall)
                        .foregroundStyle(CandyColor.pinkDeep)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 4)
                }
            } else {
                accountSection(label: "Tài khoản", selectedId: vm.selectedAccountId) { vm.selectedAccountId = $0 }
            }
        }
    }

    private func accountSection(label: String, selectedId: Int64?, onSelect: @escaping (Int64) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionLabel(label)
            FlowLayout(spacing: 8) {
                ForEach(vm.accounts) { account in
                    accountChip(account, isSelected: account.id == selectedId) { onSelect(account.id) }
                }
            }
        }
    }

    private func accountChip(_ account: Account, isSelected: Bool, onSelect: @escaping () -> Void) -> some View {
        HStack(spacing: 4) {
            Text(account.icon).font(.system(size: 16))
            Text(account.name)
                .font(.system(size: 13))
                .fontWeight(isSelected ? .bold : .regular)
                .foregroundStyle(isSelected ? CandyColor.green : psyColors.onSurface)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(isSelected ? CandyColor.green.opacity(0.15) : psyColors.onSurface.opacity(0.06))
        .clipShape(Capsule())
        .overlay(Capsule().stroke(isSelected ? CandyColor.green : .clear, lineWidth: 2))
        .contentShape(Rectangle())
        .onTapGesture(perform: onSelect)
    }

    // MARK: - Date

    private var dateSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            sectionLabel("Ngày")
            DatePicker("Ngày", selection: $vm.date, displayedComponents: .date)
                .labelsHidden()
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    // MARK: - Note

    private var noteSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            sectionLabel("Ghi chú")
            TextField("Ghi chú", text: $vm.note, axis: .vertical)
                .lineLimit(1...3)
                .textFieldStyle(.roundedBorder)
        }
    }

    // MARK: - Photo

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionLabel("Đính kèm ảnh")
            if let uri = vm.photoUri {
                ZStack(alignment: .topTrailing) {
                    if let img = UIImage(contentsOfFile: uri) {
                        Image(uiImage: img)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 96, height: 96)
                            .clipShape(RoundedRectangle(cornerRadius: CandyShape.small))
                    } else {
                        RoundedRectangle(cornerRadius: CandyShape.small)
                            .fill(psyColors.onSurface.opacity(0.06))
                            .frame(width: 96, height: 96)
                    }
                    Button {
                        vm.removePhoto()
                        photoItem = nil
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(width: 24, height: 24)
                            .background(Color.black.opacity(0.5))
                            .clipShape(Circle())
                    }
                    .padding(4)
                }
            } else {
                PhotosPicker(selection: $photoItem, matching: .images) {
                    Text("📷  Đính kèm ảnh")
                        .font(PsyFont.bodyMedium)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .overlay(
                            RoundedRectangle(cornerRadius: CandyShape.small)
                                .stroke(psyColors.primary, lineWidth: 1)
                        )
                }
            }

            if let error = vm.photoErrorMessage {
                HStack {
                    Text(error)
                        .font(PsyFont.labelSmall)
                        .foregroundStyle(CandyColor.pinkDeep)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        vm.photoErrorMessage = nil
                    } label: {
                        Image(systemName: "xmark").font(.system(size: 12))
                    }
                    .tint(CandyColor.pinkDeep)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(CandyColor.pinkDeep.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Save

    private var saveButton: some View {
        Button {
            vm.save { dismiss() }
        } label: {
            Text("Lưu")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(vm.canSave ? psyColors.primary : psyColors.primary.opacity(0.4))
                .clipShape(RoundedRectangle(cornerRadius: CandyShape.medium))
        }
        .disabled(!vm.canSave)
    }

    // MARK: - Helpers

    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .font(PsyFont.bodyMedium)
            .foregroundStyle(psyColors.onSurface.opacity(0.7))
    }
}

/// Minimal flow layout for the account chips (mirrors Compose FlowRow).
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var rows: [[CGSize]] = [[]]
        var x: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, !rows[rows.count - 1].isEmpty {
                rows.append([])
                x = 0
            }
            rows[rows.count - 1].append(size)
            x += size.width + spacing
        }
        let height = rows.reduce(0) { acc, row in
            acc + (row.map(\.height).max() ?? 0) + spacing
        } - (rows.isEmpty ? 0 : spacing)
        return CGSize(width: maxWidth == .infinity ? x : maxWidth, height: max(height, 0))
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let maxWidth = bounds.width
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > bounds.minX + maxWidth, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            sub.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
