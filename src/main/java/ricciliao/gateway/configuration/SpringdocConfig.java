package ricciliao.gateway.configuration;


import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import ricciliao.gateway.filter.SpringdocModifyGatewayFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springdoc.core.utils.Constants.DEFAULT_API_DOCS_URL;

@Configuration
public class SpringdocConfig {

    @Bean
    public Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrlSet(RouteDefinitionLocator locator,
                                                                           SwaggerUiConfigProperties swaggerUiConfigProperties,
                                                                           @Autowired SpringDocConfigProperties springDocConfigProperties) {
        springDocConfigProperties.getWebjars().setPrefix("");
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();
        List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();
        if (definitions != null) {
            definitions.stream()
                    .filter(routeDefinition -> routeDefinition.getId().matches(".*-svc$"))
                    .forEach(routeDefinition -> {
                        String name = routeDefinition.getId().replace("-svc", "");
                        AbstractSwaggerUiConfigProperties.SwaggerUrl swaggerUrl =
                                new AbstractSwaggerUiConfigProperties.SwaggerUrl(name, "/" + name + DEFAULT_API_DOCS_URL, null);
                        urls.add(swaggerUrl);
                    });
        }
        swaggerUiConfigProperties.setUrls(urls);

        return urls;
    }


    @Bean
    public SpringdocModifyGatewayFilter springdocModifyGatewayFilter(@Autowired SpringDocConfigProperties springDocConfigProperties,
                                                                     @Autowired Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrlSet) {

        return new SpringdocModifyGatewayFilter(
                Ordered.HIGHEST_PRECEDENCE + 3,
                springDocConfigProperties,
                swaggerUrlSet
        );
    }

}
