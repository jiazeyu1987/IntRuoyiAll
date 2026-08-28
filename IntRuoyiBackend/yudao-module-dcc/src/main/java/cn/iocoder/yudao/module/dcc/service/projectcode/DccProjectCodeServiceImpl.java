package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeAssociatedFileAiCategoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeExportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeImportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeImportRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeUpdateReqVO;
import cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationQuery;
import cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationStatus;
import cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationStatusApi;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryMatchRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeImportBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeImportRowDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMatchRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeImportBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeImportRowMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeImportActionConstants;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeImportStatusConstants;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileMetadataUpdateService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.mdm.enums.MdmProductStatusConstants;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSOCIATED_FILE_ALREADY_CATEGORIZED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSOCIATED_FILE_CONCURRENT_MODIFICATION;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSOCIATED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DELETE_REFERENCED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_STATUS_INVALID;

@Service
@Validated
public class DccProjectCodeServiceImpl implements DccProjectCodeService {

    private static final List<String> EXPECTED_HEADERS = List.of(
            "文控", "项目名称", "项目代码", "类别", "委托生产",
            "项目组负责人", "项目工程师", "存放位置", "优先级");
    private static final String TECHNICAL_FILE_TYPE_LEVEL1 = "技术文档";
    private static final String UNCLASSIFIED_STAGE = "未分类";
    private static final String UNCLASSIFIED_FILE_TYPE = "未分类文件类型";
    private static final String CATEGORY_MATCH_TYPE_CONTAINS = "CONTAINS";
    private static final String CATEGORY_MATCH_TYPE_EXACT = "EXACT";
    private static final String CATEGORY_MATCH_TYPE_PREFIX = "PREFIX";
    private static final String CATEGORY_MATCH_TYPE_SUFFIX = "SUFFIX";
    private static final String CATEGORY_MATCH_TYPE_EXTENSION = "EXTENSION";
    private static final String ASSIGNMENT_EXECUTE_PERMISSION = "dcc:project-code-assignment:execute";
    private static final String FULL_PROJECT_CODE_SCOPE_PERMISSION = "dcc:project-code:scope:all";
    private static final Map<String, List<String>> CATEGORY_MATCH_ALIASES = Map.ofEntries(
            Map.entry("临床注册路径分析", List.of("注册临床路径")),
            Map.entry("项目立项书", List.of("项目立项申请书", "项目建议书")),
            Map.entry("法规、标准清单", List.of("法律法规标准清单", "法规标准清单")),
            Map.entry("同类产品测试方案、报告", List.of("设计和开发输入报告", "设计开发输入报告",
                    "同类产品测试方案", "同类产品测试报告")),
            Map.entry("运输包装验证方案/报告", List.of("包装运输报告", "包装运输方案", "包装运输验证报告", "包装运输验证方案")),
            Map.entry("设计转移方案/报告", List.of("设计开发转移报告", "设计开发转移方案", "设计开发转移策划书")),
            Map.entry("生产用设备清单", List.of("生产设备清单")),
            Map.entry("BOM表", List.of("产品BOM")),
            Map.entry("产品说明书", List.of("使用说明书")),
            Map.entry("物资采购清单", List.of("采购物资清单")),
            Map.entry("产品技术要求", List.of("技术要求")),
            Map.entry("包装设计", List.of("包装图纸")),
            Map.entry("风险管理报告", List.of("产品风险管理报告", "产品风险分析报告FMEA")),
            Map.entry("货架寿命验证方案/报告", List.of("货架有效期确认报告", "货架有效期确认方案",
                    "ShelfLifeStudyProtocol", "ShelfLifeStudyReport")),
            Map.entry("性能评价方案和报告", List.of("性能测试报告", "性能测试方案",
                    "性能评价报告", "性能评价方案", "SimulatedUseStudyProtocol", "SimulatedUseStudyReport")),
            Map.entry("产品过程确认主计划", List.of("过程确认主计划")),
            Map.entry("过程运行确认（OQ）方案", List.of("OQPQ验证方案", "OQ验证方案", "运行确认方案OQ",
                    "运行确认方案", "OQ方案")),
            Map.entry("过程运行确认（OQ）报告", List.of("OQPQ验证报告", "OQ验证报告", "运行确认报告OQ",
                    "运行确认报告", "OQ报告")),
            Map.entry("过程性能确认（PQ）方案", List.of("PQ验证方案", "性能确认方案PQ",
                    "运行确认方案PQ", "PQ方案")),
            Map.entry("过程性能确认（PQ）报告", List.of("PQ验证报告", "性能确认报告PQ",
                    "性能确认报告", "运行确认报告PQ", "PQ报告")),
            Map.entry("过程确认总结报告", List.of("过程确认总结报告")),
            Map.entry("灭菌确认方案/报告", List.of("灭菌验证方案", "灭菌验证报告", "灭菌确认方案", "灭菌确认报告")),
            Map.entry("工序卡/作业指导书", List.of("作业指导书", "工艺文件", "工艺验证", "工艺确认",
                    "工艺运行确认", "工艺性能确认", "工艺OQ", "工艺PQ", "工艺OQPQ")),
            Map.entry("过程检验规程", List.of("过程检验规范")),
            Map.entry("成品图纸", List.of("产品图纸", "PTCA球囊扩张导管图纸", "血管内球囊导管(锥型)",
                    "255ACS", "255ACT")),
            Map.entry("零配件图纸", List.of("零配件图纸")),
            Map.entry("来料检验规程", List.of("来料检验规程")),
            Map.entry("成品检验规程", List.of("成品检验规程")),
            Map.entry("标准测试方法", List.of("标准测试方法")),
            Map.entry("项目策划书", List.of("项目策划书", "设计开发任务书")));

    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccProjectCodeAssignmentMapper assignmentMapper;
    @Resource
    private DccProjectCodeImportBatchMapper importBatchMapper;
    @Resource
    private DccProjectCodeImportRowMapper importRowMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileCategoryMatchRuleMapper categoryMatchRuleMapper;
    @Resource
    private DccControlledFileQueryService controlledFileQueryService;
    @Resource
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DccProjectCodeConfigurationStatusApi configurationStatusApi;
    @Resource
    private MdmProductApi productApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProjectCode(DccProjectCodeSaveReqVO reqVO) {
        DccProjectCodeSaveReqVO normalizedReqVO = normalizeSaveReqVO(reqVO);
        validateProjectCodeStatus(normalizedReqVO.getStatus());
        validateProjectCodeUnique(normalizedReqVO.getProjectName(), normalizedReqVO.getProjectCode(), null);
        DccProjectCodeDO projectCode = buildProjectCodeDO(normalizedReqVO, null);
        projectCodeMapper.insert(projectCode);
        return projectCode.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProjectCode(DccProjectCodeUpdateReqVO reqVO) {
        validateProjectCodeExists(reqVO.getId());
        DccProjectCodeUpdateReqVO normalizedReqVO = normalizeSaveReqVO(reqVO);
        validateProjectCodeStatus(normalizedReqVO.getStatus());
        validateProjectCodeUnique(normalizedReqVO.getProjectName(), normalizedReqVO.getProjectCode(),
                normalizedReqVO.getId());
        updateProjectCodeFields(normalizedReqVO.getId(), normalizedReqVO, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectCode(Long id) {
        validateProjectCodeExists(id);
        validateProjectCodeDeletable(id);
        projectCodeMapper.deleteById(id);
    }

    @Override
    public PageResult<DccProjectCodeDO> getProjectCodePage(Long userId, DccProjectCodePageReqVO reqVO) {
        List<Long> scopedProjectCodeIds = resolveAssignedProjectCodeScope(userId);
        if (scopedProjectCodeIds != null && scopedProjectCodeIds.isEmpty()) {
            return new PageResult<>(List.of(), 0L);
        }
        List<DccProjectCodeDO> sortedList = listProjectCodesInDisplayOrder(reqVO, scopedProjectCodeIds);
        return buildPageResult(sortedList, reqVO);
    }

    @Override
    public PageResult<DccProjectCodeDO> getProjectCodePage(DccProjectCodePageReqVO reqVO) {
        List<DccProjectCodeDO> sortedList = listProjectCodesInDisplayOrder(reqVO, null);
        return buildPageResult(sortedList, reqVO);
    }

    @Override
    public DccProjectCodeDO getProjectCode(Long id) {
        DccProjectCodeDO projectCode = projectCodeMapper.selectById(id);
        if (projectCode == null) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        return projectCode;
    }

    @Override
    public DccProjectCodeDO getProjectCode(Long userId, Long id) {
        DccProjectCodeDO projectCode = getProjectCode(id);
        List<Long> scopedProjectCodeIds = resolveAssignedProjectCodeScope(userId);
        if (scopedProjectCodeIds != null && !scopedProjectCodeIds.contains(id)) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        return projectCode;
    }

    @Override
    public PageResult<DccControlledFileRespVO> getControlledFilePage(Long userId, Long id,
                                                                      DccProjectCodeControlledFilePageReqVO reqVO) {
        getProjectCode(userId, id);
        DccControlledFilePageReqVO controlledFilePageReqVO = new DccControlledFilePageReqVO();
        controlledFilePageReqVO.setPageNo(reqVO.getPageNo());
        controlledFilePageReqVO.setPageSize(reqVO.getPageSize());
        controlledFilePageReqVO.setKeyword(reqVO.getKeyword());
        controlledFilePageReqVO.setStatus(reqVO.getStatus());
        controlledFilePageReqVO.setDccProjectCodeId(id);
        return controlledFileQueryService.getControlledFilePage(userId, controlledFilePageReqVO);
    }

    @Override
    public List<DccProjectCodeAssociatedFileAiCategoryRespVO> getAssociatedFileAiCategoryCandidates(Long userId, Long id) {
        getProjectCode(userId, id);
        List<Long> visibleFileIds = listVisibleAssociatedControlledFileIds(userId, id);
        if (visibleFileIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> displayOrder = new LinkedHashMap<>();
        for (int index = 0; index < visibleFileIds.size(); index++) {
            displayOrder.putIfAbsent(visibleFileIds.get(index), index);
        }
        List<DccFileCategoryDO> activeCategories = listActiveAiCategories();
        return controlledFileMapper.selectList(new LambdaQueryWrapperX<DccControlledFileDO>()
                        .in(DccControlledFileDO::getId, visibleFileIds)).stream()
                .filter(file -> requiresAiCategory(file, activeCategories))
                .sorted(Comparator.comparingInt(file -> displayOrder.getOrDefault(file.getId(), Integer.MAX_VALUE)))
                .map(file -> toAiCategoryResp(file, null))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccProjectCodeAssociatedFileAiCategoryRespVO classifyAssociatedFileByName(Long userId, Long id, Long fileId) {
        getProjectCode(userId, id);
        DccControlledFileDO controlledFile = controlledFileMapper.selectById(fileId);
        if (controlledFile == null || !listVisibleAssociatedControlledFileIds(userId, id).contains(fileId)) {
            throw exception(PROJECT_CODE_ASSOCIATED_FILE_NOT_EXISTS);
        }
        List<DccFileCategoryDO> activeCategories = listActiveAiCategories();
        if (!requiresAiCategory(controlledFile, activeCategories)) {
            throw exception(PROJECT_CODE_ASSOCIATED_FILE_ALREADY_CATEGORIZED);
        }

        Map<Long, List<DccFileCategoryMatchRuleDO>> categoryMatchRules =
                listActiveCategoryMatchRules(activeCategories);
        FileTypeCategoryTarget target = resolveAiCategoryTarget(controlledFile, activeCategories, categoryMatchRules);
        if (target.ambiguous()) {
            return toAiCategoryResp(controlledFile, target);
        }
        LambdaUpdateWrapper<DccControlledFileDO> updateWrapper = new LambdaUpdateWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getId, fileId);
        if (target.taxonomyId() != null) {
            String fileTypeLevel1 = StrUtil.trimToNull(controlledFile.getFileTypeLevel1()) == null
                    ? target.level1() : controlledFile.getFileTypeLevel1();
            String fileTypeLevel4 = StrUtil.trimToNull(controlledFile.getFileTypeLevel4()) == null
                    ? target.level4() : controlledFile.getFileTypeLevel4();
            String fileTypeLevel5 = StrUtil.trimToNull(controlledFile.getFileTypeLevel5()) == null
                    ? target.level5() : controlledFile.getFileTypeLevel5();
            updateWrapper
                    .set(DccControlledFileDO::getFileTypeTaxonomyId, target.taxonomyId())
                    .set(DccControlledFileDO::getFileTypeLevel1, fileTypeLevel1)
                    .set(DccControlledFileDO::getFileTypeLevel2, target.level2())
                    .set(DccControlledFileDO::getFileTypeLevel3, target.level3())
                    .set(DccControlledFileDO::getFileTypeLevel4, fileTypeLevel4)
                    .set(DccControlledFileDO::getFileTypeLevel5, fileTypeLevel5);
            applyOriginalValueCondition(updateWrapper, DccControlledFileDO::getFileTypeTaxonomyId,
                    controlledFile.getFileTypeTaxonomyId());
            applyOriginalValueCondition(updateWrapper, DccControlledFileDO::getFileTypeLevel1,
                    controlledFile.getFileTypeLevel1());
            applyOriginalValueCondition(updateWrapper, DccControlledFileDO::getFileTypeLevel4,
                    controlledFile.getFileTypeLevel4());
            applyOriginalValueCondition(updateWrapper, DccControlledFileDO::getFileTypeLevel5,
                    controlledFile.getFileTypeLevel5());
        } else {
            updateWrapper
                    .set(DccControlledFileDO::getFileTypeLevel2, target.stage())
                    .set(DccControlledFileDO::getFileTypeLevel3, target.fileType());
            if (StrUtil.trimToNull(controlledFile.getFileTypeLevel1()) == null) {
                updateWrapper.set(DccControlledFileDO::getFileTypeLevel1, TECHNICAL_FILE_TYPE_LEVEL1);
            }
        }
        applyOriginalValueCondition(updateWrapper, DccControlledFileDO::getFileTypeLevel2,
                controlledFile.getFileTypeLevel2());
        applyOriginalValueCondition(updateWrapper, DccControlledFileDO::getFileTypeLevel3,
                controlledFile.getFileTypeLevel3());
        if (controlledFileMapper.update(null, updateWrapper) == 0) {
            throw exception(PROJECT_CODE_ASSOCIATED_FILE_CONCURRENT_MODIFICATION);
        }
        return toAiCategoryResp(controlledFile, target);
    }

    @Override
    public List<DccProjectCodeExportExcelVO> getExportList(DccProjectCodePageReqVO reqVO) {
        reqVO.setPageNo(1);
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return getProjectCodePage(reqVO).getList().stream()
                .map(DccProjectCodeExportExcelVO::from)
                .toList();
    }

    private List<DccProjectCodeDO> listProjectCodesInDisplayOrder(DccProjectCodePageReqVO reqVO,
                                                                   List<Long> scopedProjectCodeIds) {
        DccProjectCodePageReqVO queryReqVO = new DccProjectCodePageReqVO();
        queryReqVO.setPageNo(1);
        queryReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        queryReqVO.setProductMasterId(reqVO.getProductMasterId());
        queryReqVO.setKeyword(reqVO.getKeyword());
        queryReqVO.setProjectName(reqVO.getProjectName());
        queryReqVO.setProjectCode(reqVO.getProjectCode());
        queryReqVO.setCategory(reqVO.getCategory());
        queryReqVO.setPriority(reqVO.getPriority());
        queryReqVO.setStatus(reqVO.getStatus());
        List<DccProjectCodeDO> records = new ArrayList<>(projectCodeMapper.selectPage(queryReqVO).getList());
        if (scopedProjectCodeIds != null) {
            Set<Long> scope = new LinkedHashSet<>(scopedProjectCodeIds);
            records.removeIf(record -> !scope.contains(record.getId()));
        }
        records = applyDccProductCodeFilter(records, reqVO);
        records = applyConfigurationFilters(records, reqVO);
        populateAssociatedFileCounts(records);
        records.sort(projectCodeDisplayOrder(reqVO));
        return records;
    }

    private List<DccProjectCodeDO> applyDccProductCodeFilter(List<DccProjectCodeDO> records,
                                                              DccProjectCodePageReqVO reqVO) {
        if (records.isEmpty() || !Boolean.TRUE.equals(reqVO.getRequireDccProductCode())) {
            return records;
        }
        Set<Long> validProductIds = productApi.listSimpleProducts(
                        MdmProductStatusConstants.ENABLE, true, null).stream()
                .map(MdmProductRespDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (validProductIds.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .filter(record -> validProductIds.contains(record.getProductMasterId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<DccProjectCodeDO> applyConfigurationFilters(List<DccProjectCodeDO> records,
                                                              DccProjectCodePageReqVO reqVO) {
        if (records.isEmpty()
                || (reqVO.getRouteConfigured() == null
                && reqVO.getMainBatchRecordConfigured() == null
                && reqVO.getQaRegulationConfigured() == null)) {
            return records;
        }
        Map<Long, DccProjectCodeConfigurationStatus> statusByProjectCodeId = configurationStatusApi.getStatus(
                records.stream()
                        .map(record -> new DccProjectCodeConfigurationQuery(record.getId(), record.getProjectName(),
                                reqVO.getRouteConfigured() != null,
                                reqVO.getMainBatchRecordConfigured() != null,
                                reqVO.getQaRegulationConfigured() != null))
                        .toList());
        return records.stream()
                .filter(record -> matchesConfigurationFilter(reqVO.getRouteConfigured(),
                        statusByProjectCodeId.get(record.getId()),
                        DccProjectCodeConfigurationStatus::routeConfigured))
                .filter(record -> matchesConfigurationFilter(reqVO.getMainBatchRecordConfigured(),
                        statusByProjectCodeId.get(record.getId()),
                        DccProjectCodeConfigurationStatus::mainBatchRecordConfigured))
                .filter(record -> matchesConfigurationFilter(reqVO.getQaRegulationConfigured(),
                        statusByProjectCodeId.get(record.getId()),
                        DccProjectCodeConfigurationStatus::qaRegulationConfigured))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean matchesConfigurationFilter(Boolean expected,
                                               DccProjectCodeConfigurationStatus status,
                                               Function<DccProjectCodeConfigurationStatus, Boolean> valueReader) {
        return expected == null || (status != null && Objects.equals(expected, valueReader.apply(status)));
    }

    private List<Long> resolveAssignedProjectCodeScope(Long userId) {
        if (userId == null || hasFullProjectCodeScope(userId) || !hasAssignmentExecuteScope(userId)) {
            return null;
        }
        return assignmentMapper.selectActiveProjectCodeIdsByAssigneeUserId(userId, LocalDateTime.now());
    }

    private boolean hasFullProjectCodeScope(Long userId) {
        return permissionApi.hasAnyPermissions(userId, FULL_PROJECT_CODE_SCOPE_PERMISSION);
    }

    private boolean hasAssignmentExecuteScope(Long userId) {
        return permissionApi.hasAnyPermissions(userId, ASSIGNMENT_EXECUTE_PERMISSION);
    }

    private PageResult<DccProjectCodeDO> buildPageResult(List<DccProjectCodeDO> sortedList, DccProjectCodePageReqVO reqVO) {
        long total = sortedList.size();
        if (PageParam.PAGE_SIZE_NONE.equals(reqVO.getPageSize())) {
            return new PageResult<>(sortedList, total);
        }
        int fromIndex = Math.min((reqVO.getPageNo() - 1) * reqVO.getPageSize(), sortedList.size());
        int toIndex = Math.min(fromIndex + reqVO.getPageSize(), sortedList.size());
        return new PageResult<>(new ArrayList<>(sortedList.subList(fromIndex, toIndex)), total);
    }

    private void populateAssociatedFileCounts(List<DccProjectCodeDO> records) {
        Set<Long> projectCodeIds = records.stream()
                .map(DccProjectCodeDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Long> fileCountByProjectCodeId = projectCodeIds.isEmpty()
                ? Map.of()
                : controlledFileMapper.selectAssociatedFileCountsByProjectCodeIds(projectCodeIds).stream()
                        .collect(Collectors.toMap(
                                DccControlledFileMapper.ProjectCodeFileCount::getProjectCodeId,
                                DccControlledFileMapper.ProjectCodeFileCount::getFileCount));
        records.forEach(record -> record.setAssociatedFileCount(
                fileCountByProjectCodeId.getOrDefault(record.getId(), 0L)));
    }

    private List<Long> listVisibleAssociatedControlledFileIds(Long userId, Long projectCodeId) {
        DccProjectCodeControlledFilePageReqVO reqVO = new DccProjectCodeControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        return getControlledFilePage(userId, projectCodeId, reqVO).getList().stream()
                .map(DccControlledFileRespVO::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<DccFileCategoryDO> listActiveAiCategories() {
        return categoryMapper.selectList().stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .filter(category -> category.getFileTypeTaxonomyId() != null)
                .toList();
    }

    private Map<Long, List<DccFileCategoryMatchRuleDO>> listActiveCategoryMatchRules(
            List<DccFileCategoryDO> activeCategories) {
        Set<Long> categoryIds = activeCategories.stream()
                .map(DccFileCategoryDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMatchRuleMapper.selectList(new LambdaQueryWrapperX<DccFileCategoryMatchRuleDO>()
                        .in(DccFileCategoryMatchRuleDO::getCategoryId, categoryIds)
                        .eq(DccFileCategoryMatchRuleDO::getActive, true))
                .stream()
                .collect(Collectors.groupingBy(DccFileCategoryMatchRuleDO::getCategoryId));
    }

    private boolean requiresAiCategory(DccControlledFileDO file, List<DccFileCategoryDO> activeCategories) {
        Long taxonomyId = file.getFileTypeTaxonomyId();
        if (taxonomyId != null && activeCategories.stream()
                .anyMatch(category -> Objects.equals(category.getFileTypeTaxonomyId(), taxonomyId))) {
            return false;
        }
        String stage = StrUtil.trimToNull(file.getFileTypeLevel2());
        String fileType = StrUtil.trimToNull(file.getFileTypeLevel3());
        if (stage == null || fileType == null || Objects.equals(stage, UNCLASSIFIED_STAGE)
                || Objects.equals(fileType, UNCLASSIFIED_FILE_TYPE)) {
            return true;
        }
        return activeCategories.stream().noneMatch(category -> {
            DccFileTypeTaxonomyPath path = resolveCategoryTaxonomyPath(category);
            return Objects.equals(stage, StrUtil.trimToNull(path.level2()))
                    && Objects.equals(fileType, StrUtil.trimToNull(path.level3()));
        });
    }

    private FileTypeCategoryTarget resolveAiCategoryTarget(
            DccControlledFileDO controlledFile,
            List<DccFileCategoryDO> activeCategories,
            Map<Long, List<DccFileCategoryMatchRuleDO>> categoryMatchRules) {
        List<String> fileMatchTexts = resolveAssociatedFileMatchTexts(controlledFile);
        List<String> rawFileMatchTexts = resolveAssociatedFileRawMatchTexts(controlledFile);
        if (fileMatchTexts.isEmpty()) {
            return FileTypeCategoryTarget.unclassified();
        }
        List<CategoryMatch> matches = activeCategories.stream()
                .map(category -> new CategoryMatch(category, categoryMatchScore(
                        fileMatchTexts, rawFileMatchTexts, category,
                        categoryMatchRules.getOrDefault(category.getId(), List.of()))))
                .filter(match -> match.score() > 0)
                .toList();
        if (matches.isEmpty()) {
            return FileTypeCategoryTarget.unclassified();
        }
        int bestScore = matches.stream().mapToInt(CategoryMatch::score).max().orElse(0);
        List<DccFileCategoryDO> bestCategories = matches.stream()
                .filter(match -> match.score() == bestScore)
                .map(CategoryMatch::category)
                .toList();
        if (bestCategories.size() > 1) {
            String categoryNames = bestCategories.stream()
                    .map(DccFileCategoryDO::getName)
                    .map(StrUtil::trim)
                    .distinct()
                    .collect(Collectors.joining("、"));
            return FileTypeCategoryTarget.ambiguous("匹配到多个同分分类：" + categoryNames);
        }
        DccFileTypeTaxonomyPath path = resolveCategoryTaxonomyPath(bestCategories.get(0));
        return new FileTypeCategoryTarget(
                StrUtil.trim(path.level2()),
                StrUtil.trim(path.level3()),
                true,
                false,
                "MATCHED",
                null,
                path.id(),
                path.level1(),
                path.level2(),
                path.level3(),
                path.level4(),
                path.level5());
    }

    private int categoryMatchScore(List<String> fileMatchTexts, List<String> rawFileMatchTexts,
                                   DccFileCategoryDO category, List<DccFileCategoryMatchRuleDO> matchRules) {
        String categoryName = StrUtil.trim(category.getName());
        int legacyScore = resolveCategoryMatchNames(category).stream()
                .filter(matchName -> fileMatchTexts.stream().anyMatch(text -> text.contains(matchName)))
                .mapToInt(matchName -> categoryMatchScore(categoryName, matchName))
                .max()
                .orElse(0);
        int ruleScore = matchRules.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .mapToInt(rule -> categoryMatchRuleScore(rule, fileMatchTexts, rawFileMatchTexts))
                .max()
                .orElse(0);
        return Math.max(legacyScore, ruleScore);
    }

    private int categoryMatchScore(String categoryName, String matchName) {
        int score = matchName.length();
        if (Objects.equals(categoryName, "过程运行确认（OQ）方案") && matchName.contains("OQPQ")) {
            return score + 1000;
        }
        if (Objects.equals(categoryName, "过程运行确认（OQ）报告") && matchName.contains("OQPQ")) {
            return score + 1000;
        }
        return score;
    }

    private List<String> resolveCategoryMatchNames(DccFileCategoryDO category) {
        String categoryName = normalizeCategoryMatchText(category.getName());
        if (categoryName == null) {
            return List.of();
        }
        List<String> aliases = CATEGORY_MATCH_ALIASES.getOrDefault(StrUtil.trim(category.getName()), List.of());
        List<String> matchNames = new ArrayList<>();
        matchNames.add(categoryName);
        aliases.stream()
                .map(this::normalizeCategoryMatchText)
                .filter(Objects::nonNull)
                .filter(alias -> !matchNames.contains(alias))
                .forEach(matchNames::add);
        return matchNames;
    }

    private List<String> resolveAssociatedFileMatchTexts(DccControlledFileDO controlledFile) {
        return Arrays.asList(
                controlledFile.getFileName(),
                controlledFile.getTitle(),
                controlledFile.getFileNumber()).stream()
                .map(this::normalizeCategoryMatchText)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> resolveAssociatedFileRawMatchTexts(DccControlledFileDO controlledFile) {
        return Arrays.asList(
                controlledFile.getFileName(),
                controlledFile.getTitle(),
                controlledFile.getFileNumber()).stream()
                .map(StrUtil::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private int categoryMatchRuleScore(DccFileCategoryMatchRuleDO rule, List<String> fileMatchTexts,
                                       List<String> rawFileMatchTexts) {
        String matchType = StrUtil.trimToNull(rule.getMatchType());
        if (matchType == null) {
            throw new IllegalStateException("DCC file category match rule has blank matchType: " + rule.getId());
        }
        matchType = matchType.toUpperCase(Locale.ROOT);
        return switch (matchType) {
            case CATEGORY_MATCH_TYPE_CONTAINS, CATEGORY_MATCH_TYPE_EXACT, CATEGORY_MATCH_TYPE_PREFIX,
                    CATEGORY_MATCH_TYPE_SUFFIX -> categoryTextRuleScore(rule, fileMatchTexts, matchType);
            case CATEGORY_MATCH_TYPE_EXTENSION -> categoryExtensionRuleScore(rule, rawFileMatchTexts);
            default -> throw new IllegalStateException(
                    "Unsupported DCC file category match rule type: " + rule.getMatchType());
        };
    }

    private int categoryTextRuleScore(DccFileCategoryMatchRuleDO rule, List<String> fileMatchTexts, String matchType) {
        String matchText = normalizeCategoryMatchText(rule.getMatchText());
        if (matchText == null) {
            throw new IllegalStateException("DCC file category match rule has blank matchText: " + rule.getId());
        }
        boolean matched = switch (matchType) {
            case CATEGORY_MATCH_TYPE_CONTAINS -> fileMatchTexts.stream().anyMatch(text -> text.contains(matchText));
            case CATEGORY_MATCH_TYPE_EXACT -> fileMatchTexts.stream().anyMatch(text -> Objects.equals(text, matchText));
            case CATEGORY_MATCH_TYPE_PREFIX -> fileMatchTexts.stream().anyMatch(text -> text.startsWith(matchText));
            case CATEGORY_MATCH_TYPE_SUFFIX -> fileMatchTexts.stream().anyMatch(text -> text.endsWith(matchText));
            default -> throw new IllegalStateException(
                    "Unsupported DCC file category text match rule type: " + matchType);
        };
        return matched
                ? categoryMatchRuleBaseScore(rule, matchText)
                : 0;
    }

    private int categoryExtensionRuleScore(DccFileCategoryMatchRuleDO rule, List<String> rawFileMatchTexts) {
        String extension = normalizeRuleExtension(rule.getMatchText());
        if (extension == null) {
            throw new IllegalStateException("DCC file category extension rule has blank matchText: " + rule.getId());
        }
        return rawFileMatchTexts.stream()
                .map(this::extractFileExtension)
                .filter(Objects::nonNull)
                .anyMatch(extension::equals)
                ? categoryMatchRuleBaseScore(rule, extension)
                : 0;
    }

    private int categoryMatchRuleBaseScore(DccFileCategoryMatchRuleDO rule, String normalizedMatchText) {
        int weight = rule.getWeight() == null ? 0 : rule.getWeight();
        return weight + normalizedMatchText.length();
    }

    private String normalizeRuleExtension(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return StrUtil.trimToNull(normalized.toLowerCase(Locale.ROOT));
    }

    private String extractFileExtension(String value) {
        String fileName = StrUtil.trimToNull(value);
        if (fileName == null) {
            return null;
        }
        int slashIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= slashIndex || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private DccFileTypeTaxonomyPath resolveCategoryTaxonomyPath(DccFileCategoryDO category) {
        return fileTypeTaxonomyAdminService.resolveActivePath(category.getFileTypeTaxonomyId());
    }

    private String normalizeCategoryMatchText(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String withoutExtension = normalized.replaceFirst("\\.[^.\\\\/]+$", "");
        return StrUtil.trimToNull(withoutExtension
                .replace("（", "(")
                .replace("）", ")")
                .replace(" ", "")
                .replace("\u3000", ""));
    }

    private DccProjectCodeAssociatedFileAiCategoryRespVO toAiCategoryResp(DccControlledFileDO file,
                                                                          FileTypeCategoryTarget target) {
        return DccProjectCodeAssociatedFileAiCategoryRespVO.builder()
                .fileId(file.getId())
                .fileName(StrUtil.blankToDefault(file.getFileName(), file.getTitle()))
                .currentStage(StrUtil.trimToNull(file.getFileTypeLevel2()))
                .currentFileType(StrUtil.trimToNull(file.getFileTypeLevel3()))
                .targetStage(target == null ? null : target.stage())
                .targetFileType(target == null ? null : target.fileType())
                .matched(target != null && target.matched())
                .classificationStatus(target == null ? null : target.status())
                .classificationMessage(target == null ? null : target.message())
                .build();
    }

    private <T> void applyOriginalValueCondition(LambdaUpdateWrapper<DccControlledFileDO> wrapper,
                                                 com.baomidou.mybatisplus.core.toolkit.support.SFunction<DccControlledFileDO, T> column,
                                                 T value) {
        if (value == null) {
            wrapper.isNull(column);
        } else {
            wrapper.eq(column, value);
        }
    }

    private Comparator<DccProjectCodeDO> projectCodeDisplayOrder(DccProjectCodePageReqVO reqVO) {
        Comparator<DccProjectCodeDO> defaultOrder = Comparator
                .comparing((DccProjectCodeDO item) -> isNonNumericDocControlNo(item.getDocControlNo()))
                .thenComparing(this::numericDocControlNoValue, Comparator.nullsLast(BigInteger::compareTo))
                .thenComparing(item -> StrUtil.nullToEmpty(StrUtil.trim(item.getDocControlNo())))
                .thenComparing(DccProjectCodeDO::getId, Comparator.nullsLast(Long::compareTo));
        String fileCountSort = StrUtil.trimToEmpty(reqVO.getFileCountSort());
        if ("asc".equalsIgnoreCase(fileCountSort)) {
            return Comparator
                    .comparing(DccProjectCodeDO::getAssociatedFileCount, Comparator.nullsFirst(Long::compareTo))
                    .thenComparing(defaultOrder);
        }
        if ("desc".equalsIgnoreCase(fileCountSort)) {
            return Comparator
                    .comparing(DccProjectCodeDO::getAssociatedFileCount, Comparator.nullsFirst(Long::compareTo))
                    .reversed()
                    .thenComparing(defaultOrder);
        }
        return defaultOrder;
    }

    private boolean isNonNumericDocControlNo(String docControlNo) {
        String trimmed = StrUtil.trimToEmpty(docControlNo);
        return !trimmed.matches("\\d+");
    }

    private BigInteger numericDocControlNoValue(DccProjectCodeDO item) {
        String trimmed = StrUtil.trimToEmpty(item.getDocControlNo());
        return trimmed.matches("\\d+") ? new BigInteger(trimmed) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccProjectCodeImportPreviewRespVO previewImport(MultipartFile file) throws Exception {
        List<DccProjectCodeImportExcelVO> rows = parseWorkbook(file);
        if (CollUtil.isEmpty(rows)) {
            throw new IllegalStateException("DCC_PROJECT_CODE_IMPORT_EMPTY: 项目代码导入文件不能为空");
        }

        List<DccProjectCodeDO> existingProjectCodes = projectCodeMapper.selectList();
        Map<ProjectCodeKey, DccProjectCodeDO> existingByKey = existingProjectCodes.stream()
                .collect(Collectors.toMap(item -> key(item.getProjectName(), item.getProjectCode()),
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Set<ProjectCodeKey> seenKeys = new LinkedHashSet<>();
        Set<ProjectCodeKey> validImportedKeys = new LinkedHashSet<>();
        List<DccProjectCodeImportRowDO> importRows = new ArrayList<>();

        for (int index = 0; index < rows.size(); index++) {
            int rowNo = index + 2;
            DccProjectCodeImportExcelVO row = rows.get(index);
            ImportRowDraft draft = normalizeImportRow(row);
            String failure = validateImportRow(draft, seenKeys);
            DccProjectCodeDO existing = failure == null ? existingByKey.get(key(draft.projectName(), draft.projectCode())) : null;
            String action = resolveImportAction(draft, existing, failure);
            if (failure == null) {
                validImportedKeys.add(key(draft.projectName(), draft.projectCode()));
            }
            importRows.add(toImportRow(rowNo, draft, existing, action, failure));
        }

        for (DccProjectCodeDO existing : existingProjectCodes) {
            ProjectCodeKey existingKey = key(existing.getProjectName(), existing.getProjectCode());
            if (!validImportedKeys.contains(existingKey)
                    && DccProjectCodeStatusConstants.ENABLE.equals(existing.getStatus())) {
                importRows.add(DccProjectCodeImportRowDO.builder()
                        .rowNo(rows.size() + importRows.size() + 2)
                        .docControlNo(existing.getDocControlNo())
                        .projectName(existing.getProjectName())
                        .projectCode(existing.getProjectCode())
                        .category(existing.getCategory())
                        .commissionedProduction(existing.getCommissionedProduction())
                        .projectLeader(existing.getProjectLeader())
                        .projectEngineer(existing.getProjectEngineer())
                        .storageLocation(existing.getStorageLocation())
                        .priority(existing.getPriority())
                        .currentStatus(existing.getStatus())
                        .importAction(DccProjectCodeImportActionConstants.DISABLE)
                        .build());
            }
        }

        ImportSummary summary = summarize(importRows);
        DccProjectCodeImportBatchDO batch = DccProjectCodeImportBatchDO.builder()
                .status(summary.failureCount() > 0
                        ? DccProjectCodeImportStatusConstants.FAILED
                        : DccProjectCodeImportStatusConstants.PREVIEWED)
                .totalCount(summary.totalCount())
                .createCount(summary.createCount())
                .updateCount(summary.updateCount())
                .disableCount(summary.disableCount())
                .unchangedCount(summary.unchangedCount())
                .failureCount(summary.failureCount())
                .build();
        importBatchMapper.insert(batch);
        importRows.forEach(row -> row.setBatchId(batch.getId()));
        importRowMapper.insertBatch(importRows);
        return toPreviewResp(batch, importRows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccProjectCodeImportPreviewRespVO confirmImport(Long batchId) {
        DccProjectCodeImportBatchDO batch = importBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalStateException("DCC_PROJECT_CODE_IMPORT_BATCH_NOT_FOUND: batchId=" + batchId);
        }
        if (!DccProjectCodeImportStatusConstants.PREVIEWED.equals(batch.getStatus())) {
            throw new IllegalStateException("DCC_PROJECT_CODE_IMPORT_BATCH_NOT_CONFIRMABLE: status=" + batch.getStatus());
        }
        List<DccProjectCodeImportRowDO> rows = importRowMapper.selectListByBatchId(batchId);
        if (rows.stream().anyMatch(row -> row.getFailureReason() != null)) {
            throw new IllegalStateException("DCC_PROJECT_CODE_IMPORT_HAS_FAILURES: 请重新预览并修正失败行");
        }
        for (DccProjectCodeImportRowDO row : rows) {
            applyImportRow(batchId, row);
        }
        batch.setStatus(DccProjectCodeImportStatusConstants.CONFIRMED);
        batch.setConfirmedAt(LocalDateTime.now());
        importBatchMapper.updateById(batch);
        return toPreviewResp(batch, rows);
    }

    private List<DccProjectCodeImportExcelVO> parseWorkbook(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("DCC_PROJECT_CODE_IMPORT_EMPTY: 项目代码导入文件不能为空");
        }
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalStateException("DCC_PROJECT_CODE_IMPORT_EMPTY: 项目代码导入文件不能为空");
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            validateHeader(sheet.getRow(0), formatter);

            List<DccProjectCodeImportExcelVO> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row, formatter)) {
                    continue;
                }
                rows.add(DccProjectCodeImportExcelVO.builder()
                        .docControlNo(readCell(row, 0, formatter))
                        .projectName(readCell(row, 1, formatter))
                        .projectCode(readCell(row, 2, formatter))
                        .category(readCell(row, 3, formatter))
                        .commissionedProduction(readCell(row, 4, formatter))
                        .projectLeader(readCell(row, 5, formatter))
                        .projectEngineer(readCell(row, 6, formatter))
                        .storageLocation(readCell(row, 7, formatter))
                        .priority(readCell(row, 8, formatter))
                        .build());
            }
            return rows;
        }
    }

    private void validateHeader(Row headerRow, DataFormatter formatter) {
        if (headerRow == null) {
            throw new IllegalStateException("DCC_PROJECT_CODE_IMPORT_HEADER_INVALID: 缺少表头行");
        }
        List<String> actualHeaders = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < EXPECTED_HEADERS.size(); columnIndex++) {
            actualHeaders.add(readCell(headerRow, columnIndex, formatter));
        }
        for (int columnIndex = EXPECTED_HEADERS.size(); columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            String value = readCell(headerRow, columnIndex, formatter);
            if (StrUtil.isNotBlank(value)) {
                actualHeaders.add(value);
            }
        }
        if (!EXPECTED_HEADERS.equals(actualHeaders)) {
            throw new IllegalStateException("DCC_PROJECT_CODE_IMPORT_HEADER_INVALID: 期望表头="
                    + EXPECTED_HEADERS + "，实际表头=" + actualHeaders);
        }
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int columnIndex = 0; columnIndex < EXPECTED_HEADERS.size(); columnIndex++) {
            if (StrUtil.isNotBlank(readCell(row, columnIndex, formatter))) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Row row, int columnIndex, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    private ImportRowDraft normalizeImportRow(DccProjectCodeImportExcelVO row) {
        return new ImportRowDraft(
                blankToNull(row.getDocControlNo()),
                row.getProjectName(),
                StrUtil.isBlank(row.getProjectCode()) ? "" : row.getProjectCode(),
                blankToNull(row.getCategory()),
                blankToNull(row.getCommissionedProduction()),
                blankToNull(row.getProjectLeader()),
                blankToNull(row.getProjectEngineer()),
                blankToNull(row.getStorageLocation()),
                blankToNull(row.getPriority()));
    }

    private String validateImportRow(ImportRowDraft draft, Set<ProjectCodeKey> seenKeys) {
        if (StrUtil.isBlank(draft.projectName())) {
            return "项目名称不能为空";
        }
        ProjectCodeKey key = key(draft.projectName(), draft.projectCode());
        if (!seenKeys.add(key)) {
            return "项目名称+项目代码在 Excel 中重复";
        }
        return null;
    }

    private String resolveImportAction(ImportRowDraft draft, DccProjectCodeDO existing, String failure) {
        if (failure != null) {
            return DccProjectCodeImportActionConstants.INVALID;
        }
        if (existing == null) {
            return DccProjectCodeImportActionConstants.CREATE;
        }
        if (Objects.equals(existing.getDocControlNo(), draft.docControlNo())
                && Objects.equals(existing.getCategory(), draft.category())
                && Objects.equals(existing.getCommissionedProduction(), draft.commissionedProduction())
                && Objects.equals(existing.getProjectLeader(), draft.projectLeader())
                && Objects.equals(existing.getProjectEngineer(), draft.projectEngineer())
                && Objects.equals(existing.getStorageLocation(), draft.storageLocation())
                && Objects.equals(existing.getPriority(), draft.priority())
                && DccProjectCodeStatusConstants.ENABLE.equals(existing.getStatus())) {
            return DccProjectCodeImportActionConstants.UNCHANGED;
        }
        return DccProjectCodeImportActionConstants.UPDATE;
    }

    private DccProjectCodeImportRowDO toImportRow(int rowNo, ImportRowDraft draft, DccProjectCodeDO existing,
                                                 String action, String failure) {
        return DccProjectCodeImportRowDO.builder()
                .rowNo(rowNo)
                .docControlNo(draft.docControlNo())
                .projectName(draft.projectName())
                .projectCode(draft.projectCode())
                .category(draft.category())
                .commissionedProduction(draft.commissionedProduction())
                .projectLeader(draft.projectLeader())
                .projectEngineer(draft.projectEngineer())
                .storageLocation(draft.storageLocation())
                .priority(draft.priority())
                .currentStatus(existing == null ? null : existing.getStatus())
                .importAction(action)
                .failureReason(failure)
                .build();
    }

    private void applyImportRow(Long batchId, DccProjectCodeImportRowDO row) {
        switch (row.getImportAction()) {
            case DccProjectCodeImportActionConstants.CREATE -> projectCodeMapper.insert(DccProjectCodeDO.builder()
                    .docControlNo(row.getDocControlNo())
                    .projectName(row.getProjectName())
                    .projectCode(row.getProjectCode())
                    .category(row.getCategory())
                    .commissionedProduction(row.getCommissionedProduction())
                    .projectLeader(row.getProjectLeader())
                    .projectEngineer(row.getProjectEngineer())
                    .storageLocation(row.getStorageLocation())
                    .priority(row.getPriority())
                    .status(DccProjectCodeStatusConstants.ENABLE)
                    .lastImportBatchId(batchId)
                    .build());
            case DccProjectCodeImportActionConstants.UPDATE -> {
                DccProjectCodeDO existing = requireProjectCode(row.getProjectName(), row.getProjectCode());
                updateProjectCodeFields(existing.getId(), buildImportSaveReqVO(row, DccProjectCodeStatusConstants.ENABLE),
                        batchId);
            }
            case DccProjectCodeImportActionConstants.DISABLE -> {
                DccProjectCodeDO existing = requireProjectCode(row.getProjectName(), row.getProjectCode());
                projectCodeMapper.updateById(DccProjectCodeDO.builder()
                        .id(existing.getId())
                        .status(DccProjectCodeStatusConstants.DISABLE)
                        .lastImportBatchId(batchId)
                        .build());
            }
            case DccProjectCodeImportActionConstants.UNCHANGED -> {
            }
            default -> throw new IllegalStateException("DCC_PROJECT_CODE_IMPORT_ACTION_INVALID: " + row.getImportAction());
        }
    }

    private DccProjectCodeDO requireProjectCode(String projectName, String projectCode) {
        DccProjectCodeDO existing = projectCodeMapper.selectByProjectNameAndProjectCode(projectName, projectCode);
        if (existing == null) {
            throw new IllegalStateException("DCC_PROJECT_CODE_NOT_FOUND: projectName=" + projectName
                    + ", projectCode=" + projectCode);
        }
        return existing;
    }

    private DccProjectCodeDO validateProjectCodeExists(Long id) {
        DccProjectCodeDO projectCode = projectCodeMapper.selectById(id);
        if (projectCode == null) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        return projectCode;
    }

    private void validateProjectCodeUnique(String projectName, String projectCode, Long excludeId) {
        if (projectCodeMapper.selectByProjectNameAndProjectCodeExcludingId(projectName, projectCode, excludeId) != null) {
            throw exception(PROJECT_CODE_DUPLICATE);
        }
    }

    private void validateProjectCodeDeletable(Long id) {
        if (controlledFileMapper.selectCount(DccControlledFileDO::getDccProjectCodeId, id) > 0) {
            throw exception(PROJECT_CODE_DELETE_REFERENCED);
        }
    }

    private void validateProjectCodeStatus(String status) {
        if (!DccProjectCodeStatusConstants.isValid(status)) {
            throw exception(PROJECT_CODE_STATUS_INVALID);
        }
    }

    private DccProjectCodeDO buildProjectCodeDO(DccProjectCodeSaveReqVO reqVO, Long lastImportBatchId) {
        return DccProjectCodeDO.builder()
                .productMasterId(reqVO.getProductMasterId())
                .docControlNo(reqVO.getDocControlNo())
                .projectName(reqVO.getProjectName())
                .projectCode(reqVO.getProjectCode())
                .category(reqVO.getCategory())
                .commissionedProduction(reqVO.getCommissionedProduction())
                .projectLeader(reqVO.getProjectLeader())
                .projectEngineer(reqVO.getProjectEngineer())
                .storageLocation(reqVO.getStorageLocation())
                .priority(reqVO.getPriority())
                .status(reqVO.getStatus())
                .lastImportBatchId(lastImportBatchId)
                .build();
    }

    private void updateProjectCodeFields(Long id, DccProjectCodeSaveReqVO reqVO, Long lastImportBatchId) {
        projectCodeMapper.update(null, new LambdaUpdateWrapper<DccProjectCodeDO>()
                .eq(DccProjectCodeDO::getId, id)
                .set(DccProjectCodeDO::getDocControlNo, reqVO.getDocControlNo())
                .set(DccProjectCodeDO::getProjectName, reqVO.getProjectName())
                .set(DccProjectCodeDO::getProjectCode, reqVO.getProjectCode())
                .set(DccProjectCodeDO::getCategory, reqVO.getCategory())
                .set(DccProjectCodeDO::getCommissionedProduction, reqVO.getCommissionedProduction())
                .set(DccProjectCodeDO::getProjectLeader, reqVO.getProjectLeader())
                .set(DccProjectCodeDO::getProjectEngineer, reqVO.getProjectEngineer())
                .set(DccProjectCodeDO::getStorageLocation, reqVO.getStorageLocation())
                .set(DccProjectCodeDO::getPriority, reqVO.getPriority())
                .set(DccProjectCodeDO::getStatus, reqVO.getStatus())
                .set(lastImportBatchId == null, DccProjectCodeDO::getProductMasterId, reqVO.getProductMasterId())
                .set(lastImportBatchId != null, DccProjectCodeDO::getLastImportBatchId, lastImportBatchId));
    }

    private DccProjectCodeSaveReqVO buildImportSaveReqVO(DccProjectCodeImportRowDO row, String status) {
        DccProjectCodeSaveReqVO reqVO = new DccProjectCodeSaveReqVO();
        reqVO.setDocControlNo(row.getDocControlNo());
        reqVO.setProjectName(row.getProjectName());
        reqVO.setProjectCode(row.getProjectCode());
        reqVO.setCategory(row.getCategory());
        reqVO.setCommissionedProduction(row.getCommissionedProduction());
        reqVO.setProjectLeader(row.getProjectLeader());
        reqVO.setProjectEngineer(row.getProjectEngineer());
        reqVO.setStorageLocation(row.getStorageLocation());
        reqVO.setPriority(row.getPriority());
        reqVO.setStatus(status);
        return reqVO;
    }

    private <T extends DccProjectCodeSaveReqVO> T normalizeSaveReqVO(T reqVO) {
        reqVO.setDocControlNo(blankToNull(reqVO.getDocControlNo()));
        reqVO.setProjectName(StrUtil.trim(reqVO.getProjectName()));
        reqVO.setProjectCode(StrUtil.trimToEmpty(reqVO.getProjectCode()));
        reqVO.setCategory(blankToNull(reqVO.getCategory()));
        reqVO.setCommissionedProduction(blankToNull(reqVO.getCommissionedProduction()));
        reqVO.setProjectLeader(blankToNull(reqVO.getProjectLeader()));
        reqVO.setProjectEngineer(blankToNull(reqVO.getProjectEngineer()));
        reqVO.setStorageLocation(blankToNull(reqVO.getStorageLocation()));
        reqVO.setPriority(blankToNull(reqVO.getPriority()));
        reqVO.setStatus(StrUtil.trim(reqVO.getStatus()));
        return reqVO;
    }

    private ImportSummary summarize(List<DccProjectCodeImportRowDO> rows) {
        int createCount = countAction(rows, DccProjectCodeImportActionConstants.CREATE);
        int updateCount = countAction(rows, DccProjectCodeImportActionConstants.UPDATE);
        int disableCount = countAction(rows, DccProjectCodeImportActionConstants.DISABLE);
        int unchangedCount = countAction(rows, DccProjectCodeImportActionConstants.UNCHANGED);
        int failureCount = countAction(rows, DccProjectCodeImportActionConstants.INVALID);
        return new ImportSummary(rows.size(), createCount, updateCount, disableCount, unchangedCount, failureCount);
    }

    private int countAction(List<DccProjectCodeImportRowDO> rows, String action) {
        return (int) rows.stream().filter(row -> action.equals(row.getImportAction())).count();
    }

    private DccProjectCodeImportPreviewRespVO toPreviewResp(DccProjectCodeImportBatchDO batch,
                                                           List<DccProjectCodeImportRowDO> rows) {
        return DccProjectCodeImportPreviewRespVO.builder()
                .batchId(batch.getId())
                .status(batch.getStatus())
                .totalCount(batch.getTotalCount())
                .createCount(batch.getCreateCount())
                .updateCount(batch.getUpdateCount())
                .disableCount(batch.getDisableCount())
                .unchangedCount(batch.getUnchangedCount())
                .failureCount(batch.getFailureCount())
                .rows(rows.stream().map(this::toImportRowResp).toList())
                .build();
    }

    private DccProjectCodeImportRowRespVO toImportRowResp(DccProjectCodeImportRowDO row) {
        return DccProjectCodeImportRowRespVO.builder()
                .rowNo(row.getRowNo())
                .docControlNo(row.getDocControlNo())
                .projectName(row.getProjectName())
                .projectCode(row.getProjectCode())
                .category(row.getCategory())
                .commissionedProduction(row.getCommissionedProduction())
                .projectLeader(row.getProjectLeader())
                .projectEngineer(row.getProjectEngineer())
                .storageLocation(row.getStorageLocation())
                .priority(row.getPriority())
                .currentStatus(row.getCurrentStatus())
                .importAction(row.getImportAction())
                .failureReason(row.getFailureReason())
                .build();
    }

    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }

    private ProjectCodeKey key(String projectName, String projectCode) {
        return new ProjectCodeKey(projectName, StrUtil.nullToEmpty(projectCode));
    }

    private record ProjectCodeKey(String projectName, String projectCode) {
    }

    private record CategoryMatch(DccFileCategoryDO category, int score) {
    }

    private record FileTypeCategoryTarget(String stage, String fileType, boolean matched, boolean ambiguous,
                                          String status, String message, Long taxonomyId, String level1,
                                          String level2, String level3, String level4, String level5) {

        private static FileTypeCategoryTarget unclassified() {
            return new FileTypeCategoryTarget(UNCLASSIFIED_STAGE, UNCLASSIFIED_FILE_TYPE, false, false,
                    "UNCLASSIFIED", "未匹配到启用的文件分类规则",
                    null, TECHNICAL_FILE_TYPE_LEVEL1, UNCLASSIFIED_STAGE, UNCLASSIFIED_FILE_TYPE, null, null);
        }

        private static FileTypeCategoryTarget ambiguous(String message) {
            return new FileTypeCategoryTarget(null, null, false, true, "AMBIGUOUS", message,
                    null, null, null, null, null, null);
        }
    }

    private record ImportRowDraft(String docControlNo, String projectName, String projectCode, String category,
                                  String commissionedProduction, String projectLeader, String projectEngineer,
                                  String storageLocation, String priority) {
    }

    private record ImportSummary(int totalCount, int createCount, int updateCount, int disableCount,
                                 int unchangedCount, int failureCount) {
    }
}
