package edu.iiitb.team007.ledger.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import edu.iiitb.team007.ledger.domain.FieldProblem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Reads request bodies strictly before any business logic (FR-0.2, SEC-1, ADR-0004): Content-Type must be
 * application/json (415), the body must be one JSON object of the request's schema - no unknown fields, no type
 * coercion - and it must pass the Bean Validation rules of the request record (400 VALIDATION_ERROR with the
 * offending fields).
 */
@Component
public class JsonBodies {

    /** A body as parsed JSON (for the fingerprint, FR-2.4) and as the validated request record. */
    public record Parsed<T>(JsonNode tree, T value) {
    }

    private final ObjectMapper mapper;
    private final Validator validator;

    public JsonBodies(ObjectMapper mapper, Validator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    /** 415 UNSUPPORTED_MEDIA_TYPE unless the request declares Content-Type application/json (UTF-8). */
    public void requireJson(HttpServletRequest request) {
        String header = request.getContentType();
        boolean ok = false;
        if (header != null) {
            try {
                MediaType type = MediaType.parseMediaType(header);
                ok = MediaType.APPLICATION_JSON.equalsTypeAndSubtype(type)
                        && (type.getCharset() == null || StandardCharsets.UTF_8.equals(type.getCharset()));
            } catch (InvalidMediaTypeException e) {
                ok = false; // an unparsable Content-Type is not application/json
            }
        }
        if (!ok) {
            throw new ApiException(ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                    "Send the request body as JSON with the header Content-Type: application/json.");
        }
    }

    public <T> Parsed<T> read(byte[] body, Class<T> type) {
        if (body == null || body.length == 0) {
            throw invalid("The request body is required.", List.of(new FieldProblem("body", "is required")));
        }
        JsonNode tree;
        try {
            tree = mapper.readTree(body);
        } catch (IOException e) {
            throw invalid("The request body is not valid JSON.",
                    List.of(new FieldProblem("body", "is not valid JSON")));
        }
        if (tree == null || !tree.isObject()) {
            throw invalid("The request body must be a JSON object.",
                    List.of(new FieldProblem("body", "must be a JSON object")));
        }
        T value;
        try {
            value = mapper.treeToValue(tree, type);
        } catch (UnrecognizedPropertyException e) {
            throw invalid("The request body has an unknown field; remove it.",
                    List.of(new FieldProblem(e.getPropertyName(), "is not a known field")));
        } catch (JsonMappingException e) {
            throw invalid("A field of the request body has the wrong type.",
                    List.of(new FieldProblem(fieldOf(e, tree), typeMessage(fieldOf(e, tree), type))));
        } catch (JsonProcessingException e) {
            throw invalid("A field of the request body has the wrong type.",
                    List.of(new FieldProblem(outOfRangeField(tree), "is not a valid value")));
        }
        List<FieldProblem> problems = new ArrayList<>();
        for (ConstraintViolation<T> v : validator.validate(value)) {
            problems.add(new FieldProblem(snakeCase(v.getPropertyPath().toString()), v.getMessage()));
        }
        if (!problems.isEmpty()) {
            problems.sort(Comparator.comparing(FieldProblem::field).thenComparing(FieldProblem::message));
            throw invalid(problems.size() == 1 ? "One field of the request body is invalid; see errors."
                    : problems.size() + " fields of the request body are invalid; see errors.", problems);
        }
        return new Parsed<>(tree, value);
    }

    private static ApiException invalid(String detail, List<FieldProblem> problems) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, detail, problems);
    }

    private static String fieldOf(JsonMappingException e, JsonNode tree) {
        List<JsonMappingException.Reference> path = e.getPath();
        for (int i = path.size() - 1; i >= 0; i--) {
            if (path.get(i).getFieldName() != null) {
                return path.get(i).getFieldName();
            }
        }
        return outOfRangeField(tree);
    }

    /** The first numeric field that does not fit a long, or "body". */
    private static String outOfRangeField(JsonNode tree) {
        Iterator<Map.Entry<String, JsonNode>> fields = tree.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> f = fields.next();
            if (f.getValue().isNumber() && !f.getValue().canConvertToExactIntegral()) {
                return f.getKey();
            }
            if (f.getValue().isIntegralNumber() && !f.getValue().canConvertToLong()) {
                return f.getKey();
            }
        }
        return "body";
    }

    private static String typeMessage(String field, Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        for (RecordComponent component : components == null ? new RecordComponent[0] : components) {
            if (snakeCase(component.getName()).equals(field)) {
                return component.getType() == Long.class
                        ? "must be a JSON integer (no quotes, fraction or exponent)" : "must be a JSON string";
            }
        }
        return "has the wrong type";
    }

    /** sourceWalletId -> source_wallet_id, matching the JSON field names. */
    static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
