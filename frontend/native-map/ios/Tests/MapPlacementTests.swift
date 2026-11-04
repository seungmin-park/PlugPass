import XCTest
import CoreGraphics
@testable import PlugpassNativeMap

final class MapPlacementTests: XCTestCase {
    func testScrollingClipsHostButPreservesMapSizeAndTouchCoordinates() {
        let placement = MapPlacement.resolve(
            documentFrame: CGRect(x: 16, y: 600, width: 361, height: 280),
            documentClip: CGRect(x: 16, y: 600, width: 361, height: 280),
            viewport: CGRect(x: 0, y: 59, width: 393, height: 759),
            contentOffset: CGPoint(x: 0, y: 650), zoomScale: 1, visible: true)
        XCTAssertEqual(placement?.host, CGRect(x: 16, y: 59, width: 361, height: 171))
        XCTAssertEqual(placement?.map, CGRect(x: 0, y: -109, width: 361, height: 280))
    }

    func testZoomAndHorizontalScrollUseTheSameCoordinateTransform() {
        let placement = MapPlacement.resolve(
            documentFrame: CGRect(x: 10, y: 100, width: 200, height: 100),
            documentClip: CGRect(x: 20, y: 100, width: 180, height: 100),
            viewport: CGRect(x: 0, y: 0, width: 393, height: 852),
            contentOffset: CGPoint(x: 30, y: 100), zoomScale: 2, visible: true)
        XCTAssertEqual(placement?.host, CGRect(x: 10, y: 100, width: 360, height: 200))
        XCTAssertEqual(placement?.map, CGRect(x: -20, y: 0, width: 400, height: 200))
    }

    func testHiddenPanelDoesNotLeaveATouchSurface() {
        let frame = CGRect(x: 0, y: 100, width: 300, height: 280)
        XCTAssertNil(MapPlacement.resolve(documentFrame: frame, documentClip: frame,
            viewport: CGRect(x: 0, y: 0, width: 393, height: 852),
            contentOffset: .zero, zoomScale: 1, visible: false))
    }

    func testOffscreenMapDoesNotLeaveATouchSurface() {
        let frame = CGRect(x: 0, y: 1000, width: 300, height: 280)
        XCTAssertNil(MapPlacement.resolve(documentFrame: frame, documentClip: frame,
            viewport: CGRect(x: 0, y: 0, width: 393, height: 852),
            contentOffset: .zero, zoomScale: 1, visible: true))
    }

    func testKeyboardViewportClipsBottomWithoutResizingTheMap() {
        let frame = CGRect(x: 16, y: 200, width: 361, height: 280)
        let placement = MapPlacement.resolve(documentFrame: frame, documentClip: frame,
            viewport: CGRect(x: 0, y: 59, width: 393, height: 300),
            contentOffset: .zero, zoomScale: 1, visible: true)
        XCTAssertEqual(placement?.host, CGRect(x: 16, y: 200, width: 361, height: 159))
        XCTAssertEqual(placement?.map, CGRect(x: 0, y: 0, width: 361, height: 280))
    }
}
