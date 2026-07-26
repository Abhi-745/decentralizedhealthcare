package com.example.VeristasId.Dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class OpaRequest {
    private Input input;

    @Data
    @Builder
    public static class Input {
        private User user;
        private Session emergency_session;
        private String action;
    }

    @Data
    @Builder
    public static class User {
        private String role_tag;
        private boolean shift_active;
        private String userId;
    }

    @Data
    @Builder
    public static class Session {
        private String stage;
        private String esid;
    }
}