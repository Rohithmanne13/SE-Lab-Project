package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.iiitb.team007.ledger.config.LedgerProperties;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.service.EntryPageView;
import edu.iiitb.team007.ledger.service.EntryView;
import edu.iiitb.team007.ledger.service.ReconciliationReport;
import edu.iiitb.team007.ledger.service.TransactionView;
import edu.iiitb.team007.ledger.service.UserView;
import edu.iiitb.team007.ledger.service.WalletListView;
import edu.iiitb.team007.ledger.service.WalletView;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.io.File;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The API contract docs/api/openapi.yaml matches the code (SE-7, CON-4, FR-0.2, FR-0.3, UI-1): the same operations,
 * request schemas with exactly the fields the service accepts (unknown fields rejected), response schemas with the
 * fields it returns, every error code, and the role and idempotency rules.
 */
class OpenApiContractTest extends IntegrationTestSupport {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Autowired
    private LedgerProperties properties;

    private JsonNode spec;

    @BeforeEach
    void readSpec() throws Exception {
        spec = new ObjectMapper(new YAMLFactory()).readTree(new File("docs/api/openapi.yaml"));
    }

    private JsonNode schema(String name) {
        JsonNode s = spec.at("/components/schemas/" + name);
        assertThat(s.isMissingNode()).as("schema " + name).isFalse();
        return s;
    }

    private static Set<String> jsonNames(Class<?> record) {
        return Arrays.stream(record.getRecordComponents()).map(RecordComponent::getName).map(JsonBodies::snakeCase)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> fieldNames(JsonNode properties) {
        Set<String> names = new TreeSet<>();
        properties.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @Test
    void isOpenApi31WithTheReleaseVersion() {
        assertThat(spec.get("openapi").asText()).startsWith("3.1.");
        assertThat(spec.at("/info/version").asText()).isEqualTo(properties.version());
        assertThat(spec.at("/components/securitySchemes/ApiKey/name").asText()).isEqualTo("X-API-Key");
    }

    @Test
    void documentsExactlyTheOperationsTheServiceHas() {
        Set<String> code = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> m : mappings.getHandlerMethods().entrySet()) {
            for (String path : m.getKey().getPatternValues()) {
                if (path.equals("/error") || path.equals("/docs") || path.equals("/openapi.yaml")) {
                    continue;
                }
                m.getKey().getMethodsCondition().getMethods().forEach(method -> code.add(method.name() + " " + path));
            }
        }
        Set<String> documented = new TreeSet<>();
        Iterator<Map.Entry<String, JsonNode>> paths = spec.get("paths").fields();
        while (paths.hasNext()) {
            Map.Entry<String, JsonNode> p = paths.next();
            p.getValue().fieldNames().forEachRemaining(method -> {
                if (HTTP_METHODS.contains(method)) {
                    documented.add(method.toUpperCase() + " " + p.getKey());
                }
            });
        }
        assertThat(documented).isEqualTo(code).hasSize(12);
    }

    @Test
    void requestSchemasListExactlyTheAcceptedFieldsAndRejectOthers() {
        Map<String, Class<?>> requests = Map.of("CreateUserRequest", CreateUserRequest.class,
                "CreateWalletRequest", CreateWalletRequest.class, "DepositRequest", DepositRequest.class,
                "TransferRequest", TransferRequest.class, "ReversalRequest", ReversalRequest.class);
        requests.forEach((name, type) -> {
            JsonNode s = schema(name);
            assertThat(fieldNames(s.get("properties"))).as(name).isEqualTo(jsonNames(type));
            assertThat(s.get("additionalProperties").asBoolean(true)).as(name + " rejects unknown fields").isFalse();
        });
    }

    @Test
    void responseSchemasListExactlyTheReturnedFields() {
        Map<String, Class<?>> responses = Map.of("User", UserView.class, "Wallet", WalletView.class,
                "WalletList", WalletListView.class, "Entry", EntryView.class, "EntryPage", EntryPageView.class,
                "Transaction", TransactionView.class, "ReconciliationReport", ReconciliationReport.class,
                "Discrepancy", ReconciliationReport.Discrepancy.class);
        responses.forEach((name, type) -> {
            JsonNode s = schema(name);
            assertThat(fieldNames(s.get("properties"))).as(name).isEqualTo(jsonNames(type));
            Set<String> required = new TreeSet<>();
            s.get("required").forEach(r -> required.add(r.asText()));
            assertThat(required).as(name + " required").isEqualTo(jsonNames(type));
        });
    }

    @Test
    void everyErrorCodeOfTheSrsIsDocumented() {
        Set<String> documented = new TreeSet<>();
        schema("Problem").at("/properties/code/enum").forEach(c -> documented.add(c.asText()));
        assertThat(documented).isEqualTo(Arrays.stream(ErrorCode.values()).map(Enum::name)
                .collect(Collectors.toCollection(TreeSet::new)));
    }

    @Test
    void adminAndIdempotencyRulesAreDocumented() {
        Set<String> admin = Set.of("/api/v1/wallets/{walletId}/deposits",
                "/api/v1/transactions/{transactionId}/reversal", "/api/v1/admin/reconciliation");
        Set<String> idempotent = Set.of("/api/v1/wallets/{walletId}/deposits", "/api/v1/transfers",
                "/api/v1/transactions/{transactionId}/reversal");
        spec.get("paths").fields().forEachRemaining(p -> p.getValue().fields().forEachRemaining(op -> {
            JsonNode o = op.getValue();
            String where = op.getKey() + " " + p.getKey();
            assertThat(o.at("/responses/403").isMissingNode()).as(where + " 403")
                    .isEqualTo(!admin.contains(p.getKey()));
            if (admin.contains(p.getKey())) {
                assertThat(o.get("description").asText()).as(where).contains("Role ADMIN");
            }
            boolean hasKey = false;
            for (JsonNode param : o.path("parameters")) {
                hasKey |= param.path("$ref").asText().endsWith("/Idempotency-Key");
            }
            assertThat(hasKey).as(where + " Idempotency-Key").isEqualTo(idempotent.contains(p.getKey()));
            if (op.getKey().equals("post")) {
                assertThat(o.at("/responses/201").isMissingNode()).as(where + " 201").isFalse();
            }
        }));
        assertThat(spec.at("/paths/~1health/get/security")).isEmpty();
    }
}
