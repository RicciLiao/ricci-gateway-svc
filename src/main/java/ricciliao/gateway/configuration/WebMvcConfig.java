package ricciliao.gateway.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ricciliao.gateway.filter.GatewayGlobalFilter;

@Configuration
public class WebMvcConfig {

    @Bean
    public GatewayGlobalFilter gatewayGlobalFilter() {

        return new GatewayGlobalFilter();
    }

}
