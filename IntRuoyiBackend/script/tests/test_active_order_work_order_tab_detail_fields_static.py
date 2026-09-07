from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

mapper = read("yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml")
read_do = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesTeamLeaderActiveOrderDetailReadDO.java")
domain = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java")
vo = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java")
service = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java")
controller = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java")
test = read("yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImplTest.java")

required_mapper = [
    "work_order.quantity AS workOrderQuantity",
    "product_item.code AS productCode",
    "product_item.name AS productName",
    "work_order.create_time AS workOrderCreateTime",
]
for snippet in required_mapper:
    assert snippet in mapper, f"详情 SQL 缺少正式工单字段: {snippet}"

for source_name, source in [("ReadDO", read_do), ("domain", domain), ("VO", vo)]:
    for snippet in ["private BigDecimal workOrderQuantity;", "private String productCode;", "private String productName;", "private LocalDateTime workOrderCreateTime;"]:
        assert snippet in source, f"{source_name} 缺少字段: {snippet}"

for snippet in [
    ".setWorkOrderQuantity(first.getWorkOrderQuantity())",
    ".setProductCode(first.getProductCode())",
    ".setProductName(first.getProductName())",
    ".setWorkOrderCreateTime(first.getWorkOrderCreateTime())",
]:
    assert snippet in service, f"详情服务未透传字段: {snippet}"

for snippet in [
    ".setWorkOrderQuantity(detail.getWorkOrderQuantity())",
    ".setProductCode(detail.getProductCode())",
    ".setProductName(detail.getProductName())",
    ".setWorkOrderCreateTime(detail.getWorkOrderCreateTime())",
]:
    assert snippet in controller, f"Controller 未映射字段: {snippet}"

assert "shouldExposeWorkOrderHeaderFieldsForDetailTab" in test, "后端单元测试必须覆盖生产工单 tab 所需字段"

print("PASS: active order work order tab detail fields static contract")
