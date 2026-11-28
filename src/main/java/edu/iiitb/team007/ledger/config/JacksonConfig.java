package edu.iiitb.team007.ledger.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Strict JSON (ADR-0004 decision 4, FR-0.2, CI-2, TQ-6): snake_case field names, unknown fields rejected,
 * duplicate keys and trailing content rejected, and no coercion - an amount must be a JSON integer, never a
 * fraction, an exponent, a string or a boolean; a text field must be a JSON string.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictJson() {
        return builder -> builder
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                        JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT,
                        MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .postConfigurer(mapper -> {
                    mapper.coercionConfigFor(LogicalType.Integer)
                            .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
                    mapper.coercionConfigFor(LogicalType.Textual)
                            .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
                });
    }
}
