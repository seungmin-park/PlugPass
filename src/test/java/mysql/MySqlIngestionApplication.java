package mysql;

import com.plugpass.PlugPassApplication;
import e2e.ChargingDemoApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Real HTTP ingestion with the existing synthetic provider; never shipped. */
public final class MySqlIngestionApplication {
    public static void main(String[] arguments) {
        new SpringApplicationBuilder(PlugPassApplication.class, ChargingDemoApplication.DemoConfiguration.class,
                MySqlPersistenceConfiguration.class).run(arguments);
    }
}
