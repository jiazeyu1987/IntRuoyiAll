import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE_PATH = (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
    "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"
)
FRONTEND_PAGE_PATH = (
    "../IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue"
)


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def test_stage1_simulates_on_clicked_active_order():
    service = read(SERVICE_PATH)
    simulate_method = service[
        service.index("public MesStage1ActiveOrderCompleteSimulationResult simulate("):
        service.index("private MesProcessPoolActiveOrderDO createFixture(")
    ]

    assert "MesProcessPoolActiveOrderDO templateActiveOrder" in simulate_method
    assert "MesProWorkOrderDO templateWorkOrder" in simulate_method
    assert "createFixture(templateActiveOrder" not in simulate_method
    assert re.search(
        r"simulateActiveOrderCompletion\(validated\.getActorUserId\(\),\s*templateActiveOrder\.getId\(\),\s*STAGE,",
        simulate_method,
    )
    assert "calculateStage1PersistedProgress(templateActiveOrder)" in simulate_method
    assert "ensureFormalProductIssue(templateActiveOrder, templateWorkOrder, validated)" in simulate_method


def test_stage1_frontend_opens_clicked_order_detail():
    page = read(FRONTEND_PAGE_PATH)
    handler = page[
        page.index("const handleSimulateStage1 = async"):
        page.index("const handleSimulateStage2_5 = async")
    ]

    assert "navigateActiveOrderSubmissionDetail(activeOrderId)" in handler
    assert "generatedActiveOrderId" not in handler
    assert "stage1GeneratedDetailTargets" not in page


if __name__ == "__main__":
    test_stage1_simulates_on_clicked_active_order()
    test_stage1_frontend_opens_clicked_order_detail()
    print("PASS: stage1 clicked active-order simulation static contract")
