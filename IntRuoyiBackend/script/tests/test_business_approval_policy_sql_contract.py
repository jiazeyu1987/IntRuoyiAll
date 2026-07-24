from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260719_business_approval_policy.sql"


def main():
    assert SQL.exists(), f"missing SQL migration: {SQL}"
    source = SQL.read_text(encoding="utf-8")
    required_tokens = [
        "CREATE TABLE IF NOT EXISTS `bpm_business_approval_policy`",
        "CREATE TABLE IF NOT EXISTS `bpm_business_approval_request`",
        "`policy_mode`",
        "BPM_REQUIRED",
        "DIRECT",
        "SIGNATURE_REQUIRED",
        "`effect_executor_code`",
        "`form_policy_type`",
        "`form_slots_json`",
        "`process_definition_key`",
        "`request_status`",
        "`process_instance_id`",
        "`last_event_key`",
        "uk_bpm_business_approval_policy_published",
        "uk_bpm_business_approval_request_pending",
    ]
    for token in required_tokens:
        assert token in source, f"SQL contract missing token: {token}"
    forbidden_tokens = ["DROP TABLE", "TRUNCATE TABLE", "DELETE FROM"]
    upper = source.upper()
    for token in forbidden_tokens:
        assert token not in upper, f"SQL migration must not contain destructive token: {token}"
    print("PASS: business approval policy SQL contract")


if __name__ == "__main__":
    main()
