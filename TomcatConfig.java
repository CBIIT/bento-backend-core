package gov.nih.nci.bento;

import org.apache.catalina.valves.ErrorReportValve;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            Rfc6265CookieProcessor cookieProcessor = new Rfc6265CookieProcessor();
            cookieProcessor.setSameSiteCookies("Lax");
            context.setCookieProcessor(cookieProcessor);

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
