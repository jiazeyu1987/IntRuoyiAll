from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


REWRITE_CONTROL_PLAN_DOC = (
    REPO_ROOT / "docs" / "system" / "mes-scheduling-rewrite-control-plan.md"
)
DOMAIN_CONTRACT_DOC = REPO_ROOT / "docs" / "system" / "mes-scheduling-domain-contracts.md"


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def stage_block(text: str, title: str) -> str:
    start = text.index(title)
    next_start = text.find("\n## ", start + len(title))
    if next_start == -1:
        return text[start:]
    return text[start:next_start]


def test_mes_scheduling_rewrite_control_plan_declares_stage_1_to_5_guardrails() -> None:
    assert REWRITE_CONTROL_PLAN_DOC.exists()
    text = read_text(REWRITE_CONTROL_PLAN_DOC)

    required_snippets = [
        "# MES 排产模块阶段性改写控制方案",
        "## 总控原则",
        "## 不改范围",
        "## 业务不变量",
        "## 阶段入口门禁",
        "## 阶段 1：身份 Key 固化",
        "## 阶段 2：自动排产服务拆分",
        "## 阶段 3：默认值与历史兼容治理",
        "## 阶段 4：报工联动治理",
        "## 阶段 5：前端页面拆分",
        "## 阶段验收总矩阵",
        "## 停止条件",
        "## 回退策略",
        "禁止一次性大重构",
        "缺前置即阻塞",
        "不引入 fallback",
        "RED -> GREEN -> REGRESSION",
        "真实 E2E",
        "mes-scheduling-domain-contracts.md",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_mes_scheduling_rewrite_control_plan_keeps_identity_and_business_invariants() -> None:
    text = read_text(REWRITE_CONTROL_PLAN_DOC)
    domain_text = read_text(DOMAIN_CONTRACT_DOC)

    assert "## 排产域词典" in domain_text
    assert "## 身份口径判定矩阵" in domain_text

    required_snippets = [
        "routeVersionId + routeProcessId",
        "scheduleOrderId + routeProcessId",
        "taskId -> scheduleOrderProcessId -> scheduleOrderId -> workOrderId",
        "lineId + date + shift",
        "calendarContextToken",
        "受保护任务",
        "predecessorRouteProcessId",
        "routeProcessId",
        "processId",
        "禁止用 processId 跨路线合并",
        "禁止只凭产品号、工序名或 Excel 行文本归属报工",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_mes_scheduling_rewrite_control_plan_gives_each_stage_test_and_stop_rules() -> None:
    text = read_text(REWRITE_CONTROL_PLAN_DOC)

    stage_titles = [
        "## 阶段 1：身份 Key 固化",
        "## 阶段 2：自动排产服务拆分",
        "## 阶段 3：默认值与历史兼容治理",
        "## 阶段 4：报工联动治理",
        "## 阶段 5：前端页面拆分",
    ]
    required_stage_terms = [
        "改写目标",
        "禁止变更",
        "主要风险",
        "BDD 场景",
        "RED",
        "GREEN",
        "REGRESSION",
        "停止条件",
        "回退策略",
        "验收通过口径",
    ]

    for title in stage_titles:
        block = stage_block(text, title)
        for term in required_stage_terms:
            assert term in block, f"{title} missing {term}"
