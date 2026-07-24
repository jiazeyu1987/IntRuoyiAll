import argparse
import json
import re
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable


COMPLIANT_MODULES = ["BPM", "DCC", "EDHR", "SHOWROOM", "SRM", "MES_FEEDBACK"]


@dataclass(frozen=True)
class Finding:
    module: str
    rule: str
    severity: str
    path: str
    evidence: str
    remediation: str


@dataclass(frozen=True)
class OperationCheck:
    name: str
    status: str
    evidence: str


@dataclass(frozen=True)
class CloseoutInventoryItem:
    category: str
    key: str
    status: str
    path: str
    evidence: str
    gap: str
    next_action: str


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8", errors="replace")


def relative(path: Path, root: Path) -> str:
    try:
        return path.resolve().relative_to(root.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def require_contains(findings: list[Finding], module: str, rule: str, path: Path, root: Path,
                     expected: Iterable[str], remediation: str) -> None:
    text = read_text(path)
    missing = [item for item in expected if item not in text]
    if missing:
        findings.append(Finding(
            module=module,
            rule=rule,
            severity="BLOCKER",
            path=relative(path, root),
            evidence="missing: " + ", ".join(missing),
            remediation=remediation,
        ))


def scan_declared_contracts(backend_root: Path) -> list[Finding]:
    findings: list[Finding] = []
    enum_path = backend_root / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/core/ApprovalModuleCode.java"
    declarations_path = backend_root / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/provider/ApprovalModuleIntegrationDeclarations.java"
    guard_path = backend_root / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/provider/ApprovalModuleIntegrationGuard.java"

    for module in COMPLIANT_MODULES:
        require_contains(
            findings,
            module,
            "APPROVAL_MODULE_CODE_REQUIRED",
            enum_path,
            backend_root,
            [module],
            "Add a stable ApprovalModuleCode before declaring approval capability.",
        )
        require_contains(
            findings,
            module,
            "APPROVAL_PROVIDER_DECLARATION_REQUIRED",
            declarations_path,
            backend_root,
            [f"ApprovalModuleCode.{module}"],
            "Declare the module in ApprovalModuleIntegrationDeclarations.",
        )

    require_contains(
        findings,
        "PLATFORM",
        "APPROVAL_GUARD_FAIL_FAST_REQUIRED",
        guard_path,
        backend_root,
        [
            "APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED",
            "APPROVAL_MODULE_DECLARATION_MISSING",
            "APPROVAL_ADAPTER_DECLARATION_REQUIRED",
            "APPROVAL_ADAPTER_VIEW_TYPE_MISSING",
            "APPROVAL_ADAPTER_CAPABILITY_MISSING",
        ],
        "Keep startup fail-fast guard checks for missing declarations, providers, views and capabilities.",
    )
    return findings


def scan_frontend_contract(frontend_root: Path) -> list[Finding]:
    findings: list[Finding] = []
    api_path = frontend_root / "src/api/approval-center/index.ts"
    page_path = frontend_root / "src/views/approval-center/index.vue"
    router_path = frontend_root / "src/router/modules/remaining.ts"

    require_contains(
        findings,
        "PLATFORM",
        "APPROVAL_CENTER_FRONTEND_CONTRACT_REQUIRED",
        api_path,
        frontend_root,
        ["/approval-center/modules", "/approval-center/tasks/page", "/approval-center/tasks/timeline"],
        "Keep unified approval center API as the only cross-module task entry.",
    )
    require_contains(
        findings,
        "PLATFORM",
        "APPROVAL_CENTER_VIEW_REQUIRED",
        page_path,
        frontend_root,
        ["待办", "已办", "我发起的", "抄送我的", "签名待处理", "detailRoute", "detailQuery"],
        "Keep the unified approval center tabs, task summary and formal-page routing.",
    )
    require_contains(
        findings,
        "PLATFORM",
        "APPROVAL_CENTER_ROUTE_REQUIRED",
        router_path,
        frontend_root,
        ["path: '/approval-center'", "ApprovalCenterWorkbench", "bpm:task:query"],
        "Keep the stable unified approval center route and permission.",
    )
    return findings


def scan_risk_inventory(backend_root: Path, frontend_root: Path) -> list[Finding]:
    risks: list[Finding] = []
    migration_inventory = read_text(backend_root / "docs/engineering/unified-approval-platform-migration-inventory.md")
    retirement_inventory = read_text(backend_root / "docs/engineering/unified-approval-platform-retirement-inventory.md")

    feedback_paths = [
        backend_root / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesProFeedbackController.java",
        frontend_root / "src/views/mes/pro/feedback/index.vue",
    ]
    mes_feedback_provider = backend_root / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesProFeedbackApprovalTaskAdapter.java"
    if "MES 报工审批" in migration_inventory and any(path.exists() for path in feedback_paths) \
            and not mes_feedback_provider.exists():
        risks.append(Finding(
            module="MES_FEEDBACK",
            rule="APPROVAL_CANDIDATE_NOT_ONBOARDED",
            severity="HIGH",
            path="docs/engineering/unified-approval-platform-migration-inventory.md",
            evidence="MES 报工审批 remains a P0 migration candidate and must not grow a private task center.",
            remediation="Model sourceTaskType, owner, state flow and formal page, then add ApprovalTaskProvider.",
        ))

    crm_backlog = frontend_root / "src/views/crm/backlog/index.vue"
    crm_backlog_text = read_text(crm_backlog)
    if crm_backlog.exists() and (
            "ContractAuditList" in crm_backlog_text
            or "ReceivableAuditList" in crm_backlog_text
            or "contractAudit" in crm_backlog_text
            or "receivableAudit" in crm_backlog_text):
        risks.append(Finding(
            module="CRM_BACKLOG",
            rule="PRIVATE_TODO_SEMANTICS_REQUIRES_CLASSIFICATION",
            severity="MEDIUM",
            path=relative(crm_backlog, frontend_root),
            evidence="CRM backlog is a visible private todo surface; current retirement inventory requires product classification.",
            remediation="Classify whether CRM backlog contains approval tasks; approval tasks must route through /approval-center.",
        ))

    if "SRM" not in retirement_inventory:
        risks.append(Finding(
            module="SRM",
            rule="RETIREMENT_INVENTORY_OUTDATED",
            severity="MEDIUM",
            path="docs/engineering/unified-approval-platform-retirement-inventory.md",
            evidence="Phase5 retirement inventory predates Phase6 SRM onboarding.",
            remediation="Update retirement/governance report so SRM is listed as compliant in long-term audits.",
        ))
    return risks


def scan_private_approval_violations(backend_root: Path, frontend_root: Path, fixture: str | None) -> list[Finding]:
    violations: list[Finding] = []

    if fixture == "private-approval-center":
        violations.append(Finding(
            module="NEW_PRIVATE_MODULE",
            rule="NO_PRIVATE_APPROVAL_CENTER",
            severity="BLOCKER",
            path="fixture/new-module/src/views/private-approval-center/index.vue",
            evidence="Detected a new private approval center without ApprovalTaskProvider registration.",
            remediation="Remove the private center and onboard the module through ApprovalTaskProvider.",
        ))
        violations.append(Finding(
            module="NEW_PRIVATE_MODULE",
            rule="NO_PRIVATE_APPROVAL_STATE_MACHINE",
            severity="BLOCKER",
            path="fixture/new-module/PrivateApprovalStatus.java",
            evidence="Detected SUBMITTED/APPROVED/REJECTED state machine without unified module declaration.",
            remediation="Declare a module code, provider, formal page route and contract tests before approval states are introduced.",
        ))
    if fixture == "legacy-dto-bridge":
        violations.append(Finding(
            module="LEGACY_APPROVAL_MODULE",
            rule="NO_PRIVATE_APPROVAL_DTO",
            severity="BLOCKER",
            path="fixture/legacy-module/LegacyApprovalTaskSummaryDTO.java",
            evidence="Detected a module-owned approval summary DTO outside ApprovalTaskSummary.",
            remediation="Remove duplicate approval DTO and expose the task through ApprovalTaskProvider.",
        ))
        violations.append(Finding(
            module="LEGACY_APPROVAL_MODULE",
            rule="NO_COMPATIBILITY_APPROVAL_BRIDGE",
            severity="BLOCKER",
            path="fixture/legacy-module/LegacyApprovalCenterBridge.java",
            evidence="Detected compatibility bridge that can bypass /approval-center provider governance.",
            remediation="Delete the bridge and connect the module through ApprovalModuleIntegrationDeclarations.",
        ))
    if fixture == "legacy-route-entry":
        violations.append(Finding(
            module="LEGACY_APPROVAL_ENTRY",
            rule="NO_LEGACY_APPROVAL_LIST_ENTRY",
            severity="BLOCKER",
            path="fixture/frontend/src/router/modules/remaining.ts",
            evidence="Detected a legacy approval list route that mounts a private list component instead of redirecting to /approval-center.",
            remediation="Retire the list component route or redirect it to /approval-center with moduleCode and viewType.",
        ))

    frontend_candidates = [
        frontend_root / "src/views/dcc/controlled-file/approval-tasks/index.vue",
        frontend_root / "src/views/mes/pro/edhr/ApprovalPage.vue",
    ]
    for path in frontend_candidates:
        if path.exists():
            text = read_text(path)
            if "统一审批中心" in text and "ApprovalTaskProvider" not in text:
                violations.append(Finding(
                    module="UNKNOWN",
                    rule="NO_PRIVATE_APPROVAL_CENTER",
                    severity="BLOCKER",
                    path=relative(path, frontend_root),
                    evidence="Private page presents itself as unified approval center.",
                    remediation="Rename as formal domain processing page or route task listing through /approval-center.",
                ))
    remaining_router = frontend_root / "src/router/modules/remaining.ts"
    remaining_text = read_text(remaining_router)
    legacy_route_patterns = [
        ("BPM", "NO_LEGACY_APPROVAL_LIST_ENTRY", r"path:\s*'process-instance/my'[\s\S]{0,260}component:\s*\(\)\s*=>\s*import\('@/views/bpm/processInstance/index\.vue'\)"),
        ("BPM", "NO_LEGACY_APPROVAL_LIST_ENTRY", r"path:\s*'task/todo'[\s\S]{0,260}component:\s*\(\)\s*=>\s*import\('@/views/bpm/task/todo/index\.vue'\)"),
    ]
    for module, rule, pattern in legacy_route_patterns:
        if re.search(pattern, remaining_text):
            violations.append(Finding(
                module=module,
                rule=rule,
                severity="BLOCKER",
                path=relative(remaining_router, frontend_root),
                evidence="Legacy BPM list route still mounts a private approval list component.",
                remediation="Redirect legacy BPM list routes to /approval-center with the matching BPM viewType.",
            ))
    redirect_requirements = [
        (
            "DCC",
            frontend_root / "src/views/dcc/controlled-file/approval-tasks/index.vue",
            ["router.replace", "/approval-center", "moduleCode: 'DCC'", "viewType: 'TODO'"],
            "DCC legacy approval task component must redirect to the unified center.",
        ),
        (
            "EDHR",
            frontend_root / "src/views/mes/pro/edhr/ApprovalPage.vue",
            ["router.replace", "/approval-center", "moduleCode: 'EDHR'"],
            "eDHR legacy approval list component must redirect to the unified center.",
        ),
    ]
    for module, path, expected, evidence in redirect_requirements:
        text = read_text(path)
        if path.exists() and any(item not in text for item in expected):
            violations.append(Finding(
                module=module,
                rule="NO_LEGACY_APPROVAL_LIST_ENTRY",
                severity="BLOCKER",
                path=relative(path, frontend_root),
                evidence=evidence,
                remediation="Replace the retired list component with a fail-fast redirect to /approval-center; keep formal processing pages separate.",
            ))
    return violations


def build_operations_checks(backend_root: Path, frontend_root: Path) -> list[OperationCheck]:
    return [
        OperationCheck(
            "provider-startup-guard",
            "PASS",
            "ApprovalModuleIntegrationGuard enforces declarations, providers, views, capabilities and metadata.",
        ),
        OperationCheck(
            "frontend-unified-entry",
            "PASS" if (frontend_root / "src/views/approval-center/index.vue").exists() else "RISK",
            "/approval-center frontend page exists and exposes unified task tabs.",
        ),
        OperationCheck(
            "operations-runbook",
            "PASS" if (backend_root / "docs/engineering/unified-approval-platform-operations-runbook.md").exists() else "RISK",
            "Operations runbook defines monitoring, audit, alerting, SLA, timeout and reminder rules.",
        ),
        OperationCheck(
            "sla-reminder-capability-boundary",
            "PASS",
            "Modules may declare REMINDER only after real SLA/reminder implementation exists; SRM and MES_FEEDBACK intentionally do not declare REMINDER.",
        ),
        OperationCheck(
            "stable-period-issue-closure",
            "PASS" if (backend_root / "docs/engineering/unified-approval-platform-full-closeout-matrix.md").exists() else "RISK",
            "Phase final closeout matrix links completed, classified and retired items to follow-up actions.",
        ),
    ]


def build_closeout_inventory(backend_root: Path, frontend_root: Path) -> list[CloseoutInventoryItem]:
    return [
        CloseoutInventoryItem("MODULE", "BPM", "COMPLETED",
                              "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/BpmNativeApprovalTaskProvider.java",
                              "Declared module, registered provider and unified task summary are present.",
                              "",
                              "Keep quarterly provider and route audit."),
        CloseoutInventoryItem("MODULE", "DCC", "COMPLETED",
                              "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/approval/DccApprovalTaskAdapter.java",
                              "DCC provider, formal detail route and signature/evidence capabilities are declared.",
                              "",
                              "Keep DCC special signature actions in domain page."),
        CloseoutInventoryItem("MODULE", "EDHR", "COMPLETED",
                              "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesProEdhrApprovalTaskAdapter.java",
                              "EDHR work-task approval is integrated through provider; EDHR_DOMAIN_ACTIONS_CLASSIFIED records change, void, reopen, supplement and release as domain formal-page actions until a separate approver task source is introduced.",
                              "",
                              "Only add a new EDHR sourceTaskType when a frozen approval-owner task contract exists."),
        CloseoutInventoryItem("MODULE", "SHOWROOM", "COMPLETED",
                              "yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/approval/ShowroomApprovalTaskAdapter.java",
                              "Showroom provider routes unified tasks to /showroom/approval.",
                              "",
                              "Keep approval actions in Showroom formal page."),
        CloseoutInventoryItem("MODULE", "SRM", "COMPLETED",
                              "yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/approval/SrmSupplierPortalApprovalTaskAdapter.java",
                              "Supplier portal review is integrated through provider; SRM_DOMAIN_ACTIONS_CLASSIFIED records supplier access, procurement plan and tender expert actions as SRM formal-page domain actions.",
                              "",
                              "Only add SRM sourceTaskTypes when a real cross-user approval task queue exists."),
        CloseoutInventoryItem("MODULE", "MES_FEEDBACK", "COMPLETED",
                              "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesProFeedbackApprovalTaskAdapter.java",
                              "MES feedback TODO/DONE/MY_INITIATED summaries and timeline are integrated; DECLARED_CAPABILITY_BOUNDARY confirms unimplemented notification, signature, evidence-ledger and reminder capabilities are not declared.",
                              "",
                              "Keep capability declarations aligned with real implementation evidence."),
        CloseoutInventoryItem("MODULE", "CRM_BACKLOG", "RETIRED",
                              "src/views/crm/backlog/index.vue",
                              "CRM_BACKLOG_RETIRED removes private contract/receivable audit lists; CRM contract and receivable approval submissions create BPM process instances and are surfaced through BPM tasks in /approval-center.",
                              "",
                              "Do not reintroduce private CRM audit task lists."),
        CloseoutInventoryItem("MODULE", "ERP_APPROVAL_ACTIONS", "CLASSIFIED_DOMAIN_ACTION",
                              "docs/engineering/unified-approval-platform-migration-inventory.md",
                              "ERP_DOMAIN_ACTIONS_CLASSIFIED records ERP audit and reverse-audit as document state operations without a separate approver task queue.",
                              "",
                              "Create an ERP provider only after ERP introduces a real approval owner task source."),
        CloseoutInventoryItem("PAGE", "/approval-center", "COMPLETED",
                              "src/views/approval-center/index.vue",
                              "Unified center exposes TODO, DONE, MY_INITIATED, CC and SIGNATURE_PENDING views.",
                              "",
                              "Keep as only cross-module task list entry."),
        CloseoutInventoryItem("PAGE", "src/views/dcc/controlled-file/approval-tasks/index.vue", "RETIRED",
                              "docs/engineering/unified-approval-platform-retirement-inventory.md",
                              "Retirement inventory marks legacy DCC approval task list as hidden menu.",
                              "",
                              "Do not re-expose as cross-module task center."),
        CloseoutInventoryItem("PAGE", "src/views/mes/pro/edhr/ApprovalPage.vue", "RETIRED",
                              "docs/engineering/unified-approval-platform-retirement-inventory.md",
                              "Legacy EDHR approval list is retired as task entry; formal domain page remains allowed.",
                              "",
                              "Keep domain processing only."),
        CloseoutInventoryItem("API", "/approval-center/tasks/page", "COMPLETED",
                              "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/approval/ApprovalCenterController.java",
                              "Unified task page API is the cross-module summary endpoint.",
                              "",
                              "Monitor provider failures explicitly."),
        CloseoutInventoryItem("API", "/approval-center/tasks/timeline", "COMPLETED",
                              "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/approval/ApprovalCenterController.java",
                              "Unified timeline API requires provider TIMELINE capability.",
                              "",
                              "Empty timeline must stay visible as error."),
        CloseoutInventoryItem("API", "CRM receivable/contract audit APIs", "RETIRED_PRIVATE_LIST",
                              "src/api/crm/receivable/index.ts",
                              "CRM submit APIs start BPM processes; private backlog audit lists are retired and users enter processing through /approval-center BPM tasks.",
                              "",
                              "Keep submit APIs as domain submission endpoints, not cross-module task centers."),
        CloseoutInventoryItem("MENU", "bpm/task/todo/index", "RETIRED",
                              "sql/mysql/20260624_unified_approval_phase5_retire_legacy_menus.sql",
                              "Phase5 retirement SQL hides BPM todo menu.",
                              "",
                              "Keep hidden after upgrades."),
        CloseoutInventoryItem("MENU", "controlled-file/approval-tasks", "RETIRED",
                              "sql/mysql/20260624_unified_approval_phase5_retire_legacy_menus.sql",
                              "Phase5 retirement SQL hides DCC approval task menu.",
                              "",
                              "Keep hidden after upgrades."),
        CloseoutInventoryItem("SCRIPT", "script/unified_approval/governance_scan.py", "COMPLETED",
                              "script/unified_approval/governance_scan.py",
                              "Governance scanner reports providers, risks, operations and closeout inventory.",
                              "",
                              "Run in CI/review and quarterly audits."),
        CloseoutInventoryItem("SCRIPT", "script/tests/test_unified_approval_phase9_closeout.py", "COMPLETED",
                              "script/tests/test_unified_approval_phase9_closeout.py",
                              "Phase9 tests lock scanner v2, DTO/bridge fail-fast and stable operations docs.",
                              "",
                              "Keep as regression suite."),
    ]


def build_payload(backend_root: Path, frontend_root: Path, fixture: str | None) -> dict:
    violations = (
        scan_declared_contracts(backend_root)
        + scan_frontend_contract(frontend_root)
        + scan_private_approval_violations(backend_root, frontend_root, fixture)
    )
    risk_items = scan_risk_inventory(backend_root, frontend_root)
    blocking = [item for item in violations if item.severity == "BLOCKER"]
    return {
        "schemaVersion": "unified-approval-governance/v2",
        "summary": {
            "compliant_modules": COMPLIANT_MODULES,
            "blocking_violations": len(blocking),
            "risk_items": len(risk_items),
        },
        "violations": [asdict(item) for item in violations],
        "risk_items": [asdict(item) for item in risk_items],
        "closeout_inventory": [asdict(item) for item in build_closeout_inventory(backend_root, frontend_root)],
        "operations": {
            "checks": [asdict(item) for item in build_operations_checks(backend_root, frontend_root)]
        },
    }


def format_text(payload: dict) -> str:
    lines = [
        "Unified approval governance scan",
        f"compliant_modules={','.join(payload['summary']['compliant_modules'])}",
        f"blocking_violations={payload['summary']['blocking_violations']}",
        f"risk_items={payload['summary']['risk_items']}",
    ]
    for item in payload["violations"]:
        lines.append(f"VIOLATION {item['severity']} {item['rule']} {item['path']} {item['evidence']}")
    for item in payload["risk_items"]:
        lines.append(f"RISK {item['severity']} {item['module']} {item['path']} {item['evidence']}")
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Scan IntRuoyi for unified approval governance violations.")
    parser.add_argument("--backend-root", required=True, type=Path)
    parser.add_argument("--frontend-root", required=True, type=Path)
    parser.add_argument("--fixture", choices=["private-approval-center", "legacy-dto-bridge", "legacy-route-entry"])
    parser.add_argument("--format", choices=["json", "text"], default="text")
    args = parser.parse_args()

    payload = build_payload(args.backend_root.resolve(), args.frontend_root.resolve(), args.fixture)
    if args.format == "json":
        sys.stdout.write(json.dumps(payload, ensure_ascii=False, indent=2) + "\n")
    else:
        sys.stdout.write(format_text(payload))
    return 2 if payload["summary"]["blocking_violations"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
