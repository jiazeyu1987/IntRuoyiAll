from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
EVENT_PARTY = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesTeamLeaderActiveOrderEventPartyReadDO.java"
MAPPER_XML = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml"
SERVICE_DETAIL = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java"
DETAIL_SERVICE = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java"
RESP_VO = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java"
CONTROLLER = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_active_order_detail_exposes_pqc_scrap_quantity_from_pqc_event_payload():
    event_party = read(EVENT_PARTY)
    mapper_xml = read(MAPPER_XML)
    service_detail = read(SERVICE_DETAIL)
    detail_service = read(DETAIL_SERVICE)
    resp_vo = read(RESP_VO)
    controller = read(CONTROLLER)

    assert "private Integer scrapQuantity;" in event_party
    assert "JSON_EXTRACT(pool_event.raw_payload, '$.scrapQuantity')" in mapper_xml
    assert "private Integer scrapQuantity;" in service_detail
    assert "private Integer scrapQuantity;" in resp_vo
    assert ".setScrapQuantity(scrapQuantity)" in detail_service
    assert "eventParty.getScrapQuantity()" in detail_service
    assert ".setScrapQuantity(submission.getScrapQuantity())" in controller
