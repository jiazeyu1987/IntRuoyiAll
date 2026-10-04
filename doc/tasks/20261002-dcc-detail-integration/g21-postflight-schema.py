"""Prepare/validate final MySQL schema from19 candidates; offline only, noDB/network/subprocess."""
from __future__ import annotations

import argparse
import copy
import gzip
import hashlib
import importlib.util
import json
from pathlib import Path
import re

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
spec = importlib.util.spec_from_file_location("prerequisite", HERE / "g21_structure_prerequisites.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)


class ExpressionParser:
    def __init__(self, text):
        text = str(text).replace("\\'", "'")
        text = re.sub(r"_[a-zA-Z0-9]+(?=')", "", text)
        self.tokens = re.findall(r"'(?:''|[^'])*'|`[^`]+`|[a-zA-Z_][a-zA-Z0-9_]*|[0-9]+|<>|<=|>=|[(),=<>+*/%-]", text)
        self.tokens = [t if t.startswith("'") else t.strip("`").lower() for t in self.tokens]
        self.at = 0

    def peek(self):
        return self.tokens[self.at] if self.at < len(self.tokens) else None

    def eat(self, wanted=None):
        token = self.peek()
        if wanted is not None and token != wanted:
            raise ValueError(f"Generated expression expected{wanted}, got{token}")
        self.at += 1
        return token

    def expr(self, minimum=0):
        token = self.eat()
        if token is None:
            raise ValueError("Missing generated expression")
        if token == "(":
            value = self.expr()
            self.eat(")")
        elif token == "case":
            branches = []
            while self.peek() == "when":
                self.eat(); condition = self.expr(); self.eat("then"); outcome = self.expr()
                branches.append([condition, outcome])
            self.eat("else"); otherwise = self.expr(); self.eat("end")
            value = ["case", branches, otherwise]
        elif self.peek() == "(":
            self.eat()
            if token in ("convert", "cast"):
                arg = self.expr()
                self.eat("using" if token == "convert" else "as")
                self.eat("binary"); self.eat(")")
                value = ["binaryCast", arg]
            else:
                args = []
                if self.peek() != ")":
                    args.append(self.expr())
                    while self.peek() == ",":
                        self.eat(); args.append(self.expr())
                self.eat(")")
                value = ["function", token, args]
        else:
            value = ["literal" if token.startswith("'") or token.isdigit() or token == "null" else "identifier", token]
        precedence = {"or": 1, "and": 2, "=": 3, "<>": 3, ">": 3, "<": 3, ">=": 3, "<=": 3, "is": 3, "+": 4, "-": 4, "*": 5, "/": 5}
        while self.peek() in precedence and precedence[self.peek()] >= minimum:
            operator = self.eat()
            if operator == "is":
                negate = self.peek() == "not"
                if negate: self.eat()
                self.eat("null")
                value = ["isNotNull" if negate else "isNull", value]
            else:
                value = ["operator", operator, value, self.expr(precedence[operator] + 1)]
        return value


def expression_ast(text):
    if not str(text).strip():
        return None
    parser = ExpressionParser(text)
    result = parser.expr()
    if parser.peek() is not None:
        raise ValueError("Unsupported generated expression tail: " + str(parser.tokens[parser.at:]))
    return result


def charset_from_collation(collation):
    return str(collation).split("_", 1)[0] if collation else None


def column_definition(part, table):
    match = re.match(r"`?(\w+)`?\s+([a-zA-Z]+(?:\([0-9,]+\))?(?:\s+unsigned)?)(.*)", part, re.S)
    if not match:
        raise ValueError("Unsupported column definition: " + part)
    name, data_type, rest = match.groups()
    charset = re.search(r"CHARACTER\s+SET\s+(\w+)", rest, re.I)
    collation = re.search(r"COLLATE\s+(\w+)", rest, re.I)
    is_text = bool(re.match(r"(?:char|varchar|text|longtext)", data_type, re.I))
    if is_text:
        col = collation.group(1) if collation else "$environment.utf8mb4DefaultCollation" if charset and charset.group(1).lower() == "utf8mb4" else table["collation"]
        char = charset.group(1) if charset else charset_from_collation(col) if not str(col).startswith("$") else table["charset"]
    else:
        char, col = None, None
    default = re.search(r"\bDEFAULT\s+(b'[01]+'|'(?:''|\\.|[^'])*'|CURRENT_TIMESTAMP(?:\(\))?|NULL|[+-]?\d+(?:\.\d+)?)", rest, re.I)
    generated = re.search(r"GENERATED\s+ALWAYS\s+AS\s*\(", rest, re.I)
    nullable_definition = rest[:generated.start()] if generated else re.split(r"\bCOMMENT\b", rest, flags=re.I)[0]
    expression = ""
    if generated:
        end = base.end_parenthesis(rest, generated.end() - 1)
        expression = rest[generated.end():end].strip()
        expression_ast(expression)  # Fail preparation instead of silently approximating unsupported SQL.
    kind = "STORED GENERATED" if generated and re.search(r"\bSTORED\b", rest, re.I) else "VIRTUAL GENERATED" if generated else ""
    extra = " ".join(filter(None, ["auto_increment" if "AUTO_INCREMENT" in rest.upper() else "", kind, "on update CURRENT_TIMESTAMP" if "ON UPDATE CURRENT_TIMESTAMP" in rest.upper() else ""]))
    return name, {"type": base.normalize_type(data_type), "nullable": "NO" if re.search(r"\bNOT\s+NULL\b|\bPRIMARY\s+KEY\b", nullable_definition, re.I) else "YES",
                  "charset": char, "collation": col, "default": base.quoted_value(default.group(1)) if default else None,
                  "extra": base.normalize_extra(extra), "expression": expression}


def index_definition(part):
    match = re.match(r"(?:(?:CONSTRAINT)\s+`?(\w+)`?\s+)?(PRIMARY\s+KEY|UNIQUE(?:\s+KEY|\s+INDEX)?|KEY|INDEX)\s*(?:`?(\w+)`?\s*)?\((.*)\)(?:\s+.*)?$", part, re.I | re.S)
    if not match:
        raise ValueError("Unsupported index definition: " + part)
    constraint_name, kind, name, fields = match.groups()
    columns = []
    for field in base.split_body(fields):
        key = re.fullmatch(r"`?(\w+)`?(?:\((\d+)\))?(?:\s+(ASC|DESC))?", field, re.I)
        if not key: raise ValueError("Unsupported key expression: " + field)
        columns.append({"column": key.group(1), "prefix": int(key.group(2)) if key.group(2) else None, "direction": "D" if (key.group(3) or "ASC").upper() == "DESC" else "A", "expression": None})
    anonymous = not name and not constraint_name and not kind.upper().startswith("PRIMARY")
    name = "PRIMARY" if kind.upper().startswith("PRIMARY") else name or constraint_name or "@ordered-unique:" + ",".join(c["column"] for c in columns)
    return name, {"unique": kind.upper().startswith(("PRIMARY", "UNIQUE")), "type": "BTREE", "columns": columns}, anonymous


def parse_create_tables(text, baseline=False, only_names=None):
    tables = {}
    for match in re.finditer(r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?\s*\(", text, re.I):
        name = match.group(1)
        if only_names is not None and name not in only_names: continue
        end = base.end_parenthesis(text, match.end() - 1)
        tail = text[end + 1:text.index(";", end)]
        engine = re.search(r"ENGINE\s*=\s*(\w+)", tail, re.I)
        charset = re.search(r"(?:DEFAULT\s+)?CHARSET\s*=\s*(\w+)", tail, re.I)
        coll = re.search(r"COLLATE\s*=\s*(\w+)", tail, re.I)
        table = {"engine": engine.group(1) if engine else "$environment.defaultStorageEngine", "charset": charset.group(1) if charset else "$environment.databaseCharset",
                 "collation": coll.group(1) if coll else "$environment.utf8mb4DefaultCollation" if charset and charset.group(1).lower() == "utf8mb4" else "$environment.databaseCollation"}
        columns, indexes, anonymous = {}, {}, []
        for part in base.split_body(text[match.end():end]):
            if re.match(r"(?:CONSTRAINT\s+`?\w+`?\s+)?(?:FOREIGN\s+KEY|CHECK)\b", part, re.I): continue
            if re.match(r"(?:CONSTRAINT\s+|PRIMARY\s+KEY|UNIQUE|KEY\s+|INDEX\s+)", part, re.I):
                key, shape, unknown_name = index_definition(part)
                if key in indexes: raise ValueError("Duplicate index shape definition " + name + "." + key)
                indexes[key] = shape
                if unknown_name: anonymous.append(key)
            else:
                field, shape = column_definition(part, table)
                columns[field] = shape
                if re.search(r"\bPRIMARY\s+KEY\b", part, re.I):
                    indexes["PRIMARY"] = {"unique": True, "type": "BTREE", "columns": [{"column": field, "prefix": None, "direction": "A", "expression": None}]}
        tables[name] = {"table": table, "columns": columns, "indexes": indexes, "anonymousIndexes": anonymous}
    return tables


def ddl_statements(sql):
    # Direct ALTERs plus string-literal DDL used by guarded CALL helpers. Discard dynamic concatenated templates.
    result = []
    for literal in re.finditer(r"'(?:''|[^'])*'", sql, re.S):
        value = literal.group(0)[1:-1].replace("''", "'")
        if re.match(r"ALTER\s+TABLE\b", value, re.I) and re.search(r"(?:ADD|MODIFY)\s+COLUMN\s+`?\w+`?\s+(?:BIGINT|INT|VARCHAR|CHAR|VARBINARY|DATETIME|LONGTEXT)\b|ADD\s+(?:UNIQUE\s+)?(?:KEY|INDEX)|DROP\s+INDEX", value, re.I): result.append(value)
    for match in re.finditer(r"(?m)^\s*ALTER\s+TABLE\s+`?\w+`?\s+([\s\S]*?);", sql, re.I): result.append(match.group(0).strip().removesuffix(";"))
    return result


def prepare(root_task):
    root_task = Path(root_task)
    scope_path = root_task / "g18-approved-scope-candidate.json"
    scope = json.loads(scope_path.read_text(encoding="utf-8"))
    if len(scope["executionOrder"]) != 19: raise ValueError("Expected19 approved-scope candidates")
    receipt = json.loads((root_task / "g18-backup-receipt.json").read_text(encoding="utf-8"))
    backup = next(a for a in receipt["artifacts"] if a["kind"] == "schema")
    if base.sha(backup["path"]) != backup["sha256"]: raise ValueError("Protected schema SHA changed")
    candidate_sql = [(item, (REPO / "IntRuoyiBackend" / item["file"]).read_text(encoding="utf-8")) for item in scope["executionOrder"]]
    affected_names = {match.group(1) for _, sql in candidate_sql for match in re.finditer(r"(?:CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?|ALTER\s+TABLE\s+)`?(\w+)`?", sql, re.I)}
    original = parse_create_tables(gzip.decompress(Path(backup["path"]).read_bytes()).decode("utf-8"), True, affected_names)
    target, affected = {}, set()
    new_tables, added_columns = [], set()
    contract = {"version": "G21-POST-1", "executionAuthorized": False, "status": "prepared_exact_schema_postflight_pending_execution", "candidates": [],
                "allowedDatabases": [scope["sourceDatabase"], scope["rehearsalDatabase"]], "tables": {}, "columns": {}, "indexes": {}, "anonymousIndexes": {}, "absentIndexes": {},
                "sources": {"candidateScopeSha256": base.sha(scope_path), "protectedSchemaSha256": backup["sha256"], "prerequisiteContractSha256": base.sha(HERE / "g21-structure-prerequisites.json")},
                "runtimeParametersRequiredBeforeMigration": ["database", "serverUuid", "databaseCharset", "databaseCollation", "utf8mb4DefaultCollation", "defaultStorageEngine"],
                "todo": ["Root captures and freezes exact environment SELECT before first migration; implicit charset/collation/engine references require this fact, never guessed."]}
    for candidate in scope["executionOrder"]:
        path = REPO / "IntRuoyiBackend" / candidate["file"]
        if base.sha(path) != candidate["sha256"]: raise ValueError("Candidate SQL drift: " + candidate["migrationId"])
        contract["candidates"].append({"migrationId": candidate["migrationId"], "file": candidate["file"], "sha256": candidate["sha256"], "schemaAssertions": candidate["type"] == "schema"})
        sql = path.read_text(encoding="utf-8")
        for name, shape in parse_create_tables(sql).items():
            if name.startswith("tmp_"): continue
            if name not in original:
                new_tables.append(name)
                target[name] = shape
            else: target.setdefault(name, copy.deepcopy(original[name]))
            affected.add(name)
        statements = ddl_statements(sql)
        if candidate["migrationId"] == "20260906_dcc_new_file_lifecycle_p1":
            statements += ["ALTER TABLE " + m.group(1) + " DROP INDEX " + m.group(2) for m in re.finditer(r"CALL drop_dcc_p1_index\('([^']+)',\s*'([^']+)'\)", sql)]
        if candidate["migrationId"] == "20261003_dcc_approval_file_owner_snapshot":
            statements += ["ALTER TABLE dcc_controlled_file ADD COLUMN " + m.group(1) + " " + m.group(2) for m in re.finditer(r"CALL ensure_dcc_file_owner_column\('([^']+)','([^']+)'\)", sql)]
        if candidate["migrationId"] == "20261003_dcc_project_reviewer_configuration":
            statements += ["ALTER TABLE dcc_project_product_create_request ADD COLUMN " + m.group(1) + " " + m.group(2) for m in re.finditer(r"CALL ensure_dcc_reviewer_snapshot_column\('([^']+)','([^']+)'\)", sql)]
        for statement in statements:
            head = re.match(r"ALTER\s+TABLE\s+`?(\w+)`?\s+(.*)", statement, re.I | re.S)
            if not head: raise ValueError("DDL parse missing table")
            name, body = head.groups()
            if name not in target:
                if name not in original: raise ValueError("ALTER requires prior table: " + name)
                target[name] = copy.deepcopy(original[name])
            affected.add(name)
            for operation in base.split_body(body):
                column = re.match(r"(ADD|MODIFY)\s+COLUMN\s+(.*)", operation, re.I | re.S)
                if column:
                    field, shape = column_definition(column.group(2), target[name]["table"])
                    # AFTER affects ordinal only; this contract does not falsely demand ordinal column positioning.
                    if column.group(1).upper() == "ADD" and field not in original.get(name, {}).get("columns", {}): added_columns.add(name + "." + field)
                    target[name]["columns"][field] = shape
                    continue
                add_index = re.match(r"ADD\s+((?:UNIQUE\s+)?(?:KEY|INDEX)\b.*)", operation, re.I | re.S)
                if add_index:
                    key, shape, anonymous = index_definition(add_index.group(1))
                    target[name]["indexes"][key] = shape
                    if anonymous: target[name]["anonymousIndexes"].append(key)
                    continue
                drop_index = re.match(r"DROP\s+INDEX\s+`?(\w+)`?", operation, re.I)
                if drop_index:
                    key = drop_index.group(1)
                    target[name]["indexes"].pop(key, None)
                    contract["absentIndexes"].setdefault(name, []).append(key)
                    continue
                raise ValueError("Unsupported schema operation: " + operation)
    # Final declarations replace earlier versions, but an index dropped/readded inone statement is not absent.
    for name in sorted(affected):
        shape = target[name]
        contract["tables"][name] = shape["table"]
        contract["columns"][name] = shape["columns"]
        contract["indexes"][name] = shape["indexes"]
        contract["anonymousIndexes"][name] = shape["anonymousIndexes"]
        if name in contract["absentIndexes"]:
            contract["absentIndexes"][name] = sorted({key for key in contract["absentIndexes"][name] if key not in shape["indexes"]})
    contract["newTables"] = sorted(set(new_tables))
    contract["newColumns"] = sorted(added_columns)
    if len(contract["newTables"]) != 17 or len(contract["newColumns"]) != 51:
        raise ValueError(f"Candidate scope mismatch: {len(contract['newTables'])}tables/{len(contract['newColumns'])}newcolumns")
    contract["ledgerPreservation"] = json.loads((HERE / "g21-structure-prerequisites.json").read_text(encoding="utf-8"))["ledger"]
    base.dump_json(HERE / "g21-postflight-schema-contract.json", contract)
    (HERE / "g21-postflight-environment-query.sql").write_text(environment_query(), encoding="utf-8")
    (HERE / "g21-postflight-schema-queries.sql").write_text(build_queries(contract), encoding="utf-8")
    return contract


def environment_query():
    return """-- Read and freeze before migration; use same connection environment during candidate execution.
SELECT JSON_OBJECT('kind','environment','database',DATABASE(),'serverUuid',@@server_uuid,'databaseCharset',DEFAULT_CHARACTER_SET_NAME,
 'databaseCollation',DEFAULT_COLLATION_NAME,'utf8mb4DefaultCollation',@@default_collation_for_utf8mb4,
 'defaultStorageEngine',@@default_storage_engine) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=DATABASE();
"""


def build_queries(contract):
    tables = ",".join("'" + t + "'" for t in sorted(contract["tables"]))
    ledger = ",".join("'" + m + "'" for m in sorted(contract["ledgerPreservation"]))
    return f"""-- Read-only final schema capture; first and repeat collect the same complete affected-table facts.
SELECT JSON_OBJECT('kind','runtime','database',DATABASE(),'serverUuid',@@server_uuid,'version',@@version);
SELECT JSON_OBJECT('kind','table','table',TABLE_NAME,'engine',ENGINE,'charset',SUBSTRING_INDEX(TABLE_COLLATION,'_',1),'collation',TABLE_COLLATION)
 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({tables}) ORDER BY TABLE_NAME;
SELECT JSON_OBJECT('kind','column','table',TABLE_NAME,'column',COLUMN_NAME,'type',COLUMN_TYPE,'nullable',IS_NULLABLE,
 'charset',CHARACTER_SET_NAME,'collation',COLLATION_NAME,'default',COLUMN_DEFAULT,'extra',EXTRA,'expression',GENERATION_EXPRESSION)
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({tables}) ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT JSON_OBJECT('kind','index','table',TABLE_NAME,'name',INDEX_NAME,'unique',NON_UNIQUE=0,'type',INDEX_TYPE,
 'sequence',SEQ_IN_INDEX,'column',COLUMN_NAME,'prefix',SUB_PART,'direction',COLLATION,'expression',EXPRESSION)
 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ({tables}) ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;
SELECT JSON_OBJECT('kind','ledger','migrationId',migration_id,'sha256',sha256,'status',status,'environment',target_environment)
 FROM infra_release_migration WHERE deleted=b'0' AND target_environment='test' AND migration_id IN ({ledger}) ORDER BY migration_id;
"""


def resolve(value, environment):
    if isinstance(value, str) and value.startswith("$environment."):
        key = value.split(".", 1)[1]
        if key not in environment or not environment[key]: raise ValueError("Missing frozen runtime environment: " + key)
        return environment[key]
    if isinstance(value, dict): return {k: resolve(v, environment) for k, v in value.items()}
    if isinstance(value, list): return [resolve(v, environment) for v in value]
    return value


def positive_fixture(contract, environment):
    return {"database": environment["database"], "serverUuid": environment["serverUuid"], "tables": resolve(contract["tables"], environment), "columns": resolve(contract["columns"], environment),
            "indexes": copy.deepcopy(contract["indexes"]), "ledger": copy.deepcopy(contract["ledgerPreservation"])}


def validate(contract, facts, environment, previous=None):
    errors = []
    try:
        for key in contract["runtimeParametersRequiredBeforeMigration"]:
            if key not in environment or not environment[key]: raise ValueError("Missing frozen runtime environment: " + key)
        if facts.get("database") not in contract["allowedDatabases"] or facts.get("database") != environment["database"]:
            errors.append("runtime.database does not match allowed target and frozen environment")
        if facts.get("serverUuid") != environment["serverUuid"]: errors.append("runtime.serverUuid changed from frozen pre-migration environment")
        if environment["databaseCharset"] != "utf8mb4" or not environment["databaseCollation"].startswith("utf8mb4_") or not environment["utf8mb4DefaultCollation"].startswith("utf8mb4_"):
            errors.append("runtime implicit charset/collation is not a supported utf8mb4 environment")
        if environment["defaultStorageEngine"].lower() != "innodb": errors.append("runtime implicit storage engine must support the declared transactional InnoDB contracts")
        expected = resolve({key: contract[key] for key in ("tables", "columns", "indexes")}, environment)
        for table, shape in expected["tables"].items():
            actual = facts.get("tables", {}).get(table, {})
            for field in ("engine", "charset", "collation"):
                if str(actual.get(field, "")).lower() != str(shape[field]).lower(): errors.append(f"{table}.{field}: final table shape mismatch")
        for table, columns in expected["columns"].items():
            unexpected_columns = set(facts.get("columns", {}).get(table, {})) - set(columns)
            for column in sorted(unexpected_columns): errors.append(f"{table}.{column}: unexpected column beyond protected baseline plus19 candidates")
            for column, shape in columns.items():
                actual = facts.get("columns", {}).get(table, {}).get(column)
                if actual is None:
                    errors.append(f"{table}.{column}: missing final column"); continue
                for field in ("type", "nullable", "charset", "collation", "default", "extra"):
                    if actual.get(field) != shape[field]: errors.append(f"{table}.{column}.{field}: final column shape mismatch")
                try:
                    if expression_ast(actual.get("expression", "")) != expression_ast(shape["expression"]): errors.append(f"{table}.{column}.expression: final generated semantics mismatch")
                except ValueError as exc: errors.append(f"{table}.{column}.expression: {exc}")
        for table, indexes in expected["indexes"].items():
            actual_indexes = facts.get("indexes", {}).get(table, {})
            matched = set()
            for name, shape in indexes.items():
                if name in contract["anonymousIndexes"].get(table, []):
                    found = [key for key, index in actual_indexes.items() if index == shape]
                    if len(found) != 1: errors.append(f"{table}.{name}: anonymous ordered unique shape missing/ambiguous")
                    else: matched.add(found[0])
                elif actual_indexes.get(name) != shape: errors.append(f"{table}.{name}: final ordered index shape mismatch")
                else: matched.add(name)
            for name in sorted(set(actual_indexes) - matched): errors.append(f"{table}.{name}: unexpected/mismatched index beyond final schema")
        for table, names in contract["absentIndexes"].items():
            for name in names:
                if name in facts.get("indexes", {}).get(table, {}): errors.append(f"{table}.{name}: obsolete index must be absent")
        for name, ledger in contract["ledgerPreservation"].items():
            if facts.get("ledger", {}).get(name) != ledger: errors.append(f"{name}.ledger: previous original evidence changed")
    except ValueError as exc:
        errors.append(str(exc))
    fingerprint_facts = copy.deepcopy(facts)
    for columns in fingerprint_facts.get("columns", {}).values():
        for column in columns.values():
            try: column["expression"] = expression_ast(column.get("expression", ""))
            except ValueError: pass
    schema_fingerprint = hashlib.sha256(json.dumps(fingerprint_facts, sort_keys=True, ensure_ascii=False, separators=(",", ":")).encode("utf-8")).hexdigest()
    contract_fingerprint = hashlib.sha256(json.dumps(contract, sort_keys=True, ensure_ascii=False, separators=(",", ":")).encode("utf-8")).hexdigest()
    environment_fingerprint = hashlib.sha256(json.dumps(environment, sort_keys=True, ensure_ascii=False, separators=(",", ":")).encode("utf-8")).hexdigest()
    if previous and (previous.get("status") != "POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE" or previous.get("schemaFingerprint") != schema_fingerprint):
        errors.append("replay final schema differs from first-run PASS fingerprint")
    if previous and (previous.get("contractFingerprint") != contract_fingerprint or previous.get("environmentFingerprint") != environment_fingerprint): errors.append("replay frozen contract/environment differs from first-run evidence")
    return {"status": "FAIL" if errors else "POSTFLIGHT_SCHEMA_PASS_NOT_EXECUTION_EVIDENCE", "executionAuthorized": False, "schemaFingerprint": schema_fingerprint, "contractFingerprint": contract_fingerprint,
            "environmentFingerprint": environment_fingerprint, "database": facts.get("database"), "serverUuid": facts.get("serverUuid"), "candidates": 19, "newTables": 17, "newColumns": 51, "errors": errors,
            "meaning": "Schema facts only, does not prove candidate execution, seed/config success or historical business data unchanged."}


def read_facts(path):
    facts = {"database": None, "serverUuid": None, "tables": {}, "columns": {}, "indexes": {}, "ledger": {}}
    runtime = False
    for line in Path(path).read_text(encoding="utf-8-sig").splitlines():
        if not line.strip(): continue
        row = json.loads(line)
        kind = row.get("kind")
        if kind == "runtime":
            if runtime: raise ValueError("Duplicate runtime row")
            runtime = True; facts["database"] = row["database"]; facts["serverUuid"] = row["serverUuid"]
        elif kind == "table":
            if row["table"] in facts["tables"]: raise ValueError("Duplicate table")
            facts["tables"][row["table"]] = {k: row[k] for k in ("engine", "charset", "collation")}
        elif kind == "column":
            columns = facts["columns"].setdefault(row["table"], {})
            if row["column"] in columns: raise ValueError("Duplicate column")
            shape = {k: row[k] for k in ("type", "nullable", "charset", "collation", "default", "extra", "expression")}
            shape["type"] = base.normalize_type(shape["type"]); shape["default"] = base.quoted_value(shape["default"]); shape["extra"] = base.normalize_extra(shape["extra"])
            columns[row["column"]] = shape
        elif kind == "index":
            indexes = facts["indexes"].setdefault(row["table"], {})
            shape = indexes.setdefault(row["name"], {"unique": bool(row["unique"]), "type": row["type"], "columns": []})
            if row["sequence"] != len(shape["columns"]) + 1 or bool(row["unique"]) != shape["unique"]: raise ValueError("Inconsistent index facts")
            shape["columns"].append({k: row[k] for k in ("column", "prefix", "direction", "expression")})
        elif kind == "ledger":
            if row["migrationId"] in facts["ledger"]: raise ValueError("Duplicate ledger row")
            facts["ledger"][row["migrationId"]] = {k: row[k] for k in ("sha256", "status", "environment")}
        else: raise ValueError("Unknown schema fact kind: " + str(kind))
    return facts


def read_environment(path):
    rows = [json.loads(line) for line in Path(path).read_text(encoding="utf-8-sig").splitlines() if line.strip()]
    if len(rows) != 1 or rows[0].get("kind") != "environment": raise ValueError("One exact frozen environment SELECT row required")
    return {k: v for k, v in rows[0].items() if k != "kind"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    build = commands.add_parser("prepare"); build.add_argument("--root-task", required=True)
    check = commands.add_parser("validate"); check.add_argument("--facts", required=True); check.add_argument("--environment", required=True); check.add_argument("--result", required=True); check.add_argument("--previous")
    args = parser.parse_args()
    if args.command == "prepare":
        contract = prepare(args.root_task)
        print(json.dumps({"status": contract["status"], "candidates": 19, "tables": len(contract["tables"]), "newTables": len(contract["newTables"]), "newColumns": len(contract["newColumns"])}))
    else:
        try:
            contract = json.loads((HERE / "g21-postflight-schema-contract.json").read_text(encoding="utf-8"))
            previous = json.loads(Path(args.previous).read_text(encoding="utf-8")) if args.previous else None
            result = validate(contract, read_facts(args.facts), read_environment(args.environment), previous)
        except (ValueError, KeyError) as exc:
            result = {"status": "FAIL", "executionAuthorized": False, "errors": [str(exc)]}
        base.dump_json(args.result, result); print(json.dumps(result, ensure_ascii=False))
        if result["status"] == "FAIL": raise SystemExit(1)


if __name__ == "__main__":
    main()
