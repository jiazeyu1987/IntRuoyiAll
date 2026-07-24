from __future__ import annotations

from tenant_clone_fixtures import (
    parse_json_stdout,
    run_tenant_clone,
    write_contract_candidate_inventory,
    write_contract_missing_table_and_reference,
)


def test_contract_validation_fails_when_candidate_table_is_unclassified(tmp_path) -> None:
    contract = write_contract_missing_table_and_reference(tmp_path / "tenant-clone-contract.json")
    candidates = write_contract_candidate_inventory(tmp_path / "candidate-inventory.json")

    completed = run_tenant_clone(
        "validate-contract",
        "--contract",
        str(contract),
        "--candidate-inventory",
        str(candidates),
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["phase"] == "CONTRACT_VALIDATE"
    assert payload["errorCode"] == "TENANT_CLONE_CONTRACT_MISSING_TABLE"
    assert payload["missingTables"] == ["system_dept"]
    assert "system_dept" in payload["message"]


def test_contract_validation_fails_when_reference_field_is_not_declared(tmp_path) -> None:
    contract = write_contract_missing_table_and_reference(tmp_path / "tenant-clone-contract.json")
    candidates = write_contract_candidate_inventory(tmp_path / "candidate-inventory.json")

    completed = run_tenant_clone(
        "validate-contract",
        "--contract",
        str(contract),
        "--candidate-inventory",
        str(candidates),
        "--require-reference-fields",
    )

    payload = parse_json_stdout(completed)
    assert completed.returncode != 0
    assert payload["success"] is False
    assert payload["phase"] == "CONTRACT_VALIDATE"
    assert payload["errorCode"] == "TENANT_CLONE_CONTRACT_INVALID_REFERENCE"
    assert payload["invalidReferences"] == [
        {
            "table": "system_users",
            "field": "dept_id",
            "refTable": "system_dept",
            "reason": "reference field is not declared in contract",
        }
    ]
