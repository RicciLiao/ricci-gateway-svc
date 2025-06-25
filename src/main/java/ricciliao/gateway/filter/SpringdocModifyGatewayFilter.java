package ricciliao.gateway.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.annotation.Nonnull;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ricciliao.gateway.common.GatewayConstants;
import ricciliao.x.log.AuditLoggerFactory;
import ricciliao.x.log.logger.AuditLogger;

import java.nio.charset.StandardCharsets;

public class SpringdocModifyGatewayFilter implements GatewayFilter, Ordered {

    private static final AuditLogger logger = AuditLoggerFactory.getLogger(SpringdocModifyGatewayFilter.class);

    private final Integer order;

    private final TypeReference<OpenAPI> typeReference = new TypeReference<>() {
    };

    public SpringdocModifyGatewayFilter(Integer order) {
        super();

        this.order = order;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return chain.filter(exchange.mutate().response(this.responseDecorator(exchange)).build());
    }

    @Override
    public int getOrder() {

        return order;
    }

    private ServerHttpResponseDecorator responseDecorator(ServerWebExchange exchange) {
        String uriString = exchange.getRequest().getURI().toString();
        String gatewayUrl = uriString.substring(0, uriString.lastIndexOf(GatewayConstants.API_DOCS_URL));
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();

        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Nonnull
            @Override
            public Mono<Void> writeWith(@Nonnull Publisher<? extends DataBuffer> body) {
                if (!(body instanceof Flux<? extends DataBuffer> fluxBody)) {

                    return super.writeWith(body);
                }

                return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                    DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                    DataBuffer join = dataBufferFactory.join(dataBuffers);
                    byte[] content = new byte[join.readableByteCount()];
                    join.read(content);
                    DataBufferUtils.release(join);
                    try {
                        String responseData = new String(content, StandardCharsets.UTF_8);
                        OpenAPI openAPI = ObjectMapperFactory.createJsonConverter().readValue(responseData, typeReference);
                        if (!CollectionUtils.isEmpty(openAPI.getServers())) {
                            for (Server server : openAPI.getServers()) {
                                server.setUrl(gatewayUrl);
                                server.setDescription("Gateway server url");
                            }
                            byte[] uppedContent = ObjectMapperFactory.createJsonConverter().writeValueAsString(openAPI).getBytes(StandardCharsets.UTF_8);
                            exchange.getResponse().getHeaders().setContentLength(uppedContent.length);

                            return bufferFactory.wrap(uppedContent);
                        }
                    } catch (Exception e) {
                        logger.error("Cannot overwrite Springdoc.", e);
                    }

                    return bufferFactory.wrap(content);
                }));
            }
        };
    }

}

