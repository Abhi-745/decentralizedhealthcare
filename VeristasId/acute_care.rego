package veritas.emergency

import rego.v1

default allow := false

# Helper to strip "Bearer " and decode the JWT payload
token_payload := payload if {
    startswith(input.token, "Bearer ")
    raw_token := substring(input.token, 7, -1)
    [_, payload, _] := io.jwt.decode(raw_token)
}

# Rule 1: Paramedics can read data when a patient is dispatched
allow if {
    input.action == "read"
    input.session.stage == "dispatched"
    token_payload.role == "paramedic"
}

# Rule 2: Surgeons can read and update data when a patient has arrived
allow if {
    input.session.stage == "arrived"
    token_payload.role == "surgeon"
    input.action in {"read", "update"}
}