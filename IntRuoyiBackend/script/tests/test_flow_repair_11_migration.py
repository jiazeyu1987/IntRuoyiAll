"""BDD/TDD contract tests for the flow-repair-11 historical migration classifier."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from flow_repair_11_migration import (
    MigrationClassification,
    classify_legacy_batch,
    build_rollback_plan,
    build_dry_run_report,
)


def _materials(*, include_record=True, status="COMPLETED", approved=True):
    nodes = {
        "incoming_inspection_report": {"status": status, "approved": approved, "version": "v1", "file_hash": "file-incoming", "source_snapshot_hash": "snapshot-1"},
        "sterilization_report": {"status": status, "approved": approved, "version": "v1", "file_hash": "file-sterilization", "source_snapshot_hash": "snapshot-1"},
        "finished_product_inspection_report": {"status": status, "approved": approved, "version": "v1", "file_hash": "file-finished-report", "source_snapshot_hash": "snapshot-1"},
    }
    if include_record:
        nodes["finished_product_inspection_record"] = {"status": status, "approved": approved, "version": "v1", "file_hash": "file-finished-record", "source_snapshot_hash": "snapshot-1"}
    return nodes


def _record(**overrides):
    record = {
        "released": False,
        "completion_receipt_status": "BACKFILL_SUCCEEDED",
        "source_relation_provable": True,
        "trace_mapping_complete": True,
        "materials": _materials(),
        "material_source_snapshot_hashes": {
            "incoming_inspection_report": "snapshot-1",
            "sterilization_report": "snapshot-1",
            "finished_product_inspection_report": "snapshot-1",
            "finished_product_inspection_record": "snapshot-1",
        },
        "material_versions": {
            "incoming_inspection_report": "v1",
            "sterilization_report": "v1",
            "finished_product_inspection_report": "v1",
            "finished_product_inspection_record": "v1",
        },
        "material_file_hashes": {
            "incoming_inspection_report": "file-incoming",
            "sterilization_report": "file-sterilization",
            "finished_product_inspection_report": "file-finished-report",
            "finished_product_inspection_record": "file-finished-record",
        },
    }
    record.update(overrides)
    return record


def _with_batch_id(batch_execution_id, **overrides):
    return _record(batch_execution_id=batch_execution_id, **overrides)


def test_bdd_receipt_bound_complete_is_classified_only_when_all_four_nodes_match():
    # BDD: Given a successful receipt and provable mapping,
    # When all four current materials are COMPLETED with one source snapshot,
    # Then the record is RECEIPT_BOUND_COMPLETE.
    assert classify_legacy_batch(_record()) is MigrationClassification.RECEIPT_BOUND_COMPLETE


def test_bdd_old_three_material_history_is_blocked_legacy():
    # BDD: Given a history with only the old three material nodes,
    # When it is evaluated for migration,
    # Then it is BLOCKED_LEGACY and never a current-flow success.
    record = _record(materials=_materials(include_record=False),
                     material_source_snapshot_hashes={
                         "incoming_inspection_report": "snapshot-1",
                         "sterilization_report": "snapshot-1",
                         "finished_product_inspection_report": "snapshot-1",
                     })
    assert classify_legacy_batch(record) is MigrationClassification.BLOCKED_LEGACY


def test_bdd_provable_unbound_requires_manual_approval_before_write_plan():
    # BDD: Given a formal source relation without a completion receipt,
    # When migration is reviewed,
    # Then it is PROVABLE_UNBOUND and the write plan stays blocked until approval.
    record = _record(completion_receipt_status=None)
    assert classify_legacy_batch(record) is MigrationClassification.PROVABLE_UNBOUND
    plan = build_rollback_plan(
        record, migration_batch_id="mig-1", batch_execution_id="batch-1",
        review_status="PENDING",
    )
    assert plan["write_allowed"] is False
    assert plan["write_operations"] == []
    assert plan["preserves_original_production_facts"] is True
    assert plan["preserves_released_facts"] is True
    assert plan["reason_code"] == "MIGRATION_APPROVAL_REQUIRED"


def test_bdd_ambiguous_missing_hash_or_mapping_is_incomplete():
    # BDD: Given incomplete or conflicting source evidence,
    # When migration classification runs,
    # Then it is INCOMPLETE_OR_AMBIGUOUS.
    record = _record(trace_mapping_complete=False,
                     material_source_snapshot_hashes={
                         "incoming_inspection_report": "snapshot-1",
                         "sterilization_report": "snapshot-2",
                         "finished_product_inspection_report": "snapshot-1",
                         "finished_product_inspection_record": "snapshot-1",
                     })
    assert classify_legacy_batch(record) is MigrationClassification.INCOMPLETE_OR_AMBIGUOUS


def test_bdd_missing_persisted_material_evidence_is_incomplete():
    # BDD: Given a four-node record whose status is complete but a node lacks
    # persisted version/file hash evidence, When it is classified, Then it is
    # INCOMPLETE_OR_AMBIGUOUS rather than a migration success.
    materials = _materials()
    del materials["finished_product_inspection_record"]["file_hash"]
    record = _record(materials=materials)
    assert classify_legacy_batch(record) is MigrationClassification.INCOMPLETE_OR_AMBIGUOUS


def test_bdd_independent_receipt_without_source_relation_is_incomplete():
    # BDD: Given an independent prerequisite receipt without a formal source
    # relation, When migration classification runs, Then it remains ambiguous.
    record = _record(
        completion_receipt_status=None,
        source_relation_provable=False,
        independent_prerequisite_receipt=True,
        batch_execution_source_relation=False,
    )
    assert classify_legacy_batch(record) is MigrationClassification.INCOMPLETE_OR_AMBIGUOUS


def test_bdd_independent_receipt_with_source_relation_is_provable_unbound():
    # BDD: Given an independent prerequisite receipt and formal source relation
    # without completion receipt, When migration classification runs, Then the
    # record is PROVABLE_UNBOUND and remains approval-gated.
    record = _record(
        completion_receipt_status=None,
        source_relation_provable=False,
        independent_prerequisite_receipt=True,
        batch_execution_source_relation=True,
    )
    assert classify_legacy_batch(record) is MigrationClassification.PROVABLE_UNBOUND


def test_bdd_released_incomplete_history_requires_review():
    # BDD: Given a RELEASED record with incomplete trace evidence,
    # When migration classification runs,
    # Then it is ALREADY_RELEASED_REVIEW_REQUIRED.
    record = _record(released=True, trace_mapping_complete=False)
    assert classify_legacy_batch(record) is MigrationClassification.ALREADY_RELEASED_REVIEW_REQUIRED


def test_approved_provable_unbound_plan_is_explicit_and_reversible():
    record = _record(completion_receipt_status=None)
    plan = build_rollback_plan(
        record, migration_batch_id="mig-2", batch_execution_id="batch-2",
        review_status="APPROVED",
    )
    assert plan == {
        "migration_batch_id": "mig-2",
        "batch_execution_id": "batch-2",
        "classification": "PROVABLE_UNBOUND",
        "write_allowed": True,
        "rollback_scope": "NEW_ORIGIN_TRACE_LINKS_ONLY",
        "write_operations": ["CREATE_ORIGIN_TRACE_LINKS"],
        "preserves_original_production_facts": True,
        "preserves_released_facts": True,
        "reason_code": "MIGRATION_APPROVED",
    }


def test_bdd_failure_attempt_without_success_receipt_is_blocked():
    # BDD: Given only a recorded failed backfill attempt and no successful
    # receipt, When the history is classified, Then migration remains blocked.
    record = _record(completion_receipt_status=None, backfill_failure_attempt=True)
    assert classify_legacy_batch(record) is MigrationClassification.INCOMPLETE_OR_AMBIGUOUS


def test_bdd_dry_run_matrix_is_counted_without_writes():
    # BDD: Given a matrix spanning every migration class and review gate,
    # When a dry-run report is built, Then counts and IDs are deterministic,
    # and no entry is marked writeable by classification alone.
    records = [
        _with_batch_id("complete"),
        _with_batch_id(
            "independent", completion_receipt_status=None,
            source_relation_provable=False,
            independent_prerequisite_receipt=True,
            batch_execution_source_relation=True,
        ),
        _with_batch_id(
            "legacy", materials=_materials(include_record=False),
            material_source_snapshot_hashes={
                "incoming_inspection_report": "snapshot-1",
                "sterilization_report": "snapshot-1",
                "finished_product_inspection_report": "snapshot-1",
            },
        ),
        _with_batch_id(
            "missing-fourth", materials={**_materials(include_record=False), "unclassified": {"status": "COMPLETED"}},
        ),
        _with_batch_id(
            "conflict", material_source_snapshot_hashes={
                "incoming_inspection_report": "snapshot-1",
                "sterilization_report": "snapshot-2",
                "finished_product_inspection_report": "snapshot-1",
                "finished_product_inspection_record": "snapshot-1",
            },
        ),
        _with_batch_id("unknown-source", source_relation_provable=False),
        _with_batch_id("released-review", released=True, trace_mapping_complete=False),
        _with_batch_id("failed-attempt", completion_receipt_status=None, backfill_failure_attempt=True),
    ]
    report = build_dry_run_report(records, migration_batch_id="migration-1")
    assert report["total"] == 8
    assert report["unique_batch_execution_ids"] == 8
    assert report["classification_counts"] == {
        "RECEIPT_BOUND_COMPLETE": 1,
        "PROVABLE_UNBOUND": 1,
        "INCOMPLETE_OR_AMBIGUOUS": 4,
        "BLOCKED_LEGACY": 1,
        "ALREADY_RELEASED_REVIEW_REQUIRED": 1,
    }
    assert report["entries"][0]["source_snapshot_hash"] == "snapshot-1"
    assert report["entries"][0]["material_evidence"]["finished_product_inspection_record"]["file_hash"] == "file-finished-record"
    assert report["write_allowed"] is False
    assert report["side_effects"] == []
    assert all(entry["write_allowed"] is False for entry in report["entries"])


def test_bdd_dry_run_rejects_duplicate_batch_execution_ids():
    # BDD: Given duplicate business IDs in one migration batch, When dry-run
    # starts, Then it fails fast rather than hiding a non-unique migration.
    try:
        build_dry_run_report(
            [_with_batch_id("duplicate"), _with_batch_id("duplicate")],
            migration_batch_id="migration-duplicate",
        )
    except ValueError as exc:
        assert str(exc) == "DUPLICATE_BATCH_EXECUTION_ID"
    else:
        raise AssertionError("duplicate IDs must be rejected")
