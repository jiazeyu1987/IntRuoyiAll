"""Offline preparation/validation only. No database driver, subprocess or network access."""
from __future__ import annotations

import argparse
import copy
import gzip
import hashlib
import json
from pathlib import Path
import re

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
OMIT = {
    "20260922_dcc_three_workflow_bpmn_seed", "20260923_dcc_three_workflow_candidate_strategy_fix",
    "20260926_dcc_three_workflow_matrix_multi_instance_fix", "20260719_business_approval_policy",
    "20260719_dcc_upload_form_policy_seed", "20260720_dcc_publish_form_policy_seed",
    "20260906_dcc_new_file_lifecycle_p4", "20260907_dcc_publication_notification",
}


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def dump_json(path, value):
    Path(path).write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def quoted_value(value):
    if value is None:
        return None
    text = str(value)
    if text.upper() == "NULL":
        return None
    if re.fullmatch(r"b'[01]+'", text, re.I):
        return str(int(text[2:-1], 2))
    if text.startswith("'") and text.endswith("'"):
        return text[1:-1].replace("''", "'").replace("\\'", "'")
    if re.fullmatch(r"current_timestamp(?:\(\))?", text, re.I):
        return "current_timestamp"
    return text


def normalize_type(value):
    text = re.sub(r"\s+", " ", str(value).lower()).strip()
    return "bit(1)" if text == "bit" else text


def normalize_extra(value):
    text = str(value).lower()
    return {"autoIncrement": "auto_increment" in text, "generated": "stored" if "stored generated" in text else "virtual" if "virtual generated" in text else None,
            "onUpdate": "current_timestamp" if "on update current_timestamp" in text else None}


def expression_tokens(value):
    # Only the two simple CASE expressions in this prerequisite scope are supported.
    # MySQL adds parentheses/backticks/charset literal introducers; preserve literal case and operators.
    text = str(value).replace("\\'", "'")
    text = re.sub(r"_[a-zA-Z0-9]+(?=')", "", text)
    tokens = re.findall(r"'(?:''|[^'])*'|`[^`]+`|[a-zA-Z_][a-zA-Z0-9_]*|[0-9]+|<>|<=|>=|[=+*/%-]", text)
    return [t if t.startswith("'") else t.strip("`").lower() for t in tokens]


def end_parenthesis(text, begin):
    depth, quote = 0, None
    i = begin
    while i < len(text):
        char = text[i]
        if quote:
            if char == quote and i + 1 < len(text) and text[i + 1] == quote:
                i += 2
                continue
            if char == quote and (i == 0 or text[i - 1] != "\\"):
                quote = None
        elif char in "'\"`":
            quote = char
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError("Unbalanced CREATE TABLE definition")


def split_body(body):
    parts, start, depth, quote = [], 0, 0, None
    i = 0
    while i < len(body):
        c = body[i]
        if quote:
            if c == quote and i + 1 < len(body) and body[i + 1] == quote:
                i += 2
                continue
            if c == quote and (i == 0 or body[i - 1] != "\\"):
                quote = None
        elif c in "'\"`":
            quote = c
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
        elif c == "," and depth == 0:
            parts.append(body[start:i].strip())
            start = i + 1
        i += 1
    parts.append(body[start:].strip())
    return parts


def parse_tables(text):
    result = {}
    for match in re.finditer(r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?\s*\(", text, re.I):
        name = match.group(1)
        end = end_parenthesis(text, match.end() - 1)
        tail = text[end + 1:text.index(";", end)]
        engine = re.search(r"ENGINE\s*=\s*(\w+)", tail, re.I)
        collation = re.search(r"COLLATE\s*=\s*(\w+)", tail, re.I)
        table = {"engine": engine.group(1) if engine else None, "collation": collation.group(1) if collation else None, "columns": {}, "indexes": {}}
        for part in split_body(text[match.end():end]):
            if re.match(r"(?:CONSTRAINT\s+`?\w+`?\s+)?(?:FOREIGN\s+KEY|CHECK)\b", part, re.I):
                continue  # None of the17 scoped prerequisite SQLs adds a foreign-key contract.
            index = re.match(r"(PRIMARY\s+KEY|UNIQUE\s+(?:KEY|INDEX)|KEY|INDEX)\s*(?:`?(\w+)`?\s*)?\((.*)\)(?:\s+.*)?$", part, re.I | re.S)
            if index:
                kind, index_name, fields = index.groups()
                index_name = "PRIMARY" if kind.upper().startswith("PRIMARY") else index_name
                if not index_name:
                    raise ValueError(f"Unnamed index needs explicit review: {name}: {part}")
                cols = []
                for item in split_body(fields):
                    key = re.fullmatch(r"`?(\w+)`?(?:\((\d+)\))?(?:\s+(ASC|DESC))?", item.strip(), re.I)
                    if not key:
                        raise ValueError(f"Unsupported index expression: {part}")
                    cols.append({"column": key.group(1), "prefix": int(key.group(2)) if key.group(2) else None, "direction": "D" if (key.group(3) or "ASC").upper() == "DESC" else "A", "expression": None})
                table["indexes"][index_name] = {"unique": kind.upper().startswith(("PRIMARY", "UNIQUE")), "type": "BTREE", "columns": cols}
                continue
            column = re.match(r"`?(\w+)`?\s+([a-zA-Z]+(?:\([0-9,]+\))?(?:\s+unsigned)?)(.*)", part, re.S)
            if not column:
                raise ValueError(f"Unsupported table definition: {name}: {part}")
            field, raw_type, rest = column.groups()
            default = re.search(r"\bDEFAULT\s+(b'[01]+'|'(?:''|\\.|[^'])*'|CURRENT_TIMESTAMP(?:\(\))?|NULL|[+-]?\d+(?:\.\d+)?)", rest, re.I)
            coll = re.search(r"\bCOLLATE\s+(\w+)", rest, re.I)
            nullable = "NO" if re.search(r"\bNOT\s+NULL\b|\bPRIMARY\s+KEY\b", rest, re.I) else "YES"
            generated = re.search(r"GENERATED\s+ALWAYS\s+AS\s*\(", rest, re.I)
            expression = ""
            if generated:
                stop = end_parenthesis(rest, generated.end() - 1)
                expression = rest[generated.end():stop]
            kind = "STORED GENERATED" if generated and re.search(r"\bSTORED\b", rest, re.I) else "VIRTUAL GENERATED" if generated else ""
            extra = " ".join(filter(None, ["auto_increment" if "AUTO_INCREMENT" in rest.upper() else "", kind, "on update CURRENT_TIMESTAMP" if "ON UPDATE CURRENT_TIMESTAMP" in rest.upper() else ""]))
            table["columns"][field] = {"type": normalize_type(raw_type), "nullable": nullable, "collation": coll.group(1) if coll else (table["collation"] if re.match(r"(?:char|varchar|text|longtext)", raw_type, re.I) else None), "default": quoted_value(default.group(1)) if default else None, "extra": normalize_extra(extra), "expression": expression}
        result[name] = table
    return result


BASE_FIELDS = {
    "dcc_controlled_file": ["id", "master_id", "category_id", "directory_id", "source_file_id", "file_name", "file_number", "version_no", "status", "tenant_id", "requester_id", "process_instance_id", "process_definition_key", "deleted"],
    "dcc_controlled_file_master": ["id", "category_id", "current_active_controlled_file_id", "tenant_id", "deleted"],
    "dcc_project_code": ["id", "project_name", "project_code", "status", "tenant_id", "deleted"],
    "dcc_controlled_file_signature": ["id", "controlled_file_id", "task_id", "actor_id", "comment", "signed_at", "version_no", "tenant_id", "deleted"],
    "dcc_controlled_file_obsolete_audit": ["id", "controlled_file_id", "operator_id", "tenant_id", "deleted"],
    "dcc_controlled_file_access_log": ["id", "controlled_file_id", "reason", "tenant_id", "deleted"],
    "dcc_file_category": ["id", "code", "active", "tenant_id", "deleted"],
    "dcc_file_directory": ["id", "parent_id", "code", "name", "active", "tenant_id", "deleted"],
    "dcc_category_approval_route": ["id", "category_id", "version_no", "active", "tenant_id", "deleted"],
    "dcc_category_approval_route_node": ["id", "route_id", "stage_code", "candidate_source_type", "candidate_source_id", "tenant_id", "deleted"],
}


def prepare(root_task, protected_dump):
    root_task, protected_dump = Path(root_task), Path(protected_dump)
    receipt = json.loads((root_task / "g18-backup-receipt.json").read_text(encoding="utf-8"))
    expected_dump = next(a for a in receipt["artifacts"] if a["kind"] == "schema")
    if sha(protected_dump) != expected_dump["sha256"]:
        raise ValueError("Protected schema dump SHA mismatch")
    dump = parse_tables(gzip.decompress(protected_dump.read_bytes()).decode("utf-8"))
    checklist_path = root_task / "g20-external-prerequisite-checklist.json"
    checklist = json.loads(checklist_path.read_text(encoding="utf-8"))
    selected = [p for p in checklist["prerequisites"] if p["migrationId"] not in OMIT]
    if len(selected) != 17:
        raise ValueError("Expected exactly17 structural prerequisites")
    contract = {"version": "G21-1", "status": "prepared_read_only_contract_not_execution", "executionAuthorized": False,
                "database": "ruoyi-vue-pro", "tables": {}, "columns": {}, "indexes": {}, "forbiddenIndexes": {}, "prerequisites": [], "ledger": {},
                "scalars": {"route_empty_action_type": 0, "notify_duplicate_business_keys": 0},
                "sources": {"checklist": str(checklist_path), "checklistSha256": sha(checklist_path), "protectedDumpSha256": sha(protected_dump), "currentSqlHashes": {},
                            "schemaCompleteSha256": sha(root_task / "g14-schema-complete.log"), "defaultsSha256": sha(root_task / "g18-runtime-configuration-preflight.log"),
                            "completeLedgerSha256": sha(root_task / "g13-runtime-complete-ledger.log"), "originalSourceProofSha256": sha(root_task / "g17-ledger-source-search.json")},
                "scopeBoundary": "Current prerequisites only. Extra/new candidate fields ignored. No APPLY/APPLIED state inferred, no data or menu replay.", "compatibilityReview": []}
    def take(table, fields=None, index_names=None, reason="formal migration CREATE contract"):
        if table not in dump:
            raise ValueError(f"Required protected table missing: {table}")
        actual = dump[table]
        contract["tables"][table] = {"engine": actual["engine"], "collation": actual["collation"], "basis": reason}
        for col in (fields or actual["columns"].keys()):
            if col not in actual["columns"]:
                raise ValueError(f"Protected prerequisite column missing {table}.{col}")
            contract["columns"].setdefault(table, {})[col] = copy.deepcopy(actual["columns"][col])
        for index_name in (index_names if index_names is not None else actual["indexes"].keys()):
            if index_name not in actual["indexes"]:
                raise ValueError(f"Protected prerequisite index missing {table}.{index_name}")
            contract["indexes"].setdefault(table, {})[index_name] = copy.deepcopy(actual["indexes"][index_name])
    for prerequisite in selected:
        migration_id = prerequisite["migrationId"]
        sql_file = REPO / "IntRuoyiBackend" / "sql" / "mysql" / (migration_id + ".sql")
        current_sha = sha(sql_file)
        if current_sha != prerequisite["currentFileSha256"]:
            raise ValueError(f"Current prerequisite SQL drift: {migration_id}")
        contract["sources"]["currentSqlHashes"][migration_id] = current_sha
        sql = sql_file.read_text(encoding="utf-8")
        declared = parse_tables(sql)
        tables = []
        if migration_id == "20260513_dcc_base_schema":
            for table, fields in BASE_FIELDS.items():
                formal = declared.get(table, {})
                for field in fields:
                    declared_column = formal.get("columns", {}).get(field)
                    if declared_column and declared_column["type"] != dump[table]["columns"][field]["type"]:
                        # reason255 is precisely the existing source for the candidate2000 forward ALTER.
                        if not (table == "dcc_controlled_file_access_log" and field == "reason" and dump[table]["columns"][field]["type"] == "varchar(255)"):
                            raise ValueError(f"Base prerequisite type differs {table}.{field}")
                take(table, fields, ["PRIMARY"], "Reviewed existing legacy base fields needed by19 forward candidates; not full current base CREATE")
                tables.append(table)
            contract["compatibilityReview"].append({"migrationId": migration_id, "rule": "Legacy nullable file identity fields and existing old base shapes are frozen as read prerequisites. No current base added fields are demanded. reason255 and obsolete operator nonnullable are expected before forward ALTER."})
        elif migration_id == "20260615_system_config_package_menu":
            for table in ("system_menu", "system_role_menu", "system_tenant_package"):
                take(table, index_names=["PRIMARY"], reason="Exact protected base table shape plus matching historical menu ledger; no menu/role/package DML certification")
                tables.append(table)
        else:
            for table, expected in declared.items():
                if table.startswith("tmp_"):
                    continue
                actual = dump.get(table)
                if not actual:
                    raise ValueError(f"Protected prerequisite table missing {table}")
                if expected["engine"] and actual["engine"] != expected["engine"]:
                    raise ValueError(f"Formal/protected table engine differs {table}")
                if expected["collation"] and actual["collation"] != expected["collation"]:
                    raise ValueError(f"Formal/protected table collation differs {table}")
                for col, spec in expected["columns"].items():
                    found = actual["columns"].get(col)
                    if not found:
                        raise ValueError(f"Formal prerequisite column missing {migration_id}:{table}.{col}")
                    for field in ("type", "nullable", "default", "extra"):
                        if spec[field] != found[field]:
                            raise ValueError(f"Formal/protected shape differs {migration_id}:{table}.{col}.{field}: {spec[field]!r} vs {found[field]!r}")
                    if expected["collation"] and spec["collation"] != found["collation"]:
                        raise ValueError(f"Formal collation differs {table}.{col}")
                    if spec["expression"] and expression_tokens(spec["expression"]) != expression_tokens(found["expression"]):
                        raise ValueError(f"Formal generated expression differs {table}.{col}")
                for name, spec in expected["indexes"].items():
                    if actual["indexes"].get(name) != spec:
                        raise ValueError(f"Formal/protected index differs {table}.{name}")
                take(table, list(expected["columns"]), list(expected["indexes"]), "Formal SQL declaration verified against protected dump; unspecified table collation frozen from actual dump")
                tables.append(table)
            if migration_id == "20260921_dcc_category_approval_route_action_type":
                take("dcc_category_approval_route", ["action_type"], ["uk_dcc_category_route_action_version"])
                contract["forbiddenIndexes"]["dcc_category_approval_route"] = ["uk_dcc_category_route_version"]
                tables.append("dcc_category_approval_route")
            if migration_id == "20260815_system_notify_message_business_key":
                take("system_notify_message", ["id", "tenant_id", "business_key", "deleted"], ["PRIMARY", "uk_system_notify_message_tenant_business_key"])
                tables.append("system_notify_message")
            if migration_id == "20260906_dcc_new_file_lifecycle_p3":
                take("dcc_controlled_file", ["revision_base_active_controlled_file_id"], [])
                tables.append("dcc_controlled_file")
            if migration_id == "20260906_dcc_new_file_lifecycle_p2":
                take("dcc_controlled_file", ["checked_out_reason", "predecessor_controlled_file_id", "source_sha256", "previous_source_sha256", "change_description"], [])
                tables.append("dcc_controlled_file")
            if migration_id == "20260719_dcc_file_type_taxonomy":
                for table in ("dcc_file_category", "dcc_controlled_file", "dcc_controlled_file_recognition_record"):
                    take(table, ["file_type_taxonomy_id"], [])
                    tables.append(table)
            if migration_id == "20260717_bpm_form_center":
                take("system_tenant_package", ["menu_ids"], [])
                tables.append("system_tenant_package")
                contract["compatibilityReview"].append({"migrationId": migration_id, "rule": "menu_ids is current LONGTEXT NOT NULL unicode_ci, strengthened by20260724_system_codex_test_management. Freeze existing larger type; do not shrink to old TEXT or rerun menu grants."})
        contract["prerequisites"].append({"migrationId": migration_id, "migrationMustNotExecute": True, "requiredBy": prerequisite["requiredBy"], "tables": sorted(set(tables)),
                                           "assertionMeaning": "structure facts only; exact historical ledger kept separately; no invented execution history"})
    # Preserve6 prior ledger facts in this scope, including2 exact historical sources, not current replacements.
    ledger_lines = (root_task / "g13-runtime-complete-ledger.log").read_text(encoding="utf-8").splitlines()
    historical_sources = json.loads((root_task / "g17-ledger-source-search.json").read_text(encoding="utf-8"))
    expected_historical = {item["migrationId"]: item["ledgerSha256"] for item in historical_sources}
    ids = {p["migrationId"] for p in selected}
    for line in ledger_lines:
        values = line.split("\t")
        if len(values) >= 4 and values[0] in ids:
            if values[0] in contract["ledger"]:
                raise ValueError("Duplicate ledger records in source")
            contract["ledger"][values[0]] = {"sha256": values[1], "status": values[2], "environment": values[3]}
            expected_sha = expected_historical.get(values[0], contract["sources"]["currentSqlHashes"][values[0]])
            if values[1] != expected_sha or values[2] not in ("APPLIED", "SKIPPED_ALREADY_APPLIED") or values[3] != "test":
                raise ValueError(f"Previous ledger is not exact original evidence: {values[0]}")
    if len(contract["ledger"]) != 6:
        raise ValueError("Expected4 current and2 exact historical ledger facts in this17 scope")
    dump_json(HERE / "g21-structure-prerequisites.json", contract)
    (HERE / "g21-structure-prerequisite-queries.sql").write_text(build_queries(contract), encoding="utf-8")
    return contract


def build_queries(contract):
    names = ",".join("'" + t + "'" for t in sorted(contract["tables"]))
    ledger_names = ",".join("'" + m + "'" for m in sorted(contract["ledger"]))
    # JSON preserves null/default/empty strings and expression literal case. Root runs with --batch --raw --skip-column-names.
    return f"""-- G21 read-only collection only; do not concatenate migration SQL. No session writes/DDL/routines.
-- Run in explicitly selected ruoyi-vue-pro; validator rejects different database.
SELECT JSON_OBJECT('kind','runtime','database',DATABASE(),'version',@@version,'hostname',@@hostname);
SELECT JSON_OBJECT('kind','table','table',TABLE_NAME,'engine',ENGINE,'collation',TABLE_COLLATION)
 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({names}) ORDER BY TABLE_NAME;
SELECT JSON_OBJECT('kind','column','table',TABLE_NAME,'column',COLUMN_NAME,'type',COLUMN_TYPE,
 'nullable',IS_NULLABLE,'collation',COLLATION_NAME,'default',COLUMN_DEFAULT,'extra',EXTRA,'expression',GENERATION_EXPRESSION)
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({names}) ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT JSON_OBJECT('kind','index','table',TABLE_NAME,'name',INDEX_NAME,'unique',NON_UNIQUE=0,
 'sequence',SEQ_IN_INDEX,'column',COLUMN_NAME,'prefix',SUB_PART,'direction',COLLATION,'expression',EXPRESSION,'type',INDEX_TYPE)
 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({names}) ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;
SELECT JSON_OBJECT('kind','ledger','migrationId',migration_id,'sha256',sha256,'status',status,'environment',target_environment)
 FROM infra_release_migration WHERE deleted=b'0' AND target_environment='test' AND migration_id IN ({ledger_names}) ORDER BY migration_id;
SELECT JSON_OBJECT('kind','scalar','name','route_empty_action_type','value',COUNT(*)) FROM dcc_category_approval_route WHERE action_type IS NULL OR BINARY action_type=BINARY '';
SELECT JSON_OBJECT('kind','scalar','name','notify_duplicate_business_keys','value',COUNT(*)) FROM
 (SELECT tenant_id,business_key FROM system_notify_message WHERE business_key IS NOT NULL GROUP BY tenant_id,business_key HAVING COUNT(*)>1) duplicate_keys;
"""


def split_select_statements(sql):
    without_comments = re.sub(r"^\s*--.*$", "", sql, flags=re.M)
    return [s.strip() for s in without_comments.split(";") if s.strip()]


def positive_fixture(contract):
    return {"database": contract["database"], "tables": {k: {x: v[x] for x in ("engine", "collation")} for k, v in contract["tables"].items()},
            "columns": copy.deepcopy(contract["columns"]), "indexes": copy.deepcopy(contract["indexes"]), "ledger": copy.deepcopy(contract["ledger"]), "scalars": copy.deepcopy(contract["scalars"])}


def read_facts(path):
    facts = {"database": None, "tables": {}, "columns": {}, "indexes": {}, "ledger": {}, "scalars": {}}
    runtime_seen = False
    for number, line in enumerate(Path(path).read_text(encoding="utf-8-sig").splitlines(), 1):
        if not line.strip():
            continue
        try:
            row = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ValueError(f"Fact log line{number} is not JSON (mysql warning/error must not be hidden): {exc}") from exc
        kind = row.get("kind")
        if kind == "runtime":
            if runtime_seen:
                raise ValueError("Duplicate runtime identity")
            runtime_seen = True
            facts["database"] = row["database"]
        elif kind == "table":
            if row["table"] in facts["tables"]:
                raise ValueError("Duplicate table fact")
            facts["tables"][row["table"]] = {k: row[k] for k in ("engine", "collation")}
        elif kind == "column":
            cols = facts["columns"].setdefault(row["table"], {})
            if row["column"] in cols:
                raise ValueError("Duplicate column fact")
            cols[row["column"]] = {k: row[k] for k in ("type", "nullable", "collation", "default", "extra", "expression")}
            cols[row["column"]]["extra"] = normalize_extra(cols[row["column"]]["extra"])
            cols[row["column"]]["default"] = quoted_value(cols[row["column"]]["default"])
            cols[row["column"]]["type"] = normalize_type(cols[row["column"]]["type"])
        elif kind == "index":
            indexes = facts["indexes"].setdefault(row["table"], {})
            index = indexes.setdefault(row["name"], {"unique": bool(row["unique"]), "type": row["type"], "columns": []})
            if row["sequence"] != len(index["columns"]) + 1 or bool(row["unique"]) != index["unique"]:
                raise ValueError("Index fact order/uniqueness changed within one index")
            index["columns"].append({k: row[k] for k in ("column", "prefix", "direction", "expression")})
        elif kind == "ledger":
            if row["migrationId"] in facts["ledger"]:
                raise ValueError("Duplicate ledger fact")
            facts["ledger"][row["migrationId"]] = {k: row[k] for k in ("sha256", "status", "environment")}
        elif kind == "scalar":
            if row["name"] in facts["scalars"]:
                raise ValueError("Duplicate scalar fact")
            facts["scalars"][row["name"]] = row["value"]
        else:
            raise ValueError(f"Unknown fact kind: {kind}")
    return facts


def validate_facts(contract, facts):
    errors = []
    if facts.get("database") != contract["database"]:
        errors.append("runtime.database must be " + contract["database"])
    for table, expected in contract["tables"].items():
        actual = facts.get("tables", {}).get(table, {})
        for field in ("engine", "collation"):
            if actual.get(field) != expected[field]:
                errors.append(f"{table}.{field}: expected {expected[field]!r}, got {actual.get(field)!r}")
    for table, columns in contract["columns"].items():
        for column, expected in columns.items():
            actual = facts.get("columns", {}).get(table, {}).get(column)
            if actual is None:
                errors.append(f"{table}.{column}: missing prerequisite column")
                continue
            for field in ("type", "nullable", "collation", "default", "extra"):
                if actual.get(field) != expected[field]:
                    errors.append(f"{table}.{column}.{field}: expected {expected[field]!r}, got {actual.get(field)!r}")
            if expression_tokens(actual.get("expression", "")) != expression_tokens(expected["expression"]):
                errors.append(f"{table}.{column}.expression: generated expression mismatch")
    for table, indexes in contract["indexes"].items():
        for name, expected in indexes.items():
            if facts.get("indexes", {}).get(table, {}).get(name) != expected:
                errors.append(f"{table}.{name}: unique/nonunique, ordered columns, prefix, expression or BTREE shape mismatch")
    for table, names in contract["forbiddenIndexes"].items():
        for name in names:
            if name in facts.get("indexes", {}).get(table, {}):
                errors.append(f"{table}.{name}: obsolete route index is present")
    for migration_id, expected in contract["ledger"].items():
        actual = facts.get("ledger", {}).get(migration_id, {})
        for field in ("sha256", "status", "environment"):
            if actual.get(field) != expected[field]:
                errors.append(f"{migration_id}.ledger.{field}: original prior evidence must be preserved")
    for key, expected in contract["scalars"].items():
        if facts.get("scalars", {}).get(key) != expected:
            errors.append(f"{key}: expected {expected}, got {facts.get('scalars', {}).get(key)!r}")
    prerequisite_results = []
    scalar_owners = {"20260921_dcc_category_approval_route_action_type": ["route_empty_action_type"],
                     "20260815_system_notify_message_business_key": ["notify_duplicate_business_keys"]}
    for item in contract["prerequisites"]:
        prefixes = [t + "." for t in item["tables"]] + [item["migrationId"] + ".ledger.", "runtime.database"] + scalar_owners.get(item["migrationId"], [])
        own_errors = [e for e in errors if any(e.startswith(p) for p in prefixes)]
        prerequisite_results.append({"migrationId": item["migrationId"], "status": "FAIL" if own_errors else "SATISFIED_READ_ONLY_FACTS_NOT_APPLIED", "migrationMustNotExecute": True, "errors": own_errors})
    return {"status": "FAIL" if errors else "SATISFIED_READ_ONLY_FACTS_NOT_APPLIED", "executionAuthorized": False, "prerequisiteCount": len(contract["prerequisites"]),
            "errors": errors, "prerequisites": prerequisite_results, "meaning": "Actual collected prerequisite facts only. Never inserts/updates ledger or declares an unrecorded migration APPLIED."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    build = sub.add_parser("prepare")
    build.add_argument("--root-task", required=True)
    build.add_argument("--protected-schema", required=True)
    validate = sub.add_parser("validate")
    validate.add_argument("--facts", required=True)
    validate.add_argument("--result", required=True)
    args = parser.parse_args()
    if args.command == "prepare":
        contract = prepare(args.root_task, args.protected_schema)
        print(json.dumps({"status": contract["status"], "prerequisites": len(contract["prerequisites"]), "tables": len(contract["tables"]), "columns": sum(map(len, contract["columns"].values()))}, ensure_ascii=False))
    else:
        contract = json.loads((HERE / "g21-structure-prerequisites.json").read_text(encoding="utf-8"))
        try:
            result = validate_facts(contract, read_facts(args.facts))
        except (ValueError, KeyError) as exc:
            result = {"status": "FAIL", "executionAuthorized": False, "errors": [str(exc)]}
        dump_json(args.result, result)
        print(json.dumps(result, ensure_ascii=False))
        if result["status"] == "FAIL":
            raise SystemExit(1)


if __name__ == "__main__":
    main()
