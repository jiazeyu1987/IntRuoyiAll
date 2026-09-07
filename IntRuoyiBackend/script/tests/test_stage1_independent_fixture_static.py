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


def test_stage1_simulates_on_independent_generated_active_order():
    service = read(SERVICE_PATH)
    simulate_method = service[
        service.index("public MesStage1ActiveOrderCompleteSimulationResult simulate("):
        service.index("private MesProcessPoolActiveOrderDO createFixture(")
    ]

    assert "MesProcessPoolActiveOrderDO templateActiveOrder" in simulate_method
    assert "MesProWorkOrderDO templateWorkOrder" in simulate_method
    assert re.search(
        r"createFixture\(templateActiveOrder,\s*templateWorkOrder,\s*sourceBindings,\s*validated\)",
        simulate_method,
    )
    assert ".simulateActiveOrderCompletion(validated.getActorUserId(), activeOrder.getId(), STAGE," in simulate_method
    assert "ensureFormalProductIssue(templateActiveOrder" not in simulate_method


def test_stage1_frontend_opens_generated_order_detail_with_source_order_context():
    page = read(FRONTEND_PAGE_PATH)
    handler = page[
        page.index("const handleSimulateStage1 = async"):
        page.index("const handleSimulateStage2_5 = async")
    ]

    assert "navigateActiveOrderSubmissionDetail(generatedActiveOrderId, row.workOrderCode)" in handler
    assert "navigateActiveOrderSubmissionDetail(activeOrderId)" not in handler


if __name__ == "__main__":
    test_stage1_simulates_on_independent_generated_active_order()
    test_stage1_frontend_opens_generated_order_detail_with_source_order_context()
    print("PASS: stage1 independent fixture static contract")
