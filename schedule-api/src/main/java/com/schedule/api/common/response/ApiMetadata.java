package com.schedule.api.common.response;

import java.time.Instant;

public record ApiMetadata(
        Instant timestamp
) {

    public static ApiMetadata now() {
        return new ApiMetadata(Instant.now());
    }
}
