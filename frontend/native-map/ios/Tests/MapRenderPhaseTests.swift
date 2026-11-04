import XCTest
@testable import PlugpassNativeMap

final class MapRenderPhaseTests: XCTestCase {
    func testHiddenMapStillRendersWhileSDKAddsItsView() {
        XCTAssertTrue(MapRenderPhase.loading.shouldRender(appActive: true, visible: false))
    }
    func testVisibleReadyMapRenders() {
        XCTAssertTrue(MapRenderPhase.ready.shouldRender(appActive: true, visible: true))
    }
    func testReadyMapOutsideViewportDoesNotRender() {
        XCTAssertFalse(MapRenderPhase.ready.shouldRender(appActive: true, visible: false))
    }
    func testBackgroundMapDoesNotRenderEvenDuringInitialization() {
        XCTAssertFalse(MapRenderPhase.loading.shouldRender(appActive: false, visible: true))
    }
    func testClosedMapCannotResumeRendering() {
        XCTAssertFalse(MapRenderPhase.closed.shouldRender(appActive: true, visible: true))
    }
}
