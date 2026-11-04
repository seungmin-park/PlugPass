enum MapRenderPhase {
    case loading, ready, closed
    func shouldRender(appActive: Bool, visible: Bool) -> Bool {
        guard appActive else { return false }
        switch self {
        case .loading: return true
        case .ready: return visible
        case .closed: return false
        }
    }
}
