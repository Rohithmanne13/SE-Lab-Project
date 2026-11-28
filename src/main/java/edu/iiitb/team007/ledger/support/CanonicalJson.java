package edu.iiitb.team007.ledger.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Canonical JSON for request fingerprints (FR-2.4, ADR-0002): the parsed body re-serialised with object keys
 * sorted at every level and no insignificant whitespace.
 */
public final class CanonicalJson {

    private static final ObjectMapper SORTED = JsonMapper.builder()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private CanonicalJson() {
    }

    public static String of(JsonNode body) {
        try {
            Object plain = SORTED.convertValue(body, Object.class);
            return SORTED.writeValueAsString(plain);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Body cannot be written as canonical JSON", e);
        }
    }
}
