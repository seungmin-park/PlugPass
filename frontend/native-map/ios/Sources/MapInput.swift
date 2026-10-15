import Foundation
import CoreGraphics

enum MapInputError: Error, Equatable { case invalidInput }
protocol MapValidating: Decodable { func validate() throws }

enum MapInput {
    static func decode<Value: MapValidating>(_ type: Value.Type, from options: [String: Any]) throws -> Value {
        do {
            let value = try JSONDecoder().decode(type, from: JSONSerialization.data(withJSONObject: options))
            try value.validate()
            return value
        } catch { throw MapInputError.invalidInput }
    }
}

struct MapCoordinate: MapValidating, Equatable {
    let latitude: Double
    let longitude: Double
    func validate() throws {
        guard latitude.isFinite, longitude.isFinite,
              (-90...90).contains(latitude), (-180...180).contains(longitude) else { throw MapInputError.invalidInput }
    }
}
struct MapStationInput: Decodable {
    let id: Int
    let name: String
    let latitude: Double
    let longitude: Double
    var coordinate: MapCoordinate { MapCoordinate(latitude: latitude, longitude: longitude) }
}
struct MapStationsInput: MapValidating {
    let stations: [MapStationInput]
    func validate() throws {
        guard Set(stations.map(\.id)).count == stations.count else { throw MapInputError.invalidInput }
        for station in stations {
            guard station.id > 0, station.id <= 9_007_199_254_740_991, !station.name.isEmpty else { throw MapInputError.invalidInput }
            try station.coordinate.validate()
        }
    }
}
struct MapRectInput: Decodable {
    let x: Double
    let y: Double
    let width: Double
    let height: Double
    var rect: CGRect { CGRect(x: x, y: y, width: width, height: height) }
    func validate() throws {
        guard x.isFinite, y.isFinite, width.isFinite, height.isFinite,
              width >= 0, height >= 0 else { throw MapInputError.invalidInput }
    }
}
struct MapLayoutInput: MapValidating {
    let frame: MapRectInput
    let clip: MapRectInput
    let visible: Bool
    let viewportHeight: Double?
    func validate() throws {
        try frame.validate()
        try clip.validate()
        if let viewportHeight, !viewportHeight.isFinite || viewportHeight < 0 { throw MapInputError.invalidInput }
    }
}
struct MapAreaInput: MapValidating {
    let south: Double
    let north: Double
    let west: Double
    let east: Double
    func validate() throws {
        try MapCoordinate(latitude: south, longitude: west).validate()
        try MapCoordinate(latitude: north, longitude: east).validate()
        guard south < north, west < east else { throw MapInputError.invalidInput }
    }
}
