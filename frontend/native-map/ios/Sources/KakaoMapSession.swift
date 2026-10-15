import UIKit
import WebKit
import KakaoMapsSDK

final class KakaoMapSession: NSObject, MapControllerDelegate, KakaoMapEventDelegate {
    let id: String
    private let viewport: NativeMapViewport
    private let controller: KMController
    private let center: MapCoordinate
    private var stations: [MapStationInput]
    private var map: KakaoMap?
    private var markers: StationMapMarkers?
    private var closed = false
    private var renderPhase = MapRenderPhase.loading
    private var completion: ((String?) -> Void)?
    private var timeout: DispatchWorkItem?
    private var notifications: [NSObjectProtocol] = []
    private let emit: ([String: Any]) -> Void

    init(id: String, webView: WKWebView, layout: MapLayoutInput, center: MapCoordinate,
         stations: [MapStationInput], emit: @escaping ([String: Any]) -> Void,
         completion: @escaping (String?) -> Void) throws {
        self.id = id
        self.center = center
        self.stations = stations
        self.emit = emit
        self.completion = completion
        viewport = try NativeMapViewport(webView: webView, layout: layout)
        controller = KMController(viewContainer: viewport.container)
        super.init()
        controller.delegate = self
        viewport.visibilityChanged = { [weak self] in self?.updateRendering() }
        notifications = [
            NotificationCenter.default.addObserver(forName: UIApplication.willResignActiveNotification,
                object: nil, queue: .main) { [weak self] _ in self?.controller.pauseEngine() },
            NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification,
                object: nil, queue: .main) { [weak self] _ in self?.updateRendering() }
        ]
    }

    func start() {
        let timeout = DispatchWorkItem { [weak self] in self?.fail("MAP_TIMEOUT") }
        self.timeout = timeout
        DispatchQueue.main.asyncAfter(deadline: .now() + 8, execute: timeout)
        if !controller.prepareEngine() { fail("MAP_PREPARE_FAILED"); return }
        updateRendering()
    }

    func close() {
        guard !closed else { return }
        closed = true
        renderPhase = .closed
        timeout?.cancel()
        timeout = nil
        notifications.forEach(NotificationCenter.default.removeObserver)
        notifications.removeAll()
        viewport.close()
        map?.eventDelegate = nil
        controller.delegate = nil
        controller.pauseEngine()
        controller.resetEngine()
        map = nil
        markers = nil
        let pending = completion
        completion = nil
        pending?("MAP_CANCELLED")
    }

    func updateLayout(_ layout: MapLayoutInput) { if !closed { viewport.update(layout) } }
    func updateStations(_ stations: [MapStationInput]) {
        guard !closed else { return }
        self.stations = stations
        markers?.replace(stations)
    }
    func select(_ stationId: Int?) {
        guard !closed else { return }
        markers?.select(stationId)
        if let station = stations.first(where: { $0.id == stationId }) { move(to: station.coordinate) }
    }
    func move(to center: MapCoordinate) {
        guard !closed, let map else { return }
        map.moveCamera(CameraUpdate.make(target: MapPoint(longitude: center.longitude, latitude: center.latitude), mapView: map))
    }
    func show(_ area: MapAreaInput) {
        guard !closed, let map else { return }
        map.moveCamera(CameraUpdate.make(area: AreaRect(
            southWest: MapPoint(longitude: area.west, latitude: area.south),
            northEast: MapPoint(longitude: area.east, latitude: area.north))))
    }
    func zoom(inward: Bool) {
        guard !closed, let map else { return }
        let level = min(map.cameraMaxLevel, max(map.cameraMinLevel, map.zoomLevel + (inward ? 1 : -1)))
        map.moveCamera(CameraUpdate.make(zoomLevel: level, mapView: map))
        reportArea()
    }

    func addViews() {
        guard !closed else { return }
        controller.addView(MapviewInfo(viewName: "plugpass", defaultPosition:
            MapPoint(longitude: center.longitude, latitude: center.latitude), defaultLevel: 15),
            viewSize: viewport.container.bounds.size, timeout: 5_000)
        updateRendering()
    }
    func addViewSucceeded(_ viewName: String, viewInfoName: String) {
        guard !closed, let map = controller.getView(viewName) as? KakaoMap else { return }
        self.map = map
        map.eventDelegate = self
        map.viewRect = CGRect(origin: .zero, size: viewport.container.bounds.size)
        map.keepLevelOnResize = true
        map.setGestureEnable(type: .rotate, enable: false)
        map.setGestureEnable(type: .tilt, enable: false)
        map.setGestureEnable(type: .rotateZoom, enable: false)
        do {
            markers = try StationMapMarkers(map: map)
            markers?.replace(stations)
        } catch { fail("MAP_MARKERS_FAILED"); return }
        timeout?.cancel()
        timeout = nil
        renderPhase = .ready
        updateRendering()
        event(["type": "ready"])
        reportArea()
        let pending = completion
        completion = nil
        pending?(nil)
    }
    func addViewFailed(_ viewName: String, viewInfoName: String) { fail("MAP_VIEW_FAILED") }
    func authenticationSucceeded() { updateRendering() }
    func authenticationFailed(_ errorCode: Int, desc: String) { fail("MAP_AUTH_\(errorCode)") }
    func containerDidResized(_ size: CGSize) {
        guard !closed else { return }
        map?.viewRect = CGRect(origin: .zero, size: size)
        reportArea()
    }
    func cameraWillMove(kakaoMap: KakaoMap, by: MoveBy) {
        event(["type": "moving", "userInitiated": by != .notUserAction])
    }
    func cameraDidStopped(kakaoMap: KakaoMap, by: MoveBy) { reportArea() }
    func poiDidTapped(kakaoMap: KakaoMap, layerID: String, poiID: String, position: MapPoint) {
        guard layerID == "plugpass-stations", let stationId = Int(poiID), markers?.contains(stationId) == true else { return }
        event(["type": "selected", "stationId": stationId])
    }

    private func reportArea() {
        guard !closed, let map else { return }
        let topLeft = map.getPosition(.zero)
        let bottomRight = map.getPosition(CGPoint(x: map.viewRect.width, y: map.viewRect.height))
        event(["type": "settled", "area": ["south": bottomRight.wgsCoord.latitude,
            "north": topLeft.wgsCoord.latitude, "west": topLeft.wgsCoord.longitude, "east": bottomRight.wgsCoord.longitude],
            "zoom": ["canZoomIn": map.zoomLevel < map.cameraMaxLevel, "canZoomOut": map.zoomLevel > map.cameraMinLevel,
                     "description": "지도 확대 수준 \(map.zoomLevel)단계"]])
    }
    private func updateRendering() {
        guard !closed else { return }
        if controller.isEnginePrepared && renderPhase.shouldRender(
            appActive: UIApplication.shared.applicationState == .active, visible: viewport.isVisible) {
            controller.activateEngine()
        } else { controller.pauseEngine() }
    }
    private func event(_ payload: [String: Any]) {
        guard !closed else { return }
        emit(payload.merging(["sessionId": id]) { _, sessionId in sessionId })
    }
    private func fail(_ code: String) {
        guard !closed else { return }
        let pending = completion
        completion = nil
        event(["type": "failed", "code": code])
        close()
        pending?(code)
    }
}
