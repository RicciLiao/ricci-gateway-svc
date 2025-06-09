package ricciliao.gateway.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ricciliao.x.log.AuditLoggerFactory;
import ricciliao.x.log.logger.AuditLogger;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

public class SpringdocModifyGatewayFilter implements GlobalFilter, Ordered {

    private static final AuditLogger logger = AuditLoggerFactory.getLogger(SpringdocModifyGatewayFilter.class);

    private final Integer order;
    private final SpringDocConfigProperties springDocConfigProperties;
    private final Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrlSet;

    private final TypeReference<OpenAPI> typeReference = new TypeReference<>() {
    };

    public SpringdocModifyGatewayFilter(Integer order,
                                        SpringDocConfigProperties springDocConfigProperties,
                                        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrlSet) {
        super();

        this.order = order;
        this.springDocConfigProperties = springDocConfigProperties;
        this.swaggerUrlSet = swaggerUrlSet;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        if (Objects.nonNull(statusCode)
                && statusCode.value() != HttpStatus.OK.value()) {

            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        if (StringUtils.isBlank(path)
                || swaggerUrlSet.stream().noneMatch(url -> url.getUrl().equalsIgnoreCase(path))) {

            return chain.filter(exchange);
        }
        ServerHttpResponseDecorator decoratedResponse = this.responseDecorator(exchange);

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {

        return order;
    }

    private ServerHttpResponseDecorator responseDecorator(ServerWebExchange exchange) {
        String uriString = exchange.getRequest().getURI().toString();
        String gatewayUrl = uriString.substring(0, uriString.lastIndexOf(springDocConfigProperties.getApiDocs().getPath()));
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();

        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @NonNull
            @Override
            public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
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

