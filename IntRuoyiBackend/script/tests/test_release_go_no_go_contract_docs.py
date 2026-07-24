import os
from pathlib import Path


_RELEASE_DOC = Path("docs/releases/20260524-int-ruoyi-ops-go-no-go.md")


def _docs_root() -> Path:
    explicit_root = os.environ.get("DOCS_ROOT")
    if explicit_root:
        root = Path(explicit_root).expanduser().resolve()
        if not (root / _RELEASE_DOC).exists():
            raise AssertionError(f"DOCS_ROOT does not contain {_RELEASE_DOC}: {root}")
        return root

    current = Path(__file__).resolve()
    for candidate in current.parents:
        if (candidate / _RELEASE_DOC).exists():
            return candidate

    raise AssertionError(
        f"Could not locate docs root containing {_RELEASE_DOC} from {current}"
    )


def _release_doc_text() -> str:
    return (_docs_root() / _RELEASE_DOC).read_text(encoding="utf-8")


def _doc_text(relative_path: str) -> str:
    return (_docs_root() / relative_path).read_text(encoding="utf-8")


def test_go_no_go_doc_defines_g8_g9_trigger_and_target_contracts() -> None:
    text = _release_doc_text()

    required_markers = [
        "## G8-G11 Confirmation Interfaces",
        "### G8 rollback-app confirmation",
        "rollbackTriggerId",
        "rollbackTriggerCondition",
        "SelectedImageTag",
        "imageTagSelectionRule",
        "releaseOwnerApproval",
        "backupRecoveryOperatorApproval",
        "rollbackValidationEvidence",
        "只更新 `IMAGE_TAG`",
        "不恢复 MySQL、MinIO 或 Redis",
        "### G9 restore-data confirmation",
        "restoreTriggerId",
        "restoreTriggerCondition",
        "SelectedBackupId",
        "backupIdSelectionRule",
        "preRestoreSnapshotId",
        "dataOwnerApproval",
        "businessImpactScope",
        "same `backupId`",
    ]

    for marker in required_markers:
        assert marker in text


def test_go_no_go_doc_defines_g10_alert_route_contract() -> None:
    text = _release_doc_text()

    required_markers = [
        "### G10 alert route confirmation",
        "notify.enabled=true",
        "notify.channel=webhook",
        "notify.webhook.url",
        "alertOwner",
        "alertTarget",
        "notificationStatus=sent",
        "sendEvidencePath",
        "disabled、pending、unsupported 或 failed",
    ]

    for marker in required_markers:
        assert marker in text


def test_go_no_go_doc_defines_g11_owner_matrix_contract() -> None:
    text = _release_doc_text()

    required_markers = [
        "### G11 owner matrix confirmation",
        "releaseOwner",
        "backupRecoveryOperator",
        "dataOwner",
        "acceptanceOwner",
        "releaseGateReviewer",
        "ownerName",
        "contact",
        "approvalTime",
        "未指定（BLOCKED）",
    ]

    for marker in required_markers:
        assert marker in text


def test_go_no_go_doc_records_prod_owner_candidates_without_unblocking_g11() -> None:
    text = _release_doc_text()

    required_markers = [
        "### G11 PROD owner candidates",
        "| candidateName | source | currentScope | roleMappingStatus | gateImpact |",
        "| jiazeyu | 用户 2026-05-24 提供 | PROD 责任人候选 | 未映射到必填角色（BLOCKED） | G11 仍 BLOCKED |",
        "| tangbin | 用户 2026-05-24 提供 | PROD 责任人候选 | 未映射到必填角色（BLOCKED） | G11 仍 BLOCKED |",
        "候选名单不是批准证据",
        "releaseOwner、backupRecoveryOperator、dataOwner、acceptanceOwner、alertOwner、releaseGateReviewer",
        "contact、approvalTime、approvalEvidence",
    ]

    for marker in required_markers:
        assert marker in text


def test_g8_rollback_app_runbook_defines_executable_contract() -> None:
    text = _doc_text("docs/recovery/rollback-app.md")

    required_markers = [
        "# IntRuoyi rollback-app runbook",
        "适用范围",
        "禁止范围",
        "-Mode rollback-app",
        "-SelectedImageTag",
        "候选 tag 选择规则",
        "deploy/image-tag.txt",
        "只更新 `IMAGE_TAG`",
        "不恢复 MySQL、MinIO 或 Redis",
        "action=rollback-app",
        "context.imageTag",
        "rollbackValidationEvidence",
        "BLOCKED 条件",
        "责任人确认模板",
    ]

    for marker in required_markers:
        assert marker in text


def test_g9_restore_data_runbook_defines_destructive_restore_contract() -> None:
    text = _doc_text("docs/recovery/restore-data.md")

    required_markers = [
        "# IntRuoyi restore-data runbook",
        "破坏性范围",
        "-Mode restore-data",
        "-SelectedBackupId",
        "-TargetEnvironment test",
        "--target-environment test",
        "测试服务器正常 runtime",
        "不能覆盖正式服",
        "正式服目标必须 fail-fast",
        "不是 rehearsal 演练槽位",
        "恢复点资格",
        "manifest/manifest.json",
        "manifest/checksums.txt",
        "preRestoreSnapshotId",
        "same `backupId`",
        "冻结写入",
        "businessImpactScope",
        "action=restore-data",
        "context.restorePoint",
        "restoreValidationEvidence",
        "BLOCKED 条件",
        "责任人确认模板",
    ]

    for marker in required_markers:
        assert marker in text

    forbidden_markers = [
        "默认目标环境为正式环境",
        "恢复到正式环境",
    ]
    for marker in forbidden_markers:
        assert marker not in text


def test_g10_alert_routing_runbook_defines_webhook_and_evidence_contract() -> None:
    text = _doc_text("docs/operations/backup-ops-alert-routing.md")

    required_markers = [
        "# IntRuoyi backup-ops alert routing runbook",
        "当前状态：BLOCKED",
        "notify.enabled=true",
        "notify.channel=webhook",
        "notify.webhook.url",
        "action/status 路由矩阵",
        "backup-now",
        "backup-scheduled",
        "rollback-app",
        "restore-data started",
        "restore-data finished",
        "rehearsal",
        "cleanup",
        "notificationStatus=sent",
        "sendEvidencePath",
        "disabled、pending、unsupported 或 failed",
        "真实发送证据模板",
        "责任人确认模板",
    ]

    for marker in required_markers:
        assert marker in text
