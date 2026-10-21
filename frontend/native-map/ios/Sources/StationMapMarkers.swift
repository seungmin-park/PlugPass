import UIKit
import KakaoMapsSDK

final class StationMapMarkers {
    private let layer: LabelLayer
    private var markers: [Int: Poi] = [:]
    private var selected: Int?

    init(map: KakaoMap) throws {
        let manager = map.getLabelManager()
        let options = LabelLayerOptions(layerID: "plugpass-stations", competitionType: .none,
            competitionUnit: .poi, orderType: .rank, zOrder: 1000)
        guard let layer = manager.addLabelLayer(option: options) else { throw MapInputError.invalidInput }
        self.layer = layer
        layer.setClickable(true)
        for (name, color) in [("station", UIColor(red: 0, green: 0.43, blue: 0.39, alpha: 1)),
                              ("selected", UIColor(red: 0.05, green: 0.13, blue: 0.14, alpha: 1))] {
            let glyph = UIImage(systemName: "mappin.circle.fill", withConfiguration: UIImage.SymbolConfiguration(pointSize: 44))?
                .withTintColor(color, renderingMode: .alwaysOriginal)
            let symbol = UIGraphicsImageRenderer(size: CGSize(width: 44, height: 44)).image { _ in
                glyph?.draw(in: CGRect(x: 0, y: 0, width: 44, height: 44))
            }
            manager.addPoiStyle(PoiStyle(styleID: name, styles: [PerLevelPoiStyle(
                iconStyle: PoiIconStyle(symbol: symbol), level: 0)]))
        }
    }

    func replace(_ stations: [MapStationInput]) {
        layer.clearAllItems()
        markers.removeAll()
        for station in stations {
            let options = PoiOptions(styleID: station.id == selected ? "selected" : "station", poiID: String(station.id))
            options.clickable = true
            options.rank = station.id == selected ? 1 : 0
            markers[station.id] = layer.addPoi(option: options,
                at: MapPoint(longitude: station.longitude, latitude: station.latitude))
        }
        layer.showAllPois()
    }

    func select(_ stationId: Int?) {
        if let previous = selected { markers[previous]?.changeStyle(styleID: "station") }
        selected = stationId
        if let stationId { markers[stationId]?.changeStyle(styleID: "selected") }
    }

    func contains(_ stationId: Int) -> Bool { markers[stationId] != nil }
}
