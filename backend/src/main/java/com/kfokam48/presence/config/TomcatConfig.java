package com.kfokam48.presence.config;

import org.apache.catalina.core.StandardHost;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import com.kfokam48.presence.exception.ErreurJsonValve;

/** Erreurs rejetées par Tomcat lui-même : réponse JSON au format du contrat (B4). */
@Component
public class TomcatConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addContextCustomizers(context -> {
            if (context.getParent() instanceof StandardHost hote) {
                hote.setErrorReportValveClass(ErreurJsonValve.class.getName());
            }
        });
    }
}
