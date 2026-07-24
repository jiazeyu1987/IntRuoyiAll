package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeUpdateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeExportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeImportRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeAssociatedFileAiCategoryRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeImportBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeImportRowMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryLifecycleStageEnum;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeImportActionConstants;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeImportStatusConstants;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import cn.idev.excel.annotation.ExcelProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.STATUS_ACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.STATUS_REVOKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSOCIATED_FILE_ALREADY_CATEGORIZED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSOCIATED_FILE_CONCURRENT_MODIFICATION;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSOCIATED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DELETE_REFERENCED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DUPLICATE;

@Import(DccProjectCodeServiceImpl.class)
class DccProjectCodeServiceImplTest extends BaseDbUnitTest {

    private static final List<String> EXPECTED_HEADERS = List.of(
            "文控", "项目名称", "项目代码", "类别", "委托生产",
            "项目组负责人", "项目工程师", "存放位置", "优先级");

    @Resource
    private DccProjectCodeService projectCodeService;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccProjectCodeImportBatchMapper importBatchMapper;
    @Resource
    private DccProjectCodeImportRowMapper importRowMapper;
    @Resource
    private DccProjectCodeAssignmentMapper assignmentMapper;
    @MockitoSpyBean
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @MockitoBean
    private DccControlledFileQueryService controlledFileQueryService;
    @MockitoBean
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @MockitoBean
    private PermissionApi permissionApi;

    @Test
    void createUpdateDeleteShouldPersistNormalizedFieldsAndAllowBlankProjectCode() {
        DccProjectCodeSaveReqVO createReqVO = new DccProjectCodeSaveReqVO();
        createReqVO.setDocControlNo(" 12 ");
        createReqVO.setProjectName(" 项目A ");
        createReqVO.setProjectCode(" ");
        createReqVO.setCategory(" 类别A ");
        createReqVO.setCommissionedProduction(" √ ");
        createReqVO.setProjectLeader(" 负责人A ");
        createReqVO.setProjectEngineer(" 工程师A ");
        createReqVO.setStorageLocation(" 新N ");
        createReqVO.setPriority(" 高 ");
        createReqVO.setStatus(DccProjectCodeStatusConstants.ENABLE);

        Long id = projectCodeService.createProjectCode(createReqVO);

        DccProjectCodeDO created = projectCodeMapper.selectById(id);
        assertNotNull(created);
        assertEquals("12", created.getDocControlNo());
        assertEquals("项目A", created.getProjectName());
        assertEquals("", created.getProjectCode());
        assertEquals("类别A", created.getCategory());
        assertEquals("√", created.getCommissionedProduction());
        assertEquals("负责人A", created.getProjectLeader());
        assertEquals("工程师A", created.getProjectEngineer());
        assertEquals("新N", created.getStorageLocation());
        assertEquals("高", created.getPriority());

        DccProjectCodeUpdateReqVO updateReqVO = new DccProjectCodeUpdateReqVO();
        updateReqVO.setId(id);
        updateReqVO.setDocControlNo(" ");
        updateReqVO.setProjectName(" 项目A-更新 ");
        updateReqVO.setProjectCode(" CODE-A ");
        updateReqVO.setCategory(" ");
        updateReqVO.setCommissionedProduction(" ");
        updateReqVO.setProjectLeader(" ");
        updateReqVO.setProjectEngineer(" 工程师B ");
        updateReqVO.setStorageLocation(" ");
        updateReqVO.setPriority(" 中 ");
        updateReqVO.setStatus(DccProjectCodeStatusConstants.DISABLE);

        projectCodeService.updateProjectCode(updateReqVO);

        DccProjectCodeDO updated = projectCodeMapper.selectById(id);
        assertNotNull(updated);
        assertNull(updated.getDocControlNo());
        assertEquals("项目A-更新", updated.getProjectName());
        assertEquals("CODE-A", updated.getProjectCode());
        assertNull(updated.getCategory());
        assertNull(updated.getCommissionedProduction());
        assertNull(updated.getProjectLeader());
        assertEquals("工程师B", updated.getProjectEngineer());
        assertNull(updated.getStorageLocation());
        assertEquals("中", updated.getPriority());
        assertEquals(DccProjectCodeStatusConstants.DISABLE, updated.getStatus());

        projectCodeService.deleteProjectCode(id);

        assertNull(projectCodeMapper.selectById(id));
    }

    @Test
    void createOrUpdateShouldRejectDuplicateProjectNameAndProjectCode() {
        projectCodeMapper.insert(DccProjectCodeDO.builder()
                .docControlNo("1")
                .projectName("项目A")
                .projectCode("CODE-A")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());

        DccProjectCodeSaveReqVO createReqVO = new DccProjectCodeSaveReqVO();
        createReqVO.setProjectName("项目A");
        createReqVO.setProjectCode("CODE-A");
        createReqVO.setStatus(DccProjectCodeStatusConstants.ENABLE);
        assertServiceException(() -> projectCodeService.createProjectCode(createReqVO), PROJECT_CODE_DUPLICATE);

        DccProjectCodeDO another = DccProjectCodeDO.builder()
                .docControlNo("2")
                .projectName("项目B")
                .projectCode("CODE-B")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCodeMapper.insert(another);

        DccProjectCodeUpdateReqVO updateReqVO = new DccProjectCodeUpdateReqVO();
        updateReqVO.setId(another.getId());
        updateReqVO.setProjectName("项目A");
        updateReqVO.setProjectCode("CODE-A");
        updateReqVO.setStatus(DccProjectCodeStatusConstants.ENABLE);
        assertServiceException(() -> projectCodeService.updateProjectCode(updateReqVO), PROJECT_CODE_DUPLICATE);
    }

    @Test
    void deleteShouldRejectReferencedProjectCode() {
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .docControlNo("1")
                .projectName("项目A")
                .projectCode("CODE-A")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCodeMapper.insert(projectCode);
        controlledFileMapper.insert(DccControlledFileDO.builder()
                .masterId(1L)
                .categoryId(1L)
                .directoryId(1L)
                .sourceFileId(1L)
                .originalFileId(1L)
                .fileName("seed.pdf")
                .title("seed")
                .fileNumber("DOC-001")
                .dccProjectCodeId(projectCode.getId())
                .needTraining(false)
                .processType("CONTROLLED_FILE")
                .versionNo("A1")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .submitterId(1L)
                .requesterId(1L)
                .build());

        assertServiceException(() -> projectCodeService.deleteProjectCode(projectCode.getId()),
                PROJECT_CODE_DELETE_REFERENCED);
    }

    @Test
    void pageShouldSupportKeywordCategoryPriorityAndStatusFilters() {
        projectCodeMapper.insert(DccProjectCodeDO.builder()
                .docControlNo("1")
                .projectName("项目A")
                .projectCode("CODE-A")
                .category("类别A")
                .projectLeader("负责人甲")
                .priority("高")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        projectCodeMapper.insert(DccProjectCodeDO.builder()
                .docControlNo("2")
                .projectName("项目B")
                .projectCode("CODE-B")
                .category("类别B")
                .projectLeader("负责人乙")
                .priority("低")
                .status(DccProjectCodeStatusConstants.DISABLE)
                .build());

        DccProjectCodePageReqVO reqVO = new DccProjectCodePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setKeyword("负责人甲");
        reqVO.setCategory("类别A");
        reqVO.setPriority("高");
        reqVO.setStatus(DccProjectCodeStatusConstants.ENABLE);

        PageResult<DccProjectCodeDO> pageResult = projectCodeService.getProjectCodePage(reqVO);

        assertEquals(1, pageResult.getList().size());
        assertEquals("项目A", pageResult.getList().get(0).getProjectName());
    }

    @Test
    void pageShouldIncludeAssociatedFileCountAndSortByCount() {
        DccProjectCodeDO emptyProjectCode = insertProjectCode("1", "项目A", "CODE-A");
        DccProjectCodeDO oneFileProjectCode = insertProjectCode("2", "项目B", "CODE-B");
        DccProjectCodeDO twoFileProjectCode = insertProjectCode("3", "项目C", "CODE-C");
        insertControlledFile(oneFileProjectCode.getId(), "B-001");
        insertControlledFile(twoFileProjectCode.getId(), "C-001");
        insertControlledFile(twoFileProjectCode.getId(), "C-002");

        DccProjectCodePageReqVO ascReqVO = new DccProjectCodePageReqVO();
        ascReqVO.setPageNo(1);
        ascReqVO.setPageSize(20);
        ascReqVO.setFileCountSort("asc");

        PageResult<DccProjectCodeDO> ascPageResult = projectCodeService.getProjectCodePage(ascReqVO);

        assertEquals(List.of(emptyProjectCode.getId(), oneFileProjectCode.getId(), twoFileProjectCode.getId()),
                ascPageResult.getList().stream().map(DccProjectCodeDO::getId).toList());
        assertEquals(List.of(0L, 1L, 2L),
                ascPageResult.getList().stream().map(DccProjectCodeDO::getAssociatedFileCount).toList());

        DccProjectCodePageReqVO descReqVO = new DccProjectCodePageReqVO();
        descReqVO.setPageNo(1);
        descReqVO.setPageSize(20);
        descReqVO.setFileCountSort("desc");

        PageResult<DccProjectCodeDO> descPageResult = projectCodeService.getProjectCodePage(descReqVO);

        assertEquals(List.of(twoFileProjectCode.getId(), oneFileProjectCode.getId(), emptyProjectCode.getId()),
                descPageResult.getList().stream().map(DccProjectCodeDO::getId).toList());
        assertEquals(List.of(2L, 1L, 0L),
                descPageResult.getList().stream().map(DccProjectCodeDO::getAssociatedFileCount).toList());
    }

    @Test
    void pageForAssignedUserShouldOnlyReturnOwnActiveAssignedProjectCodes() {
        DccProjectCodeDO ownProjectCode = insertProjectCode("1", "项目A", "CODE-A");
        DccProjectCodeDO otherProjectCode = insertProjectCode("2", "项目B", "CODE-B");
        insertAssignment(ownProjectCode.getId(), 123L, STATUS_ACTIVE, LocalDateTime.now().plusDays(1));
        insertAssignment(otherProjectCode.getId(), 456L, STATUS_ACTIVE, LocalDateTime.now().plusDays(1));
        insertAssignment(otherProjectCode.getId(), 123L, STATUS_REVOKED, LocalDateTime.now().plusDays(1));
        when(permissionApi.hasAnyRolesOrSuperAdmin(eq(123L), any(String[].class))).thenReturn(false);
        when(permissionApi.hasAnyPermissions(eq(123L), any(String[].class))).thenReturn(false);
        when(permissionApi.hasAnyPermissions(123L, "dcc:project-code-assignment:execute")).thenReturn(true);

        DccProjectCodePageReqVO reqVO = new DccProjectCodePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        PageResult<DccProjectCodeDO> pageResult = projectCodeService.getProjectCodePage(123L, reqVO);

        assertEquals(1L, pageResult.getTotal());
        assertEquals(List.of(ownProjectCode.getId()), pageResult.getList().stream().map(DccProjectCodeDO::getId).toList());
    }

    @Test
    void pageForDocControlUserShouldKeepFullProjectCodeScope() {
        DccProjectCodeDO firstProjectCode = insertProjectCode("1", "项目A", "CODE-A");
        DccProjectCodeDO secondProjectCode = insertProjectCode("2", "项目B", "CODE-B");
        insertAssignment(firstProjectCode.getId(), 123L, STATUS_ACTIVE, LocalDateTime.now().plusDays(1));
        when(permissionApi.hasAnyRolesOrSuperAdmin(eq(99L), any(String[].class))).thenReturn(true);

        DccProjectCodePageReqVO reqVO = new DccProjectCodePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        PageResult<DccProjectCodeDO> pageResult = projectCodeService.getProjectCodePage(99L, reqVO);

        assertEquals(2L, pageResult.getTotal());
        assertEquals(List.of(firstProjectCode.getId(), secondProjectCode.getId()),
                pageResult.getList().stream().map(DccProjectCodeDO::getId).toList());
    }

    @Test
    void aiCategoryCandidatesShouldIncludeBlankLegacyAndPreviousUnclassifiedAssociatedFiles() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_005", "项目策划书", DccFileCategoryLifecycleStageEnum.PLAN);
        DccControlledFileDO blankStage = insertControlledFile(projectCode.getId(), "A-001",
                "项目策划书-V1.pdf", null, null);
        DccControlledFileDO legacyStage = insertControlledFile(projectCode.getId(), "A-002",
                "风险管理计划.docx", "旧阶段", null);
        DccControlledFileDO missingFileType = insertControlledFile(projectCode.getId(), "A-003",
                "项目策划书-缺类型.pdf", taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), null);
        DccControlledFileDO invalidFileType = insertControlledFile(projectCode.getId(), "A-004",
                "项目策划书-旧类型.pdf", taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), "已停用类型");
        insertControlledFile(projectCode.getId(), "A-003",
                "已分类.pdf", taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), "项目策划书");
        DccControlledFileDO previousUnclassified = insertControlledFile(projectCode.getId(), "A-004",
                "未知文件.pdf", "未分类", "未分类文件类型");
        insertControlledFile(projectCode.getId(), "A-005",
                "不可见项目策划书.pdf", null, null);
        mockVisibleAssociatedFiles(userId, blankStage, legacyStage, missingFileType, invalidFileType,
                previousUnclassified);

        List<DccProjectCodeAssociatedFileAiCategoryRespVO> candidates =
                projectCodeService.getAssociatedFileAiCategoryCandidates(userId, projectCode.getId());

        assertEquals(List.of(blankStage.getId(), legacyStage.getId(), missingFileType.getId(),
                        invalidFileType.getId(), previousUnclassified.getId()),
                candidates.stream().map(DccProjectCodeAssociatedFileAiCategoryRespVO::getFileId).toList());
    }

    @Test
    void classifyAssociatedFileByNameShouldLeaveAmbiguousTopScoreUnchanged() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_901", "测试方案甲", DccFileCategoryLifecycleStageEnum.PLAN);
        insertCategory("DCC_FVM_DHF_902", "测试报告乙", DccFileCategoryLifecycleStageEnum.OUTPUT);
        DccControlledFileDO file = insertControlledFile(projectCode.getId(), "A-901",
                "测试方案甲-测试报告乙.pdf", null, null);
        mockVisibleAssociatedFiles(userId, file);

        DccProjectCodeAssociatedFileAiCategoryRespVO result =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), file.getId());

        assertEquals("AMBIGUOUS", result.getClassificationStatus());
        assertTrue(Boolean.FALSE.equals(result.getMatched()));
        assertTrue(result.getClassificationMessage().contains("测试方案甲"));
        assertTrue(result.getClassificationMessage().contains("测试报告乙"));
        DccControlledFileDO unchanged = controlledFileMapper.selectById(file.getId());
        assertNull(unchanged.getFileTypeLevel1());
        assertNull(unchanged.getFileTypeLevel2());
        assertNull(unchanged.getFileTypeLevel3());
    }

    @Test
    void classifyAssociatedFileByNameShouldMapMatchingFileNameToLifecycleStage() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_005", "项目策划书", DccFileCategoryLifecycleStageEnum.PLAN);
        DccControlledFileDO file = insertControlledFile(projectCode.getId(), "A-001",
                "IRPTCA-项目策划书-V1.pdf", null, null);
        mockVisibleAssociatedFiles(userId, file);

        DccProjectCodeAssociatedFileAiCategoryRespVO result =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), file.getId());

        assertTrue(Boolean.TRUE.equals(result.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), result.getTargetStage());
        assertEquals("项目策划书", result.getTargetFileType());
        DccControlledFileDO updated = controlledFileMapper.selectById(file.getId());
        assertEquals("技术文档", updated.getFileTypeLevel1());
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), updated.getFileTypeLevel2());
        assertEquals("项目策划书", updated.getFileTypeLevel3());
    }

    @Test
    void classifyAssociatedFileByNameShouldPersistConfiguredTaxonomyPathWhenCategoryBound() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_005", "项目策划书", DccFileCategoryLifecycleStageEnum.PLAN, 8801L);
        DccControlledFileDO file = insertControlledFile(projectCode.getId(), "A-001",
                "IRPTCA-项目策划书-V1.pdf", null, null);
        mockVisibleAssociatedFiles(userId, file);
        when(fileTypeTaxonomyAdminService.resolveActivePath(8801L)).thenReturn(new DccFileTypeTaxonomyPath(
                8801L, "技术文档", taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), "项目策划书", "草案", "归档件"));

        DccProjectCodeAssociatedFileAiCategoryRespVO result =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), file.getId());

        assertTrue(Boolean.TRUE.equals(result.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), result.getTargetStage());
        assertEquals("项目策划书", result.getTargetFileType());
        DccControlledFileDO updated = controlledFileMapper.selectById(file.getId());
        assertEquals(8801L, updated.getFileTypeTaxonomyId());
        assertEquals("技术文档", updated.getFileTypeLevel1());
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), updated.getFileTypeLevel2());
        assertEquals("项目策划书", updated.getFileTypeLevel3());
        assertEquals("草案", updated.getFileTypeLevel4());
        assertEquals("归档件", updated.getFileTypeLevel5());
    }

    @Test
    void classifyAssociatedFileByNameShouldUseFileNumberWhenFileNameLacksCategoryText() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_016", "设计和开发输入报告", DccFileCategoryLifecycleStageEnum.INPUT);
        DccControlledFileDO file = insertControlledFile(projectCode.getId(), "血管内球囊导管设计和开发输入报告",
                "血管内球囊导管设计和开发输入报告.pdf", null, null);
        file.setFileName("血管内球囊导管.pdf");
        controlledFileMapper.updateById(file);
        mockVisibleAssociatedFiles(userId, file);

        DccProjectCodeAssociatedFileAiCategoryRespVO result =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), file.getId());

        assertTrue(Boolean.TRUE.equals(result.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.INPUT), result.getTargetStage());
        assertEquals("设计和开发输入报告", result.getTargetFileType());
        DccControlledFileDO updated = controlledFileMapper.selectById(file.getId());
        assertEquals("技术文档", updated.getFileTypeLevel1());
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.INPUT), updated.getFileTypeLevel2());
        assertEquals("设计和开发输入报告", updated.getFileTypeLevel3());
    }

    @Test
    void classifyAssociatedFileByNameShouldMatchTestServerSynonymFileNames() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_003", "临床注册路径分析", DccFileCategoryLifecycleStageEnum.INPUT);
        insertCategory("DCC_FVM_DHF_004", "项目立项书", DccFileCategoryLifecycleStageEnum.PLAN);
        insertCategory("DCC_FVM_DHF_009", "法规、标准清单", DccFileCategoryLifecycleStageEnum.INPUT);
        insertCategory("DCC_FVM_DHF_007", "同类产品测试方案、报告", DccFileCategoryLifecycleStageEnum.INPUT);
        insertCategory("DCC_FVM_DHF_017", "运输包装验证方案/报告", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_020", "设计转移方案/报告", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_021", "生产用设备清单", DccFileCategoryLifecycleStageEnum.OUTPUT);
        insertCategory("DCC_FVM_DHF_026", "货架寿命验证方案/报告", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_027", "性能评价方案和报告", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_028", "产品过程确认主计划", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_029", "过程运行确认（OQ）方案", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_030", "过程运行确认（OQ）报告", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_031", "过程性能确认（PQ）方案", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_032", "过程性能确认（PQ）报告", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_033", "过程确认总结报告", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_034", "灭菌确认方案/报告", DccFileCategoryLifecycleStageEnum.VALIDATION);
        insertCategory("DCC_FVM_DHF_022", "BOM表", DccFileCategoryLifecycleStageEnum.OUTPUT);
        insertCategory("DCC_FVM_DHF_023", "产品说明书", DccFileCategoryLifecycleStageEnum.OUTPUT);
        insertCategory("DCC_FVM_DHF_024", "物资采购清单", DccFileCategoryLifecycleStageEnum.OUTPUT);
        insertCategory("DCC_FVM_DHF_025", "风险管理报告", DccFileCategoryLifecycleStageEnum.OUTPUT);
        insertCategory("DCC_FVM_DMR_013", "工序卡/作业指导书", DccFileCategoryLifecycleStageEnum.OUTPUT);
        insertCategory("DCC_FVM_DMR_016", "过程检验规程", DccFileCategoryLifecycleStageEnum.OUTPUT);
        insertCategory("DCC_FVM_DMR_017", "成品图纸", DccFileCategoryLifecycleStageEnum.OUTPUT);
        DccControlledFileDO registrationPath = insertControlledFile(projectCode.getId(), "血管内球囊导管注册临床路径",
                "血管内球囊导管注册临床路径.pdf", null, null);
        DccControlledFileDO initiation = insertControlledFile(projectCode.getId(), "血管内球囊导管项目立项申请书",
                "血管内球囊导管项目立项申请书.pdf", null, null);
        DccControlledFileDO regulationList = insertControlledFile(projectCode.getId(), "血管内球囊导管法律法规标准清单",
                "血管内球囊导管法律法规标准清单.pdf", null, null);
        DccControlledFileDO designInput = insertControlledFile(projectCode.getId(), "血管内球囊导管设计和开发输入报告",
                "血管内球囊导管设计和开发输入报告.pdf", null, null);
        DccControlledFileDO packageTransportReport = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管包装运输报告修改版",
                "PTCA球囊扩张导管包装运输报告修改版.docx", null, null);
        DccControlledFileDO transferReport = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管设计开发转移报告",
                "PTCA球囊扩张导管设计开发转移报告.pdf", null, null);
        DccControlledFileDO productionEquipment = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管生产设备清单",
                "PTCA球囊扩张导管生产设备清单.pdf", null, null);
        DccControlledFileDO bom = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管产品BOM",
                "PTCA球囊扩张导管产品BOM DMR-PTCABC-005 A2.pdf", null, null);
        DccControlledFileDO instruction = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管使用说明书",
                "PTCA球囊扩张导管使用说明书.pdf", null, null);
        DccControlledFileDO purchaseList = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管采购物资清单",
                "PTCA球囊扩张导管采购物资清单P-PTCABC A3.pdf", null, null);
        DccControlledFileDO riskReport = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管产品风险管理报告",
                "PTCA球囊扩张导管产品风险管理报告.pdf", null, null);
        DccControlledFileDO shelfLifeReport = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管货架有效期确认报告",
                "PTCA球囊扩张导管货架有效期确认报告.pdf", null, null);
        DccControlledFileDO performanceReport = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管性能测试报告实时1年",
                "PTCA球囊扩张导管性能测试报告实时1年.pdf", null, null);
        DccControlledFileDO processMasterPlan = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管过程确认主计划",
                "PTCA球囊扩张导管过程确认主计划.pdf", null, null);
        DccControlledFileDO oqProtocol = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管Rx口焊接工艺OQPQ验证方案",
                "PTCA球囊扩张导管Rx口焊接工艺OQPQ验证方案.pdf", null, null);
        DccControlledFileDO oqReport = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管Rx口焊接工艺OQ验证报告",
                "PTCA球囊扩张导管Rx口焊接工艺OQ验证报告.pdf", null, null);
        DccControlledFileDO pqProtocol = insertControlledFile(projectCode.getId(), "球囊导管-外管清洗工艺性能确认方案PQ",
                "球囊导管-外管清洗工艺性能确认方案PQ.pdf", null, null);
        DccControlledFileDO pqReport = insertControlledFile(projectCode.getId(), "球囊导管-外管清洗工艺性能确认报告PQ",
                "球囊导管-外管清洗工艺性能确认报告PQ.pdf", null, null);
        DccControlledFileDO processSummary = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管过程确认总结报告",
                "PTCA球囊扩张导管过程确认总结报告.pdf", null, null);
        DccControlledFileDO sterilization = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管灭菌验证方案",
                "PTCA球囊扩张导管灭菌验证方案.pdf", null, null);
        DccControlledFileDO workInstruction = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管作业指导书",
                "PTCA球囊扩张导管作业指导书.pdf", null, null);
        DccControlledFileDO processInspection = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管组装过程检验规范",
                "PTCA球囊扩张导管组装过程检验规范.pdf", null, null);
        DccControlledFileDO finishedDrawing = insertControlledFile(projectCode.getId(), "PTCA球囊扩张导管 255ACSXXXXXX-X-CP-105",
                "PTCA球囊扩张导管 255ACSXXXXXX-X-CP-105.pdf", null, null);
        mockVisibleAssociatedFiles(userId, registrationPath, initiation, regulationList, designInput, packageTransportReport,
                transferReport, productionEquipment, bom, instruction, purchaseList, riskReport, shelfLifeReport,
                performanceReport, processMasterPlan, oqProtocol, oqReport, pqProtocol, pqReport, processSummary,
                sterilization, workInstruction, processInspection, finishedDrawing);

        DccProjectCodeAssociatedFileAiCategoryRespVO registrationResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), registrationPath.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO initiationResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), initiation.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO regulationResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), regulationList.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO designInputResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), designInput.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO packageResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), packageTransportReport.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO transferResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), transferReport.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO productionEquipmentResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), productionEquipment.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO bomResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), bom.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO instructionResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), instruction.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO purchaseListResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), purchaseList.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO riskReportResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), riskReport.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO shelfLifeReportResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), shelfLifeReport.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO performanceReportResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), performanceReport.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO processMasterPlanResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), processMasterPlan.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO oqProtocolResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), oqProtocol.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO oqReportResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), oqReport.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO pqProtocolResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), pqProtocol.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO pqReportResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), pqReport.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO processSummaryResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), processSummary.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO sterilizationResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), sterilization.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO workInstructionResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), workInstruction.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO processInspectionResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), processInspection.getId());
        DccProjectCodeAssociatedFileAiCategoryRespVO finishedDrawingResult =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), finishedDrawing.getId());

        assertTrue(Boolean.TRUE.equals(registrationResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.INPUT), registrationResult.getTargetStage());
        assertEquals("临床注册路径分析", registrationResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(initiationResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), initiationResult.getTargetStage());
        assertEquals("项目立项书", initiationResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(regulationResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.INPUT), regulationResult.getTargetStage());
        assertEquals("法规、标准清单", regulationResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(designInputResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.INPUT), designInputResult.getTargetStage());
        assertEquals("同类产品测试方案、报告", designInputResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(packageResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), packageResult.getTargetStage());
        assertEquals("运输包装验证方案/报告", packageResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(transferResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), transferResult.getTargetStage());
        assertEquals("设计转移方案/报告", transferResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(productionEquipmentResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.OUTPUT), productionEquipmentResult.getTargetStage());
        assertEquals("生产用设备清单", productionEquipmentResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(bomResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.OUTPUT), bomResult.getTargetStage());
        assertEquals("BOM表", bomResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(instructionResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.OUTPUT), instructionResult.getTargetStage());
        assertEquals("产品说明书", instructionResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(purchaseListResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.OUTPUT), purchaseListResult.getTargetStage());
        assertEquals("物资采购清单", purchaseListResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(riskReportResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.OUTPUT), riskReportResult.getTargetStage());
        assertEquals("风险管理报告", riskReportResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(shelfLifeReportResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), shelfLifeReportResult.getTargetStage());
        assertEquals("货架寿命验证方案/报告", shelfLifeReportResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(performanceReportResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), performanceReportResult.getTargetStage());
        assertEquals("性能评价方案和报告", performanceReportResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(processMasterPlanResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), processMasterPlanResult.getTargetStage());
        assertEquals("产品过程确认主计划", processMasterPlanResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(oqProtocolResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), oqProtocolResult.getTargetStage());
        assertEquals("过程运行确认（OQ）方案", oqProtocolResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(oqReportResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), oqReportResult.getTargetStage());
        assertEquals("过程运行确认（OQ）报告", oqReportResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(pqProtocolResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), pqProtocolResult.getTargetStage());
        assertEquals("过程性能确认（PQ）方案", pqProtocolResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(pqReportResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), pqReportResult.getTargetStage());
        assertEquals("过程性能确认（PQ）报告", pqReportResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(processSummaryResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), processSummaryResult.getTargetStage());
        assertEquals("过程确认总结报告", processSummaryResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(sterilizationResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.VALIDATION), sterilizationResult.getTargetStage());
        assertEquals("灭菌确认方案/报告", sterilizationResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(workInstructionResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.OUTPUT), workInstructionResult.getTargetStage());
        assertEquals("工序卡/作业指导书", workInstructionResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(processInspectionResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.OUTPUT), processInspectionResult.getTargetStage());
        assertEquals("过程检验规程", processInspectionResult.getTargetFileType());
        assertTrue(Boolean.TRUE.equals(finishedDrawingResult.getMatched()));
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.OUTPUT), finishedDrawingResult.getTargetStage());
        assertEquals("成品图纸", finishedDrawingResult.getTargetFileType());
    }

    @Test
    void classifyAssociatedFileByNameShouldPersistUnclassifiedAndKeepCandidateForFutureRules() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_005", "项目策划书", DccFileCategoryLifecycleStageEnum.PLAN);
        DccControlledFileDO file = insertControlledFile(projectCode.getId(), "A-001",
                "完全未知文件.pdf", null, null);
        mockVisibleAssociatedFiles(userId, file);

        DccProjectCodeAssociatedFileAiCategoryRespVO result =
                projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), file.getId());

        assertTrue(Boolean.FALSE.equals(result.getMatched()));
        assertEquals("未分类", result.getTargetStage());
        assertEquals("未分类文件类型", result.getTargetFileType());
        DccControlledFileDO updated = controlledFileMapper.selectById(file.getId());
        assertEquals("技术文档", updated.getFileTypeLevel1());
        assertEquals("未分类", updated.getFileTypeLevel2());
        assertEquals("未分类文件类型", updated.getFileTypeLevel3());
        assertEquals(List.of(file.getId()), projectCodeService.getAssociatedFileAiCategoryCandidates(userId, projectCode.getId())
                .stream().map(DccProjectCodeAssociatedFileAiCategoryRespVO::getFileId).toList());
    }

    @Test
    void classifyAssociatedFileByNameShouldPreserveExistingNonBlankFileTypeLevel1() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_005", "项目策划书", DccFileCategoryLifecycleStageEnum.PLAN);
        DccControlledFileDO file = insertControlledFile(projectCode.getId(), "A-001",
                "IRPTCA-项目策划书-V1.pdf", null, null);
        file.setFileTypeLevel1("QMS文档");
        file.setFileTypeLevel4("现有四级分类");
        file.setFileTypeLevel5("现有五级分类");
        controlledFileMapper.updateById(file);
        mockVisibleAssociatedFiles(userId, file);

        projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), file.getId());

        DccControlledFileDO updated = controlledFileMapper.selectById(file.getId());
        assertEquals("QMS文档", updated.getFileTypeLevel1());
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), updated.getFileTypeLevel2());
        assertEquals("项目策划书", updated.getFileTypeLevel3());
        assertEquals("现有四级分类", updated.getFileTypeLevel4());
        assertEquals("现有五级分类", updated.getFileTypeLevel5());
    }

    @Test
    void classifyAssociatedFileByNameShouldRejectInvisibleAssociatedFile() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_005", "项目策划书", DccFileCategoryLifecycleStageEnum.PLAN);
        DccControlledFileDO file = insertControlledFile(projectCode.getId(), "A-001",
                "IRPTCA-项目策划书-V1.pdf", null, null);
        mockVisibleAssociatedFiles(userId);

        assertServiceException(
                () -> projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), file.getId()),
                PROJECT_CODE_ASSOCIATED_FILE_NOT_EXISTS);

        DccControlledFileDO updated = controlledFileMapper.selectById(file.getId());
        assertNull(updated.getFileTypeLevel2());
        assertNull(updated.getFileTypeLevel3());
    }

    @Test
    void classifyAssociatedFileByNameShouldRejectAlreadyCategorizedVisibleFile() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_005", "项目策划书", DccFileCategoryLifecycleStageEnum.PLAN);
        DccControlledFileDO file = insertControlledFile(projectCode.getId(), "A-001",
                "风险管理计划.docx", taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), "项目策划书");
        file.setFileTypeLevel1("QMS文档");
        file.setFileTypeLevel4("现有四级分类");
        file.setFileTypeLevel5("现有五级分类");
        controlledFileMapper.updateById(file);
        mockVisibleAssociatedFiles(userId, file);

        assertServiceException(
                () -> projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), file.getId()),
                PROJECT_CODE_ASSOCIATED_FILE_ALREADY_CATEGORIZED);

        DccControlledFileDO updated = controlledFileMapper.selectById(file.getId());
        assertEquals("QMS文档", updated.getFileTypeLevel1());
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), updated.getFileTypeLevel2());
        assertEquals("项目策划书", updated.getFileTypeLevel3());
        assertEquals("现有四级分类", updated.getFileTypeLevel4());
        assertEquals("现有五级分类", updated.getFileTypeLevel5());
    }

    @Test
    void classifyAssociatedFileByNameShouldExposeConcurrentModificationErrorCode() {
        Long userId = 1L;
        DccProjectCodeDO projectCode = insertProjectCode("1", "项目A", "CODE-A");
        insertCategory("DCC_FVM_DHF_005", "项目策划书", DccFileCategoryLifecycleStageEnum.PLAN);
        DccControlledFileDO file = insertControlledFile(projectCode.getId(), "A-001",
                "IRPTCA-项目策划书-V1.pdf", null, null);
        mockVisibleAssociatedFiles(userId, file);
        doReturn(0).when(controlledFileMapper).update(isNull(), any(LambdaUpdateWrapper.class));

        assertServiceException(
                () -> projectCodeService.classifyAssociatedFileByName(userId, projectCode.getId(), file.getId()),
                PROJECT_CODE_ASSOCIATED_FILE_CONCURRENT_MODIFICATION);
    }

    @Test
    void previewAndConfirmShouldPreserveDisplayValuesAllowDuplicateCodeAndDisableMissingRows() throws Exception {
        DccProjectCodeDO legacy = DccProjectCodeDO.builder()
                .docControlNo("旧文控")
                .projectName("旧项目")
                .projectCode("OLD")
                .category("旧类别")
                .commissionedProduction("否")
                .projectLeader("旧负责人")
                .projectEngineer("旧工程师")
                .storageLocation("旧柜")
                .priority("旧")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCodeMapper.insert(legacy);

        MockMultipartFile file = workbook(
                row("√", "项目A", "DUP", "类别1", "/", "负责人A", "工程师A", 9, "新1"),
                row("文控B", "项目B", "DUP", "类别2", "√", "负责人B", "工程师B", "新2", "中"),
                row("文控C", "项目C", "", "类别3", "", "负责人C", "工程师C", 3, "√")
        );

        DccProjectCodeImportPreviewRespVO preview = projectCodeService.previewImport(file);

        assertEquals(DccProjectCodeImportStatusConstants.PREVIEWED, preview.getStatus());
        assertEquals(4, preview.getTotalCount());
        assertEquals(3, preview.getCreateCount());
        assertEquals(1, preview.getDisableCount());
        assertEquals(0, preview.getFailureCount());
        assertEquals(List.of(
                DccProjectCodeImportActionConstants.CREATE,
                DccProjectCodeImportActionConstants.CREATE,
                DccProjectCodeImportActionConstants.CREATE,
                DccProjectCodeImportActionConstants.DISABLE),
                preview.getRows().stream().map(DccProjectCodeImportRowRespVO::getImportAction).toList());

        projectCodeService.confirmImport(preview.getBatchId());

        DccProjectCodeDO projectA = projectCodeMapper.selectByProjectNameAndProjectCode("项目A", "DUP");
        assertNotNull(projectA);
        assertEquals("√", projectA.getDocControlNo());
        assertEquals("/", projectA.getCommissionedProduction());
        assertEquals("9", projectA.getStorageLocation());
        assertEquals(DccProjectCodeStatusConstants.ENABLE, projectA.getStatus());

        DccProjectCodeDO projectB = projectCodeMapper.selectByProjectNameAndProjectCode("项目B", "DUP");
        assertNotNull(projectB, "项目代码单列重复必须允许");

        DccProjectCodeDO blankCode = projectCodeMapper.selectByProjectNameAndProjectCode("项目C", "");
        assertNotNull(blankCode, "空项目代码应以空字符串参与匹配");
        assertEquals("3", blankCode.getStorageLocation());
        assertEquals("√", blankCode.getPriority());

        DccProjectCodeDO disabledLegacy = projectCodeMapper.selectById(legacy.getId());
        assertEquals(DccProjectCodeStatusConstants.DISABLE, disabledLegacy.getStatus());
    }

    @Test
    void previewAndConfirmShouldUpdateExistingByProjectNameAndProjectCode() throws Exception {
        DccProjectCodeDO existing = DccProjectCodeDO.builder()
                .docControlNo("旧文控")
                .projectName("项目A")
                .projectCode("CODE-A")
                .category("旧类别")
                .commissionedProduction("否")
                .projectLeader("旧负责人")
                .projectEngineer("旧工程师")
                .storageLocation("旧柜")
                .priority("低")
                .status(DccProjectCodeStatusConstants.DISABLE)
                .build();
        projectCodeMapper.insert(existing);

        MockMultipartFile file = workbook(
                row("新文控", "项目A", "CODE-A", "新类别", "√", "新负责人", "新工程师", "新N", "高")
        );

        DccProjectCodeImportPreviewRespVO preview = projectCodeService.previewImport(file);

        assertEquals(DccProjectCodeImportStatusConstants.PREVIEWED, preview.getStatus());
        assertEquals(1, preview.getUpdateCount());
        assertEquals(0, preview.getCreateCount());
        assertEquals(0, preview.getDisableCount());

        projectCodeService.confirmImport(preview.getBatchId());

        DccProjectCodeDO updated = projectCodeMapper.selectById(existing.getId());
        assertEquals("新文控", updated.getDocControlNo());
        assertEquals("新类别", updated.getCategory());
        assertEquals("√", updated.getCommissionedProduction());
        assertEquals("新N", updated.getStorageLocation());
        assertEquals(DccProjectCodeStatusConstants.ENABLE, updated.getStatus());
    }

    @Test
    void previewShouldRejectHeaderMismatchEmptyFileAndDuplicateMatchKey() throws Exception {
        MockMultipartFile wrongHeader = workbook(List.of("文控", "项目名称", "错误列"), row("DCC", "项目A", "A"));
        IllegalStateException headerException = assertThrows(IllegalStateException.class,
                () -> projectCodeService.previewImport(wrongHeader));
        assertTrue(headerException.getMessage().contains("DCC_PROJECT_CODE_IMPORT_HEADER_INVALID"));

        MockMultipartFile emptyFile = workbook();
        IllegalStateException emptyException = assertThrows(IllegalStateException.class,
                () -> projectCodeService.previewImport(emptyFile));
        assertTrue(emptyException.getMessage().contains("DCC_PROJECT_CODE_IMPORT_EMPTY"));

        MockMultipartFile duplicateKeyFile = workbook(
                row("文控A", "项目A", "CODE-A", "类别", "", "", "", "", ""),
                row("文控B", "项目A", "CODE-A", "类别", "", "", "", "", "")
        );
        DccProjectCodeImportPreviewRespVO preview = projectCodeService.previewImport(duplicateKeyFile);

        assertEquals(DccProjectCodeImportStatusConstants.FAILED, preview.getStatus());
        assertEquals(1, preview.getFailureCount());
        assertEquals(DccProjectCodeImportActionConstants.INVALID, preview.getRows().get(1).getImportAction());
        assertTrue(preview.getRows().get(1).getFailureReason().contains("项目名称+项目代码"));

        IllegalStateException confirmException = assertThrows(IllegalStateException.class,
                () -> projectCodeService.confirmImport(preview.getBatchId()));
        assertTrue(confirmException.getMessage().contains("DCC_PROJECT_CODE_IMPORT_BATCH_NOT_CONFIRMABLE"));
    }

    @Test
    void pageAndExportShouldOrderByNumericDocControlNoAscendingBeforeNonNumeric() {
        projectCodeMapper.insert(DccProjectCodeDO.builder()
                .docControlNo("30")
                .projectName("Project-30")
                .projectCode("CODE-30")
                .category("Category")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        projectCodeMapper.insert(DccProjectCodeDO.builder()
                .docControlNo("A-1")
                .projectName("Project-A")
                .projectCode("CODE-A")
                .category("Category")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        projectCodeMapper.insert(DccProjectCodeDO.builder()
                .docControlNo("2")
                .projectName("Project-2")
                .projectCode("CODE-2")
                .category("Category")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        projectCodeMapper.insert(DccProjectCodeDO.builder()
                .docControlNo("10")
                .projectName("Project-10")
                .projectCode("CODE-10")
                .category("Category")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());

        DccProjectCodePageReqVO firstPageReqVO = new DccProjectCodePageReqVO();
        firstPageReqVO.setPageNo(1);
        firstPageReqVO.setPageSize(2);
        PageResult<DccProjectCodeDO> firstPage = projectCodeService.getProjectCodePage(firstPageReqVO);

        assertEquals(List.of("2", "10"), firstPage.getList().stream()
                .map(DccProjectCodeDO::getDocControlNo)
                .toList());
        assertEquals(4L, firstPage.getTotal());

        DccProjectCodePageReqVO secondPageReqVO = new DccProjectCodePageReqVO();
        secondPageReqVO.setPageNo(2);
        secondPageReqVO.setPageSize(2);
        PageResult<DccProjectCodeDO> secondPage = projectCodeService.getProjectCodePage(secondPageReqVO);

        assertEquals(List.of("30", "A-1"), secondPage.getList().stream()
                .map(DccProjectCodeDO::getDocControlNo)
                .toList());

        DccProjectCodePageReqVO exportReqVO = new DccProjectCodePageReqVO();
        assertEquals(List.of("2", "10", "30", "A-1"), projectCodeService.getExportList(exportReqVO).stream()
                .map(DccProjectCodeExportExcelVO::getDocControlNo)
                .toList());
    }

    @Test
    void exportShouldUseRequiredHeaderOrderAndOriginalValues() throws Exception {
        projectCodeMapper.insert(DccProjectCodeDO.builder()
                .docControlNo("/")
                .projectName("项目A")
                .projectCode("")
                .category("类别")
                .commissionedProduction("√")
                .projectLeader("负责人")
                .projectEngineer("工程师")
                .storageLocation("新N")
                .priority("高")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());

        DccProjectCodePageReqVO reqVO = new DccProjectCodePageReqVO();
        List<DccProjectCodeExportExcelVO> exports = projectCodeService.getExportList(reqVO);

        assertEquals(1, exports.size());
        assertEquals("/", exports.get(0).getDocControlNo());
        assertEquals("", exports.get(0).getProjectCode());
        assertEquals("√", exports.get(0).getCommissionedProduction());
        assertEquals("新N", exports.get(0).getStorageLocation());
        assertEquals(EXPECTED_HEADERS, readExcelHeaders(DccProjectCodeExportExcelVO.class));
    }

    @Test
    void importRowsShouldPersistPreviewEvidence() throws Exception {
        DccProjectCodeImportPreviewRespVO preview = projectCodeService.previewImport(workbook(
                row("文控A", "项目A", "CODE-A", "类别", "", "", "", "", "")
        ));

        assertNotNull(importBatchMapper.selectById(preview.getBatchId()));
        assertEquals(1, importRowMapper.selectListByBatchId(preview.getBatchId()).size());
    }

    private DccProjectCodeDO insertProjectCode(String docControlNo, String projectName, String projectCode) {
        DccProjectCodeDO projectCodeDO = DccProjectCodeDO.builder()
                .docControlNo(docControlNo)
                .projectName(projectName)
                .projectCode(projectCode)
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCodeMapper.insert(projectCodeDO);
        return projectCodeDO;
    }

    private void insertAssignment(Long projectCodeId, Long assigneeUserId, String status, LocalDateTime expireTime) {
        assignmentMapper.insert(DccProjectCodeAssignmentDO.builder()
                .assignmentNo("DCC-PC-A-" + projectCodeId + "-" + assigneeUserId + "-" + status)
                .projectCodeId(projectCodeId)
                .scopeMode("PROJECT_CODE_CURRENT_FILES")
                .assigneeUserId(assigneeUserId)
                .assignedBy(99L)
                .assignedTime(LocalDateTime.now())
                .expireTime(expireTime)
                .status(status)
                .fileCount(1)
                .changedFileCount(0)
                .changedFieldCount(0)
                .build());
    }

    private DccControlledFileDO insertControlledFile(Long dccProjectCodeId, String fileNumber) {
        return insertControlledFile(dccProjectCodeId, fileNumber, fileNumber + ".pdf", null, null);
    }

    private DccControlledFileDO insertControlledFile(Long dccProjectCodeId, String fileNumber, String fileName,
                                                    String fileTypeLevel2, String fileTypeLevel3) {
        DccControlledFileDO controlledFile = DccControlledFileDO.builder()
                .masterId(1L)
                .categoryId(1L)
                .directoryId(1L)
                .sourceFileId(1L)
                .originalFileId(1L)
                .fileName(fileName)
                .title(fileNumber)
                .fileNumber(fileNumber)
                .dccProjectCodeId(dccProjectCodeId)
                .fileTypeLevel2(fileTypeLevel2)
                .fileTypeLevel3(fileTypeLevel3)
                .needTraining(false)
                .processType("CONTROLLED_FILE")
                .versionNo("A1")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .submitterId(1L)
                .requesterId(1L)
                .build();
        controlledFileMapper.insert(controlledFile);
        return controlledFile;
    }

    private String taxonomyStageName(DccFileCategoryLifecycleStageEnum lifecycleStage) {
        return switch (lifecycleStage) {
            case PLAN -> "设计和开发策划阶段";
            case INPUT -> "设计和开发输入阶段";
            case OUTPUT -> "设计和开发输出阶段";
            case VERIFICATION -> "设计和开发验证";
            case VALIDATION -> "设计确认";
            case TRANSFER -> "设计和开发转换阶段";
        };
    }

    private void insertCategory(String code, String name, DccFileCategoryLifecycleStageEnum lifecycleStage) {
        insertCategory(code, name, lifecycleStage, null);
    }

    private void insertCategory(String code, String name, DccFileCategoryLifecycleStageEnum lifecycleStage,
                                Long fileTypeTaxonomyId) {
        Long resolvedTaxonomyId = fileTypeTaxonomyId != null ? fileTypeTaxonomyId : 10_000L + Math.abs((long) code.hashCode());
        when(fileTypeTaxonomyAdminService.resolveActivePath(resolvedTaxonomyId)).thenReturn(new DccFileTypeTaxonomyPath(
                resolvedTaxonomyId, "技术文档", taxonomyStageName(lifecycleStage), name, null, null));
        categoryMapper.insert(DccFileCategoryDO.builder()
                .code(code)
                .name(name)
                .active(Boolean.TRUE)
                .sort(1)
                .source("FVM")
                .lifecycleStage(lifecycleStage.getCode())
                .fileTypeTaxonomyId(resolvedTaxonomyId)
                .distributionRequired(Boolean.FALSE)
                .trainingRequired(Boolean.FALSE)
                .build());
    }

    private void mockVisibleAssociatedFiles(Long userId, DccControlledFileDO... files) {
        List<DccControlledFileRespVO> visibleFiles = Arrays.stream(files)
                .map(this::toControlledFileRespVO)
                .toList();
        when(controlledFileQueryService.getControlledFilePage(eq(userId), any()))
                .thenReturn(new PageResult<>(visibleFiles, (long) visibleFiles.size()));
    }

    private DccControlledFileRespVO toControlledFileRespVO(DccControlledFileDO file) {
        DccControlledFileRespVO respVO = new DccControlledFileRespVO();
        respVO.setId(file.getId());
        respVO.setFileName(file.getFileName());
        respVO.setTitle(file.getTitle());
        respVO.setFileTypeLevel1(file.getFileTypeLevel1());
        respVO.setFileTypeLevel2(file.getFileTypeLevel2());
        respVO.setFileTypeLevel3(file.getFileTypeLevel3());
        respVO.setFileTypeLevel4(file.getFileTypeLevel4());
        respVO.setFileTypeLevel5(file.getFileTypeLevel5());
        return respVO;
    }

    private static List<String> readExcelHeaders(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(DccProjectCodeServiceImplTest::excelHeader)
                .toList();
    }

    private static String excelHeader(Field field) {
        ExcelProperty annotation = field.getAnnotation(ExcelProperty.class);
        assertNotNull(annotation, field.getName() + " must declare @ExcelProperty");
        return annotation.value()[0];
    }

    private static MockMultipartFile workbook(Object[]... rows) throws Exception {
        return workbook(EXPECTED_HEADERS, rows);
    }

    private static MockMultipartFile workbook(List<String> headers, Object[]... rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row sheetRow = sheet.createRow(rowIndex + 1);
                Object[] values = rows[rowIndex];
                for (int colIndex = 0; colIndex < values.length; colIndex++) {
                    Cell cell = sheetRow.createCell(colIndex);
                    Object value = values[colIndex];
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(value == null ? "" : String.valueOf(value));
                    }
                }
            }
            workbook.write(outputStream);
            return new MockMultipartFile("file", "项目代码.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray());
        }
    }

    private static Object[] row(Object... values) {
        return values;
    }
}
