package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_REQUIRED_RULE_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
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
    @Mock
    private MesQaInspectionRegulationItemEquipmentMapper itemEquipmentMapper;

    private MesQaInspectionRegulationService service;

    @BeforeEach
    void setUp() {
        service = new MesQaInspectionRegulationServiceImpl(regulationMapper, versionMapper, itemMapper,
                itemEquipmentMapper);
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
        assertEquals(true, result.getFinalInspectionApplicable());
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

    @Test
    void getProjectStatuses_returnsConfiguredAndUnconfiguredProductsInRequestOrder() {
        MesQaInspectionRegulationDO published = MesQaInspectionRegulationDO.builder()
                .id(9101L)
                .productId(1001L)
                .regulationCode("QA-IDI-001")
                .regulationName("按压式球囊扩充压力泵 QA 检验规程")
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(9102L)
                .build();
        MesQaInspectionRegulationDO draft = MesQaInspectionRegulationDO.builder()
                .id(9201L)
                .productId(1003L)
                .regulationCode("QA-NEW-001")
                .regulationName("新产品 QA 检验规程")
                .lifecycleStatus("DRAFT")
                .currentVersionId(9202L)
                .build();
        when(regulationMapper.selectListByProductIds(List.of(1001L, 1002L, 1003L)))
                .thenReturn(List.of(draft, published));

        List<MesQaInspectionRegulationProjectStatusRespVO> result =
                service.getProjectStatuses(List.of(1001L, 1002L, 1003L));

        assertEquals(3, result.size());
        assertEquals(1001L, result.get(0).getProductId());
        assertTrue(result.get(0).getConfigured());
        assertEquals(9101L, result.get(0).getRegulationId());
        assertEquals("PUBLISHED", result.get(0).getLifecycleStatus());
        assertEquals("QA-IDI-001", result.get(0).getRegulationCode());
        assertEquals(1002L, result.get(1).getProductId());
        assertEquals(false, result.get(1).getConfigured());
        assertEquals(1003L, result.get(2).getProductId());
        assertTrue(result.get(2).getConfigured());
        assertEquals("DRAFT", result.get(2).getLifecycleStatus());
    }

    @Test
    void publish_rejectsMissingFinalInspectionRule() {
        MesQaInspectionRegulationSaveReqVO reqVO = saveReq(List.of(
                saveItem("FIRST", "首检外观", 5, null),
                saveItem("PATROL", "巡检耐压", null, new BigDecimal("0.050000"))));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.publish(reqVO));

        assertEquals(QA_INSPECTION_REGULATION_REQUIRED_RULE_MISSING.getCode(), ex.getCode());
    }

    @Test
    void publish_rejectsMissingFinalApplicability() {
        MesQaInspectionRegulationSaveReqVO reqVO = saveReq(List.of(
                saveItem("FIRST", "首检外观", 5, null),
                saveItem("PATROL", "巡检耐压", null, new BigDecimal("0.050000")),
                saveItem("FINAL", "末检包装", 3, null)));
        reqVO.setFinalInspectionApplicable(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.publish(reqVO));

        assertEquals(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID.getCode(), ex.getCode());
    }

    @Test
    void publish_allowsMissingFinalOnlyWhenExplicitlyNotApplicable() {
        MesQaInspectionRegulationSaveReqVO reqVO = saveReq(List.of(
                saveItem("FIRST", "首检外观", 5, null),
                saveItem("PATROL", "巡检耐压", null, new BigDecimal("0.050000"))));
        reqVO.setFinalInspectionApplicable(false);
        reqVO.setFinalInspectionNotApplicableReason("该工序后续 OQC 覆盖最终包装确认");
        when(regulationMapper.selectByRouteProcess(1001L, 2001L, 3001L, 4001L, 5001L)).thenReturn(null);
        doAnswer(invocation -> {
            MesQaInspectionRegulationDO regulation = invocation.getArgument(0);
            regulation.setId(REGULATION_ID);
            return 1;
        }).when(regulationMapper).insert(any(MesQaInspectionRegulationDO.class));
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "V21-QA-2")).thenReturn(null);
        doAnswer(invocation -> {
            MesQaInspectionRegulationVersionDO version = invocation.getArgument(0);
            version.setId(VERSION_ID);
            return 1;
        }).when(versionMapper).insert(any(MesQaInspectionRegulationVersionDO.class));

        MesQaInspectionRegulationPublishedVersionRespVO result = service.publish(reqVO);

        assertEquals(false, result.getFinalInspectionApplicable());
        assertEquals("该工序后续 OQC 覆盖最终包装确认", result.getFinalInspectionNotApplicableReason());
        assertEquals(0, result.getFinalInspectionRules().size());
        verify(itemMapper, org.mockito.Mockito.times(2)).insert(any(MesQaInspectionRegulationItemDO.class));
    }

    @Test
    void publish_rejectsFinalItemsWhenFinalInspectionNotApplicable() {
        MesQaInspectionRegulationSaveReqVO reqVO = saveReq(List.of(
                saveItem("FIRST", "首检外观", 5, null),
                saveItem("PATROL", "巡检耐压", null, new BigDecimal("0.050000")),
                saveItem("FINAL", "末检包装", 3, null)));
        reqVO.setFinalInspectionApplicable(false);
        reqVO.setFinalInspectionNotApplicableReason("该工序后续 OQC 覆盖最终包装确认");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.publish(reqVO));

        assertEquals(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID.getCode(), ex.getCode());
    }

    @Test
    void publish_createsImmutablePublishedVersionAndItems() {
        MesQaInspectionRegulationSaveReqVO reqVO = saveReq(List.of(
                saveItem("FIRST", "首检外观", 5, null),
                saveItem("PATROL", "巡检耐压", null, new BigDecimal("0.050000")),
                saveItem("FINAL", "末检包装", 3, null)));
        when(regulationMapper.selectByRouteProcess(1001L, 2001L, 3001L, 4001L, 5001L)).thenReturn(null);
        doAnswer(invocation -> {
            MesQaInspectionRegulationDO regulation = invocation.getArgument(0);
            regulation.setId(REGULATION_ID);
            return 1;
        }).when(regulationMapper).insert(any(MesQaInspectionRegulationDO.class));
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "V21-QA-2")).thenReturn(null);
        doAnswer(invocation -> {
            MesQaInspectionRegulationVersionDO version = invocation.getArgument(0);
            version.setId(VERSION_ID);
            return 1;
        }).when(versionMapper).insert(any(MesQaInspectionRegulationVersionDO.class));

        MesQaInspectionRegulationPublishedVersionRespVO result = service.publish(reqVO);

        assertEquals(REGULATION_ID, result.getRegulationId());
        assertEquals(VERSION_ID, result.getPublishedVersionId());
        assertEquals("V21-QA-2", result.getVersionNo());
        assertTrue(result.getImmutable());
        assertEquals(true, result.getFinalInspectionApplicable());
        assertEquals(1, result.getFirstInspectionRules().size());
        assertEquals(1, result.getPatrolInspectionRules().size());
        assertEquals(1, result.getFinalInspectionRules().size());
        verify(regulationMapper).updateById(any(MesQaInspectionRegulationDO.class));
        verify(versionMapper).updateById(any(MesQaInspectionRegulationVersionDO.class));
        verify(itemMapper, org.mockito.Mockito.times(3)).insert(any(MesQaInspectionRegulationItemDO.class));
    }

    @Test
    void saveDraft_persistsFormalItemEquipmentOptions() {
        MesQaInspectionRegulationSaveReqVO.InspectionItem firstItem =
                saveItem("FIRST", "首检外观", 5, null);
        firstItem.setEquipmentRequired(true);
        firstItem.setEquipmentOptions(List.of(equipmentOption(8101L, "EQ-001", "检验灯箱", "BOX-001")));
        MesQaInspectionRegulationSaveReqVO reqVO = saveReq(List.of(firstItem));
        when(regulationMapper.selectByRouteProcess(1001L, 2001L, 3001L, 4001L, 5001L)).thenReturn(null);
        doAnswer(invocation -> {
            MesQaInspectionRegulationDO regulation = invocation.getArgument(0);
            regulation.setId(REGULATION_ID);
            return 1;
        }).when(regulationMapper).insert(any(MesQaInspectionRegulationDO.class));
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "V21-QA-2")).thenReturn(null);
        doAnswer(invocation -> {
            MesQaInspectionRegulationVersionDO version = invocation.getArgument(0);
            version.setId(VERSION_ID);
            return 1;
        }).when(versionMapper).insert(any(MesQaInspectionRegulationVersionDO.class));

        service.saveDraft(reqVO);

        ArgumentCaptor<MesQaInspectionRegulationItemEquipmentDO> equipmentCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationItemEquipmentDO.class);
        verify(itemEquipmentMapper).insert(equipmentCaptor.capture());
        MesQaInspectionRegulationItemEquipmentDO equipment = equipmentCaptor.getValue();
        assertEquals(VERSION_ID, equipment.getRegulationVersionId());
        assertEquals("FIRST", equipment.getInspectionType());
        assertEquals("FIRST-SAVE", equipment.getItemCode());
        assertEquals(8101L, equipment.getEquipmentId());
        assertEquals("EQ-001", equipment.getEquipmentCode());
        assertEquals("检验灯箱", equipment.getEquipmentName());
        assertEquals("BOX-001", equipment.getEquipmentNumber());
    }

    @Test
    void saveDraft_rejectsEquipmentOptionsWhenEquipmentNotRequired() {
        MesQaInspectionRegulationSaveReqVO.InspectionItem firstItem =
                saveItem("FIRST", "首检外观", 5, null);
        firstItem.setEquipmentRequired(false);
        firstItem.setEquipmentOptions(List.of(equipmentOption(8101L, "EQ-001", "检验灯箱", "BOX-001")));
        MesQaInspectionRegulationSaveReqVO reqVO = saveReq(List.of(firstItem));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveDraft(reqVO));

        assertEquals(QA_INSPECTION_REGULATION_ITEM_INVALID.getCode(), ex.getCode());
    }

    private static MesQaInspectionRegulationVersionDO publishedVersion() {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(VERSION_ID)
                .regulationId(REGULATION_ID)
                .versionNo("V21-QA-1")
                .lifecycleStatus("PUBLISHED")
                .publishedAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .finalInspectionApplicable(true)
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

    private static MesQaInspectionRegulationSaveReqVO saveReq(
            List<MesQaInspectionRegulationSaveReqVO.InspectionItem> items) {
        MesQaInspectionRegulationSaveReqVO reqVO = new MesQaInspectionRegulationSaveReqVO();
        reqVO.setProductId(1001L);
        reqVO.setProductName("球囊扩张压力泵");
        reqVO.setRouteId(2001L);
        reqVO.setRouteName("球囊扩张压力泵路线");
        reqVO.setRouteVersionId(3001L);
        reqVO.setRouteVersionNo("v21");
        reqVO.setRouteProcessId(4001L);
        reqVO.setProcessId(5001L);
        reqVO.setRouteProcessName("粗洗");
        reqVO.setBatchRecordBindingSummary("粗洗工序生产记录");
        reqVO.setRegulationCode("QA-PUMP-V21-002");
        reqVO.setRegulationName("球囊扩张压力泵 QA 检验规程");
        reqVO.setVersionNo("V21-QA-2");
        reqVO.setFinalInspectionApplicable(true);
        reqVO.setItems(items);
        return reqVO;
    }

    private static MesQaInspectionRegulationSaveReqVO.InspectionItem saveItem(String inspectionType, String name,
                                                                              Integer firstQuantity,
                                                                              BigDecimal patrolRatio) {
        MesQaInspectionRegulationSaveReqVO.InspectionItem item =
                new MesQaInspectionRegulationSaveReqVO.InspectionItem();
        item.setInspectionType(inspectionType);
        item.setItemCode(inspectionType + "-SAVE");
        item.setItemName(name);
        item.setInspectionMethod(name + "方法");
        item.setStandardText(name + "标准");
        item.setResultType("BOOLEAN");
        item.setFirstInspectionQuantity(firstQuantity);
        item.setPatrolInspectionRatio(patrolRatio);
        return item;
    }

    private static MesQaInspectionRegulationSaveReqVO.EquipmentOption equipmentOption(Long equipmentId,
                                                                                      String equipmentCode,
                                                                                      String equipmentName,
                                                                                      String equipmentNumber) {
        MesQaInspectionRegulationSaveReqVO.EquipmentOption option =
                new MesQaInspectionRegulationSaveReqVO.EquipmentOption();
        option.setEquipmentId(equipmentId);
        option.setEquipmentCode(equipmentCode);
        option.setEquipmentName(equipmentName);
        option.setEquipmentNumber(equipmentNumber);
        option.setDefaultFlag(true);
        option.setSort(1);
        return option;
    }
}
