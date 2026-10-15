import XCTest
@testable import PlugpassNativeMap

final class MapInputTests: XCTestCase {
    func testCoordinatesAtGeographicBoundaryAreAccepted() throws {
        let coordinate = try MapInput.decode(MapCoordinate.self, from: ["latitude": 90, "longitude": -180])
        XCTAssertEqual(coordinate, MapCoordinate(latitude: 90, longitude: -180))
    }

    func testCoordinatesOutsideLatitudeBoundaryAreRejected() {
        XCTAssertThrowsError(try MapInput.decode(MapCoordinate.self, from: ["latitude": 90.01, "longitude": 127])) {
            XCTAssertEqual($0 as? MapInputError, .invalidInput)
        }
    }

    func testBooleanCannotBeUsedAsCoordinate() {
        XCTAssertThrowsError(try MapInput.decode(MapCoordinate.self, from: ["latitude": true, "longitude": 127])) {
            XCTAssertEqual($0 as? MapInputError, .invalidInput)
        }
    }

    func testDuplicateStationIDsAreRejectedBeforeMarkersAreChanged() {
        let first: [String: Any] = ["id": 1, "name": "충전소1", "latitude": 37.5, "longitude": 127]
        let duplicate: [String: Any] = ["id": 1, "name": "충전소2", "latitude": 37.6, "longitude": 127.1]
        XCTAssertThrowsError(try MapInput.decode(MapStationsInput.self, from: ["stations": [first, duplicate]])) {
            XCTAssertEqual($0 as? MapInputError, .invalidInput)
        }
    }

    func testEmptyStationsClearMarkers() throws {
        let input = try MapInput.decode(MapStationsInput.self, from: ["stations": []])
        XCTAssertTrue(input.stations.isEmpty)
    }

    func testNegativeFrameSizeIsRejected() {
        let frame: [String: Any] = ["x": 16, "y": 600, "width": -1, "height": 280]
        XCTAssertThrowsError(try MapInput.decode(MapLayoutInput.self, from: ["frame": frame, "clip": frame, "visible": true])) {
            XCTAssertEqual($0 as? MapInputError, .invalidInput)
        }
    }

    func testReversedAreaIsRejected() {
        XCTAssertThrowsError(try MapInput.decode(MapAreaInput.self,
            from: ["south": 37.6, "north": 37.5, "west": 126.9, "east": 127.1])) {
            XCTAssertEqual($0 as? MapInputError, .invalidInput)
        }
    }
}
