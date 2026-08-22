import SwiftUI

enum AuriColors {
    static let purple = Color(red: 104 / 255, green: 52 / 255, blue: 153 / 255)
    static let purpleDark = Color(red: 62 / 255, green: 24 / 255, blue: 92 / 255)
    static let violet = Color(red: 146 / 255, green: 86 / 255, blue: 190 / 255)
    static let lavender = Color(red: 218 / 255, green: 190 / 255, blue: 237 / 255)
    static let purpleTint = Color(red: 243 / 255, green: 234 / 255, blue: 249 / 255)
    static let background = Color(red: 232 / 255, green: 245 / 255, blue: 251 / 255)
    static let surface = Color(red: 248 / 255, green: 253 / 255, blue: 255 / 255)
    static let navBlue = Color(red: 211 / 255, green: 235 / 255, blue: 246 / 255)
    static let ink = Color(red: 52 / 255, green: 38 / 255, blue: 61 / 255)
    static let muted = Color(red: 122 / 255, green: 105 / 255, blue: 130 / 255)
    static let line = Color(red: 204 / 255, green: 224 / 255, blue: 234 / 255)
}

struct AuriCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(AuriColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(AuriColors.line.opacity(0.8), lineWidth: 1)
            }
            .shadow(color: AuriColors.purpleDark.opacity(0.06), radius: 8, y: 3)
    }
}

extension View {
    func auriCard() -> some View {
        modifier(AuriCardModifier())
    }
}

struct AuriHero<Content: View>: View {
    let colors: [Color]
    let content: Content

    init(colors: [Color], @ViewBuilder content: () -> Content) {
        self.colors = colors
        self.content = content()
    }

    var body: some View {
        content
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20)
            .background(
                LinearGradient(
                    colors: colors,
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
    }
}

struct EmptyStateView: View {
    let symbol: String
    let title: String
    let detail: String

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 25, weight: .bold))
                .foregroundStyle(AuriColors.purple)
                .frame(width: 52, height: 52)
                .background(AuriColors.purpleTint)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

            Text(title)
                .font(.headline)
                .foregroundStyle(AuriColors.ink)
                .multilineTextAlignment(.center)

            Text(detail)
                .font(.subheadline)
                .foregroundStyle(AuriColors.muted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(26)
        .auriCard()
    }
}

struct AuriPrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(minHeight: 54)
            .padding(.horizontal, 18)
            .background(configuration.isPressed ? AuriColors.purple : AuriColors.purpleDark)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
    }
}
