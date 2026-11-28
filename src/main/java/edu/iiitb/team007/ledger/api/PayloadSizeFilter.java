package edu.iiitb.team007.ledger.api;

import edu.iiitb.team007.ledger.domain.ApiException;
import edu.iiitb.team007.ledger.domain.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects bodies larger than 16 KiB with 413 PAYLOAD_TOO_LARGE without processing them (FR-0.5). A declared
 * Content-Length above the limit is refused at once; a body without a length is counted while it is read.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PayloadSizeFilter extends OncePerRequestFilter {

    /** 16 KiB. */
    public static final int MAX_BYTES = 16 * 1024;

    private final ProblemWriter problems;

    public PayloadSizeFilter(ProblemWriter problems) {
        this.problems = problems;
    }

    /** The 413 problem sent for an oversized body. */
    public static ApiException tooLarge() {
        return new ApiException(ErrorCode.PAYLOAD_TOO_LARGE,
                "The request body is larger than 16 KiB (16384 bytes); send a smaller body.");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BYTES) {
            problems.write(request, response, tooLarge());
            return;
        }
        chain.doFilter(new LimitedRequest(request), response);
    }

    /** Request whose body stream fails once more than {@link #MAX_BYTES} bytes are read. */
    private static final class LimitedRequest extends HttpServletRequestWrapper {

        private ServletInputStream limited;

        LimitedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (limited == null) {
                limited = new LimitedInputStream(super.getInputStream());
            }
            return limited;
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream in;
        private long count;

        LimitedInputStream(ServletInputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            int b = in.read();
            if (b >= 0) {
                count(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = in.read(buffer, offset, length);
            if (n > 0) {
                count(n);
            }
            return n;
        }

        private void count(int n) throws IOException {
            count += n;
            if (count > MAX_BYTES) {
                throw new PayloadTooLargeException();
            }
        }

        @Override
        public boolean isFinished() {
            return in.isFinished();
        }

        @Override
        public boolean isReady() {
            return in.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            in.setReadListener(listener);
        }
    }
}
