package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesQaInspectionRegulationWordImportServiceTest {

    private static final Long DCC_PROJECT_ID = 147L;
    private static final Long REGULATION_ID = 61L;
    private static final Long PUBLISHED_VERSION_ID = 62L;

    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock
    private MesQaInspectionRegulationMapper regulationMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper versionMapper;
    @Mock
    private MesQaInspectionRegulationWordParser parser;
    @Mock
    private MesQaInspectionRegulationService regulationService;

    private MesQaInspectionRegulationWordImportService importService;

    @BeforeEach
    void setUp() {
        importService = new MesQaInspectionRegulationWordImportService(
                dccProjectCodeMapper, regulationMapper, versionMapper, parser, regulationService);
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(enabledDccProject());
    }

    @Test
    void importWordDraft_createsNewDraftWithExplicitFinalNotApplicable() throws Exception {
        when(parser.parse(any(), any())).thenReturn(parsed("B/0"));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(null);
        when(regulationService.saveDraft(any())).thenReturn(savedDraft(71L, 72L, "B/0"));

        MesQaInspectionRegulationImportRespVO response = importService.importWordDraft(file(), DCC_PROJECT_ID);

        ArgumentCaptor<MesQaInspectionRegulationSaveReqVO> requestCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationSaveReqVO.class);
        verify(regulationService).saveDraft(requestCaptor.capture());
        MesQaInspectionRegulationSaveReqVO request = requestCaptor.getValue();
        assertEquals("CREATE", response.getRoute());
        assertEquals(1, response.getProcessCount());
        assertEquals(1, response.getItemCount());
        assertEquals(0, response.getInheritedItemCount());
        assertFalse(request.getFinalInspectionApplicable());
        assertEquals("源 QA 模板未规定末检", request.getFinalInspectionNotApplicableReason());
        assertEquals(List.of("FIRST", "PATROL"),
                request.getProcesses().get(0).getItems().get(0).getApplicableInspectionTypes());
        assertEquals(new BigDecimal("0.4"),
                request.getProcesses().get(0).getItems().get(0).getPatrolInspectionRatio());
        assertTrue(request.getProcesses().get(0).getProcessCode().startsWith("PQC-TEST-001-P"));
        assertTrue(request.getProcesses().get(0).getItems().get(0).getItemCode()
                .startsWith("PQC-TEST-001-I"));
    }

    @Test
    void importWordDraft_upgradesAndInheritsExactNamedItemConfiguration() throws Exception {
        MesQaInspectionRegulationDO regulation = publishedRegulation();
        when(parser.parse(any(), any())).thenReturn(parsed("B/1"));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(regulation);
        when(versionMapper.selectListDraftByRegulationId(REGULATION_ID)).thenReturn(List.of());
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "B/1")).thenReturn(null);
        when(regulationService.getPublishedVersion(DCC_PROJECT_ID, null))
                .thenReturn(existingConfiguration());
        when(regulationService.saveDraft(any())).thenReturn(savedDraft(REGULATION_ID, 73L, "B/1"));

        MesQaInspectionRegulationImportRespVO response = importService.importWordDraft(file(), DCC_PROJECT_ID);

        ArgumentCaptor<MesQaInspectionRegulationSaveReqVO> requestCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationSaveReqVO.class);
        verify(regulationService).saveDraft(requestCaptor.capture());
        MesQaInspectionRegulationSaveReqVO request = requestCaptor.getValue();
        MesQaInspectionRegulationSaveReqVO.InspectionItem item =
                request.getProcesses().get(0).getItems().get(0);
        assertEquals("UPGRADE", response.getRoute());
        assertEquals(1, response.getInheritedItemCount());
        assertEquals("LEGACY-P01", request.getProcesses().get(0).getProcessCode());
        assertEquals("LEGACY-I01", item.getItemCode());
        assertEquals("NUMERIC", item.getResultType());
        assertEquals(new BigDecimal("1.2"), item.getStandardLowerLimit());
        assertEquals(new BigDecimal("2.4"), item.getStandardUpperLimit());
        assertEquals(1, item.getEquipmentOptions().size());
        assertTrue(item.getCritical());
        assertEquals("原失败规则", item.getFailureRule());
        assertEquals("表面清洁，无异物", item.getStandardText());
        assertEquals("目视检查", item.getInspectionMethod());
        assertEquals(List.of("FIRST", "PATROL", "FINAL"), item.getApplicableInspectionTypes());
    }

    @Test
    void importWordDraft_rejectsPublishedSameVersionWithoutSaving() throws Exception {
        when(parser.parse(any(), any())).thenReturn(parsed("B/0"));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectListDraftByRegulationId(REGULATION_ID)).thenReturn(List.of());
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "B/0"))
                .thenReturn(MesQaInspectionRegulationVersionDO.builder()
                        .id(PUBLISHED_VERSION_ID)
                        .regulationId(REGULATION_ID)
                        .versionNo("B/0")
                        .lifecycleStatus("PUBLISHED")
                        .build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> importService.importWordDraft(file(), DCC_PROJECT_ID));

        assertTrue(exception.getMessage().contains("已发布"));
        verify(regulationService, never()).saveDraft(any());
    }

    @Test
    void importWordDraft_usesLatestPublishedVersionWhenRegulationPointerIsStale() throws Exception {
        when(parser.parse(any(), any())).thenReturn(parsed("B/1"));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectListDraftByRegulationId(REGULATION_ID)).thenReturn(List.of());
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "B/1")).thenReturn(null);
        MesQaInspectionRegulationPublishedVersionRespVO latest = existingConfiguration();
        latest.setPublishedVersionId(66L);
        latest.getProcesses().get(0).getItems().get(0).setItemCode("LATEST-I01");
        when(regulationService.getPublishedVersion(DCC_PROJECT_ID, null)).thenReturn(latest);
        when(regulationService.saveDraft(any())).thenReturn(savedDraft(REGULATION_ID, 73L, "B/1"));

        importService.importWordDraft(file(), DCC_PROJECT_ID);

        ArgumentCaptor<MesQaInspectionRegulationSaveReqVO> requestCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationSaveReqVO.class);
        verify(regulationService).getPublishedVersion(DCC_PROJECT_ID, null);
        verify(regulationService).saveDraft(requestCaptor.capture());
        assertEquals("LATEST-I01", requestCaptor.getValue().getProcesses().get(0).getItems().get(0).getItemCode());
    }

    @Test
    void importWordDraft_rejectsDifferentOpenDraftWithoutSaving() throws Exception {
        when(parser.parse(any(), any())).thenReturn(parsed("B/1"));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectListDraftByRegulationId(REGULATION_ID)).thenReturn(List.of(
                MesQaInspectionRegulationVersionDO.builder()
                        .id(70L)
                        .regulationId(REGULATION_ID)
                        .versionNo("B/2")
                        .lifecycleStatus("DRAFT")
                        .build()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> importService.importWordDraft(file(), DCC_PROJECT_ID));

        assertTrue(exception.getMessage().contains("B/2"));
        verify(regulationService, never()).saveDraft(any());
    }

    @Test
    void importWordDraft_updatesSameVersionDraft() throws Exception {
        when(parser.parse(any(), any())).thenReturn(parsed("B/1"));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        MesQaInspectionRegulationVersionDO draft = MesQaInspectionRegulationVersionDO.builder()
                .id(70L)
                .regulationId(REGULATION_ID)
                .versionNo("B/1")
                .lifecycleStatus("DRAFT")
                .build();
        when(versionMapper.selectListDraftByRegulationId(REGULATION_ID)).thenReturn(List.of(draft));
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "B/1")).thenReturn(draft);
        when(regulationService.getCurrent(DCC_PROJECT_ID)).thenReturn(existingConfiguration());
        when(regulationService.saveDraft(any())).thenReturn(savedDraft(REGULATION_ID, 70L, "B/1"));

        MesQaInspectionRegulationImportRespVO response = importService.importWordDraft(file(), DCC_PROJECT_ID);

        ArgumentCaptor<MesQaInspectionRegulationSaveReqVO> requestCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationSaveReqVO.class);
        verify(regulationService).saveDraft(requestCaptor.capture());
        assertEquals("UPGRADE", response.getRoute());
        assertEquals(REGULATION_ID, requestCaptor.getValue().getRegulationId());
        assertEquals(1, response.getInheritedItemCount());
    }

    @Test
    void importWordDraft_upgradesByFullSourceOriginalItemWhenTerminalItemNamesRepeat() throws Exception {
        when(parser.parse(any(), any())).thenReturn(parsedWithItem("B/1",
                "大包装工序", "外箱 / 外观"));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectListDraftByRegulationId(REGULATION_ID)).thenReturn(List.of());
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "B/1")).thenReturn(null);
        when(regulationService.getPublishedVersion(DCC_PROJECT_ID, null))
                .thenReturn(existingPackagingTerminalNameConfiguration());
        when(regulationService.saveDraft(any())).thenReturn(savedDraft(REGULATION_ID, 73L, "B/1"));

        MesQaInspectionRegulationImportRespVO response = importService.importWordDraft(file(), DCC_PROJECT_ID);

        ArgumentCaptor<MesQaInspectionRegulationSaveReqVO> requestCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationSaveReqVO.class);
        verify(regulationService).saveDraft(requestCaptor.capture());
        MesQaInspectionRegulationSaveReqVO.InspectionItem item =
                requestCaptor.getValue().getProcesses().get(0).getItems().get(0);
        assertEquals("UPGRADE", response.getRoute());
        assertEquals(1, response.getInheritedItemCount());
        assertEquals("LEGACY-PACK-BOX", item.getItemCode());
        assertEquals("NUMERIC", item.getResultType());
        assertEquals("新标准", item.getStandardText());
        assertEquals("大包装工序 / 外箱 / 外观", item.getSourceOriginalItem());
    }

    @Test
    void importWordDraft_ignoresAmbiguousLegacyTerminalNameWhenNewItemHasFullPath() throws Exception {
        when(parser.parse(any(), any())).thenReturn(parsedWithItem("B/1",
                "大包装工序", "外箱 / 外观"));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectListDraftByRegulationId(REGULATION_ID)).thenReturn(List.of());
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "B/1")).thenReturn(null);
        when(regulationService.getPublishedVersion(DCC_PROJECT_ID, null))
                .thenReturn(existingAmbiguousTerminalNameConfiguration());
        when(regulationService.saveDraft(any())).thenReturn(savedDraft(REGULATION_ID, 73L, "B/1"));

        MesQaInspectionRegulationImportRespVO response = importService.importWordDraft(file(), DCC_PROJECT_ID);

        ArgumentCaptor<MesQaInspectionRegulationSaveReqVO> requestCaptor =
                ArgumentCaptor.forClass(MesQaInspectionRegulationSaveReqVO.class);
        verify(regulationService).saveDraft(requestCaptor.capture());
        MesQaInspectionRegulationSaveReqVO.InspectionItem item =
                requestCaptor.getValue().getProcesses().get(0).getItems().get(0);
        assertEquals("UPGRADE", response.getRoute());
        assertEquals(0, response.getInheritedItemCount());
        assertTrue(item.getItemCode().startsWith("PQC-TEST-001-I"));
        assertEquals("BOOLEAN", item.getResultType());
        assertEquals("大包装工序 / 外箱 / 外观", item.getSourceOriginalItem());
    }

    @Test
    void importWordDraft_rejectsAmbiguousLegacyTerminalNameWhenNewItemNeedsThatKey() throws Exception {
        when(parser.parse(any(), any())).thenReturn(parsedWithItem("B/1",
                "大包装工序", "外观"));
        when(regulationMapper.selectByDccProjectCodeId(DCC_PROJECT_ID)).thenReturn(publishedRegulation());
        when(versionMapper.selectListDraftByRegulationId(REGULATION_ID)).thenReturn(List.of());
        when(versionMapper.selectByRegulationIdAndVersionNo(REGULATION_ID, "B/1")).thenReturn(null);
        when(regulationService.getPublishedVersion(DCC_PROJECT_ID, null))
                .thenReturn(existingAmbiguousTerminalNameConfiguration());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> importService.importWordDraft(file(), DCC_PROJECT_ID));

        assertTrue(exception.getMessage().contains("同名检验项目不唯一：大包装工序 / 外观"));
        verify(regulationService, never()).saveDraft(any());
    }

    @Test
    void importWordDraft_rejectsInvalidDccProjectWithoutParsingOrSaving() throws Exception {
        when(dccProjectCodeMapper.selectById(DCC_PROJECT_ID)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> importService.importWordDraft(file(), DCC_PROJECT_ID));

        assertTrue(exception.getMessage().contains("DCC"));
        verify(parser, never()).parse(any(), any());
        verify(regulationService, never()).saveDraft(any());
    }

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "测试QA模板.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1, 2, 3});
    }

    private static DccProjectCodeDO enabledDccProject() {
        DccProjectCodeDO project = new DccProjectCodeDO();
        project.setId(DCC_PROJECT_ID);
        project.setStatus(DccProjectCodeStatusConstants.ENABLE);
        return project;
    }

    private static MesQaInspectionRegulationDO publishedRegulation() {
        return MesQaInspectionRegulationDO.builder()
                .id(REGULATION_ID)
                .dccProjectCodeId(DCC_PROJECT_ID)
                .regulationCode("PQC-TEST-001")
                .regulationName("测试产品组装过程检验规程")
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(PUBLISHED_VERSION_ID)
                .build();
    }

    private static MesQaInspectionRegulationWordParser.ParsedRegulation parsed(String versionNo) {
        return new MesQaInspectionRegulationWordParser.ParsedRegulation(
                "PQC-TEST-001", "测试产品组装过程检验规程", versionNo,
                LocalDate.of(2026, 8, 17), List.of(
                new MesQaInspectionRegulationWordParser.ParsedItem(
                        "清洗", "外观", "表面清洁，无异物", "目视检查", "目测",
                        "首件：5件；GB/T 2828.1，I，AQL=0.4", 5, new BigDecimal("0.4"))),
                "测试QA模板.docx");
    }

    private static MesQaInspectionRegulationWordParser.ParsedRegulation parsedWithItem(
            String versionNo, String processName, String itemName) {
        return new MesQaInspectionRegulationWordParser.ParsedRegulation(
                "PQC-TEST-001", "测试产品组装过程检验规程", versionNo,
                LocalDate.of(2026, 8, 17), List.of(
                new MesQaInspectionRegulationWordParser.ParsedItem(
                        processName, itemName, "新标准", "目视检查", "目测",
                        "首件：5件；GB/T 2828.1，I，AQL=0.4", 5, new BigDecimal("0.4"))),
                "测试QA模板.docx");
    }

    private static MesQaInspectionRegulationSaveRespVO savedDraft(
            Long regulationId, Long draftVersionId, String versionNo) {
        return MesQaInspectionRegulationSaveRespVO.builder()
                .dccProjectCodeId(DCC_PROJECT_ID)
                .regulationId(regulationId)
                .draftVersionId(draftVersionId)
                .versionNo(versionNo)
                .lifecycleStatus("DRAFT")
                .immutable(false)
                .build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO existingConfiguration() {
        MesQaInspectionRegulationPublishedVersionRespVO.EquipmentOption equipment =
                MesQaInspectionRegulationPublishedVersionRespVO.EquipmentOption.builder()
                        .equipmentId(11L)
                        .equipmentCode("EQ-11")
                        .equipmentName("压力表")
                        .equipmentNumber("NO-11")
                        .defaultFlag(true)
                        .sort(1)
                        .build();
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem item =
                MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem.builder()
                        .itemSort(1)
                        .itemCode("LEGACY-I01")
                        .itemName("外观")
                        .inspectionMethod("旧方法")
                        .inspectionTool("旧器具")
                        .samplingPlanText("AQL=1.0")
                        .standardText("旧标准")
                        .standardLowerLimit(new BigDecimal("1.2"))
                        .standardUpperLimit(new BigDecimal("2.4"))
                        .standardUnit("MPa")
                        .standardPrecision(2)
                        .equipmentRequired(true)
                        .equipmentOptions(List.of(equipment))
                        .resultType("NUMERIC")
                        .applicableInspectionTypes(List.of("FIRST", "PATROL", "FINAL"))
                        .firstInspectionQuantity(3)
                        .patrolInspectionRatio(new BigDecimal("1.0"))
                        .critical(true)
                        .failureRule("原失败规则")
                        .build();
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess process =
                MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess.builder()
                        .qaProcessId(81L)
                        .processCode("LEGACY-P01")
                        .processName("清洗")
                        .sort(1)
                        .items(List.of(item))
                        .build();
        return MesQaInspectionRegulationPublishedVersionRespVO.builder()
                .dccProjectCodeId(DCC_PROJECT_ID)
                .regulationId(REGULATION_ID)
                .publishedVersionId(PUBLISHED_VERSION_ID)
                .versionNo("B/0")
                .effectiveDate(LocalDate.of(2026, 1, 4))
                .lifecycleStatus("PUBLISHED")
                .regulationCode("PQC-TEST-001")
                .regulationName("测试产品组装过程检验规程")
                .finalInspectionApplicable(true)
                .inspectionTypeRules(List.of(
                        rule("FIRST", "FIRST", null),
                        rule("PATROL_AM", "PATROL", null),
                        rule("PATROL_PM", "PATROL", null),
                        rule("FINAL", "FINAL", 3)))
                .processes(List.of(process))
                .build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO existingPackagingTerminalNameConfiguration() {
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem labelAppearance =
                legacyItem("LEGACY-PACK-LABEL", "外观", "大包装工序 / 标签 / 外观");
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem boxAppearance =
                legacyItem("LEGACY-PACK-BOX", "外观", "大包装工序 / 外箱 / 外观");
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess process =
                MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess.builder()
                        .qaProcessId(82L)
                        .processCode("LEGACY-PACK")
                        .processName("大包装工序")
                        .sort(1)
                        .items(List.of(labelAppearance, boxAppearance))
                        .build();
        return MesQaInspectionRegulationPublishedVersionRespVO.builder()
                .dccProjectCodeId(DCC_PROJECT_ID)
                .regulationId(REGULATION_ID)
                .publishedVersionId(PUBLISHED_VERSION_ID)
                .versionNo("B/0")
                .effectiveDate(LocalDate.of(2026, 1, 4))
                .lifecycleStatus("PUBLISHED")
                .regulationCode("PQC-TEST-001")
                .regulationName("测试产品组装过程检验规程")
                .finalInspectionApplicable(false)
                .finalInspectionNotApplicableReason("原版本不适用末检")
                .inspectionTypeRules(List.of(
                        rule("FIRST", "FIRST", null),
                        rule("PATROL_AM", "PATROL", null),
                        rule("PATROL_PM", "PATROL", null)))
                .processes(List.of(process))
                .build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO existingAmbiguousTerminalNameConfiguration() {
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem labelAppearance =
                legacyItem("LEGACY-PACK-LABEL", "外观", "大包装工序 / 外观");
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem boxAppearance =
                legacyItem("LEGACY-PACK-BOX", "外观", "大包装工序 / 外观");
        MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess process =
                MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess.builder()
                        .qaProcessId(82L)
                        .processCode("LEGACY-PACK")
                        .processName("大包装工序")
                        .sort(1)
                        .items(List.of(labelAppearance, boxAppearance))
                        .build();
        return MesQaInspectionRegulationPublishedVersionRespVO.builder()
                .dccProjectCodeId(DCC_PROJECT_ID)
                .regulationId(REGULATION_ID)
                .publishedVersionId(PUBLISHED_VERSION_ID)
                .versionNo("B/0")
                .effectiveDate(LocalDate.of(2026, 1, 4))
                .lifecycleStatus("PUBLISHED")
                .regulationCode("PQC-TEST-001")
                .regulationName("测试产品组装过程检验规程")
                .finalInspectionApplicable(false)
                .finalInspectionNotApplicableReason("原版本不适用末检")
                .inspectionTypeRules(List.of(
                        rule("FIRST", "FIRST", null),
                        rule("PATROL_AM", "PATROL", null),
                        rule("PATROL_PM", "PATROL", null)))
                .processes(List.of(process))
                .build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem legacyItem(
            String itemCode, String itemName, String sourceOriginalItem) {
        return MesQaInspectionRegulationPublishedVersionRespVO.InspectionItem.builder()
                .itemSort(1)
                .itemCode(itemCode)
                .itemName(itemName)
                .inspectionMethod("旧方法")
                .inspectionTool("旧器具")
                .samplingPlanText("AQL=1.0")
                .standardText("旧标准")
                .standardLowerLimit(new BigDecimal("1.2"))
                .standardUpperLimit(new BigDecimal("2.4"))
                .standardUnit("MPa")
                .standardPrecision(2)
                .equipmentRequired(false)
                .equipmentOptions(List.of())
                .resultType("NUMERIC")
                .applicableInspectionTypes(List.of("FIRST", "PATROL"))
                .firstInspectionQuantity(3)
                .patrolInspectionRatio(new BigDecimal("1.0"))
                .critical(true)
                .failureRule("原失败规则")
                .sourceOriginalItem(sourceOriginalItem)
                .build();
    }

    private static MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule rule(
            String key, String type, Integer fixedQuantity) {
        return MesQaInspectionRegulationPublishedVersionRespVO.InspectionTypeRule.builder()
                .key(key)
                .inspectionType(type)
                .label(key)
                .roundLabel(key)
                .required(true)
                .fixedQuantity(fixedQuantity)
                .taskRule(key)
                .releaseGate(key)
                .build();
    }
}
