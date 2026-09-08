from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SERVICE = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/MesStage1ActiveOrderCompleteSimulationServiceImpl.java"


def main() -> None:
    source = SERVICE.read_text(encoding="utf-8")
    match = re.search(
        r"return new MesStage1ActiveOrderCompleteSimulationResult\(\)(?P<body>.*?)"
        r"\.setActiveOrderCompleteSnapshot\(snapshot\);",
        source,
        re.S,
    )
    if not match:
        raise AssertionError("未找到 Stage1 模拟响应构造代码，无法校验返回身份。")
    response_body = match.group("body")
    if ".setActiveOrderId(activeOrder.getId())" in response_body:
        raise AssertionError(
            "Stage1 模拟响应不能返回临时生成 activeOrder.getId()；该临时单会被 cleanup 清理，详情页会报活跃订单不存在。"
        )
    expected = ".setActiveOrderId(templateActiveOrder.getId())"
    if expected not in response_body:
        raise AssertionError("Stage1 模拟响应必须返回被点击来源活跃订单 templateActiveOrder.getId()。")
    if ".setWorkOrderId(activeOrder.getWorkOrderId())" in response_body:
        raise AssertionError(
            "Stage1 模拟响应不能返回临时生成 activeOrder.getWorkOrderId()；生产工单身份也必须指向来源订单。"
        )
    expected_work_order = ".setWorkOrderId(templateWorkOrder.getId())"
    if expected_work_order not in response_body:
        raise AssertionError("Stage1 模拟响应必须返回被点击来源活跃订单对应的 templateWorkOrder.getId()。")


if __name__ == "__main__":
    main()
