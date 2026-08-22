"""Read-only historical migration classification for flow-repair-11.

The module deliberately has no database or filesystem side effects. Callers pass a
normalized legacy record and receive a deterministic classification or a reversible
write-plan proposal that still requires explicit human approval.
"""

from __future__ import annotations

from enum import Enum
from typing import Any, Iterable, Mapping


class MigrationClassification(str, Enum):
    RECEIPT_BOUND_COMPLETE = "RECEIPT_BOUND_COMPLETE"
    PROVABLE_UNBOUND = "PROVABLE_UNBOUND"
    INCOMPLETE_OR_AMBIGUOUS = "INCOMPLETE_OR_AMBIGUOUS"
    BLOCKED_LEGACY = "BLOCKED_LEGACY"
    ALREADY_RELEASED_REVIEW_REQUIRED = "ALREADY_RELEASED_REVIEW_REQUIRED"


CURRENT_MATERIAL_NODES = (
    "incoming_inspection_report",
    "sterilization_report",
    "finished_product_inspection_report",
    "finished_product_inspection_record",
)
LEGACY_THREE_MATERIAL_NODES = frozenset(CURRENT_MATERIAL_NODES[:3])
VALID_COMPLETION_RECEIPT = "BACKFILL_SUCCEEDED"
_CLASSIFICATION_REASON_CODES = {
    MigrationClassification.RECEIPT_BOUND_COMPLETE: "RECEIPT_BOUND_COMPLETE",
    MigrationClassification.PROVABLE_UNBOUND: "PROVABLE_UNBOUND_REVIEW_REQUIRED",
    MigrationClassification.INCOMPLETE_OR_AMBIGUOUS: "INCOMPLETE_OR_AMBIGUOUS",
    MigrationClassification.BLOCKED_LEGACY: "BLOCKED_LEGACY",
    MigrationClassification.ALREADY_RELEASED_REVIEW_REQUIRED: "ALREADY_RELEASED_REVIEW_REQUIRED",
}


def classify_legacy_batch(record: Mapping[str, Any]) -> MigrationClassification:
    """Classify a historical batch without mutating it.

    The released branch is evaluated first so an already released record with
    incomplete provenance can never be silently promoted by migration. The
    legacy-three-material branch is intentionally separate from current
    four-node completeness.
    """

    released = bool(record.get("released")) or record.get("release_status") == "RELEASED"
    source_provable = bool(record.get("source_relation_provable"))
    trace_complete = bool(record.get("trace_mapping_complete"))
    materials = _mapping(record.get("materials"))
    four_materials_ready = _four_materials_ready(record)

    if released and not (source_provable and trace_complete and four_materials_ready):
        return MigrationClassification.ALREADY_RELEASED_REVIEW_REQUIRED

    if _is_legacy_three_material_history(materials):
        return MigrationClassification.BLOCKED_LEGACY

    receipt_bound = record.get("completion_receipt_status") == VALID_COMPLETION_RECEIPT
    if receipt_bound and source_provable and trace_complete and four_materials_ready:
        return MigrationClassification.RECEIPT_BOUND_COMPLETE

    if record.get("backfill_failure_attempt") and not receipt_bound:
        return MigrationClassification.INCOMPLETE_OR_AMBIGUOUS

    independent_credential = bool(record.get("independent_prerequisite_receipt"))
    source_relation = bool(record.get("batch_execution_source_relation"))
    independent_entry_provable = independent_credential and source_relation
    if (source_provable or independent_entry_provable) and not receipt_bound:
        return MigrationClassification.PROVABLE_UNBOUND

    return MigrationClassification.INCOMPLETE_OR_AMBIGUOUS


def build_rollback_plan(
    record: Mapping[str, Any],
    *,
    migration_batch_id: str,
    batch_execution_id: str,
    review_status: str,
) -> dict[str, Any]:
    """Build an explicit, non-executing migration write/rollback proposal.

    Only a PROVABLE_UNBOUND record with an APPROVED review may write new
    Origin/TraceLink relations. Rollback scope is limited to those new
    relations; original production/release facts are never deleted.
    """

    classification = classify_legacy_batch(record)
    approved = review_status == "APPROVED"
    write_allowed = classification is MigrationClassification.PROVABLE_UNBOUND and approved
    return {
        "migration_batch_id": migration_batch_id,
        "batch_execution_id": batch_execution_id,
        "classification": classification.value,
        "write_allowed": write_allowed,
        "rollback_scope": "NEW_ORIGIN_TRACE_LINKS_ONLY",
        "write_operations": ["CREATE_ORIGIN_TRACE_LINKS"] if write_allowed else [],
        "preserves_original_production_facts": True,
        "preserves_released_facts": True,
        "reason_code": "MIGRATION_APPROVED" if write_allowed else "MIGRATION_APPROVAL_REQUIRED",
    }


def build_dry_run_report(
    records: Iterable[Mapping[str, Any]],
    *,
    migration_batch_id: str,
) -> dict[str, Any]:
    """Build a deterministic, non-writing migration dry-run report.

    The caller supplies already-normalized records. This function does not
    discover records, infer relations, or execute any persistence operation.
    Duplicate business IDs fail fast so a report cannot conceal a non-unique
    migration batch.
    """

    normalized_batch_id = str(migration_batch_id).strip()
    if not normalized_batch_id:
        raise ValueError("MISSING_MIGRATION_BATCH_ID")

    entries: list[dict[str, Any]] = []
    seen_ids: set[str] = set()
    counts = {classification.value: 0 for classification in MigrationClassification}

    for record in records:
        if not isinstance(record, Mapping):
            raise TypeError("NORMALIZED_RECORD_REQUIRED")
        batch_execution_id = str(record.get("batch_execution_id", "")).strip()
        if not batch_execution_id:
            raise ValueError("MISSING_BATCH_EXECUTION_ID")
        if batch_execution_id in seen_ids:
            raise ValueError("DUPLICATE_BATCH_EXECUTION_ID")
        seen_ids.add(batch_execution_id)

        classification = classify_legacy_batch(record)
        counts[classification.value] += 1
        entries.append(
            {
                "batch_execution_id": batch_execution_id,
                "classification": classification.value,
                "reason_code": _CLASSIFICATION_REASON_CODES[classification],
                "source_snapshot_hash": _manifest_source_snapshot_hash(record),
                "four_node_status": _four_node_status(record),
                "material_evidence": _material_evidence(record),
                "write_allowed": False,
            }
        )

    return {
        "migration_batch_id": normalized_batch_id,
        "total": len(entries),
        "unique_batch_execution_ids": len(seen_ids),
        "classification_counts": counts,
        "entries": entries,
        "write_allowed": False,
        "side_effects": [],
    }


def _mapping(value: Any) -> Mapping[str, Any]:
    return value if isinstance(value, Mapping) else {}


def _optional_text(value: Any) -> str | None:
    text = str(value).strip() if value is not None else ""
    return text or None


def _four_node_status(record: Mapping[str, Any]) -> dict[str, str]:
    materials = _mapping(record.get("materials"))
    return {
        node: str(_mapping(materials.get(node)).get("status", "MISSING"))
        for node in CURRENT_MATERIAL_NODES
    }


def _manifest_source_snapshot_hash(record: Mapping[str, Any]) -> str | None:
    explicit = _optional_text(record.get("source_snapshot_hash"))
    if explicit:
        return explicit
    hashes = _mapping(record.get("material_source_snapshot_hashes"))
    values = {_optional_text(hashes.get(node)) for node in CURRENT_MATERIAL_NODES}
    return next(iter(values)) if len(values) == 1 and None not in values else None


def _material_evidence(record: Mapping[str, Any]) -> dict[str, dict[str, Any]]:
    materials = _mapping(record.get("materials"))
    return {
        node: {
            "status": _mapping(materials.get(node)).get("status", "MISSING"),
            "approved": _mapping(materials.get(node)).get("approved"),
            "version": _mapping(materials.get(node)).get("version"),
            "file_hash": _mapping(materials.get(node)).get("file_hash"),
            "source_snapshot_hash": _mapping(materials.get(node)).get("source_snapshot_hash"),
        }
        for node in CURRENT_MATERIAL_NODES
    }


def _is_legacy_three_material_history(materials: Mapping[str, Any]) -> bool:
    names = set(materials)
    return names == set(LEGACY_THREE_MATERIAL_NODES)


def _four_materials_ready(record: Mapping[str, Any]) -> bool:
    materials = _mapping(record.get("materials"))
    if set(materials) != set(CURRENT_MATERIAL_NODES):
        return False
    for node in CURRENT_MATERIAL_NODES:
        evidence = _mapping(materials.get(node))
        if evidence.get("status") != "COMPLETED":
            return False
        if "approved" in evidence and evidence.get("approved") is not True:
            return False
        if not str(evidence.get("version", "")).strip():
            return False
        if not str(evidence.get("file_hash", "")).strip():
            return False
        if not str(evidence.get("source_snapshot_hash", "")).strip():
            return False

    hashes = _mapping(record.get("material_source_snapshot_hashes"))
    if set(hashes) != set(CURRENT_MATERIAL_NODES):
        return False
    versions = _mapping(record.get("material_versions"))
    file_hashes = _mapping(record.get("material_file_hashes"))
    if set(versions) != set(CURRENT_MATERIAL_NODES) or set(file_hashes) != set(CURRENT_MATERIAL_NODES):
        return False
    normalized_hashes = {str(hashes[node]).strip() for node in CURRENT_MATERIAL_NODES}
    if len(normalized_hashes) != 1 or "" in normalized_hashes:
        return False
    return all(
        str(_mapping(materials[node]).get("version", "")).strip()
        == str(versions[node]).strip()
        and str(_mapping(materials[node]).get("file_hash", "")).strip()
        == str(file_hashes[node]).strip()
        and str(_mapping(materials[node]).get("source_snapshot_hash", "")).strip()
        == str(hashes[node]).strip()
        for node in CURRENT_MATERIAL_NODES
    )
