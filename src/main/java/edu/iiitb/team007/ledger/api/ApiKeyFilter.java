package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.config.ApiKeys;
import edu.iiitb.team007.ledger.config.ApiRole;
import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authentication and authorisation (FR-0.1, SEC-4, SRS 3.3.3): every request except GET /health needs an
 * X-API-Key matching a configured key - missing or unknown is 401 UNAUTHENTICATED. Deposits, reversals and
 * reconciliation are ADMIN-only - a CLIENT key there is 403 FORBIDDEN. These checks run before any validation
 * (SRS 4.7.2).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class ApiKeyFilter extends OncePerRequestFilter {

    /** Header name (SRS 3.3.3). */
    public static final String HEADER = "X-API-Key";

    private static final Pattern ADMIN_PATHS =
            Pattern.compile("^/api/v1/(admin(/.*)?|wallets/[^/]+/deposits|transactions/[^/]+/reversal)$");

    private final ApiKeys keys;
    private final ProblemWriter problems;

    public ApiKeyFilter(ApiKeys keys, ProblemWriter problems) {
        this.keys = keys;
        this.problems = problems;
    }

    /** Paths reachable without a key: the health check (FR-6.4). */
    static boolean isPublic(HttpServletRequest request) {
        return "/health".equals(request.getRequestURI());
    }

    /** True for the administrative operations (BR-5). */
    static boolean isAdminPath(String path) {
        return ADMIN_PATHS.matcher(path).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isPublic(request)) {
            chain.doFilter(request, response);
            return;
        }
        Optional<ApiRole> role = keys.roleOf(request.getHeader(HEADER));
        if (role.isEmpty()) {
            problems.write(request, response, new ApiException(ErrorCode.UNAUTHENTICATED,
                    "Send a valid API key in the X-API-Key header."));
            return;
        }
        if (role.get() == ApiRole.CLIENT && isAdminPath(request.getRequestURI())) {
            problems.write(request, response, new ApiException(ErrorCode.FORBIDDEN,
                    "This operation needs an ADMIN key; CLIENT keys cannot deposit, reverse or reconcile."));
            return;
        }
        request.setAttribute(RequestAttributes.ROLE, role.get());
        chain.doFilter(request, response);
    }
}
