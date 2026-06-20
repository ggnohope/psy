import Foundation

enum APIError: Error { case http(Int), noData }

struct APIClient {
    let baseURL: URL
    let tokenProvider: () -> String?

    init(baseURLString: String, tokenProvider: @escaping () -> String?) {
        self.baseURL = URL(string: baseURLString)!
        self.tokenProvider = tokenProvider
    }

    /// POST JSON, decode response. `authed` adds the Bearer header.
    func post<B: Encodable, R: Decodable>(_ path: String, body: B, authed: Bool) async throws -> R {
        var req = URLRequest(url: baseURL.appendingPathComponent(path))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if authed, let t = tokenProvider() { req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization") }
        req.httpBody = try JSONEncoder().encode(body)
        let (data, resp) = try await URLSession.shared.data(for: req)
        try ensureOK(resp)
        return try JSONDecoder().decode(R.self, from: data)
    }

    /// POST JSON; ignores the response body, only checks the status code.
    func postNoContent<B: Encodable>(_ path: String, body: B, authed: Bool) async throws {
        var req = URLRequest(url: baseURL.appendingPathComponent(path))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if authed, let t = tokenProvider() { req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization") }
        req.httpBody = try JSONEncoder().encode(body)
        let (_, resp) = try await URLSession.shared.data(for: req)
        try ensureOK(resp)
    }

    /// GET; returns nil on 204. `R` decoded otherwise.
    func get<R: Decodable>(_ path: String, authed: Bool) async throws -> R? {
        var req = URLRequest(url: baseURL.appendingPathComponent(path))
        if authed, let t = tokenProvider() { req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization") }
        let (data, resp) = try await URLSession.shared.data(for: req)
        if let http = resp as? HTTPURLResponse, http.statusCode == 204 { return nil }
        try ensureOK(resp)
        if data.isEmpty { return nil }
        return try JSONDecoder().decode(R.self, from: data)
    }

    private func ensureOK(_ resp: URLResponse) throws {
        guard let http = resp as? HTTPURLResponse else { throw APIError.noData }
        guard (200..<300).contains(http.statusCode) else { throw APIError.http(http.statusCode) }
    }
}
