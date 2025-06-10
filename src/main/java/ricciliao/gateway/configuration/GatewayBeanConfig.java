package ricciliao.gateway.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import ricciliao.gateway.filter.GatewayGlobalFilter;
import ricciliao.gateway.filter.SpringdocModifyGatewayFilterFactory;
import ricciliao.x.log.pojo.MdcSupportGlobalFilter;

@Configuration
public class GatewayBeanConfig {

    @Bean
    public GatewayGlobalFilter gatewayGlobalFilter() {

        return new GatewayGlobalFilter(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Bean
    public MdcSupportGlobalFilter mdcSupportGlobalFilter() {

        return new MdcSupportGlobalFilter(Ordered.HIGHEST_PRECEDENCE);
    }

    @Bean
    public SpringdocModifyGatewayFilterFactory springdocModifyGatewayFilterFactory() {

        return new SpringdocModifyGatewayFilterFactory(Ordered.HIGHEST_PRECEDENCE + 2);
    }

}
