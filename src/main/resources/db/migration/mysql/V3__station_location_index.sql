-- Additive: lets the bounding-box candidate query range-scan station coordinates instead of the whole table.
CREATE INDEX station_location ON station (latitude, longitude);
