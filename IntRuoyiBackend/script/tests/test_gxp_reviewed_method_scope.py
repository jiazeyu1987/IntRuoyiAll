"""Explicit, source-bound R1 review; never an implicit exclusion of new writers."""
import hashlib
import json
import os
from pathlib import Path

import pytest

from test_gxp_audit_method_boundaries import fixture, gate


def reviewed_fixture(tmp_path, decision="READ_ONLY"):
    source, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void read() { digest.update(bytes); }
    """)
    policy["approvalReference"] = "TASK-LOCAL-APPROVAL"
    policy["coverageScope"]["mode"] = "R1"
    evidence = dict(candidate=source.relative_to(tmp_path).as_posix(),
                    sourceLocator="demo.OrderService#read", signature="read()",
                    candidateSha256=gate.source_sha256(source),
                    decision=decision, approvedBy="test-reviewer",
                    approvalReference=policy["approvalReference"],
                    analysis="Fixture digest.update mutates only a method-local MessageDigest.")
    (tmp_path / "review.json").write_text(json.dumps({"READ-1": evidence}), encoding="utf-8")
    policy["coverageScope"]["excludedReferences"] = [dict(reference="READ-1",
        reason="Reviewed method is outside the counted operation set", evidenceReference="review.json#READ-1",
        futureOwnerRole="test-owner", futureTaskReference="TEST-REVIEW")]
    return source, policy, operations, evidence


@pytest.mark.parametrize("decision", ["READ_ONLY", "OUT_OF_RELEASE_SCOPE"])
def test_exact_review_is_reported_separately_from_registered(tmp_path, decision):
    _, policy, operations, _ = reviewed_fixture(tmp_path, decision)
    report = tmp_path / "boundaries.jsonl"
    gate.validate_boundary_scan(tmp_path, policy, operations, report)
    records = [json.loads(line) for line in report.read_text().splitlines()]
    entry = next(row for row in records if row.get("sourceLocator") == "demo.OrderService#read")
    assert entry["decision"] == "REVIEWED_" + decision
    assert entry["reviewReference"] == "READ-1"


@pytest.mark.parametrize("change", ["source", "signature", "approval", "missing", "blank-reviewer", "blank-analysis"])
def test_changed_or_incomplete_review_never_exempts_writer(tmp_path, change):
    source, policy, operations, evidence = reviewed_fixture(tmp_path)
    if change == "source":
        source.write_text(source.read_text().replace("digest.update(bytes)", "mapper.update(row)"))
    elif change == "signature":
        evidence["signature"] = "read(Long id)"
    elif change == "approval":
        evidence["approvalReference"] = "OLD"
    elif change == "missing":
        policy["coverageScope"]["excludedReferences"][0]["evidenceReference"] = "absent.json#READ-1"
    elif change == "blank-reviewer":
        evidence["approvedBy"] = ""
    else:
        evidence["analysis"] = ""
    (tmp_path / "review.json").write_text(json.dumps({"READ-1": evidence}), encoding="utf-8")
    with pytest.raises(SystemExit):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_r1_exclusion_cannot_make_full_coverage_pass(tmp_path):
    _, policy, operations, _ = reviewed_fixture(tmp_path, "OUT_OF_RELEASE_SCOPE")
    policy["coverageScope"]["mode"] = "FULL_COVERAGE"
    with pytest.raises(SystemExit):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_review_does_not_cover_adjacent_new_writer(tmp_path):
    source, policy, operations, evidence = reviewed_fixture(tmp_path)
    source.write_text(source.read_text().replace("\n}", "\npublic void purge() { mapper.delete(id); }\n}"))
    evidence["candidateSha256"] = gate.source_sha256(source)
    (tmp_path / "review.json").write_text(json.dumps({"READ-1": evidence}), encoding="utf-8")
    with pytest.raises(SystemExit, match="#purge"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_activation_report_binds_exact_policy_and_review_results(tmp_path):
    _, policy, operations, _ = reviewed_fixture(tmp_path, "OUT_OF_RELEASE_SCOPE")
    policy.update(schemaVersion="gxp-audit-policy.v2", policyVersion="local-test", status="APPROVED")
    policy_path = tmp_path / "policy.yaml"
    import yaml
    policy_path.write_text(yaml.safe_dump(policy), encoding="utf-8")
    boundary_path = tmp_path / "boundaries.jsonl"
    scan = gate.validate_boundary_scan(tmp_path, policy, operations, boundary_path)
    result = gate.activation_report(policy_path, policy, scan)
    assert result["status"] == "PASS"
    assert result["coverageMode"] == "R1"
    assert result["reviewedOutOfScopeCount"] == 1
    assert result["unresolvedCount"] == 0
    assert result["artifactHash"] == hashlib.sha256(policy_path.read_bytes()).hexdigest()
    canonical = json.dumps(policy, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    assert result["policyHash"] == hashlib.sha256(canonical.encode()).hexdigest()
    assert result["boundarySha256"] == scan[3]
    assert result["boundarySha256"] == hashlib.sha256(boundary_path.read_bytes()).hexdigest()


@pytest.mark.skipif(os.name != "nt", reason="Windows path-length contract")
def test_windows_source_scan_reads_long_paths(tmp_path):
    root = gate.source_root_path(tmp_path)
    source = root / ("long" * 40) / "subdirectory" / "LongWriter.java"
    source.parent.mkdir(parents=True)
    source.write_text("public class LongWriter { public void save() {} }", encoding="utf-8")
    assert len(str(source)) > 260
    found = gate.discover_boundary_candidates(root, dict(sourceType="DOMAIN_SERVICE", paths=["**/*.java"]))
    assert found == [source.resolve()]


def test_source_digest_has_one_explicit_newline_contract(tmp_path):
    source = tmp_path / "Writer.java"
    source.write_bytes(b"class Writer {\r\n void save() {}\r\n}\r\n")
    first = gate.source_sha256(source)
    source.write_bytes(b"class Writer {\n void save() {}\n}\n")
    assert gate.source_sha256(source) == first
    source.write_bytes(b"class Writer {\n void save() { mapper.insert(row); }\n}\n")
    assert gate.source_sha256(source) != first
    source.write_bytes(b"invalid utf8 \xff")
    with pytest.raises(UnicodeDecodeError):
        gate.source_sha256(source)


def file_scope_fixture(tmp_path):
    _, policy, operations = fixture(tmp_path, "public void addOrder() { mapper.insert(row); }")
    policy.update(approvalReference="TASK-LOCAL-APPROVAL")
    policy["coverageScope"]["mode"] = "R1"
    candidate = tmp_path / "LegacyWriter.java"
    candidate.write_text("public class LegacyWriter { public void save() { mapper.insert(row); } }", encoding="utf-8")
    domain = next(c for c in policy["coverageScope"]["writeBoundaryScan"]["categories"] if c["sourceType"] == "DOMAIN_SERVICE")
    domain["paths"].append(candidate.name)
    digest = gate.source_sha256(candidate)
    record = dict(candidate=candidate.name, candidateSha256=digest, decision="REVIEWED_OUT_OF_RELEASE_SCOPE",
                  sourceType="DOMAIN_SERVICE", reasonCode="LEGACY_SCOPE", reason=f"{candidate.name} {digest}: explicit legacy writer gap",
                  approvedBy="test-reviewer", approvalReference=policy["approvalReference"],
                  evidenceReference="file-review.json#LEGACY", futureOwnerRole="test-owner", futureTaskReference="FOLLOWUP-LEGACY")
    review = dict(record, analysis="The legacy save entry persists separate legacy workflow data; not read-only or an audited operation.")
    (tmp_path / "file-review.json").write_text(json.dumps({"LEGACY": review}), encoding="utf-8")
    (tmp_path / "approved.jsonl").write_text(json.dumps(record) + "\n", encoding="utf-8")
    return candidate, policy, operations, record


def test_explicit_file_scope_is_a_visible_gap_not_registered_coverage(tmp_path):
    _, policy, operations, _ = file_scope_fixture(tmp_path)
    scan = gate.validate_boundary_scan(tmp_path, policy, operations)
    assert sum(row["decision"] == "REVIEWED_OUT_OF_RELEASE_SCOPE" for row in scan[4]) == 1
    assert not any(row["candidate"] == "LegacyWriter.java" and row["decision"] == "REGISTERED" for row in scan[4])


@pytest.mark.parametrize("change", ["full-coverage", "missing-evidence", "changed-source", "wrong-approval", "missing-owner"])
def test_file_scope_cannot_silently_cover_unreviewed_or_full_scope(tmp_path, change):
    candidate, policy, operations, record = file_scope_fixture(tmp_path)
    if change == "full-coverage":
        policy["coverageScope"]["mode"] = "FULL_COVERAGE"
    elif change == "missing-evidence":
        record["evidenceReference"] = "missing.json#LEGACY"
    elif change == "changed-source":
        candidate.write_text(candidate.read_text() + "\nclass NewWriter { void delete() {} }", encoding="utf-8")
    elif change == "wrong-approval":
        record["approvalReference"] = "OLD"
    else:
        record["futureOwnerRole"] = ""
    (tmp_path / "approved.jsonl").write_text(json.dumps(record) + "\n", encoding="utf-8")
    with pytest.raises(SystemExit):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def mixed_scope_fixture(tmp_path):
    candidate, policy, operations, record = file_scope_fixture(tmp_path)
    policy["operations"] = [operation.values for operation in operations]
    record["decision"] = "REVIEWED_MIXED_SCOPE"
    review = dict(record, analysis="Order assignment delegates to its audited owner; independent legacy metadata remains outside R1.",
                  dependentSources=[dict(candidate="IntRuoyiBackend/demo/src/main/java/demo/OrderService.java",
                                         candidateSha256=gate.source_sha256(tmp_path / "IntRuoyiBackend/demo/src/main/java/demo/OrderService.java"))],
                  delegations=[dict(effect="order assignment", operationIds=["order.add"],
                                    analysis="Delegated write joins the parent's transaction and actual state envelope.")],
                  outOfScopeEffects=[dict(effect="legacy metadata lifecycle", analysis="Separate mapper persistence lacks its own audit operation.",
                                          futureOwnerRole="legacy-owner", futureTaskReference="FOLLOWUP-LEGACY")])
    (tmp_path / "file-review.json").write_text(json.dumps({"LEGACY": review}), encoding="utf-8")
    (tmp_path / "approved.jsonl").write_text(json.dumps(record) + "\n", encoding="utf-8")
    return candidate, policy, operations, record, review


def test_mixed_file_reports_delegated_effects_and_visible_scope_gaps(tmp_path):
    _, policy, operations, _, _ = mixed_scope_fixture(tmp_path)
    scan = gate.validate_boundary_scan(tmp_path, policy, operations)
    mixed = next(row for row in scan[4] if row["decision"] == "REVIEWED_MIXED_SCOPE")
    assert mixed["delegatedOperationIds"] == ["order.add"]
    assert mixed["outOfScopeEffectCount"] == 1
    assert not any(row["candidate"] == "LegacyWriter.java" and row["decision"] == "REGISTERED" for row in scan[4])


@pytest.mark.parametrize("change", ["full-coverage", "missing-delegation", "unknown-operation", "missing-gap", "missing-followup", "changed-source", "changed-delegate", "missing-delegate-proof", "wrong-approval"])
def test_mixed_scope_rejects_unproven_effects_or_stale_review(tmp_path, change):
    candidate, policy, operations, record, review = mixed_scope_fixture(tmp_path)
    if change == "full-coverage":
        policy["coverageScope"]["mode"] = "FULL_COVERAGE"
    elif change == "missing-delegation":
        review["delegations"] = []
    elif change == "unknown-operation":
        review["delegations"][0]["operationIds"] = ["unknown.write"]
    elif change == "missing-gap":
        review["outOfScopeEffects"] = []
    elif change == "missing-followup":
        review["outOfScopeEffects"][0]["futureTaskReference"] = ""
    elif change == "changed-source":
        candidate.write_text(candidate.read_text() + "\nclass AnotherWriter {}", encoding="utf-8")
    elif change == "changed-delegate":
        delegate = tmp_path / review["dependentSources"][0]["candidate"]
        delegate.write_text(delegate.read_text() + "\n// changed dependency", encoding="utf-8")
    elif change == "missing-delegate-proof":
        review["dependentSources"] = []
    else:
        review["approvalReference"] = "OLD"
    (tmp_path / "file-review.json").write_text(json.dumps({"LEGACY": review}), encoding="utf-8")
    with pytest.raises(SystemExit):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def delegated_method_fixture(tmp_path):
    source, policy, operations, review = reviewed_fixture(tmp_path, "DELEGATED_EFFECT")
    policy["operations"] = [operation.values for operation in operations]
    review.update(operationIds=["order.add"], analysis="Exact wrapper delegates only its assignment effect to the registered owner in the same transaction.",
                  dependentSources=[dict(candidate=source.relative_to(tmp_path).as_posix(), candidateSha256=gate.source_sha256(source))])
    (tmp_path / "review.json").write_text(json.dumps({"READ-1": review}), encoding="utf-8")
    return source, policy, operations, review


def test_exact_delegate_is_visible_as_delegate_not_read_only_or_excluded(tmp_path):
    _, policy, operations, _ = delegated_method_fixture(tmp_path)
    scan = gate.validate_boundary_scan(tmp_path, policy, operations)
    entry = next(row for row in scan[4] if row.get("sourceLocator") == "demo.OrderService#read")
    assert entry["decision"] == "REVIEWED_DELEGATED_EFFECT"
    assert entry["delegatedOperationIds"] == ["order.add"]


@pytest.mark.parametrize("change", ["missing-operations", "unknown-operation", "missing-source", "changed-delegate"])
def test_delegate_review_requires_current_actual_owner_evidence(tmp_path, change):
    _, policy, operations, review = delegated_method_fixture(tmp_path)
    if change == "missing-operations":
        review["operationIds"] = []
    elif change == "unknown-operation":
        review["operationIds"] = ["new.unreviewed.operation"]
    elif change == "missing-source":
        review["dependentSources"] = []
    else:
        review["dependentSources"][0]["candidateSha256"] = "0" * 64
    (tmp_path / "review.json").write_text(json.dumps({"READ-1": review}), encoding="utf-8")
    with pytest.raises(SystemExit):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_file_delegate_is_not_reported_as_an_out_of_scope_gap(tmp_path):
    _, policy, operations, record, review = mixed_scope_fixture(tmp_path)
    record["decision"] = review["decision"] = "REVIEWED_DELEGATED_EFFECT"
    review["outOfScopeEffects"] = []
    (tmp_path / "file-review.json").write_text(json.dumps({"LEGACY": review}), encoding="utf-8")
    (tmp_path / "approved.jsonl").write_text(json.dumps(record) + "\n", encoding="utf-8")
    scan = gate.validate_boundary_scan(tmp_path, policy, operations)
    entry = next(row for row in scan[4] if row["candidate"] == "LegacyWriter.java")
    assert entry["decision"] == "REVIEWED_DELEGATED_EFFECT"
    assert entry["delegatedOperationIds"] == ["order.add"]
    assert entry["outOfScopeEffectCount"] == 0


@pytest.mark.parametrize("gaps", [None, [], [{"effect": "account counter"}]])
def test_mixed_method_requires_explicit_separate_gap_evidence(tmp_path, gaps):
    _, policy, operations, review = delegated_method_fixture(tmp_path)
    review["decision"] = "MIXED_SCOPE"
    review["outOfScopeEffects"] = gaps
    (tmp_path / "review.json").write_text(json.dumps({"READ-1": review}), encoding="utf-8")
    with pytest.raises(SystemExit):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_mixed_method_preserves_delegate_and_account_counter_gap(tmp_path):
    _, policy, operations, review = delegated_method_fixture(tmp_path)
    review["decision"] = "MIXED_SCOPE"
    review["outOfScopeEffects"] = [dict(effect="account-security counters", analysis="Credential validation changes lock/failure counters outside this business operation.", futureOwnerRole="security-owner", futureTaskReference="COUNTER-FOLLOWUP")]
    (tmp_path / "review.json").write_text(json.dumps({"READ-1": review}), encoding="utf-8")
    scan = gate.validate_boundary_scan(tmp_path, policy, operations)
    entry = next(row for row in scan[4] if row.get("sourceLocator") == "demo.OrderService#read")
    assert entry["decision"] == "REVIEWED_MIXED_SCOPE"
    assert entry["delegatedOperationIds"] == ["order.add"]
    assert entry["outOfScopeEffectCount"] == 1
    policy["coverageScope"]["mode"] = "FULL_COVERAGE"
    with pytest.raises(SystemExit):
        gate.validate_boundary_scan(tmp_path, policy, operations)
