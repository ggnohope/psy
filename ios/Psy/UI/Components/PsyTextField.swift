import SwiftUI

/// HostGuardIQ themed single-line text field: surface bg, `hair` border, chip-radius corners.
/// Mirrors Android's PsyTextField. Reads colors from `@Environment(\.psyColors)`.
struct PsyTextField: View {
    let placeholder: String
    @Binding var text: String
    @Environment(\.psyColors) private var psyColors

    init(_ placeholder: String, text: Binding<String>) {
        self.placeholder = placeholder
        self._text = text
    }

    var body: some View {
        TextField(placeholder, text: $text)
            .font(PsyFont.bodyLarge)
            .foregroundStyle(psyColors.text)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(psyColors.surface)
            .overlay(RoundedRectangle(cornerRadius: PsyRadius.chip).stroke(psyColors.hair, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: PsyRadius.chip))
    }
}
