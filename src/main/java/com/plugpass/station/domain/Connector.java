package com.plugpass.station.domain;

import java.util.Set;

public enum Connector {
    DC_CHADEMO(Set.of("01","03","05","06")), AC_SLOW(Set.of("02")),
    AC_THREE_PHASE(Set.of("03","06","07")), DC_COMBO(Set.of("04","05","06","08","10")), NACS(Set.of("09","10"));
    private final Set<String> providerCodes;
    Connector(Set<String> providerCodes) { this.providerCodes = providerCodes; }
    public Set<String> providerCodes() { return providerCodes; }
    public boolean matches(String providerCode) { return providerCode != null && providerCodes.contains(providerCode); }
}
