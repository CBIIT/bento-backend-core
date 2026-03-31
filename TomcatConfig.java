package gov.nih.nci.bento;

import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            for (org.apache.catalina.Valve valve : context.getParent().getPipeline().getValves()) {
                if (valve instanceof ErrorReportValve errorReportValve) {
                    errorReportValve.setShowReport(false);
                    errorReportValve.setShowServerInfo(false);
                    return;
                }
            }
        });
    }
}
