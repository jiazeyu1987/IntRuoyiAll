from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
READ_DO = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesTeamLeaderActiveOrderDetailReadDO.java"
MAPPER_XML = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml"
SERVICE_DETAIL = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java"
DETAIL_SERVICE = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java"
RESP_VO = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java"
CONTROLLER = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_active_order_detail_returns_work_order_drawing_number_for_summary_tab():
    read_do = read(READ_DO)
    mapper_xml = read(MAPPER_XML)
    service_detail = read(SERVICE_DETAIL)
    detail_service = read(DETAIL_SERVICE)
    resp_vo = read(RESP_VO)
    controller = read(CONTROLLER)

    assert "private String drawingNumber;" in read_do
    assert "work_order.drawing_number AS drawingNumber" in mapper_xml
    assert "private String drawingNumber;" in service_detail
    assert ".setDrawingNumber(first.getDrawingNumber())" in detail_service
    assert "private String drawingNumber;" in resp_vo
    assert ".setDrawingNumber(detail.getDrawingNumber())" in controller
