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
        private String action; // read, update, start_session, end_session
    }

    @Data
    @Builder
    public static class User {
        private String role_tag;     // From VC Claims: 'c', 'a', or 'h'
        private boolean shift_active; // From VC Claims
        private String userId;
    }

    @Data
    @Builder
    public static class Session {
        private String stage; // From DB: 'dispatched', 'in_transit', 'arrived'
        private String esid;  // Emergency Session ID
    }
}