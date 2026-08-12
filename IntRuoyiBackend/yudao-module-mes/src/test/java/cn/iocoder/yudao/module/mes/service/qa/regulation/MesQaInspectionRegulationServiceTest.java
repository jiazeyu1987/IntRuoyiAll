package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_IMMUTABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesQaInspectionRegulationServiceTest {

    private static final Long DCC_PROJECT_ID = 147L;
    private static final Long REGULATION_ID = 61L;
    private static final Long VERSION_ID = 62L;
    private static final Long QA_PROCESS_ID = 63L;

    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock
    private MesQaInspectionRegulationMapper regulationMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper versionMapper;
    @Mock
    private MesQaInspectionRegulationProcessMapper processMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper itemMapper;
    @Mock
    private MesQaInspectionRegulationItemEquipmentMapper itemEquipmentMapper;

    private MesQaInspectionRegulationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesQaInspectionRegulationServiceImpl(dccProjectCodeMapper, regulationMapper,
                versionMapper, processMapper, itemMapper, itemEquipmentMapper);
    }

    @Test
    void getCurrent_returnsDccOwnedQaProcessesWithoutMesRouteIdentity() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectById(VERSION_ID)).thenReturn(publishedVersion());
        when(processMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(qaProcess()));
        when(itemMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(
                item("FIRST", 5, null), item("PATROL", null, new BigDecimal("0.400000")),
                item("FINAL", 3, null)));
        when(itemEquipmentMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of());

        MesQaInspectionRegulationPublishedVersionRespVO result = service.getCurrent(DCC_PROJECT_ID);

        assertEquals(DCC_PROJECT_ID, result.getDccProjectCodeId());
        assertEquals(1, result.getProcesses().size());
        assertEquals(QA_PROCESS_ID, result.getProcesses().get(0).getQaProcessId());
        assertEquals("清洗", result.getProcesses().get(0).getProcessName());
        assertEquals(List.of("FIRST", "PATROL", "FINAL"),
                result.getProcesses().get(0).getItems().get(0).getApplicableInspectionTypes());
    }

    @Test
    void getCurrent_returnsNullForEnabledDccWithoutQaConfiguration() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(null);

        assertNull(service.getCurrent(DCC_PROJECT_ID));
    }

    @Test
    void getCurrent_prefersLatestSavedDraftOverPublishedVersion() {
        MesQaInspectionRegulationVersionDO draftVersion = publishedVersion()
                .setId(72L)
                .setVersionNo("G/1")
                .setLifecycleStatus("DRAFT")
                .setPublishedAt(null);
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectLatestDraftByRegulationId(REGULATION_ID)).thenReturn(draftVersion);
        when(processMapper.selectListByVersionId(72L)).thenReturn(List.of(qaProcess()
                .setId(73L).setRegulationVersionId(72L).setProcessName("QA新增工序")));
        when(itemMapper.selectListByVersionId(72L)).thenReturn(List.of(item("PATROL", null,
                new BigDecimal("0.400000")).setRegulationVersionId(72L).setQaProcessId(73L)));
        when(itemEquipmentMapper.selectListByVersionId(72L)).thenReturn(List.of());

        MesQaInspectionRegulationPublishedVersionRespVO result = service.getCurrent(DCC_PROJECT_ID);

        assertEquals("DRAFT", result.getLifecycleStatus());
        assertEquals("G/1", result.getVersionNo());
        assertEquals(false, result.getImmutable());
        assertEquals("QA新增工序", result.getProcesses().get(0).getProcessName());
    }

    @Test
    void saveDraft_persistsDccRootQaProcessAndExpandedInspectionRows() {
        MesQaInspectionRegulationSaveReqVO reqVO = validRequest();
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MesQaInspectionRegulationDO>getArgument(0).setId(REGULATION_ID);
            return 1;
        }).when(regulationMapper).insert(any(MesQaInspectionRegulationDO.class));
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "G/1")).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MesQaInspectionRegulationVersionDO>getArgument(0).setId(VERSION_ID);
            return 1;
        }).when(versionMapper).insert(any(MesQaInspectionRegulationVersionDO.class));
        doAnswer(invocation -> {
            invocation.<MesQaInspectionRegulationProcessDO>getArgument(0).setId(QA_PROCESS_ID);
            return 1;
        }).when(processMapper).insert(any(MesQaInspectionRegulationProcessDO.class));

        service.saveDraft(reqVO);

        ArgumentCaptor<MesQaInspectionRegulationDO> regulationCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationDO.class);
        verify(regulationMapper).insert(regulationCaptor.capture());
        assertEquals(DCC_PROJECT_ID, regulationCaptor.getValue().getDccProjectCodeId());
        assertNull(regulationCaptor.getValue().getRouteProcessId());
        ArgumentCaptor<MesQaInspectionRegulationProcessDO> processCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationProcessDO.class);
        verify(processMapper).insert(processCaptor.capture());
        assertEquals("ID-QA-001", processCaptor.getValue().getProcessCode());
        ArgumentCaptor<MesQaInspectionRegulationItemDO> itemCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationItemDO.class);
        verify(itemMapper, org.mockito.Mockito.times(3)).insert(itemCaptor.capture());
        itemCaptor.getAllValues().forEach(item -> assertEquals(QA_PROCESS_ID, item.getQaProcessId()));
    }

    @Test
    void publish_rejectsPublishedVersionMutation() {
        MesQaInspectionRegulationSaveReqVO reqVO = validRequest();
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "G/1"))
                .thenReturn(MesQaInspectionRegulationVersionDO.builder()
                        .id(VERSION_ID).regulationId(REGULATION_ID).versionNo("G/1")
                        .lifecycleStatus("PUBLISHED").snapshotJson("{}").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.publish(reqVO));

        assertEquals(QA_INSPECTION_REGULATION_VERSION_IMMUTABLE.getCode(), ex.getCode());
    }

    @Test
    void saveDraft_rejectsDisabledDccProject() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(
                DccProjectCodeDO.builder().id(DCC_PROJECT_ID).status("DISABLE").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveDraft(validRequest()));

        assertEquals(QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID.getCode(), ex.getCode());
    }

    @Test
    void getProjectStatuses_preservesRequestedDccOrder() {
        MesQaInspectionRegulationDO configured = publishedRegulation().setDccProjectCodeId(200L);
        when(regulationMapper.selectListByDccProjectCodeIds(List.of(300L, 200L)))
                .thenReturn(List.of(configured));

        List<MesQaInspectionRegulationProjectStatusRespVO> result =
                service.getProjectStatuses(List.of(300L, 200L));

        assertEquals(300L, result.get(0).getDccProjectCodeId());
        assertEquals(false, result.get(0).getConfigured());
        assertEquals(200L, result.get(1).getDccProjectCodeId());
        assertEquals(true, result.get(1).getConfigured());
    }

    private static DccProjectCodeDO enabledDccProject() {
        return DccProjectCodeDO.builder()
                .id(DCC_PROJECT_ID)
                .projectCode("ID")
                .projectName("球囊扩张压力泵")
                .docControlNo("112")
                .status("ENABLE")
                .build();
    }

    private static MesQaInspectionRegulationDO publishedRegulation() {
        return MesQaInspectionRegulationDO.builder()
                .id(REGULATION_ID)
                .dccProjectCodeId(DCC_PROJECT_ID)
                .ownerModule("MES_QA")
                .regulationCode("PQC-ID-001")
                .regulationName("（椎体）球囊扩张压力泵组装过程检验规程")
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(VERSION_ID)
                .build();
    }

    private static MesQaInspectionRegulationVersionDO publishedVersion() {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(VERSION_ID)
                .regulationId(REGULATION_ID)
                .versionNo("G/0")
                .lifecycleStatus("PUBLISHED")
                .effectiveDate(LocalDate.of(2025, 9, 30))
                .publishedAt(LocalDateTime.of(2026, 8, 11, 10, 0))
                .finalInspectionApplicable(true)
                .inspectionTypeRulesJson("[{\"key\":\"FINAL\",\"inspectionType\":\"FINAL\"," +
                        "\"label\":\"末检\",\"required\":true,\"fixedQuantity\":3}]")
                .snapshotJson("{\"regulationCode\":\"PQC-ID-001\"," +
                        "\"regulationName\":\"（椎体）球囊扩张压力泵组装过程检验规程\"}")
                .build();
    }

    private static MesQaInspectionRegulationProcessDO qaProcess() {
        return MesQaInspectionRegulationProcessDO.builder()
                .id(QA_PROCESS_ID)
                .regulationVersionId(VERSION_ID)
                .processCode("ID-QA-001")
                .processName("清洗")
                .sort(1)
                .build();
    }

    private static MesQaInspectionRegulationItemDO item(
            String inspectionType, Integer quantity, BigDecimal patrolRatio) {
        return MesQaInspectionRegulationItemDO.builder()
                .id((long) inspectionType.hashCode())
                .regulationVersionId(VERSION_ID)
                .qaProcessId(QA_PROCESS_ID)
                .itemSort(1)
                .inspectionType(inspectionType)
                .itemCode("ID-001-WASH-APP")
                .itemName("外观")
                .inspectionMethod("目视检查")
                .inspectionTool("目测")
                .samplingPlanText("AQL=0.4")
                .standardText("表面清洁")
                .equipmentRequired(false)
                .resultType("BOOLEAN")
                .firstInspectionQuantity(quantity)
                .patrolInspectionRatio(patrolRatio)
                .critical(false)
                .build();
    }

    private static MesQaInspectionRegulationSaveReqVO validRequest() {
        MesQaInspectionRegulationSaveReqVO reqVO = new MesQaInspectionRegulationSaveReqVO();
        reqVO.setDccProjectCodeId(DCC_PROJECT_ID);
        reqVO.setRegulationCode("PQC-ID-001");
        reqVO.setRegulationName("（椎体）球囊扩张压力泵组装过程检验规程");
        reqVO.setVersionNo("G/1");
        reqVO.setEffectiveDate(LocalDate.of(2026, 8, 11));
        reqVO.setFinalInspectionApplicable(true);
        reqVO.setInspectionTypeRules(List.of(
                inspectionTypeRule("FIRST", "FIRST", null),
                inspectionTypeRule("PATROL", "PATROL", null),
                inspectionTypeRule("FINAL", "FINAL", 3)));

        MesQaInspectionRegulationSaveReqVO.InspectionItem item =
                new MesQaInspectionRegulationSaveReqVO.InspectionItem();
        item.setItemSort(1);
        item.setItemCode("ID-001-WASH-APP");
        item.setItemName("外观");
        item.setInspectionMethod("目视检查");
        item.setInspectionTool("目测");
        item.setSamplingPlanText("首件：5件；AQL=0.4");
        item.setStandardText("表面清洁");
        item.setEquipmentRequired(false);
        item.setEquipmentOptions(List.of());
        item.setResultType("BOOLEAN");
        item.setApplicableInspectionTypes(List.of("FIRST", "PATROL", "FINAL"));
        item.setFirstInspectionQuantity(5);
        item.setPatrolInspectionRatio(new BigDecimal("0.400000"));
        item.setCritical(false);

        MesQaInspectionRegulationSaveReqVO.InspectionProcess process =
                new MesQaInspectionRegulationSaveReqVO.InspectionProcess();
        process.setProcessCode("ID-QA-001");
        process.setProcessName("清洗");
        process.setSort(1);
        process.setItems(List.of(item));
        reqVO.setProcesses(List.of(process));
        return reqVO;
    }

    private static MesQaInspectionRegulationSaveReqVO.InspectionTypeRule inspectionTypeRule(
            String key, String type, Integer fixedQuantity) {
        MesQaInspectionRegulationSaveReqVO.InspectionTypeRule rule =
                new MesQaInspectionRegulationSaveReqVO.InspectionTypeRule();
        rule.setKey(key);
        rule.setInspectionType(type);
        rule.setLabel(key);
        rule.setRequired(true);
        rule.setFixedQuantity(fixedQuantity);
        return rule;
    }
}
