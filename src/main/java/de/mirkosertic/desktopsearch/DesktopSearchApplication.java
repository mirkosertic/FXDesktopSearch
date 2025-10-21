/*
 * FXDesktopSearch Copyright 2013 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.desktopsearch;

import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class DesktopSearchApplication extends Application {

    private ConfigurableApplicationContext context;

    public DesktopSearchApplication() {
        log.info("Creating DesktopSearchApplication");
    }

    @Override
    public void start(final Stage stage) {
        log.info("Starting JavaFX application");
        SplashScreen.showMe();

        log.info("Starting Spring Boot application");
        context = new SpringApplicationBuilder(DesktopSearchApplication.class)
                .initializers(applicationContext -> {
                    final ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
                    beanFactory.registerSingleton("stage", stage);
                    beanFactory.registerSingleton("application", DesktopSearchApplication.this);
                })
                .sources(DesktopSearchMain.class)
                .web(WebApplicationType.SERVLET)
                .registerShutdownHook(true)
                .run(getParameters().getRaw().toArray(new String[0]));

        log.info("Spring Boot application started");
    }

    @Override
    public void stop() {
        log.info("Stopping Spring Boot application");
        context.close();

        log.info("Terminating JVM");
        System.exit(0);
    }
}