package ricciliao.gateway.filter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class GatewayGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(GatewayGlobalFilter.class);

    private final Integer order;

    public GatewayGlobalFilter(Integer order) {
        super();

        this.order = order;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange.Builder mutateBuilder = exchange.mutate().request(
                exchange.getRequest().mutate()
                        .header("GATEWAY_GLOBAL_FILTER", Boolean.TRUE.toString())
                        .build()
        );

        final String requestUrl = exchange.getRequest().getURI().getPath();
        logger.info("Gateway Route Request: Pre Route to {}.", requestUrl);

        return chain
                .filter(mutateBuilder.build())
                .doOnSuccess(result -> logger.info("Gateway Route Result: Post Route to {}.", requestUrl));
    }

    @Override
    public int getOrder() {

        return order;
    }
}
