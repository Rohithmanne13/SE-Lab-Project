package edu.iiitb.team007.ledger.api;

import static org.assertj.core.api.Assertions.assertThat;

import edu.iiitb.team007.ledger.testsupport.ApiClient;
import edu.iiitb.team007.ledger.testsupport.IntegrationTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Swagger UI at /docs generated from the OpenAPI file, reachable without an API key (UI-1, FR-0.1). */
class DocsApiTest extends IntegrationTestSupport {

    @Test
    void docsPageLoadsSwaggerUiForTheOpenApiFileWithoutAKey() {
        ApiClient.Response page = api().get("/docs", null);
        assertThat(page.status()).isEqualTo(200);
        assertThat(page.header("Content-Type")).startsWith("text/html");
        assertThat(page.body()).contains("swagger-ui", "url: \"/openapi.yaml\"");
        Matcher assets = Pattern.compile("(?:src|href)=\"(/webjars/[^\"]+)\"").matcher(page.body());
        int found = 0;
        while (assets.find()) {
            assertThat(api().get(assets.group(1), null).status()).as(assets.group(1)).isEqualTo(200);
            found++;
        }
        assertThat(found).isEqualTo(2);
    }

    @Test
    void openApiFileIsServedExactlyAsInTheRepository() throws Exception {
        ApiClient.Response yaml = api().get("/openapi.yaml", null);
        assertThat(yaml.status()).isEqualTo(200);
        assertThat(yaml.header("Content-Type")).startsWith("application/yaml");
        assertThat(yaml.body()).isEqualTo(Files.readString(Path.of("docs", "api", "openapi.yaml")));
    }

    @Test
    void otherWebjarPathsStillNeedAKey() {
        assertThat(api().get("/webjars/other/1.0/x.js", null).status()).isEqualTo(401);
    }
}
