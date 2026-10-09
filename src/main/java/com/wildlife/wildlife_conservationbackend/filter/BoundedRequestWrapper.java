package com.wildlife.wildlife_conservationbackend.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

final class BoundedRequestWrapper extends ContentCachingRequestWrapper {
    private boolean truncated;

    BoundedRequestWrapper(HttpServletRequest request, int limit) {
        super(request, limit);
    }

    @Override
    protected void handleContentOverflow(int contentCacheLimit) {
        truncated = true;
    }

    boolean isTruncated() {
        return truncated;
    }
}
