package com.parking.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    RouterFunction<ServerResponse> serviceRoutes() {
        return route("auth-service")
                .route(path("/api/auth/**"), http())
                .before(uri("lb://auth-service"))
                .build()
                .and(route("user-service")
                        .route(path("/api/users/**"), http())
                        .before(uri("lb://user-service"))
                        .build())
                // .and(route("vehicle-service")
                //         .route(path("/api/vehicles/**"), http(URI.create("lb://user-service")))
                //         .build())
                .and(route("parking-service")
                        .route(path("/api/parking/**"), http())
                         .before(uri("lb://parking-service"))
                        .build())
                .and(route("booking-service")
                        .route(path("/api/bookings/**"), http())
                         .before(uri("lb://booking-service"))
                        .build())
                .and(route("payment-service")
                        .route(path("/api/payments/**"), http())
                         .before(uri("lb://payment-service"))
                        .build());
    }
}
