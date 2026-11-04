import Foundation
import CoreGraphics

struct MapPlacement: Equatable {
    let host: CGRect
    let map: CGRect

    static func resolve(documentFrame: CGRect, documentClip: CGRect, viewport: CGRect,
                        contentOffset: CGPoint, zoomScale: CGFloat, visible: Bool) -> MapPlacement? {
        guard visible, zoomScale.isFinite, zoomScale > 0,
              documentFrame.width > 0, documentFrame.height > 0 else { return nil }
        func screen(_ rectangle: CGRect) -> CGRect {
            CGRect(x: rectangle.minX * zoomScale - contentOffset.x,
                   y: rectangle.minY * zoomScale - contentOffset.y,
                   width: rectangle.width * zoomScale, height: rectangle.height * zoomScale)
        }
        let full = screen(documentFrame)
        let host = full.intersection(screen(documentClip)).intersection(viewport)
        guard !host.isNull, !host.isEmpty else { return nil }
        return MapPlacement(host: host, map: CGRect(x: full.minX - host.minX,
            y: full.minY - host.minY, width: full.width, height: full.height))
    }
}
