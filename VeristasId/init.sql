-- ==========================================
-- 1. FIX POSTGRES 15+ SCHEMA PERMISSIONS
-- ==========================================
GRANT ALL PRIVILEGES ON SCHEMA public TO "NagSigma";
GRANT CREATE ON SCHEMA public TO "NagSigma";
ALTER SCHEMA public OWNER TO "NagSigma";

-- ==========================================
-- 2. CREATE TABLES (Subjects & Objects)
-- ==========================================
CREATE TABLE IF NOT EXISTS users (
    did VARCHAR(255) PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    trust_level INT NOT NULL, -- Scale of 1 to 5 (5 is highest, needed for Emergency/Break-Glass)
    is_active BOOLEAN DEFAULT TRUE
);

-- Ensure NagSigma owns this table
ALTER TABLE users OWNER TO "NagSigma";

CREATE TABLE IF NOT EXISTS resources (
    resource_id VARCHAR(255) PRIMARY KEY,
    resource_type VARCHAR(50) NOT NULL,
    sensitivity VARCHAR(20) NOT NULL, -- e.g., low, medium, high, critical
    department_owner VARCHAR(50) NOT NULL
);

-- Ensure NagSigma owns this table
ALTER TABLE resources OWNER TO "NagSigma";


-- ==========================================
-- 3. SEED DATA (Dummy Data for Testing)
-- ==========================================

-- Insert Dummy Users (Using mock secp256k1 Ethereum-style DIDs)
INSERT INTO users (did, full_name, role, department, trust_level, is_active)
VALUES
    ('did:ethr:0x123abcDoctorAlice', 'Dr. Alice Smith', 'Attending_Physician', 'Emergency_Room', 5, TRUE),
    ('did:ethr:0x456defNurseBob', 'Bob Jones', 'Registered_Nurse', 'Cardiology', 3, TRUE),
    ('did:ethr:0x789ghiAdminCharlie', 'Charlie Davis', 'IT_Admin', 'IT_Support', 2, TRUE),
    ('did:ethr:0x999xyzRevokedDr', 'Dr. Eve Hacker', 'Surgeon', 'Neurology', 1, FALSE) -- Revoked/Inactive user
ON CONFLICT (did) DO NOTHING;

-- Insert Dummy Resources (Patient EMRs)
INSERT INTO resources (resource_id, resource_type, sensitivity, department_owner)
VALUES
    ('Patient_Record_99', 'EMR', 'critical', 'Emergency_Room'),
    ('Patient_Record_100', 'EMR', 'high', 'Cardiology'),
    ('Billing_Report_Q1', 'Financial', 'medium', 'Administration')
ON CONFLICT (resource_id) DO NOTHING;