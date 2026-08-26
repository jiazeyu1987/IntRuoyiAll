package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_RESET_REFERENCED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_IMMUTABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock
    private MesDvMachineryService machineryService;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;

    private MesQaInspectionRegulationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesQaInspectionRegulationServiceImpl(dccProjectCodeMapper, regulationMapper,
                versionMapper, processMapper, itemMapper,
                itemEquipmentMapper,
                activeOrderMapper, pqcInspectionTaskMapper);
        lenient().when(versionMapper.selectLatestPublishedByRegulationId(REGULATION_ID))
                .thenReturn(publishedVersion());
    }

    @Test
    void getCurrent_returnsDccOwnedQaProcessesWithoutMesRouteIdentity() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(processMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(qaProcess()));
        when(itemMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(
                item("FIRST", 5, null), item("PATROL", null, new BigDecimal("0.400000")),
                item("FINAL", 3, null)));

        MesQaInspectionRegulationPublishedVersionRespVO result = service.getCurrent(DCC_PROJECT_ID);

        assertEquals(DCC_PROJECT_ID, result.getDccProjectCodeId());
        assertEquals(1, result.getProcesses().size());
        assertEquals(QA_PROCESS_ID, result.getProcesses().get(0).getQaProcessId());
        assertEquals("清洗", result.getProcesses().get(0).getProcessName());
        assertEquals(List.of("FIRST", "PATROL", "FINAL"),
                result.getProcesses().get(0).getItems().get(0).getApplicableInspectionTypes());
    }

    @Test
    void getCurrent_doesNotExposeEquipmentFromQaVersionSnapshot() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(processMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(qaProcess()));
        when(itemMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(item("FIRST", 5, null)));
        MesQaInspectionRegulationPublishedVersionRespVO result = service.getCurrent(DCC_PROJECT_ID);

        assertTrue(result.getProcesses().get(0).getItems().get(0).getEquipmentOptions().isEmpty());
        verifyNoInteractions(itemEquipmentMapper);
    }

    @Test
    void getCurrent_preservesPatrolAmPmRuleKeysAndAggregatesBusinessItemFields() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectLatestPublishedByRegulationId(REGULATION_ID)).thenReturn(publishedVersion()
                .setInspectionTypeRulesJson("[{\"key\":\"FIRST\",\"inspectionType\":\"FIRST\"," +
                        "\"label\":\"首检\",\"required\":true},{\"key\":\"PATROL_AM\"," +
                        "\"inspectionType\":\"PATROL_AM\",\"label\":\"上午巡检\",\"required\":true}," +
                        "{\"key\":\"PATROL_PM\",\"inspectionType\":\"PATROL_PM\"," +
                        "\"label\":\"下午巡检\",\"required\":true},{\"key\":\"FINAL\"," +
                        "\"inspectionType\":\"FINAL\",\"label\":\"末检\",\"required\":true," +
                        "\"fixedQuantity\":3}]"));
        when(processMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(qaProcess()));
        when(itemMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(
                item("FIRST", 5, null),
                item("PATROL_AM", null, new BigDecimal("0.400000")),
                item("PATROL_PM", null, new BigDecimal("0.400000")),
                item("FINAL", 3, null)));
        MesQaInspectionRegulationPublishedVersionRespVO result = service.getCurrent(DCC_PROJECT_ID);

        MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem businessItem =
                result.getProcesses().get(0).getItems().get(0);
        assertEquals(List.of("FIRST", "PATROL_AM", "PATROL_PM", "FINAL"),
                businessItem.getApplicableInspectionTypes());
        assertEquals(new BigDecimal("0.400000"), businessItem.getPatrolInspectionRatio());
        assertEquals("BOOLEAN", businessItem.getResultType());
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
    void saveDraft_mapsConcurrentActiveDccDuplicateToStableBusinessError() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(null);
        when(regulationMapper.insert(any(MesQaInspectionRegulationDO.class)))
                .thenThrow(new DuplicateKeyException(
                        "Duplicate entry for key 'uk_mes_qa_regulation_active_dcc'"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveDraft(validRequest()));

        assertEquals(1_040_600_310, ex.getCode());
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
    void publish_rejectsMissingFinalItemWhenFinalInspectionApplicable() {
        MesQaInspectionRegulationSaveReqVO reqVO = validRequest();
        reqVO.getProcesses().get(0).getItems().get(0)
                .setApplicableInspectionTypes(List.of("FIRST", "PATROL"));
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.publish(reqVO));

        assertEquals(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID.getCode(), ex.getCode());
        verifyNoInteractions(regulationMapper, processMapper, itemMapper);
    }

    @Test
    void publish_rejectsFinalItemWhenFinalInspectionNotApplicable() {
        MesQaInspectionRegulationSaveReqVO reqVO = validRequest();
        reqVO.setFinalInspectionApplicable(false);
        reqVO.setFinalInspectionNotApplicableReason("当前项目不执行末检");
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.publish(reqVO));

        assertEquals(QA_INSPECTION_REGULATION_FINAL_APPLICABILITY_INVALID.getCode(), ex.getCode());
        verifyNoInteractions(regulationMapper, processMapper, itemMapper);
    }

    @Test
    void publish_allowsMissingFinalOnlyWhenExplicitlyNotApplicable() {
        MesQaInspectionRegulationSaveReqVO reqVO = validRequest();
        reqVO.setFinalInspectionApplicable(false);
        reqVO.setFinalInspectionNotApplicableReason("当前项目不执行末检");
        reqVO.getInspectionTypeRules().stream()
                .filter(rule -> Objects.equals("FINAL", rule.getInspectionType()))
                .forEach(rule -> rule.setRequired(false));
        reqVO.getProcesses().get(0).getItems().get(0)
                .setApplicableInspectionTypes(List.of("FIRST", "PATROL"));
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
        when(versionMapper.selectListByRegulationId(REGULATION_ID)).thenReturn(List.of());
        when(processMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(qaProcess()));
        when(itemMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(
                item("FIRST", 5, null), item("PATROL", null, new BigDecimal("0.400000"))));

        MesQaInspectionRegulationPublishedVersionRespVO result = service.publish(reqVO);

        assertEquals(false, result.getFinalInspectionApplicable());
        verify(itemMapper, org.mockito.Mockito.times(2)).insert(any(MesQaInspectionRegulationItemDO.class));
    }

    @Test
    void publish_retiresAllExistingPublishedVersionsWhenCurrentPointerIsStale() {
        MesQaInspectionRegulationSaveReqVO reqVO = validRequest();
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "G/1")).thenReturn(null);
        doAnswer(invocation -> {
            invocation.<MesQaInspectionRegulationVersionDO>getArgument(0).setId(72L);
            return 1;
        }).when(versionMapper).insert(any(MesQaInspectionRegulationVersionDO.class));
        doAnswer(invocation -> {
            invocation.<MesQaInspectionRegulationProcessDO>getArgument(0).setId(73L);
            return 1;
        }).when(processMapper).insert(any(MesQaInspectionRegulationProcessDO.class));
        when(processMapper.selectListByVersionId(72L)).thenReturn(List.of(qaProcess()
                .setId(73L).setRegulationVersionId(72L)));
        when(itemMapper.selectListByVersionId(72L)).thenReturn(List.of(
                item("FIRST", 5, null).setRegulationVersionId(72L).setQaProcessId(73L),
                item("PATROL", null, new BigDecimal("0.400000")).setRegulationVersionId(72L)
                        .setQaProcessId(73L),
                item("FINAL", 3, null).setRegulationVersionId(72L).setQaProcessId(73L)));
        when(versionMapper.selectListByRegulationId(REGULATION_ID)).thenReturn(List.of(
                publishedVersion(),
                publishedVersion().setId(66L).setPublishedAt(LocalDateTime.of(2026, 8, 12, 10, 0))));

        service.publish(reqVO);

        ArgumentCaptor<MesQaInspectionRegulationVersionDO> versionCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationVersionDO.class);
        verify(versionMapper, org.mockito.Mockito.times(3)).updateById(versionCaptor.capture());
        assertEquals(List.of(62L, 66L), versionCaptor.getAllValues().stream()
                .filter(version -> Objects.equals("RETIRED", version.getLifecycleStatus()))
                .map(MesQaInspectionRegulationVersionDO::getId)
                .sorted()
                .toList());
        assertTrue(versionCaptor.getAllValues().stream()
                .anyMatch(version -> Objects.equals(72L, version.getId())
                        && Objects.equals("PUBLISHED", version.getLifecycleStatus())));
    }

    @Test
    void saveDraft_rejectsDisabledDccProject() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(
                DccProjectCodeDO.builder().id(DCC_PROJECT_ID).status("DISABLE").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveDraft(validRequest()));

        assertEquals(QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID.getCode(), ex.getCode());
    }

    @Test
    void saveDraft_rejectsLegacyResultTypes() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());

        for (String legacyResultType : List.of("NUMBER", "CHOICE")) {
            MesQaInspectionRegulationSaveReqVO reqVO = validRequest();
            reqVO.getProcesses().get(0).getItems().get(0).setResultType(legacyResultType);

            ServiceException ex = assertThrows(ServiceException.class, () -> service.saveDraft(reqVO));

            assertEquals(QA_INSPECTION_REGULATION_ITEM_INVALID.getCode(), ex.getCode());
        }
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

    @Test
    void getProjectStatuses_separatesLatestDraftEditTargetFromPublishedProductionTarget() {
        MesQaInspectionRegulationDO configured = publishedRegulation();
        MesQaInspectionRegulationVersionDO draft = MesQaInspectionRegulationVersionDO.builder()
                .id(64L).regulationId(REGULATION_ID).versionNo("G/1").lifecycleStatus("DRAFT").build();
        MesQaInspectionRegulationVersionDO latestPublished = publishedVersion()
                .setId(66L)
                .setVersionNo("G/2")
                .setPublishedAt(LocalDateTime.of(2026, 8, 12, 10, 0));
        lenient().when(regulationMapper.selectListByDccProjectCodeIds(List.of(DCC_PROJECT_ID)))
                .thenReturn(List.of(configured));
        lenient().when(versionMapper.selectLatestDraftByRegulationId(REGULATION_ID)).thenReturn(draft);
        lenient().when(versionMapper.selectLatestPublishedByRegulationId(REGULATION_ID))
                .thenReturn(latestPublished);

        MesQaInspectionRegulationProjectStatusRespVO status =
                service.getProjectStatuses(List.of(DCC_PROJECT_ID)).get(0);

        assertEquals(REGULATION_ID, longProperty(status, "getEditRegulationId"));
        assertEquals(64L, longProperty(status, "getEditVersionId"));
        assertEquals(REGULATION_ID, longProperty(status, "getPublishedRegulationId"));
        assertEquals(66L, longProperty(status, "getCurrentVersionId"));
        assertEquals(66L, longProperty(status, "getPublishedVersionId"));
        assertEquals("G/2", status.getPublishedVersionNo());
        assertEquals(true, booleanProperty(status, "getProductionReady"));
    }

    @Test
    void resetForTesting_deletesOnlySelectedDccRegulationTreeWhenNoProductionReferences() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectListByRegulationId(REGULATION_ID)).thenReturn(List.of(
                publishedVersion(),
                publishedVersion().setId(72L).setVersionNo("G/1").setLifecycleStatus("DRAFT")));
        when(activeOrderMapper.selectCountByQaRegulationOrVersionIds(REGULATION_ID, List.of(VERSION_ID, 72L)))
                .thenReturn(0L);
        when(pqcInspectionTaskMapper.selectCountByRegulationVersionIds(List.of(VERSION_ID, 72L)))
                .thenReturn(0L);
        when(processMapper.selectCountByVersionIds(List.of(VERSION_ID, 72L))).thenReturn(3L);
        when(itemMapper.selectCountByVersionIds(List.of(VERSION_ID, 72L))).thenReturn(18L);

        var result = service.resetForTesting(DCC_PROJECT_ID);

        assertEquals(DCC_PROJECT_ID, result.getDccProjectCodeId());
        assertEquals(REGULATION_ID, result.getRegulationId());
        assertEquals(2, result.getVersionCount());
        assertEquals(3, result.getProcessCount());
        assertEquals(18, result.getItemCount());
        assertEquals(0, result.getItemEquipmentCount());
        verify(itemMapper).deleteByVersionIds(List.of(VERSION_ID, 72L));
        verify(processMapper).deleteByVersionIds(List.of(VERSION_ID, 72L));
        verify(versionMapper).deleteByRegulationId(REGULATION_ID);
        verify(regulationMapper).deleteById(REGULATION_ID);
    }

    @Test
    void resetForTesting_rejectsReferencedRegulationWithoutDeletingAnything() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectListByRegulationId(REGULATION_ID)).thenReturn(List.of(publishedVersion()));
        when(activeOrderMapper.selectCountByQaRegulationOrVersionIds(REGULATION_ID, List.of(VERSION_ID)))
                .thenReturn(1L);
        when(pqcInspectionTaskMapper.selectCountByRegulationVersionIds(List.of(VERSION_ID)))
                .thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.resetForTesting(DCC_PROJECT_ID));

        assertEquals(QA_INSPECTION_REGULATION_RESET_REFERENCED.getCode(), ex.getCode());
        verify(itemMapper, never()).deleteByVersionIds(any());
        verify(processMapper, never()).deleteByVersionIds(any());
        verify(versionMapper, never()).deleteByRegulationId(any());
        verify(regulationMapper, never()).deleteById(any());
    }

    @Test
    void getPublishedVersion_rejectsWhenNoPublishedVersionExists() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectLatestPublishedByRegulationId(REGULATION_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getPublishedVersion(DCC_PROJECT_ID, null));

        assertEquals(QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void getPublishedVersion_usesLatestPublishedVersionWhenCurrentPointerIsStale() {
        MesQaInspectionRegulationVersionDO latestPublished = publishedVersion()
                .setId(66L)
                .setVersionNo("G/2")
                .setPublishedAt(LocalDateTime.of(2026, 8, 12, 10, 0));
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectLatestPublishedByRegulationId(REGULATION_ID)).thenReturn(latestPublished);
        when(processMapper.selectListByVersionId(66L)).thenReturn(List.of(qaProcess()
                .setRegulationVersionId(66L)));
        when(itemMapper.selectListByVersionId(66L)).thenReturn(List.of(
                item("FIRST", 5, null).setRegulationVersionId(66L),
                item("PATROL", null, new BigDecimal("0.400000")).setRegulationVersionId(66L),
                item("FINAL", 3, null).setRegulationVersionId(66L)));

        MesQaInspectionRegulationPublishedVersionRespVO result =
                service.getPublishedVersion(DCC_PROJECT_ID, null);

        assertEquals(66L, result.getPublishedVersionId());
        assertEquals("G/2", result.getVersionNo());
    }

    @Test
    void getPublishedVersion_allowsExplicitRetiredFrozenVersion() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectById(VERSION_ID)).thenReturn(publishedVersion().setLifecycleStatus("RETIRED"));
        when(processMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(qaProcess()));
        when(itemMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(
                item("FIRST", 5, null), item("PATROL", null, new BigDecimal("0.400000")),
                item("FINAL", 3, null)));

        MesQaInspectionRegulationPublishedVersionRespVO result =
                service.getPublishedVersion(DCC_PROJECT_ID, VERSION_ID);

        assertEquals(VERSION_ID, result.getPublishedVersionId());
        assertEquals("RETIRED", result.getLifecycleStatus());
    }

    @Test
    void getLockedVersionForOrder_returnsRetiredQaAggregateWithoutEnabledDccCheck() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(
                DccProjectCodeDO.builder().id(DCC_PROJECT_ID).status("DISABLE").build());
        when(regulationMapper.selectById(REGULATION_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectById(VERSION_ID)).thenReturn(publishedVersion()
                .setLifecycleStatus("RETIRED"));
        MesQaInspectionRegulationProcessDO first = qaProcess();
        MesQaInspectionRegulationProcessDO second = qaProcess()
                .setId(64L)
                .setProcessCode("ID-QA-002")
                .setProcessName("精洗")
                .setSort(2);
        when(processMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(first, second));
        when(itemMapper.selectListByVersionId(VERSION_ID)).thenReturn(List.of(
                item("FIRST", 5, null), item("PATROL", null, new BigDecimal("0.400000")),
                item("FINAL", 3, null), item("PATROL", null, new BigDecimal("0.400000"))
                        .setId(82L).setQaProcessId(64L).setItemCode("ID-002")));

        MesQaInspectionRegulationPublishedVersionRespVO result =
                service.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, VERSION_ID);

        assertEquals(VERSION_ID, result.getPublishedVersionId());
        assertEquals("RETIRED", result.getLifecycleStatus());
        assertEquals(List.of(QA_PROCESS_ID, 64L), result.getProcesses().stream()
                .map(MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess::getQaProcessId).toList());
        assertEquals(List.of("清洗", "精洗"), result.getProcesses().stream()
                .map(MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess::getProcessName).toList());
        assertEquals(List.of("FIRST", "PATROL", "FINAL"), result.getProcesses().get(0)
                .getItems().get(0).getApplicableInspectionTypes());
    }

    @Test
    void getLockedVersionForOrder_rejectsRegulationOutsideDccProject() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectById(REGULATION_ID)).thenReturn(publishedRegulation()
                .setDccProjectCodeId(999L));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, VERSION_ID));

        assertEquals(QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID.getCode(), ex.getCode());
        verifyNoInteractions(versionMapper, processMapper, itemMapper);
    }

    @Test
    void getLockedVersionForOrder_rejectsVersionOutsideRegulation() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectById(REGULATION_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectById(VERSION_ID)).thenReturn(publishedVersion()
                .setRegulationId(999L));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, VERSION_ID));

        assertEquals(QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS.getCode(), ex.getCode());
        verifyNoInteractions(processMapper, itemMapper);
    }

    @Test
    void getLockedVersionForOrder_rejectsDraftVersion() {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
        when(regulationMapper.selectById(REGULATION_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectById(VERSION_ID)).thenReturn(publishedVersion()
                .setLifecycleStatus("DRAFT"));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                service.getLockedVersionForOrder(DCC_PROJECT_ID, REGULATION_ID, VERSION_ID));

        assertEquals(QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED.getCode(), ex.getCode());
        verifyNoInteractions(processMapper, itemMapper);
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

    private static Long longProperty(Object target, String getter) {
        return (Long) property(target, getter);
    }

    private static Boolean booleanProperty(Object target, String getter) {
        return (Boolean) property(target, getter);
    }

    private static Object property(Object target, String getter) {
        try {
            Method method = target.getClass().getMethod(getter);
            return method.invoke(target);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(target.getClass().getSimpleName() + "." + getter + " is required", ex);
        }
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
