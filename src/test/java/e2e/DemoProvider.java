package e2e;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Objects;

/** Synthetic HTTP provider, confined to the test classpath. */
public final class DemoProvider implements AutoCloseable {
    private final HttpServer server;

    public DemoProvider() throws IOException {
        byte[] response;
        try (InputStream fixture = Objects.requireNonNull(getClass().getResourceAsStream("/publicdata/browser-demo.xml"))) {
            response = fixture.readAllBytes();
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fixture", exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "application/xml");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        server.start();
    }

    public URI endpoint() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/fixture"); }
    @Override public void close() { server.stop(0); }
}
