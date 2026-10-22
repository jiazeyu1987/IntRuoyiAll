"""Validate this task's document structure and cross-document contracts.

This is documentation QA, not a product or E2E test. It uses the installed skill
validators with their default output paths relocated to this task directory.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import re
from collections import Counter
from pathlib import Path

TASK = Path(__file__).resolve().parent
EXPECTED_MD = {
    "task.md", "execution-log.md", "verification-report.md",
    "prd.md", "user-flows.md", "acceptance-criteria.md",
    "frontend-design.md", "frontend-interaction.md",
    "backend-api-design.md", "data-model.md", "config-security-deployment.md",
    "development-plan.md", "test-plan.md", "consistency-review.md",
}

def require(value: bool, message: str) -> None:
    if not value:
        raise SystemExit("FAIL: " + message)

def load_validator(skills: Path, skill: str, filename: str):
    path = skills / skill / "scripts" / filename
    require(path.is_file(), f"Missing required skill validator: {path}")
    spec = importlib.util.spec_from_file_location(skill.replace("-", "_"), path)
    require(spec is not None and spec.loader is not None, str(path))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def validate(skills: Path) -> None:
    require({p.name for p in TASK.glob("*.md")} == EXPECTED_MD, "Markdown inventory mismatch")
    docs = {name: (TASK / name).read_text(encoding="utf-8") for name in EXPECTED_MD}
    for name, body in docs.items():
        require(len(body.splitlines()) >= 15, f"{name}: collapsed/truncated document")
        require(len(re.findall(r"^# ", body, re.M)) == 1, f"{name}: H1 count")
        require("\\n" not in body.splitlines()[0], f"{name}: literal newline escapes")
        require("\ufffd" not in body and "\ufeff" not in body, f"{name}: encoding artifacts")
        require(not re.search(r"^- - ", body, re.M), f"{name}: malformed list")
        require(all(x.rstrip() == x for x in body.splitlines()), f"{name}: trailing whitespace")
        heads = re.findall(r"^## (.+)$", body, re.M)
        require(len(heads) == len(set(heads)), f"{name}: duplicate sections")
        # Only validate links to local document files, not planned source files.
        for target in re.findall(r"\]\(([^)#]+\.md)(?:#[^)]*)?\)", body):
            require((TASK / target).is_file(), f"{name}: broken document link {target}")
    print("PASS: 14 UTF-8 Markdown files; headings, line breaks and document links")

    definitions = re.findall(r"^\| (P\d+-AC\d+) \|", docs["acceptance-criteria.md"], re.M)
    require(len(definitions) == 28 and len(set(definitions)) == 28, "Expected 28 unique AC definitions")
    ac_set = set(definitions)
    for name, body in docs.items():
        require(set(re.findall(r"\bP\d+-AC\d+\b", body)) <= ac_set, f"{name}: undefined AC reference")
    pairs = re.findall(r"^\| (T\d+) \| (P\d+-AC\d+) \|", docs["test-plan.md"], re.M)
    require(len(pairs) == 28 and {a for _, a in pairs} == ac_set, "AC/test mapping incomplete")
    require(len({t for t, _ in pairs}) == 28, "Duplicate test ID")
    bdd = re.findall(r"^- BDD: (T\d+) .+ -> Given .+, When .+, Then .+$", docs["test-plan.md"], re.M)
    require(len(bdd) == 28 and set(bdd) == {t for t, _ in pairs}, "Expected 28 mapped Given/When/Then cases")
    print("PASS: 28 acceptance criteria mapped one-to-one to 28 BDD cases")

    state = json.loads((TASK / "task-state.json").read_text(encoding="utf-8"))
    status = re.search(r"^## Current Status\s*\n([a-z_]+)", docs["task.md"], re.M)
    require(status is not None and status.group(1) == state["status"], "Task/state status disagreement")
    require(state["implementationStatus"] == "not_started", "Document task must not claim implementation")
    require(set(state["businessAcceptanceIds"]) == ac_set, "State acceptance IDs differ")
    plans = state["developmentMilestones"]
    require({x["id"] for x in plans} == {f"P{n}" for n in range(1, 7)}, "Development stages differ")
    require(all(x["status"] == "not_started" for x in plans), "Development must remain not_started")
    require(Counter(a for p in plans for a in p["acceptanceIds"]) == Counter(definitions), "Phase/AC ownership differs")
    for plan in plans:
        require(all(a.startswith(plan["id"] + "-") for a in plan["acceptanceIds"]), "AC attached to wrong phase")
    keep = re.findall(r"^- (doc/tasks/20260923-edhr-deviation-management/\S+)$",
                      docs["task.md"].split("## Cleanup Keep\n", 1)[1], re.M)
    require(len(keep) == len(set(keep)) == 16, "Cleanup Keep must list 16 unique permanent artifacts")
    require({Path(x).name for x in keep} == EXPECTED_MD | {"task-state.json", "validate-docs.py"},
            "Cleanup Keep does not protect the complete deliverable")
    print("PASS: task state, unstarted development phases and 16 protected artifacts")

    current = "\n".join(docs[n] for n in EXPECTED_MD - {"execution-log.md", "verification-report.md", "consistency-review.md"})
    for forbidden in ("“主要/关键”映射", "调查处理必填要求尚需确认", "草稿（如实现必须存在）",
                      "仅在FORMAL_BATCH_SOURCE_DETAIL即可签名", "待文档创建完成后填写",
                      "closedReason"):
        require(forbidden not in current, f"Stale contradictory wording: {forbidden}")
    for name in ("prd.md", "data-model.md", "config-security-deployment.md",
                 "frontend-interaction.md", "acceptance-criteria.md", "test-plan.md"):
        require("部门负责人" in docs[name] and "管理者代表" in docs[name], f"{name}: signature roles incomplete")
    require("NORMAL_COMPLETED" in docs["data-model.md"] and "TRANSFERRED_TO_NCR" in docs["data-model.md"], "Closure causes missing")
    require("readOnly" in docs["frontend-interaction.md"] and "activeOrderId" in docs["backend-api-design.md"], "Context/read-only contract missing")
    for term in ("一线生产提交", "一线 PQC 提交", "PQC 生产放行", "上市放行"):
        require(term in docs["prd.md"], f"PRD missing action: {term}")
    print("PASS: targeted signature, identity, closure and gate consistency checks")

    product = load_validator(skills, "product-requirements-docs", "validate_product_requirements.py")
    system = load_validator(skills, "system-design-docs", "validate_system_design.py")
    acceptance = load_validator(skills, "bdd-tdd-acceptance-planner", "validate_acceptance_plan.py")
    for module in (product, system):
        module.DOCS = {Path(path).name: sections for path, sections in module.DOCS.items()}
        module.validate(TASK)
    # This task deliberately consolidates the four acceptance outputs into test-plan.md.
    # Keep every required section/phrase from all four original contracts.
    sections = sorted({value for values in acceptance.DOCS.values() for value in values})
    acceptance.DOCS = {"test-plan.md": sections}
    acceptance.validate(TASK)
    print("PASS: product/system/acceptance skill validators (task-local path mapping)")
    node = load_validator(skills, "roadmap-node-dev-plan", "validate_node_dev_plan.py")
    node.validate(TASK)
    print("PASS: node development package validator")
    print("DOC_VALIDATION=PASS; PRODUCT_TESTS=NOT_RUN; E2E=NOT_RUN")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--skills-root", type=Path, default=Path.home() / ".codex" / "skills")
    args = parser.parse_args()
    validate(args.skills_root)
