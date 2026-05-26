package com.auction.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class ServerApplicationTest {

    @Test
    void constructor_shouldCreateApplicationInstance() {
        ServerApplication application = new ServerApplication();

        assertNotNull(application);
    }

    @Test
    void main_shouldDelegateToSpringApplicationRun() {
        String[] args = {"--spring.profiles.active=test"};
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(ServerApplication.class, args))
                    .thenReturn(context);

            ServerApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(ServerApplication.class, args));
        }
    }
}
