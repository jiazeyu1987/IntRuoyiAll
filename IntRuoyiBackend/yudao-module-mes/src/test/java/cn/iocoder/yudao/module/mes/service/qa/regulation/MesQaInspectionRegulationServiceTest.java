package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesQaInspectionRegulationServiceTest {

    private static final Long REGULATION_ID = 9001L;
    private static final Long VERSION_ID = 9002L;

    @Mock
    private MesQaInspectionRegulationMapper regulationMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper versionMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper itemMapper;

    private MesQaInspectionRegulationService service;

    @BeforeEach
    void setUp() {
        service = new MesQaInspectionRegulationServiceImpl(regulationMapper, versionMapper, itemMapper);
    }

    @Test
    void getPublishedVersion_returnsImmutableRouteProcessRulesAndBatchRecordSnapshot() {
        when(versionMapper.selectById(VERSION_ID)).thenReturn(publishedVersion());
        when(regulationMapper.selectById(REGULATION_ID)).thenReturn(publishedRegulation());
        when(itemMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(
                item("FIRST", "首检外观", 5, null),
                item("PATROL", "巡检耐压", null, new BigDecimal("0.050000")),
                item("FINAL", "末检包装", 3, null)));

        MesQaInspectionRegulationPublishedVersionRespVO result = service.getPublishedVersion(VERSION_ID);

        assertEquals(VERSION_ID, result.getPublishedVersionId());
        assertEquals("V21-QA-1", result.getVersionNo());
        assertTrue(result.getImmutable());
        assertEquals("球囊扩张压力泵", result.getProductName());
        assertEquals("球囊扩张压力泵路线", result.getRouteName());
        assertEquals("v21", result.getRouteVersionNo());
        assertEquals("粗洗", result.getRouteProcessName());
        assertEquals("粗洗工序生产记录", result.getBatchRecordBindingSummary());
        assertEquals(1, result.getFirstInspectionRules().size());
        assertEquals(1, result.getPatrolInspectionRules().size());
        assertEquals(1, result.getFinalInspectionRules().size());
    }

    @Test
    void getPublishedVersion_rejectsDraftVersion() {
        when(versionMapper.selectById(VERSION_ID)).thenReturn(MesQaInspectionRegulationVersionDO.builder()
                .id(VERSION_ID)
                .regulationId(REGULATION_ID)
                .versionNo("DRAFT")
                .lifecycleStatus("DRAFT")
                .snapshotJson("{}")
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.getPublishedVersion(VERSION_ID));

        assertEquals(QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED.getCode(), ex.getCode());
    }

    private static MesQaInspectionRegulationVersionDO publishedVersion() {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(VERSION_ID)
                .regulationId(REGULATION_ID)
                .versionNo("V21-QA-1")
                .lifecycleStatus("PUBLISHED")
                .publishedAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .snapshotJson("""
                        {
                          "productName": "球囊扩张压力泵",
                          "routeName": "球囊扩张压力泵路线",
                          "routeVersionNo": "v21",
                          "routeProcessName": "粗洗",
                          "batchRecordReports": [
                            { "batchRecordReportName": "粗洗工序生产记录" }
                          ]
                        }
                        """)
                .build();
    }

    private static MesQaInspectionRegulationDO publishedRegulation() {
        return MesQaInspectionRegulationDO.builder()
                .id(REGULATION_ID)
                .productId(1001L)
                .routeId(2001L)
                .routeVersionId(3001L)
                .routeProcessId(4001L)
                .processId(5001L)
                .ownerModule("MES_QA")
                .regulationCode("QA-PUMP-V21-001")
                .regulationName("球囊扩张压力泵 QA 检验规程")
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(VERSION_ID)
                .build();
    }

    private static MesQaInspectionRegulationItemDO item(String inspectionType, String name,
                                                       Integer firstQuantity, BigDecimal patrolRatio) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(VERSION_ID)
                .inspectionType(inspectionType)
                .itemCode(inspectionType + "-001")
                .itemName(name)
                .inspectionMethod(name + "方法")
                .standardText(name + "标准")
                .resultType("BOOLEAN")
                .firstInspectionQuantity(firstQuantity)
                .patrolInspectionRatio(patrolRatio)
                .build();
    }
}
