package ricciliao.gateway.filter;


import jakarta.annotation.Nonnull;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ricciliao.gateway.pojo.URIBo;

import java.util.ArrayList;
import java.util.List;

public class GatewayGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(GatewayGlobalFilter.class);
    private final List<URIBo> contentPass = new ArrayList<>();

    public GatewayGlobalFilter() {
        contentPass.add(new URIBo("/.*/swagger-ui/.*", HttpMethod.GET));
        contentPass.add(new URIBo("/.*/v3/api-docs/.*", HttpMethod.GET));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String requestUrl = exchange.getRequest().getURI().getPath();
        logger.info("Gateway Route Request: Pre Route to {}.", requestUrl);

        ServerWebExchange.Builder mutateBuilder = exchange.mutate().request(
                exchange.getRequest()
                        .mutate()
                        .header("GATEWAY_GLOBAL_FILTER", Boolean.TRUE.toString())
                        .build()
        );

        if (contentPass.stream().noneMatch(bo -> exchange.getRequest().getURI().getPath().matches(bo.getUriPath()))) {
            mutateBuilder.response(decorateResponse(exchange));
        }

        return chain
                .filter(mutateBuilder.build())
                .doOnSuccess(result -> logger.info("Gateway Route Result: Post Route to {}.", requestUrl));
    }

    private ServerHttpResponseDecorator decorateResponse(ServerWebExchange exchange) {
        ServerHttpResponse originalResponse = exchange.getResponse();

        return new ServerHttpResponseDecorator(originalResponse) {
            @Nonnull
            @Override
            public Mono<Void> writeWith(@Nonnull Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux<? extends DataBuffer> fluxBody) {

                    return fluxBody.collectList().flatMap(dataBuffers -> {
                        int totalSize = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
                        HttpHeaders originalHeaders = getDelegate().getHeaders();
                        originalHeaders.setContentLength(totalSize);
                        originalHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
                        Flux<DataBuffer> finalBody = Flux.fromIterable(dataBuffers);

                        return getDelegate().writeWith(finalBody);
                    });
                } else {

                    return getDelegate().writeWith(body);
                }
            }
        };
    }

    @Override
    public int getOrder() {

        return Ordered.HIGHEST_PRECEDENCE;
    }
}
