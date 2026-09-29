"""Behavioral tests of the gate; Java fixtures are scanner input, not runtime E2E."""
import importlib.util
import json
import sys
from pathlib import Path

import pytest


SPEC = importlib.util.spec_from_file_location(
    "gxp_method_gate", Path(__file__).resolve().parents[1] / "gxp_audit_coverage_gate.py"
)
gate = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = gate
SPEC.loader.exec_module(gate)


def fixture(tmp_path, body):
    source = tmp_path / "IntRuoyiBackend/demo/src/main/java/demo/OrderService.java"
    source.parent.mkdir(parents=True)
    source.write_text("package demo; public class OrderService {\n" + body + "\n}", encoding="utf-8")
    (tmp_path / "approved.jsonl").write_text("", encoding="utf-8")
    categories = [dict(sourceType=kind, requiredRegistration=True,
                       paths=[source.relative_to(tmp_path).as_posix()] if kind == "DOMAIN_SERVICE" else [],
                       expectedMinCandidates=0,
                       unregisteredDisposition=dict(decision="FAIL", reasonCode="UNREGISTERED", reason="Review entry"))
                  for kind in sorted(gate.BOUNDARY_SOURCE_TYPES)]
    policy = dict(coverageScope=dict(writeBoundaryScan=dict(registrationMode="REGISTERED_OR_APPROVED_EXCLUSION", candidateHashMode="UTF8_LF_SHA256",
                                        approvedExclusionsFile="approved.jsonl", categories=categories)))
    operations = [gate.Operation(dict(operationId="order.add", sourceType="SERVICE_METHOD",
                                      sourceLocators=["demo.OrderService#addOrder"]))]
    return source, policy, operations


def test_new_write_in_registered_file_fails(tmp_path):
    source, policy, operations = fixture(tmp_path, "public void addOrder() { mapper.insert(order); }")
    gate.validate_boundary_scan(tmp_path, policy, operations)
    source.write_text(source.read_text().replace("\n}",
        "\npublic void removeActiveOrder() { mapper.removeActiveOrder(id); }\n}"), encoding="utf-8")
    with pytest.raises(SystemExit, match="OrderService#removeActiveOrder"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_local_map_new_collection_chain_is_not_a_writer(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void list() {
            java.util.Map<String, List<Row>> rows = new java.util.HashMap<>();
            rows.computeIfAbsent(row.getKey(), ignored -> new java.util.ArrayList<>()).add(row);
        }
    """)
    gate.validate_boundary_scan(tmp_path, policy, operations)


@pytest.mark.parametrize("factory", ["ignored -> repository.createBucket()", "ignored -> new CustomList()"])
def test_unknown_collection_chain_remains_a_writer(tmp_path, factory):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void list() {
            Map<String, List<Row>> rows = new HashMap<>();
            rows.computeIfAbsent(row.getKey(), FACTORY).add(row);
        }
    """.replace("FACTORY", factory))
    with pytest.raises(SystemExit, match="OrderService#list"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_known_collection_chain_keeps_write_argument(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void list() {
            Map<String, List<Row>> rows = new HashMap<>();
            rows.computeIfAbsent(row.getKey(), ignored -> new java.util.ArrayList<>()).add(repository.createRow());
        }
    """)
    with pytest.raises(SystemExit, match="OrderService#list"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_field_writer_with_same_name_is_not_exempted(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        private Map<String, List<Row>> rows = repository.writer();
        public void addOrder() { mapper.insert(order); }
        public void list() {
            rows.computeIfAbsent(row.getKey(), ignored -> new java.util.ArrayList<>()).add(row);
            java.util.Map<String, List<Row>> rows = new java.util.HashMap<>();
        }
    """)
    with pytest.raises(SystemExit, match="OrderService#list"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_same_named_custom_map_is_not_exempted(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void list() {
            Map<String, List<Row>> rows = new Map<>();
            rows.computeIfAbsent(row.getKey(), ignored -> new java.util.ArrayList<>()).add(row);
        }
    """)
    with pytest.raises(SystemExit, match="OrderService#list"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_inner_same_named_standard_map_does_not_exempt_outer_writer(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        Map<String, List<Row>> rows = repository.writer();
        public void addOrder() { mapper.insert(order); }
        public void list() {
            if (enabled) {
                Map<String, List<Row>> rows = new HashMap<>();
                rows.computeIfAbsent(row.getKey(), ignored -> new java.util.ArrayList<>()).add(row);
            }
            rows.computeIfAbsent(other.getKey(), ignored -> new java.util.ArrayList<>()).add(other);
        }
    """)
    with pytest.raises(SystemExit, match="OrderService#list"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_imported_jdk_map_outer_local_is_visible_in_loop(tmp_path):
    source, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void list() {
            Map<String, List<Row>> rows = new LinkedHashMap<>();
            for (Row row : input) {
                rows.computeIfAbsent(row.getKey(), ignored -> new java.util.ArrayList<>()).add(row);
            }
        }
    """)
    source.write_text(source.read_text().replace("package demo;", "package demo; import java.util.Map; import java.util.LinkedHashMap;"), encoding="utf-8")
    gate.validate_boundary_scan(tmp_path, policy, operations)


def test_standard_map_reassigned_to_writer_is_not_exempted(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void list() {
            java.util.Map<String, List<Row>> rows = new java.util.HashMap<>();
            rows = repository.writer();
            rows.computeIfAbsent(row.getKey(), ignored -> new java.util.ArrayList<>()).add(row);
        }
    """)
    with pytest.raises(SystemExit, match="OrderService#list"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_neutral_entry_reaching_private_write_is_not_exempt(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { persist(); }
        public void reconcile() { persist(); }
        private void persist() { mapper.updateById(order); }
    """)
    with pytest.raises(SystemExit, match="OrderService#reconcile"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_private_helper_and_lexical_decoys_are_not_independent_entries(tmp_path):
    _, policy, operations = fixture(tmp_path, '''
        public void addOrder() { insertAudit(); }
        private void insertAudit() { mapper.insert(order); }
        public String getLabel() { return "public void deleteFake() { mapper.delete(); }"; }
        // public void removeFake() { mapper.delete(); }
    ''')
    report = tmp_path / "boundaries.jsonl"
    gate.validate_boundary_scan(tmp_path, policy, operations, report)
    records = [json.loads(line) for line in report.read_text(encoding="utf-8").splitlines()]
    assert {(record["sourceLocator"], record["decision"]) for record in records if "sourceLocator" in record} == {
        ("demo.OrderService#addOrder", "REGISTERED"),
        ("demo.OrderService#insertAudit", "PRIVATE_HELPER"),
    }


def test_registered_name_does_not_cover_new_overload(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void addOrder(Long id) { mapper.updateById(order); }
    """)
    with pytest.raises(SystemExit, match="ambiguous.*addOrder"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_called_name_is_not_a_resolved_registration(tmp_path):
    _, _, operations = fixture(tmp_path, "public void removeOrder() { other.addOrder(); }")
    assert not gate.source_locator_exists(tmp_path, operations[0])


def test_public_delegator_requires_own_registration(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void process() { this.addOrder(); }
    """)
    with pytest.raises(SystemExit, match="OrderService#process"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_all_unregistered_entries_are_reported(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void removeActiveOrder() { mapper.deleteById(id); }
        public void executeDataCleanup() { mapper.deleteBatch(ids); }
    """)
    with pytest.raises(SystemExit) as error:
        gate.validate_boundary_scan(tmp_path, policy, operations)
    assert "#removeActiveOrder" in str(error.value)
    assert "#executeDataCleanup" in str(error.value)


def test_local_collection_mutation_is_not_persistent_write(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public List<Long> listIds() { return applyDisplayOrder(); }
        private List<Long> applyDisplayOrder() {
            List<Long> ids = new ArrayList<>(); ids.add(1L); ids.addAll(other);
            ids.removeIf(x -> x == null); return ids;
        }
    """)
    gate.validate_boundary_scan(tmp_path, policy, operations)


def test_private_scheduled_method_is_an_entry(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        @Scheduled(cron = "0 * * * * *")
        private void tick() { mapper.updateById(order); }
    """)
    with pytest.raises(SystemExit, match="#tick"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_unknown_receiver_is_not_assumed_to_be_a_collection(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void reconcile() { items.remove(id); }
    """)
    report = tmp_path / "failed-boundaries.jsonl"
    with pytest.raises(SystemExit, match="#reconcile"):
        gate.validate_boundary_scan(tmp_path, policy, operations, report)
    assert any(record["sourceLocator"] == "demo.OrderService#reconcile" and
               record["evidence"] == ["call items.remove"]
               for record in map(json.loads, report.read_text(encoding="utf-8").splitlines()))


def test_annotations_with_array_arguments_preserve_method_boundary(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        @Transactional(rollbackFor = {Exception.class, RuntimeException.class})
        public void reconcile() { mapper.updateById(order); }
    """)
    with pytest.raises(SystemExit, match="#reconcile"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_audit_append_and_mapper_method_reference_are_write_evidence(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void reconcile() { appendAudit(); }
        private void appendAudit() { gxpAuditService.append(command); }
        public void purge() { ids.forEach(mapper::deleteById); }
    """)
    with pytest.raises(SystemExit) as error:
        gate.validate_boundary_scan(tmp_path, policy, operations)
    assert "#reconcile" in str(error.value)
    assert "#purge" in str(error.value)


@pytest.mark.parametrize("body", [
    "public BigDecimal total(BigDecimal left, BigDecimal right) { return left.add(right); }",
    "public BigDecimal total() { BigDecimal amount = BigDecimal.ZERO; return amount.add(BigDecimal.ONE); }",
])
def test_explicit_jdk_value_receiver_is_not_write(tmp_path, body):
    source, policy, operations = fixture(tmp_path, "public void addOrder() { mapper.insert(order); }" + body)
    source.write_text(source.read_text().replace("package demo;", "package demo; import java.math.BigDecimal; import java.util.function.Function;"), encoding="utf-8")
    gate.validate_boundary_scan(tmp_path, policy, operations)


@pytest.mark.parametrize("body", [
    "public void reconcile() { mapper.add(value); mapper.apply(value); }",
    "public void reconcile() { amount.add(value); idGetter.apply(value); }",
    "public void reconcile() { { BigDecimal amount = BigDecimal.ZERO; } amount.add(value); }",
    "public void reconcile() { mapper.add(value); BigDecimal mapper = BigDecimal.ZERO; }",
    "public void reconcile(BigDecimal amount) { other(amount); mapper.apply(value); }",
])
def test_unknown_or_out_of_scope_receivers_remain_blocked(tmp_path, body):
    source, policy, operations = fixture(tmp_path, "public void addOrder() { mapper.insert(order); }" + body)
    source.write_text(source.read_text().replace("package demo;", "package demo; import java.math.BigDecimal;"), encoding="utf-8")
    with pytest.raises(SystemExit, match="#reconcile"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_same_simple_type_without_jdk_import_is_not_exempt(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public void reconcile(BigDecimal amount, Function<String, Long> getter) {
            amount.add(value); getter.apply(value);
        }
    """)
    with pytest.raises(SystemExit, match="#reconcile"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_fully_qualified_value_parameters_need_no_import(tmp_path):
    _, policy, operations = fixture(tmp_path, """
        public void addOrder() { mapper.insert(order); }
        public Object calculate(java.math.BigDecimal amount) {
            return amount.add(value);
        }
    """)
    gate.validate_boundary_scan(tmp_path, policy, operations)


@pytest.mark.parametrize("function_type,import_statement", [
    ("Function<Order, Result>", "import java.util.function.Function;"),
    ("java.util.function.Function<Order, Result>", ""),
])
def test_function_callback_may_persist_and_requires_registration(tmp_path, function_type, import_statement):
    source, policy, operations = fixture(tmp_path, f"""
        public void addOrder() {{ mapper.insert(order); }}
        public void reconcile({function_type} writer) {{ writer.apply(order); }}
    """)
    source.write_text(source.read_text().replace("package demo;", "package demo; " + import_statement), encoding="utf-8")
    with pytest.raises(SystemExit, match="#reconcile"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


@pytest.mark.parametrize("receiver,imports", [
    ("BigDecimal", "import java.math.BigDecimal;"),
    ("java.math.BigDecimal", ""),
])
def test_immutable_arithmetic_method_reference_is_not_write(tmp_path, receiver, imports):
    source, policy, operations = fixture(tmp_path, f"""
        public void addOrder() {{ mapper.insert(order); }}
        public Object total() {{ return amounts.stream().reduce(java.math.BigDecimal.ZERO, {receiver}::add); }}
    """)
    source.write_text(source.read_text().replace("package demo;", "package demo; " + imports), encoding="utf-8")
    gate.validate_boundary_scan(tmp_path, policy, operations)


@pytest.mark.parametrize("body,imports", [
    ("public void reconcile() { amounts.forEach(BigDecimal::add); }", ""),
    ("public void reconcile(Writer BigDecimal) { amounts.forEach(BigDecimal::add); }", "import java.math.BigDecimal;"),
    ("private Writer BigDecimal; public void reconcile() { amounts.forEach(BigDecimal::add); }", "import java.math.BigDecimal;"),
    ("public void reconcile() { Writer BigDecimal = writer; amounts.forEach(BigDecimal::add); }", "import java.math.BigDecimal;"),
    ("class BigDecimal {} public void reconcile() { amounts.forEach(BigDecimal::add); }", "import java.math.BigDecimal;"),
    ("public void reconcile() { amounts.forEach(mapper::add); }", "import java.math.BigDecimal;"),
])
def test_unresolved_or_shadowed_arithmetic_reference_stays_blocked(tmp_path, body, imports):
    source, policy, operations = fixture(tmp_path, "public void addOrder() { mapper.insert(order); }" + body)
    source.write_text(source.read_text().replace("package demo;", "package demo; " + imports), encoding="utf-8")
    with pytest.raises(SystemExit, match="#reconcile"):
        gate.validate_boundary_scan(tmp_path, policy, operations)


def test_v2_policy_parses_all_locators_and_registers_each(tmp_path):
    import yaml
    source, policy, operations = fixture(tmp_path, "public void addOrder() { mapper.insert(row); } public void saveOrder() { mapper.update(row); }")
    values = dict(operations[0].values, sourceLocators=["demo.OrderService#addOrder", "demo.OrderService#saveOrder"], ownerRole="qa", testIds=["BDD-1"], domain="EDHR")
    policy_path = tmp_path / "policy.yaml"
    policy_path.write_text(yaml.safe_dump(dict(policyVersion="v2", operations=[values], coverageScope=policy["coverageScope"])), encoding="utf-8")
    version, parsed = gate.parse_policy(policy_path)
    assert version == "v2"
    assert len(parsed) == 1
    assert parsed[0].source_locators == values["sourceLocators"]
    assert gate.source_locator_exists(tmp_path, parsed[0])
    gate.validate_boundary_scan(tmp_path, policy, parsed)
    assert "demo.OrderService#saveOrder" in gate.canonical_report(version, parsed, {})
    invalid = gate.Operation(dict(values, sourceLocators=["demo.OrderService#addOrder", "demo.OrderService#missing"]))
    assert not gate.source_locator_exists(tmp_path, invalid)


@pytest.mark.parametrize("locators", [None, [], "demo.OrderService#addOrder", [""]])
def test_v2_policy_rejects_invalid_locator_array(tmp_path, locators):
    import yaml
    policy = tmp_path / "invalid.yaml"
    policy.write_text(yaml.safe_dump(dict(policyVersion="v2", operations=[dict(sourceLocators=locators)])), encoding="utf-8")
    with pytest.raises(SystemExit, match="sourceLocators"):
        gate.parse_policy(policy)
