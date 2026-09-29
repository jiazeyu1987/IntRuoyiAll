from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from dataclasses import dataclass
from pathlib import Path

import yaml


REQUIRED_FIELDS = {
    "operationId", "sourceType", "sourceLocators", "domain", "subjectType", "actionType",
    "reasonPolicy", "signaturePolicy", "statePolicy", "retentionClass", "testIds", "ownerRole",
    "applicability",
}
ALLOWED_SOURCE_TYPES = {"SERVICE_METHOD", "JOB", "MIGRATION", "SCRIPT"}
ALLOWED_APPLICABILITY = {"GXP", "NOT_APPLICABLE"}
REQUIRED_HIGH_RISK_DOMAINS = {"EDHR", "DCC", "SIGNATURE", "SYSTEM", "RELEASE"}
SKIPPED_DIRS = {".git", ".idea", ".mvn", "node_modules", "target", "dist", "build", ".pytest_cache"}
APPROVED_EXCLUSION_FIELDS = {
    "candidate",
    "candidateSha256",
    "decision",
    "sourceType",
    "reasonCode",
    "reason",
    "approvedBy",
    "approvalReference",
}
BOUNDARY_PATTERNS = {
    "CONTROLLER": re.compile(r"@(PostMapping|PutMapping|DeleteMapping|PatchMapping)\b"),
    "DOMAIN_SERVICE": re.compile(
        r"\b(add|create|update|delete|submit|approve|publish|void|assign|complete|apply|finalize|close|save|reject|review|record|activate|sign|upload)\w*\s*\("
    ),
    "JOB": re.compile(r"(@Scheduled\b|implements\s+Job\b|extends\s+QuartzJobBean\b|implements\s+JobHandler\b)"),
    "MESSAGE_CONSUMER": re.compile(r"(@RabbitListener\b|@KafkaListener\b|RocketMQListener\b)"),
    "IMPORT": re.compile(r"\b(import|upload|parse|sync)[A-Z]\w*\s*\("),
    "EXTERNAL_SYNC": re.compile(r"\b(sync|push|pull)[A-Z]\w*\s*\("),
    "MIGRATION": re.compile(r"\b(INSERT|UPDATE|DELETE|ALTER|CREATE|DROP)\b", re.IGNORECASE),
    "OPS_SCRIPT": re.compile(r"\b(insert|update|delete|alter|create|drop|deploy|release|migration)\b", re.IGNORECASE),
}
BOUNDARY_SOURCE_TYPES = set(BOUNDARY_PATTERNS)

# Lexical Java boundary analysis, not a proof of runtime writer invocation.
# Preserve offsets/newlines so every finding points back to the source declaration.
JAVA_NON_CODE = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'')
WRITE_NAME = re.compile(
    r"^(?:add|create|insert|update|delete|remove|submit|approve|publish|void|assign|complete|"
    r"apply|finalize|close|save|reject|review|record|append|activate|sign|upload|import|sync|push|pull|"
    r"invalidate|revoke|reset|restore|cancel|execute)(?:[A-Z_].*|$)"
)
ENTRY_ANNOTATION = re.compile(r"@(PostMapping|PutMapping|DeleteMapping|PatchMapping|Scheduled|RabbitListener|KafkaListener)\b")
COLLECTION_DECLARATION = re.compile(
    r"\b(?:List|Set|Map|Collection|ArrayList|HashSet|LinkedHashSet|HashMap|LinkedHashMap)"
    r"\s*(?:<[^;=()]*>)?\s+(\w+)\b"
)
COLLECTION_MUTATORS = {"add", "addAll", "remove", "removeAll", "removeIf", "clear", "retainAll", "put", "putAll"}


@dataclass(frozen=True)
class JavaMethod:
    name: str
    signature: str
    line: int
    private: bool
    header: str
    body: str


def java_methods(text: str) -> tuple[str, list[JavaMethod]]:
    code = JAVA_NON_CODE.sub(lambda match: re.sub(r"[^\n]", " ", match.group()), text)
    package = re.search(r"\bpackage\s+([\w.]+)\s*;", code)
    declaration = re.search(r"\bclass\s+(\w+)[^{]*\{", code)
    if not declaration:
        raise SystemExit("Java entry scan requires a concrete class declaration")
    class_name = declaration.group(1)
    fqcn = f"{package.group(1)}.{class_name}" if package else class_name
    pairs: dict[int, int] = {}
    stack: list[tuple[str, int]] = []
    for index, char in enumerate(code):
        if char in "({[":
            stack.append((char, index))
        elif char in ")}]":
            if not stack or stack[-1][0] != {")": "(", "}": "{", "]": "["}[char]:
                raise SystemExit(f"{fqcn}: unbalanced Java delimiters at offset {index}")
            _, start = stack.pop()
            pairs[start] = index
    if stack:
        raise SystemExit(f"{fqcn}: unbalanced Java delimiters")
    methods: list[JavaMethod] = []
    index = declaration.end()
    member_start = index
    class_end = pairs[index - 1]
    while index < class_end:
        char = code[index]
        if char == "{":
            header = code[member_start:index]
            # The parameter list must end immediately before optional throws and body.
            matches = list(re.finditer(r"\b(\w+)\s*\(", header))
            for match in matches:
                opening = member_start + header.index("(", match.start())
                closing = pairs[opening]
                tail = code[closing + 1:index].strip()
                if tail and not re.fullmatch(r"throws\s+[\w.,\s]+", tail):
                    continue
                name = match.group(1)
                if name == class_name:  # constructors are not registered service entries
                    break
                parameters = " ".join(code[opening + 1:closing].split())
                methods.append(JavaMethod(
                    name, f"{name}({parameters})", code.count("\n", 0, member_start + match.start()) + 1,
                    bool(re.search(r"\bprivate\b", header)), header,
                    code[index + 1:pairs[index]],
                ))
                break
            index = pairs[index] + 1
            member_start = index
            continue
        if char == ";":
            member_start = index + 1
        index += 1
    return fqcn, methods


def explicit_value_call(method: JavaMethod, source: str, receiver: str | None,
                        name: str, offset: int) -> bool:
    """Disambiguate BigDecimal.add only; callbacks may have arbitrary side effects."""
    if name == "add" and receiver in ("BigDecimal", "java.math.BigDecimal"):
        reference = re.match(re.escape(receiver) + r"\s*::\s*add\b", method.body[offset:])
        root_name = receiver.split(".")[0]
        # A type-looking name may instead resolve to a field, local, parameter,
        # nested type or type parameter. Keep ambiguous names fail-closed.
        shadowed = re.search(
            r"\b(?:class|interface|record|enum)\s+" + root_name + r"\b"
            + r"|\b[\w.<>?\[\]]+\s+" + root_name + r"\s*(?=[=,;)\[:])"
            + r"|<\s*" + root_name + r"\s*(?:>|,|extends\b)", source)
        imported = receiver == "java.math.BigDecimal" or re.search(
            r"\bimport\s+java\.math\.BigDecimal\s*;", source)
        if reference and imported and not shadowed:
            return True
    if name != "add" or receiver is None or not re.fullmatch(r"\w+", receiver):
        return False
    qualified = "java.math.BigDecimal"
    simple = qualified.rsplit(".", 1)[1]
    type_pattern = re.escape(qualified)
    if re.search(r"\bimport\s+" + re.escape(qualified) + r"\s*;", source):
        type_pattern += "|" + simple
    declaration = re.compile(r"(?<![\w.])(?:" + type_pattern + ")"
                             + r"\s+" + re.escape(receiver) + r"\b")
    parameters = method.signature.partition("(")[2].rsplit(")", 1)[0]
    if declaration.search(parameters):
        return True
    # Only BigDecimal locals in the method's outer block, declared before use.
    # Nested blocks, var, fields, lambdas and chained expressions remain unknown.
    for match in declaration.finditer(method.body[:offset]):
        prefix = method.body[:match.start()]
        if prefix.count("{") == prefix.count("}") and prefix.count("(") == prefix.count(")"):
            return True
    return False


def local_new_collection_chain(body: str, offset: int, name: str, source: str) -> bool:
    """Resolve only local.computeIfAbsent(key, x -> new java.util.Collection()).mutator.

    Arguments are still scanned independently, so side effects in key/value expressions
    are not exempted. Unknown factories and inferred lambda types stay fail-closed.
    """
    if name not in COLLECTION_MUTATORS:
        return False
    prefix = body[:offset].rstrip()
    if not prefix.endswith("."):
        return False
    prefix = prefix[:-1].rstrip()
    if not prefix.endswith(")"):
        return False
    depth = 0
    opening = None
    for index in range(len(prefix) - 1, -1, -1):
        if prefix[index] == ")":
            depth += 1
        elif prefix[index] == "(":
            depth -= 1
            if depth == 0:
                opening = index
                break
    if opening is None:
        return False
    receiver = re.search(r"\b(\w+)\s*\.\s*computeIfAbsent\s*$", prefix[:opening])
    if receiver is None:
        return False
    variable = receiver.group(1)
    # Only an outer-block local declared before this call, with an explicit JDK
    # type and fresh JDK implementation. Fields, parameters, nested scopes and
    # inferred/custom types deliberately remain unresolved.
    before = body[:offset]
    def jdk_type(simple: str) -> str:
        qualified = "java.util." + simple
        choices = [re.escape(qualified)]
        if (re.search(r"\bimport\s+" + re.escape(qualified) + r"\s*;", source)
                and not re.search(r"\b(?:class|interface|record|enum)\s+" + simple + r"\b", source)):
            choices.append(simple)
        return "(?:" + "|".join(choices) + ")"
    declaration = re.compile(
        r"(?<![\w.])" + jdk_type("Map") + r"\s*<[^;={}()]+>\s+"
        + re.escape(variable) + r"\s*=\s*new\s+"
        + "(?:" + "|".join(jdk_type(t) for t in ("HashMap", "LinkedHashMap")) + ")"
        + r"\s*<>\s*\(\s*\)\s*;"
    )
    declarations = [m for m in declaration.finditer(before)
                    if before[:m.start()].count("{") == before[:m.start()].count("}")]
    if len(declarations) != 1:
        return False
    # Reassignment after construction invalidates provenance.
    if re.search(r"\b" + re.escape(variable) + r"\s*=(?!=)", before[declarations[0].end():]):
        return False
    arguments = prefix[opening + 1:-1]
    return bool(re.fullmatch(
        r"[\s\S]*,\s*\w+\s*->\s*new\s+java\.util\."
        r"(?:ArrayList|LinkedList|HashSet|LinkedHashSet)\s*(?:<\s*>)?\s*\(\s*\)\s*",
        arguments,
    ))


def registered_method_records(root: Path, candidate: Path, operations: list[Operation]) -> tuple[list[dict], list[str]]:
    text = candidate.read_text(encoding="utf-8")
    source = JAVA_NON_CODE.sub(lambda match: re.sub(r"[^\n]", " ", match.group()), text)
    fqcn, methods = java_methods(text)
    registered_names = {
        locator.split("#", 1)[1] for op in operations for locator in op.source_locators
        if op.source_type == "SERVICE_METHOD" and locator.startswith(fqcn + "#")
    }
    names = {method.name for method in methods}
    reasons: dict[str, set[str]] = {name: set() for name in names}
    calls: dict[str, set[str]] = {name: set() for name in names}
    for method in methods:
        if not method.private and WRITE_NAME.fullmatch(method.name):
            reasons[method.name].add("write-named declaration")
        if ENTRY_ANNOTATION.search(method.header):
            reasons[method.name].add("write/scheduled/listener annotation")
        local_collections = set(COLLECTION_DECLARATION.findall(method.header + method.body))
        call_matches = list(re.finditer(r"(?:(\b[\w.]+)\s*\.\s*)?\b(\w+)\s*\(", method.body))
        call_matches.extend(re.finditer(r"\b([\w.]+)\s*::\s*(\w+)", method.body))
        for call in call_matches:
            receiver, name = call.groups()
            # Only explicit local/parameter Java collection types justify this distinction.
            # An unknown receiver named e.g. 'items' is not automatically exempt.
            if receiver in local_collections and name in COLLECTION_MUTATORS:
                continue
            if receiver is None and local_new_collection_chain(method.body, call.start(), name, source):
                continue
            if explicit_value_call(method, source, receiver, name, call.start()):
                continue
            if receiver in (None, "this") and name in names:
                calls[method.name].add(name)
            elif WRITE_NAME.fullmatch(name):
                reasons[method.name].add(f"call {receiver + '.' if receiver else ''}{name}")
    # Propagate write evidence through local private helpers and public delegators.
    # No public delegator inherits another entry's registration.
    writing = {name for name in names if reasons[name]} | registered_names
    while True:
        expanded = writing | {name for name in names if calls[name] & writing}
        if expanded == writing:
            break
        writing = expanded
    errors: list[str] = []
    for name in sorted(registered_names):
        declarations = [method for method in methods if method.name == name]
        if len(declarations) != 1:
            errors.append(f"ambiguous or missing registered declaration: {fqcn}#{name} ({len(declarations)} declarations)")
    records = []
    for method in methods:
        if method.name not in writing:
            continue
        registered = method.name in registered_names and sum(m.name == method.name for m in methods) == 1
        private_helper = method.private and not ENTRY_ANNOTATION.search(method.header)
        decision = "REGISTERED" if registered else "PRIVATE_HELPER" if private_helper else "UNREGISTERED_ENTRY"
        record = dict(candidate=candidate.relative_to(root).as_posix(), sourceType="JAVA_ENTRY",
                      sourceLocator=f"{fqcn}#{method.name}", signature=method.signature,
                      line=method.line, decision=decision,
                      evidence=sorted(reasons[method.name]),
                      localWriteCalls=sorted(calls[method.name] & writing),
                      registeredCallees=sorted(calls[method.name] & registered_names))
        records.append(record)
        if decision == "UNREGISTERED_ENTRY":
            errors.append(f"unregistered write entry {record['sourceLocator']} at {record['candidate']}:{method.line}; "
                          f"signature={method.signature}; evidence={record['evidence']}; "
                          f"localWriteCalls={record['localWriteCalls']}; registeredCallees={record['registeredCallees']}")
    return records, errors


@dataclass(frozen=True)
class Operation:
    values: dict[str, object]

    @property
    def operation_id(self) -> str:
        return self.values["operationId"]

    @property
    def source_type(self) -> str:
        return self.values["sourceType"]

    @property
    def source_locators(self) -> list[str]:
        return self.values["sourceLocators"]

    @property
    def domain(self) -> str:
        return self.values["domain"]


def parse_policy(path: Path) -> tuple[str, list[Operation]]:
    policy = load_policy_bundle(path)
    version = policy.get("policyVersion")
    if not isinstance(version, str) or not version.strip():
        raise SystemExit("policyVersion is required")
    values = policy.get("operations")
    if not isinstance(values, list) or not values:
        raise SystemExit("operations must not be empty")
    for value in values:
        locators = value.get("sourceLocators")
        if not isinstance(locators, list) or not locators or any(
                not isinstance(locator, str) or not locator.strip() for locator in locators):
            raise SystemExit("sourceLocators must be a non-empty list of strings")
    return version, [Operation(value) for value in values]


def backend_root(root: Path) -> Path:
    candidate = root / "IntRuoyiBackend"
    if candidate.is_dir():
        return candidate
    if (root / "config").is_dir() and (root / "script").is_dir():
        return root
    raise SystemExit(f"repository root must contain IntRuoyiBackend or be IntRuoyiBackend: {root}")


def discover_annotations(root: Path) -> dict[str, str]:
    source_root = backend_root(root)
    pattern = re.compile(
        r"@GxpWriteOperation\s*\(\s*operationId\s*=\s*\"([^\"]+)\"\s*\)\s*"
        r"(?:@\w+(?:\([^)]*\))?\s*)*public\s+[\w<>, ?\[\]]+\s+(\w+)\s*\(",
        re.MULTILINE,
    )
    result: dict[str, str] = {}
    source_roots = list(source_root.glob("*/src/main/java"))
    source_roots.extend(source_root.glob("yudao-framework/*/src/main/java"))
    for java_root in source_roots:
        for current_dir, dir_names, file_names in os.walk(java_root):
            dir_names[:] = [name for name in dir_names if name not in SKIPPED_DIRS]
            for file_name in file_names:
                if not file_name.endswith(".java"):
                    continue
                java_file = Path(current_dir) / file_name
                text = java_file.read_text(encoding="utf-8")
                package_match = re.search(r"^package\s+([\w.]+);", text, re.MULTILINE)
                class_match = re.search(r"\bclass\s+(\w+)", text)
                if not package_match or not class_match:
                    continue
                fqcn = f"{package_match.group(1)}.{class_match.group(1)}"
                for operation_id, method_name in pattern.findall(text):
                    result[operation_id] = f"{fqcn}#{method_name}"
    return result


def source_locator_exists(root: Path, operation: Operation) -> bool:
    return all(_source_locator_exists(root, operation.source_type, locator)
               for locator in operation.source_locators)


def _source_locator_exists(root: Path, source_type: str, locator: str) -> bool:
    source_root = backend_root(root)
    if source_type == "SERVICE_METHOD":
        class_name, _, method_name = locator.partition("#")
        if not class_name or not method_name:
            return False
        class_suffix = Path(*class_name.split(".")).with_suffix(".java")
        candidates = [
            java_root / class_suffix
            for java_root in [*source_root.glob("*/src/main/java"), *source_root.glob("yudao-framework/*/src/main/java")]
            if (java_root / class_suffix).exists()
        ]
        if not candidates:
            return False
        text = candidates[0].read_text(encoding="utf-8")
        _, methods = java_methods(text)
        return sum(method.name == method_name for method in methods) == 1
    return (source_root / locator).exists()


def load_policy_bundle(policy_path: Path) -> dict[str, object]:
    payload = yaml.safe_load(policy_path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise SystemExit("policy must be a YAML object")
    return payload


def discover_boundary_candidates(root: Path, category: dict[str, object]) -> list[Path]:
    pattern = BOUNDARY_PATTERNS.get(str(category.get("sourceType")))
    if pattern is None:
        raise SystemExit(f"unsupported write boundary sourceType: {category.get('sourceType')}")
    candidates: set[Path] = set()
    for path_pattern in category.get("paths", []):
        for candidate in root.glob(str(path_pattern)):
            if not candidate.is_file():
                continue
            text = candidate.read_text(encoding="utf-8", errors="ignore")
            if pattern.search(text):
                candidates.add(candidate.resolve())
    return sorted(candidates)


def registered_source_files(root: Path, operations: list[Operation]) -> set[Path]:
    source_root = backend_root(root)
    registered: set[Path] = set()
    for operation in operations:
        for locator in operation.source_locators:
            if operation.source_type == "SERVICE_METHOD":
                class_name, _, _ = locator.partition("#")
                class_suffix = Path(*class_name.split(".")).with_suffix(".java")
                for java_root in [*source_root.glob("*/src/main/java"), *source_root.glob("yudao-framework/*/src/main/java")]:
                    source_path = java_root / class_suffix
                    if source_path.is_file():
                        registered.add(source_path.resolve())
            elif operation.source_type in {"MIGRATION", "SCRIPT"}:
                source_path = source_root / locator.split("#", 1)[0]
                if source_path.is_file():
                    registered.add(source_path.resolve())
    return registered


def load_approved_exclusions(root: Path, scan: dict[str, object]) -> dict[tuple[str, str], dict[str, str]]:
    configured_path = scan.get("approvedExclusionsFile")
    if not isinstance(configured_path, str) or not configured_path.strip():
        raise SystemExit("writeBoundaryScan.approvedExclusionsFile is required")
    inventory_path = (root / configured_path).resolve()
    if not inventory_path.is_file():
        raise SystemExit(f"approved exclusion inventory does not exist: {configured_path}")

    records: dict[tuple[str, str], dict[str, str]] = {}
    for line_number, raw_line in enumerate(inventory_path.read_text(encoding="utf-8").splitlines(), start=1):
        if not raw_line.strip():
            continue
        try:
            payload = json.loads(raw_line)
        except json.JSONDecodeError as error:
            raise SystemExit(f"approved exclusion inventory line {line_number} is not valid JSON: {error}") from error
        if not isinstance(payload, dict) or set(payload) != APPROVED_EXCLUSION_FIELDS:
            raise SystemExit(
                f"approved exclusion inventory line {line_number} must contain exactly "
                f"{sorted(APPROVED_EXCLUSION_FIELDS)}"
            )
        record = {key: str(payload[key]).strip() for key in APPROVED_EXCLUSION_FIELDS}
        source_type = record["sourceType"]
        candidate = record["candidate"]
        if source_type not in BOUNDARY_SOURCE_TYPES:
            raise SystemExit(f"approved exclusion inventory line {line_number} has unsupported sourceType: {source_type}")
        if record["decision"] != "APPROVED_EXCLUSION":
            raise SystemExit(f"approved exclusion inventory line {line_number} must be APPROVED_EXCLUSION")
        if not candidate or Path(candidate).is_absolute() or "\\" in candidate:
            raise SystemExit(f"approved exclusion inventory line {line_number} has invalid candidate path: {candidate}")
        candidate_path = (root / Path(candidate)).resolve()
        try:
            normalized_candidate = candidate_path.relative_to(root).as_posix()
        except ValueError as error:
            raise SystemExit(f"approved exclusion inventory line {line_number} escapes repository root") from error
        if normalized_candidate != candidate or not candidate_path.is_file():
            raise SystemExit(f"approved exclusion inventory line {line_number} candidate does not resolve: {candidate}")
        if not re.fullmatch(r"[0-9a-f]{64}", record["candidateSha256"]):
            raise SystemExit(f"approved exclusion inventory line {line_number} has invalid candidateSha256")
        if any(not record[field] for field in APPROVED_EXCLUSION_FIELDS - {"candidateSha256"}):
            raise SystemExit(f"approved exclusion inventory line {line_number} contains an empty approval field")
        if candidate not in record["reason"] or record["candidateSha256"] not in record["reason"]:
            raise SystemExit(f"approved exclusion inventory line {line_number} reason is not candidate-specific")
        key = (source_type, candidate)
        if key in records:
            raise SystemExit(f"approved exclusion inventory contains duplicate candidate: {source_type}:{candidate}")
        records[key] = record
    return records


def write_boundary_report(path: Path, records: list[dict[str, str]]) -> None:
    if not path.parent.is_dir():
        raise SystemExit(f"boundary report parent directory does not exist: {path.parent}")
    content = "\n".join(
        json.dumps(record, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        for record in records
    )
    path.write_text(f"{content}\n" if content else "", encoding="utf-8")


def validate_boundary_scan(
    root: Path,
    policy: dict[str, object],
    operations: list[Operation],
    report_path: Path | None = None,
) -> tuple[int, int, int, str, list[dict[str, str]]]:
    scan = policy.get("coverageScope", {}).get("writeBoundaryScan")
    if not isinstance(scan, dict) or scan.get("registrationMode") != "REGISTERED_OR_APPROVED_EXCLUSION":
        raise SystemExit("writeBoundaryScan.registrationMode must be REGISTERED_OR_APPROVED_EXCLUSION")
    categories = scan.get("categories")
    if not isinstance(categories, list) or {category.get("sourceType") for category in categories} != BOUNDARY_SOURCE_TYPES:
        raise SystemExit("writeBoundaryScan must configure all required source types")
    registered = registered_source_files(root, operations)
    method_records: list[dict] = []
    errors: list[str] = []
    for candidate in sorted(registered):
        if candidate.suffix == ".java":
            records, findings = registered_method_records(root, candidate, operations)
            method_records.extend(records)
            errors.extend(findings)
    approved_exclusions = load_approved_exclusions(root, scan)
    rows: list[str] = []
    exclusion_records: list[dict[str, str]] = []
    total_candidates = 0
    total_registered = 0
    total_exclusions = 0
    seen_operation_files: set[Path] = set()
    for category in categories:
        if category.get("requiredRegistration") is not True:
            raise SystemExit(f"{category.get('sourceType')}: requiredRegistration must be true")
        disposition = category.get("unregisteredDisposition")
        if not isinstance(disposition, dict) or disposition.get("decision") != "FAIL":
            raise SystemExit(f"{category.get('sourceType')}: unregisteredDisposition must fail fast")
        if any(not str(disposition.get(key, "")).strip() for key in {"decision", "reasonCode", "reason"}):
            raise SystemExit(f"{category.get('sourceType')}: fail-fast disposition reason is required")
        candidates = discover_boundary_candidates(root, category)
        expected = int(category.get("expectedMinCandidates", 0))
        if len(candidates) < expected:
            raise SystemExit(f"{category.get('sourceType')}: candidates={len(candidates)} below expectedMinCandidates={expected}")
        for candidate in candidates:
            relative = candidate.relative_to(root).as_posix()
            is_registered = candidate in registered
            total_candidates += 1
            if is_registered:
                total_registered += 1
                seen_operation_files.add(candidate)
            else:
                total_exclusions += 1
            candidate_sha256 = hashlib.sha256(candidate.read_bytes()).hexdigest()
            source_type = str(category["sourceType"])
            inventory_key = (source_type, relative)
            if is_registered:
                if inventory_key in approved_exclusions:
                    errors.append(
                        f"{source_type}: candidate {relative} is both registered and approved as an exclusion"
                    )
                record = {
                    "candidate": relative,
                    "candidateSha256": candidate_sha256,
                    "decision": "REGISTERED",
                    "sourceType": source_type,
                }
            else:
                approved = approved_exclusions.get(inventory_key)
                if approved is None:
                    errors.append(
                        f"{source_type}: candidate {relative} is not present in approved exclusion inventory"
                    )
                    exclusion_records.append(dict(candidate=relative, sourceType=source_type,
                                                  decision="UNREGISTERED_CANDIDATE", candidateSha256=candidate_sha256))
                    continue
                if approved["candidateSha256"] != candidate_sha256:
                    errors.append(
                        f"{source_type}: candidate {relative} SHA-256 changed; expected "
                        f"{approved['candidateSha256']}, actual {candidate_sha256}"
                    )
                    exclusion_records.append(dict(approved, decision="STALE_APPROVAL", actualSha256=candidate_sha256))
                    continue
                record = dict(approved)
                record["candidateSha256"] = candidate_sha256
                exclusion_records.append(record)
            rows.append(json.dumps(record, ensure_ascii=False, sort_keys=True, separators=(",", ":")))
    discovered_exclusions = {
        (record["sourceType"], record["candidate"])
        for record in exclusion_records
    }
    stale_exclusions = set(approved_exclusions) - discovered_exclusions
    if stale_exclusions:
        stale = ", ".join(f"{source_type}:{candidate}" for source_type, candidate in sorted(stale_exclusions))
        errors.append(f"approved exclusion inventory contains stale candidates: {stale}")
    unaccounted = registered - seen_operation_files
    if unaccounted:
        errors.append("registered operation source files are outside the scanned boundary: "
                         + ", ".join(sorted(path.relative_to(root).as_posix() for path in unaccounted)))
    all_records = method_records + exclusion_records
    if report_path is not None:
        write_boundary_report(report_path, all_records)
    if errors:
        raise SystemExit("FAIL gxp audit coverage gate\n" + "\n".join(errors))
    rows.extend(json.dumps(record, ensure_ascii=False, sort_keys=True) for record in method_records)
    report = "\n".join(sorted(rows))
    return (
        total_candidates,
        total_registered,
        total_exclusions,
        hashlib.sha256(report.encode("utf-8")).hexdigest(),
        all_records,
    )


def canonical_report(policy_version: str, operations: list[Operation], annotations: dict[str, str]) -> str:
    rows = [
        f"{policy_version}|{op.operation_id}|{op.source_type}|{json.dumps(op.source_locators, separators=(',', ':'))}|{op.domain}|"
        f"{annotations.get(op.operation_id, '')}"
        for op in sorted(operations, key=lambda item: item.operation_id)
    ]
    return "\n".join(rows)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    parser.add_argument("--policy", default="IntRuoyiBackend/config/gxp-audit-policy.yaml")
    parser.add_argument("--boundary-report", default=None)
    args = parser.parse_args()

    root = Path(args.root).resolve()
    policy_path = (root / args.policy).resolve()
    policy_version, operations = parse_policy(policy_path)
    policy = load_policy_bundle(policy_path)
    annotations = discover_annotations(root)

    errors: list[str] = []
    seen: set[str] = set()
    for operation in operations:
        missing = REQUIRED_FIELDS - operation.values.keys()
        if missing:
            errors.append(f"{operation.operation_id}: missing fields {sorted(missing)}")
        if operation.operation_id in seen:
            errors.append(f"{operation.operation_id}: duplicate operationId")
        seen.add(operation.operation_id)
        if operation.source_type not in ALLOWED_SOURCE_TYPES:
            errors.append(f"{operation.operation_id}: invalid sourceType {operation.source_type}")
        if operation.values.get("applicability") not in ALLOWED_APPLICABILITY:
            errors.append(f"{operation.operation_id}: invalid applicability {operation.values.get('applicability')}")
        if not operation.values.get("ownerRole"):
            errors.append(f"{operation.operation_id}: ownerRole is required")
        if not isinstance(operation.values.get("testIds"), list) or not operation.values["testIds"]:
            errors.append(f"{operation.operation_id}: testIds must be a non-empty list")
        if not source_locator_exists(root, operation):
            errors.append(f"{operation.operation_id}: sourceLocator does not resolve: {operation.source_locators}")
    for operation_id, locator in annotations.items():
        if operation_id not in seen:
            errors.append(f"{operation_id}: annotated GxP write is missing from policy ({locator})")
        elif next(op for op in operations if op.operation_id == operation_id).source_locators.count(locator) != 1:
            errors.append(f"{operation_id}: annotation locator mismatch: {locator}")
    missing_domains = REQUIRED_HIGH_RISK_DOMAINS - {op.domain for op in operations if op.values.get("applicability") == "GXP"}
    if missing_domains:
        errors.append(f"missing high-risk domains: {sorted(missing_domains)}")
    if errors:
        raise SystemExit("\n".join(errors))

    boundary_candidates, boundary_registered, boundary_exclusions, boundary_hash, exclusion_records = validate_boundary_scan(
        root, policy, operations, Path(args.boundary_report).resolve() if args.boundary_report else None
    )
    report = canonical_report(policy_version, operations, annotations)
    print(f"PASS gxp audit coverage gate operations={len(operations)} annotations={len(annotations)} "
          f"sha256={hashlib.sha256(report.encode('utf-8')).hexdigest()} "
          f"boundaryCategories={len(policy['coverageScope']['writeBoundaryScan']['categories'])} "
          f"boundaryCandidates={boundary_candidates} boundaryRegistered={boundary_registered} "
          f"boundaryExclusions={boundary_exclusions} boundaryReportRecords={len(exclusion_records)} "
          f"boundarySha256={boundary_hash}")


if __name__ == "__main__":
    main()
