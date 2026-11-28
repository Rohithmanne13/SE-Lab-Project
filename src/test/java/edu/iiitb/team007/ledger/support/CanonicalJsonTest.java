package edu.iiitb.team007.ledger.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Canonical JSON: keys sorted at every level, no insignificant whitespace (FR-2.4). */
class CanonicalJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sortsKeysAtEveryLevelAndRemovesWhitespace() throws Exception {
        String json = "{ \"b\" : 1, \"a\" : { \"z\" : [3, {\"y\":1, \"x\":2}], \"c\" : \"text\" } }";
        assertThat(CanonicalJson.of(mapper.readTree(json)))
                .isEqualTo("{\"a\":{\"c\":\"text\",\"z\":[3,{\"x\":2,\"y\":1}]},\"b\":1}");
    }

    @Test
    void keepsNullsAndEscapesStrings() throws Exception {
        assertThat(CanonicalJson.of(mapper.readTree("{\"d\":null,\"q\":\"a\\\"b\"}")))
                .isEqualTo("{\"d\":null,\"q\":\"a\\\"b\"}");
    }
}
