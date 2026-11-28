package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** System tests of users through HTTP (FR-1.1, FR-1.2, FR-1.9, FR-6.1, TQ-3, TQ-4). */
class UsersApiTest extends IntegrationTestSupport {

    private ApiClient.Response createUser(String json) {
        return api().post("/api/v1/users", CLIENT, json);
    }

    @Test
    void createsAUserWith201LocationAndAllFields() {
        String email = uniqueEmail();
        ApiClient.Response r = createUser("{\"name\":\"  Alice Example  \",\"email\":\"" + email + "\"}");

        assertThat(r.status()).isEqualTo(201);
        JsonNode u = r.json();
        assertThat(u.get("user_id").asText())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
        assertThat(u.get("name").asText()).as("name is trimmed").isEqualTo("Alice Example");
        assertThat(u.get("email").asText()).isEqualTo(email);
        assertThat(u.get("created_at").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
        assertThat(u.size()).isEqualTo(4);
        assertThat(r.header("Location")).isEqualTo("/api/v1/users/" + u.get("user_id").asText());
        assertThat(r.header("Content-Type")).startsWith("application/json");
    }

    @Test
    void getReturnsTheUser() {
        String id = newUser();
        ApiClient.Response r = api().get("/api/v1/users/" + id, CLIENT);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.json().get("user_id").asText()).isEqualTo(id);
        assertThat(api().get("/api/v1/users/" + id.toUpperCase(), CLIENT).status()).isEqualTo(200);
    }

    @Test
    void duplicateEmailIgnoringCaseIs409() {
        String email = uniqueEmail();
        assertThat(createUser("{\"name\":\"A\",\"email\":\"" + email + "\"}").status()).isEqualTo(201);
        assertProblem(createUser("{\"name\":\"B\",\"email\":\"" + email.toUpperCase() + "\"}"), 409,
                "EMAIL_ALREADY_EXISTS");
    }

    @Test
    void nameMustBeOneToHundredCharactersAfterTrimming() {
        assertThat(createUser("{\"name\":\"" + "n".repeat(100) + "\",\"email\":\"" + uniqueEmail() + "\"}").status())
                .isEqualTo(201);
        assertThat(createUser("{\"name\":\"  " + "n".repeat(100) + "  \",\"email\":\"" + uniqueEmail() + "\"}")
                .status()).isEqualTo(201);
        for (String name : new String[] {"\"\"", "\"   \"", "\"" + "n".repeat(101) + "\"", "null", "42", "true"}) {
            ApiClient.Response r = createUser("{\"name\":" + name + ",\"email\":\"" + uniqueEmail() + "\"}");
            assertProblem(r, 400, "VALIDATION_ERROR");
            assertThat(r.json().get("errors").get(0).get("field").asText()).isEqualTo("name");
        }
    }

    @Test
    void emailMustBeValidAndAtMost254Characters() {
        String local = "a".repeat(64);
        String domain = "d".repeat(63) + "." + "e".repeat(63) + "." + "f".repeat(57) + ".com";
        String max = local + "@" + domain;
        assertThat(max).hasSize(254);
        assertThat(createUser("{\"name\":\"Long\",\"email\":\"" + max + "\"}").status()).isEqualTo(201);
        for (String email : new String[] {"\"plainaddress\"", "\"a@b\"", "\"a@@b.com\"", "\"a b@c.com\"",
            "\"@example.com\"", "\"x" + max + "\"", "\"\"", "null", "7"}) {
            ApiClient.Response r = createUser("{\"name\":\"N\",\"email\":" + email + "}");
            assertProblem(r, 400, "VALIDATION_ERROR");
            assertThat(r.json().get("errors").get(0).get("field").asText()).isEqualTo("email");
        }
    }

    @Test
    void missingFieldsAreListedTogether() {
        ApiClient.Response r = assertProblem(createUser("{}"), 400, "VALIDATION_ERROR");
        assertThat(r.json().get("errors")).hasSize(2);
        assertThat(r.json().get("errors").get(0).get("field").asText()).isEqualTo("email");
        assertThat(r.json().get("errors").get(1).get("field").asText()).isEqualTo("name");
    }

    @Test
    void unknownFieldsInvalidJsonAndNonObjectsAreRejected() {
        ApiClient.Response unknown = createUser("{\"name\":\"A\",\"email\":\"" + uniqueEmail() + "\",\"role\":\"x\"}");
        assertProblem(unknown, 400, "VALIDATION_ERROR");
        assertThat(unknown.json().get("errors").get(0).get("field").asText()).isEqualTo("role");
        assertProblem(createUser("{\"name\":"), 400, "VALIDATION_ERROR");
        assertProblem(createUser("[1,2]"), 400, "VALIDATION_ERROR");
        assertProblem(createUser(""), 400, "VALIDATION_ERROR");
        assertProblem(createUser("{\"name\":\"A\",\"name\":\"B\",\"email\":\"" + uniqueEmail() + "\"}"), 400,
                "VALIDATION_ERROR");
        assertProblem(createUser("{\"name\":\"A\",\"email\":\"" + uniqueEmail() + "\"} {}"), 400, "VALIDATION_ERROR");
    }

    @Test
    void malformedIdIs400AndUnknownIdIs404() {
        assertProblem(api().get("/api/v1/users/not-a-uuid", CLIENT), 400, "INVALID_ID");
        assertProblem(api().get("/api/v1/users/123", CLIENT), 400, "INVALID_ID");
        assertProblem(api().get("/api/v1/users/" + UUID.randomUUID(), CLIENT), 404, "USER_NOT_FOUND");
    }

    @Test
    void textIsStoredLiterallyNeverAsSql() {
        String name = "Robert'); DROP TABLE users;--";
        ApiClient.Response r = createUser("{\"name\":\"" + name + "\",\"email\":\"" + uniqueEmail() + "\"}");
        assertThat(r.status()).isEqualTo(201);
        assertThat(api().get("/api/v1/users/" + r.json().get("user_id").asText(), CLIENT).json().get("name").asText())
                .isEqualTo(name);
    }
}
