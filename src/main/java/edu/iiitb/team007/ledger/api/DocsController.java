package edu.iiitb.team007.ledger.api;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Interactive API documentation in development mode (UI-1, SE-7): GET /docs is Swagger UI and GET /openapi.yaml is
 * the OpenAPI file it renders - the build copies docs/api/openapi.yaml into the jar. Neither needs an API key
 * (FR-0.1). LEDGER_DOCS_ENABLED=false turns both off outside development.
 */
@RestController
public class DocsController {

    private final boolean enabled;

    public DocsController(@Value("${ledger.docs-enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @GetMapping(value = "/docs", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> docs() throws IOException, NoResourceFoundException {
        return serve("docs/index.html", MediaType.TEXT_HTML, "/docs");
    }

    @GetMapping(value = "/openapi.yaml", produces = "application/yaml")
    public ResponseEntity<byte[]> openApi() throws IOException, NoResourceFoundException {
        return serve("openapi/openapi.yaml", MediaType.parseMediaType("application/yaml"), "/openapi.yaml");
    }

    private ResponseEntity<byte[]> serve(String resource, MediaType type, String path)
            throws IOException, NoResourceFoundException {
        ClassPathResource file = new ClassPathResource(resource);
        if (!enabled || !file.exists()) {
            throw new NoResourceFoundException(HttpMethod.GET, path);
        }
        try (InputStream in = file.getInputStream()) {
            return ResponseEntity.ok().contentType(type).body(in.readAllBytes());
        }
    }
}
