import Capacitor
import KakaoMapsSDK

@objc(PlugPassMapPlugin)
public final class PlugPassMapPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "PlugPassMapPlugin"
    public let jsName = "PlugPassMap"
    public let pluginMethods: [CAPPluginMethod] = [
        "create", "updateLayout", "updateStations", "selectStation", "moveTo", "showArea", "zoom", "destroy"
    ].map { CAPPluginMethod(name: $0, returnType: CAPPluginReturnPromise) }
    private var session: KakaoMapSession?
    private static var initialized = false

    @objc func create(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            do {
                guard let id = call.getString("sessionId"), UUID(uuidString: id) != nil,
                      let webView = self.bridge?.webView else { throw MapInputError.invalidInput }
                let center = try MapInput.decode(MapCoordinate.self, from: call.getObject("center") ?? [:])
                let stations = try MapInput.decode(MapStationsInput.self, from: ["stations": call.getArray("stations") ?? []])
                let layout = try MapInput.decode(MapLayoutInput.self, from: call.getObject("layout") ?? [:])
                guard layout.frame.width > 0, layout.frame.height > 0 else { throw MapInputError.invalidInput }
                guard let key = Bundle.main.object(forInfoDictionaryKey: "KakaoNativeAppKey") as? String,
                      key.range(of: "^[a-fA-F0-9]{32}$", options: .regularExpression) != nil else {
                    call.reject("카카오 네이티브 지도 키가 설정되지 않았습니다.", "MAP_KEY_MISSING")
                    return
                }
                if !Self.initialized {
                    SDKInitializer.InitSDK(appKey: key)
                    Self.initialized = true
                }
                self.session?.close()
                let session = try KakaoMapSession(id: id, webView: webView, layout: layout,
                    center: center, stations: stations.stations,
                    emit: { [weak plugin = self] event in plugin?.notifyListeners("mapEvent", data: event) },
                    completion: { code in
                        if let code { call.reject("카카오 지도를 불러오지 못했습니다.", code) }
                        else { call.resolve() }
                    })
                self.session = session
                session.start()
            } catch { call.reject("지도 요청을 확인해 주세요.", "MAP_INVALID_INPUT") }
        }
    }

    @objc func updateLayout(_ call: CAPPluginCall) {
        command(call) { session in
            session.updateLayout(try MapInput.decode(MapLayoutInput.self, from: call.getObject("layout") ?? [:]))
        }
    }
    @objc func updateStations(_ call: CAPPluginCall) {
        command(call) { session in
            session.updateStations(try MapInput.decode(MapStationsInput.self,
                from: ["stations": call.getArray("stations") ?? []]).stations)
        }
    }
    @objc func selectStation(_ call: CAPPluginCall) {
        command(call) { session in
            let stationId = call.getInt("stationId")
            if let stationId, stationId <= 0 { throw MapInputError.invalidInput }
            session.select(stationId)
        }
    }
    @objc func moveTo(_ call: CAPPluginCall) {
        command(call) { session in
            session.move(to: try MapInput.decode(MapCoordinate.self, from: call.getObject("center") ?? [:]))
        }
    }
    @objc func showArea(_ call: CAPPluginCall) {
        command(call) { session in
            session.show(try MapInput.decode(MapAreaInput.self, from: call.getObject("area") ?? [:]))
        }
    }
    @objc func zoom(_ call: CAPPluginCall) {
        command(call) { session in
            guard let direction = call.getString("direction"), direction == "in" || direction == "out" else { throw MapInputError.invalidInput }
            session.zoom(inward: direction == "in")
        }
    }
    @objc func destroy(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            if self.session?.id == call.getString("sessionId") {
                self.session?.close()
                self.session = nil
            }
            call.resolve()
        }
    }
    private func command(_ call: CAPPluginCall, action: @escaping (KakaoMapSession) throws -> Void) {
        DispatchQueue.main.async {
            guard let session = self.session, session.id == call.getString("sessionId") else {
                call.reject("종료된 지도입니다.", "MAP_CLOSED")
                return
            }
            do { try action(session); call.resolve() }
            catch { call.reject("지도 요청을 확인해 주세요.", "MAP_INVALID_INPUT") }
        }
    }
}
