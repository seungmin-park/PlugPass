import UIKit
import WebKit
import KakaoMapsSDK

final class NativeMapViewport {
    let container: KMViewContainer
    private let host = UIView()
    private weak var webView: WKWebView?
    private var layout: MapLayoutInput
    private var observations: [NSKeyValueObservation] = []
    var visibilityChanged: (() -> Void)?
    var isVisible: Bool { !host.isHidden && host.window != nil }

    init(webView: WKWebView, layout: MapLayoutInput) throws {
        guard let parent = webView.superview else { throw MapInputError.invalidInput }
        self.webView = webView
        self.layout = layout
        container = KMViewContainer(frame: CGRect(origin: .zero, size: layout.frame.rect.size))
        container.clipsToBounds = true
        container.layer.cornerRadius = 12
        host.clipsToBounds = true
        host.isHidden = true
        host.addSubview(container)
        parent.insertSubview(host, aboveSubview: webView)
        observations = [
            webView.scrollView.observe(\.contentOffset) { [weak self] _, _ in self?.place() },
            webView.scrollView.observe(\.zoomScale) { [weak self] _, _ in self?.place() },
            webView.observe(\.bounds) { [weak self] _, _ in self?.place() }
        ]
        place()
    }

    func update(_ layout: MapLayoutInput) {
        self.layout = layout
        place()
    }

    func close() {
        observations.removeAll()
        visibilityChanged = nil
        host.isHidden = true
        host.removeFromSuperview()
    }

    private func place() {
        guard let webView, let parent = host.superview else { return }
        let previousVisible = !host.isHidden
        var visibleBounds = webView.bounds
        var insets = webView.safeAreaInsets
        if let height = layout.viewportHeight {
            visibleBounds.size.height = min(visibleBounds.height, height * webView.scrollView.zoomScale)
            if visibleBounds.height < webView.bounds.height { insets.bottom = 0 }
        }
        let placement = MapPlacement.resolve(documentFrame: layout.frame.rect, documentClip: layout.clip.rect,
            viewport: visibleBounds.inset(by: insets),
            contentOffset: webView.scrollView.contentOffset,
            zoomScale: webView.scrollView.zoomScale, visible: layout.visible)
        guard let placement else {
            host.isHidden = true
            if previousVisible { visibilityChanged?() }
            return
        }
        host.frame = webView.convert(placement.host, to: parent)
        container.frame = placement.map
        host.isHidden = false
        if !previousVisible { visibilityChanged?() }
    }
}
