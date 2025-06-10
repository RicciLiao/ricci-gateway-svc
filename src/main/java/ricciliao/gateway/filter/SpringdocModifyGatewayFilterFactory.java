package ricciliao.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

public class SpringdocModifyGatewayFilterFactory extends AbstractGatewayFilterFactory<SpringdocModifyGatewayFilterFactory.Config> {

    private final SpringdocModifyGatewayFilter gatewayFilter;

    public SpringdocModifyGatewayFilterFactory(Integer order) {
        super(SpringdocModifyGatewayFilterFactory.Config.class);

        this.gatewayFilter = new SpringdocModifyGatewayFilter(order);
    }

    @Override
    public GatewayFilter apply(Config config) {
        if (config.enabled) {

            return this.gatewayFilter;
        }

        return (exchange, chain) -> chain.filter(exchange);
    }

    public static class Config {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

}
