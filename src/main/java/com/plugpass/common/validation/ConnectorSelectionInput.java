package com.plugpass.common.validation;

import com.plugpass.station.domain.Connector;
import java.util.List;

public interface ConnectorSelectionInput {
    Connector connector();
    List<Connector> connectors();
}
