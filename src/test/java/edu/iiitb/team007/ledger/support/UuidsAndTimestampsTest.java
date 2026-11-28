package edu.iiitb.team007.ledger.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Identifier parsing (FR-1.9) and RFC 3339 timestamps from the clock provider (DR-5, SE-6). */
class UuidsAndTimestampsTest {

    @Test
    void acceptsCanonicalUuidsAndNormalisesToLowerCase() {
        assertThat(Uuids.parse("3F0C2B1A-0000-4000-8000-000000000E21"))
                .contains(UUID.fromString("3f0c2b1a-0000-4000-8000-000000000e21"));
        assertThat(Uuids.parse("3f0c2b1a-0000-4000-8000-000000000e21").orElseThrow().toString())
                .isEqualTo("3f0c2b1a-0000-4000-8000-000000000e21");
    }

    @Test
    void rejectsMalformedIdentifiers() {
        assertThat(Uuids.parse(null)).isEmpty();
        assertThat(Uuids.parse("")).isEmpty();
        assertThat(Uuids.parse("not-a-uuid")).isEmpty();
        assertThat(Uuids.parse("3f0c2b1a00004000800000000000e21")).isEmpty();
        assertThat(Uuids.parse("1-1-1-1-1")).isEmpty();
        assertThat(Uuids.parse("3f0c2b1a-0000-4000-8000-000000000e21 ")).isEmpty();
        assertThat(Uuids.parse("{3f0c2b1a-0000-4000-8000-000000000e21}")).isEmpty();
    }

    @Test
    void timestampsAreUtcWithMillisecondsAndZ() {
        Clock fixed = Clock.fixed(Instant.parse("2026-11-23T10:15:30.123456Z"), ZoneOffset.UTC);
        assertThat(Timestamps.now(fixed)).isEqualTo("2026-11-23T10:15:30.123Z");
        assertThat(Timestamps.format(Instant.parse("2026-01-02T03:04:05Z"))).isEqualTo("2026-01-02T03:04:05.000Z");
    }
}
