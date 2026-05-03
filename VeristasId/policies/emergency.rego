package veritas.emergency

import future.keywords.if

# Default is strictly DENY
default allow = false

# Helper: Crack open the JWT to read the payload
token_payload := payload if {
    # Split "Bearer <token>" and grab the token part
    [_, jwt] := split(input.token, " ")
    # Decode the JWT and grab the middle payload section
    [_, payload, _] := io.jwt.decode(jwt)
}

# 🚨 Rule 1: Paramedic Access (Read-only during Dispatch)
allow if {
    token_payload.role == "paramedic"
    input.action == "read"
    input.session.stage == "dispatched" # Notice it matches your Java nesting perfectly!
}

# 🚨 Rule 2: Surgeon Access (Update when Arrived)
allow if {
    token_payload.role == "surgeon"
    input.action == "update"
    input.session.stage == "arrived"
}