package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDirectoryBindingSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileCategorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileUploadPolicyDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileUploadPolicyMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryLifecycleStageEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DELETE_CHILD_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DELETE_RELATION_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_DELETE_REFERENCED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_LIFECYCLE_STAGE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_FILE_CATEGORY_SYNC_AMBIGUOUS;

@Service
@Validated
public class DccFileCategoryAdminServiceImpl implements DccFileCategoryAdminService {

    private static final String INTAUTH_SOURCE_PREFIX = "INTAUTH:";
    private static final String INTAUTH_CODE_PREFIX = "INTAUTH-";
    private static final String LOCAL_SOURCE = "LOCAL";

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccCategoryDirectoryBindingMapper bindingMapper;
    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Resource
    private DccCategoryViewMatrixRuleMapper viewMatrixRuleMapper;
    @Resource
    private DccFileCategoryDistributionRuleMapper distributionRuleMapper;
    @Resource
    private DccFileCategoryTrainingRuleMapper trainingRuleMapper;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccCategoryApprovalRouteMapper routeMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileUploadPolicyMapper uploadPolicyMapper;
    @Resource
    private DccIntAuthFileCategoryClient intAuthFileCategoryClient;
    @Resource
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(DccFileCategorySaveReqVO reqVO) {
        DccFileCategoryDO category = BeanUtils.toBean(reqVO, DccFileCategoryDO.class);
        applyLocalSourceDefault(category);
        applyLifecycleStageFromFileTypeTaxonomy(category);
        applyRequirementDefaults(category);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(DccFileCategorySaveReqVO reqVO) {
        validateCategoryExists(reqVO.getId());
        DccFileCategoryDO category = BeanUtils.toBean(reqVO, DccFileCategoryDO.class);
        applyLifecycleStageFromFileTypeTaxonomy(category);
        categoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        validateCategoryExists(id);
        validateCategoryDeletable(id);
        categoryMapper.deleteById(id);
    }

    @Override
    public DccFileCategoryDO getCategory(Long id) {
        return validateCategoryExists(id);
    }

    @Override
    public List<DccFileCategoryDO> getCategoryList() {
        return categoryMapper.selectList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccFileCategoryImportResult importCategoriesFromIntAuth() {
        List<DccIntAuthFileCategoryClient.IntAuthFileCategory> intAuthCategories = intAuthFileCategoryClient.listFileCategories();
        List<DccFileCategoryDO> localCategories = categoryMapper.selectList();
        Map<Long, DccFileCategoryDO> mappedCategories = buildMappedCategories(localCategories);
        Map<String, List<DccFileCategoryDO>> unmappedCategoriesByName = localCategories.stream()
                .filter(item -> parseIntAuthSourceId(item.getSource()) == null)
                .collect(Collectors.groupingBy(item -> normalizeName(item.getName())));
        Set<String> localCodes = localCategories.stream()
                .map(DccFileCategoryDO::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int nextSort = localCategories.stream()
                .map(DccFileCategoryDO::getSort)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int createdCount = 0;
        int adoptedCount = 0;
        int updatedCount = 0;

        for (DccIntAuthFileCategoryClient.IntAuthFileCategory intAuthCategory : intAuthCategories) {
            DccFileCategoryDO mappedCategory = mappedCategories.get(intAuthCategory.id());
            if (mappedCategory != null) {
                updateImportedCategory(mappedCategory, intAuthCategory);
                updatedCount++;
                continue;
            }

            List<DccFileCategoryDO> sameNameCategories = unmappedCategoriesByName
                    .getOrDefault(normalizeName(intAuthCategory.name()), List.of());
            if (sameNameCategories.size() > 1) {
                throw exception(INTAUTH_FILE_CATEGORY_SYNC_AMBIGUOUS, intAuthCategory.name());
            }
            if (sameNameCategories.size() == 1) {
                adoptExistingLocalCategory(sameNameCategories.get(0), intAuthCategory);
                adoptedCount++;
                continue;
            }

            String generatedCode = buildIntAuthCode(intAuthCategory.id());
            if (localCodes.contains(generatedCode)) {
                throw exception(INTAUTH_FILE_CATEGORY_SYNC_AMBIGUOUS, generatedCode);
            }
            createImportedCategory(intAuthCategory, ++nextSort, generatedCode);
            localCodes.add(generatedCode);
            createdCount++;
        }

        return new DccFileCategoryImportResult(intAuthCategories.size(), createdCount, adoptedCount, updatedCount);
    }

    @Override
    public Map<Long, Long> getCategoryDirectoryBindingMap() {
        return convertMap(bindingMapper.selectList(DccCategoryDirectoryBindingDO::getActive, Boolean.TRUE),
                DccCategoryDirectoryBindingDO::getCategoryId, DccCategoryDirectoryBindingDO::getDirectoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccCategoryDirectoryBindingDO bindDirectory(Long categoryId, DccCategoryDirectoryBindingSaveReqVO reqVO) {
        validateCategoryExists(categoryId);
        if (directoryMapper.selectById(reqVO.getDirectoryId()) == null) {
            throw exception(FILE_DIRECTORY_NOT_EXISTS);
        }
        // This relation is single-binding configuration data; physical delete prevents
        // historical logic-deleted rows from continuing to occupy the unique pair.
        bindingMapper.deleteAllByCategoryIdForce(categoryId);
        DccCategoryDirectoryBindingDO binding = new DccCategoryDirectoryBindingDO();
        binding.setCategoryId(categoryId);
        binding.setDirectoryId(reqVO.getDirectoryId());
        binding.setActive(reqVO.getActive());
        bindingMapper.insert(binding);
        return binding;
    }

    private DccFileCategoryDO validateCategoryExists(Long id) {
        DccFileCategoryDO category = categoryMapper.selectById(id);
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private void validateCategoryDeletable(Long id) {
        if (categoryMapper.selectCount(DccFileCategoryDO::getParentId, id) > 0) {
            throw exception(FILE_CATEGORY_DELETE_CHILD_EXISTS);
        }
        if (controlledFileMapper.selectCount(DccControlledFileDO::getCategoryId, id) > 0
                || controlledFileMasterMapper.selectCount(DccControlledFileMasterDO::getCategoryId, id) > 0) {
            throw exception(FILE_CATEGORY_DELETE_REFERENCED);
        }
        if (bindingMapper.selectCount(DccCategoryDirectoryBindingDO::getCategoryId, id) > 0
                || permissionRuleMapper.selectCount(DccFileCategoryPermissionRuleDO::getCategoryId, id) > 0
                || viewMatrixRuleMapper.selectCount(DccCategoryViewMatrixRuleDO::getCategoryId, id) > 0
                || distributionRuleMapper.selectCount(DccFileCategoryDistributionRuleDO::getCategoryId, id) > 0
                || trainingRuleMapper.selectCount(DccFileCategoryTrainingRuleDO::getCategoryId, id) > 0
                || routeMapper.selectCount(DccCategoryApprovalRouteDO::getCategoryId, id) > 0
                || uploadPolicyMapper.selectCount(DccControlledFileUploadPolicyDO::getCategoryId, id) > 0) {
            throw exception(FILE_CATEGORY_DELETE_RELATION_EXISTS);
        }
    }

    private Map<Long, DccFileCategoryDO> buildMappedCategories(List<DccFileCategoryDO> localCategories) {
        Map<Long, List<DccFileCategoryDO>> groupedBySourceId = localCategories.stream()
                .filter(item -> parseIntAuthSourceId(item.getSource()) != null)
                .collect(Collectors.groupingBy(item -> parseIntAuthSourceId(item.getSource())));
        groupedBySourceId.forEach((sourceId, categories) -> {
            if (categories.size() > 1) {
                throw exception(INTAUTH_FILE_CATEGORY_SYNC_AMBIGUOUS, String.valueOf(sourceId));
            }
        });
        return groupedBySourceId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0)));
    }

    private void updateImportedCategory(DccFileCategoryDO existingCategory,
                                        DccIntAuthFileCategoryClient.IntAuthFileCategory intAuthCategory) {
        categoryMapper.updateById(DccFileCategoryDO.builder()
                .id(existingCategory.getId())
                .name(intAuthCategory.name())
                .active(intAuthCategory.active())
                .source(buildIntAuthSource(intAuthCategory.id()))
                .lifecycleStage(resolveLifecycleStageForCategory(existingCategory.getCode(), intAuthCategory.name()))
                .build());
    }

    private void adoptExistingLocalCategory(DccFileCategoryDO existingCategory,
                                            DccIntAuthFileCategoryClient.IntAuthFileCategory intAuthCategory) {
        categoryMapper.updateById(DccFileCategoryDO.builder()
                .id(existingCategory.getId())
                .name(intAuthCategory.name())
                .active(intAuthCategory.active())
                .source(buildIntAuthSource(intAuthCategory.id()))
                .lifecycleStage(resolveLifecycleStageForCategory(existingCategory.getCode(), intAuthCategory.name()))
                .build());
    }

    private void createImportedCategory(DccIntAuthFileCategoryClient.IntAuthFileCategory intAuthCategory,
                                        int sort, String code) {
        DccFileCategoryDO category = DccFileCategoryDO.builder()
                .code(code)
                .name(intAuthCategory.name())
                .parentId(null)
                .active(intAuthCategory.active())
                .sort(sort)
                .source(buildIntAuthSource(intAuthCategory.id()))
                .remark("Imported from IntAuth")
                .description(null)
                .lifecycleStage(resolveLifecycleStageForCategory(code, intAuthCategory.name()))
                .distributionRequired(Boolean.TRUE)
                .trainingRequired(Boolean.TRUE)
                .build();
        categoryMapper.insert(category);
    }

    private void applyLifecycleStageFromFileTypeTaxonomy(DccFileCategoryDO category) {
        DccFileTypeTaxonomyPath path = validateFileTypeTaxonomy(category.getFileTypeTaxonomyId());
        category.setLifecycleStage(resolveLifecycleStageFromTaxonomyStage(path.level2(), category.getCode()));
    }

    private DccFileTypeTaxonomyPath validateFileTypeTaxonomy(Long fileTypeTaxonomyId) {
        if (fileTypeTaxonomyId == null) {
            throw exception(FILE_CATEGORY_LIFECYCLE_STAGE_INVALID, "fileTypeTaxonomyId");
        }
        return fileTypeTaxonomyAdminService.resolveActivePath(fileTypeTaxonomyId);
    }

    private String resolveLifecycleStageFromTaxonomyStage(String taxonomyStageName, String categoryCode) {
        if (!StringUtils.hasText(taxonomyStageName)) {
            throw exception(FILE_CATEGORY_LIFECYCLE_STAGE_INVALID, categoryCode);
        }
        String normalizedStageName = taxonomyStageName.trim().toLowerCase();
        if (normalizedStageName.contains("策划") || normalizedStageName.contains("plan")
                || Objects.equals(normalizedStageName, "清单")) {
            return DccFileCategoryLifecycleStageEnum.PLAN.getCode();
        }
        if (normalizedStageName.contains("输入") || normalizedStageName.contains("input")) {
            return DccFileCategoryLifecycleStageEnum.INPUT.getCode();
        }
        if (normalizedStageName.contains("输出") || normalizedStageName.contains("output")) {
            return DccFileCategoryLifecycleStageEnum.OUTPUT.getCode();
        }
        if (normalizedStageName.contains("验证") || normalizedStageName.contains("verification")) {
            return DccFileCategoryLifecycleStageEnum.VERIFICATION.getCode();
        }
        if (normalizedStageName.contains("确认") || normalizedStageName.contains("validation")) {
            return DccFileCategoryLifecycleStageEnum.VALIDATION.getCode();
        }
        if (normalizedStageName.contains("转换") || normalizedStageName.contains("转移")
                || normalizedStageName.contains("注册资料") || normalizedStageName.contains("变更")
                || normalizedStageName.contains("transfer")) {
            return DccFileCategoryLifecycleStageEnum.TRANSFER.getCode();
        }
        throw exception(FILE_CATEGORY_LIFECYCLE_STAGE_INVALID, taxonomyStageName);
    }

    public static String resolveLifecycleStageForCategory(String code, String name) {
        if (!StringUtils.hasText(code)) {
            throw exception(FILE_CATEGORY_LIFECYCLE_STAGE_INVALID, name);
        }
        String categoryCode = code.trim().toUpperCase();
        String categoryName = StringUtils.hasText(name) ? name.trim() : "";
        if (Set.of("DCC_FVM_DHF_004", "DCC_FVM_DHF_005", "DCC_FVM_DHF_010").contains(categoryCode)) {
            return DccFileCategoryLifecycleStageEnum.PLAN.getCode();
        }
        if (Set.of("DCC_FVM_DHF_001", "DCC_FVM_DHF_002", "DCC_FVM_DHF_003", "DCC_FVM_DHF_006",
                "DCC_FVM_DHF_007", "DCC_FVM_DHF_008", "DCC_FVM_DHF_009").contains(categoryCode)) {
            return DccFileCategoryLifecycleStageEnum.INPUT.getCode();
        }
        if (categoryCode.matches("DCC_FVM_DMR_\\d{3}") || "DCC_FVM_DHF_011".equals(categoryCode)) {
            return DccFileCategoryLifecycleStageEnum.OUTPUT.getCode();
        }
        if (Set.of("DCC_FVM_DHF_012", "DCC_FVM_DHF_013", "DCC_FVM_DHF_014", "DCC_FVM_DHF_015",
                "DCC_FVM_DHF_016").contains(categoryCode)) {
            return DccFileCategoryLifecycleStageEnum.VERIFICATION.getCode();
        }
        if (Set.of("DCC_FVM_DHF_017", "DCC_FVM_DHF_018", "DCC_FVM_DHF_020", "DCC_FVM_DHF_021",
                "DCC_FVM_DHF_022", "DCC_FVM_DHF_023", "DCC_FVM_DHF_024", "DCC_FVM_DHF_025",
                "DCC_FVM_DHF_026", "DCC_FVM_DHF_027", "DCC_FVM_DHF_028", "DCC_FVM_DHF_029")
                .contains(categoryCode)) {
            return DccFileCategoryLifecycleStageEnum.VALIDATION.getCode();
        }
        if ("DCC_FVM_DHF_019".equals(categoryCode) || categoryCode.matches("DCC_FVM_DHF_03[0-5]")
                || categoryCode.startsWith("DCC_OTHER_TEMPLATE_")) {
            return DccFileCategoryLifecycleStageEnum.TRANSFER.getCode();
        }
        if (Set.of("项目立项书", "项目策划书", "风险管理计划").contains(categoryName)) {
            return DccFileCategoryLifecycleStageEnum.PLAN.getCode();
        }
        if (Set.of("市场调研报告", "技术调研报告", "临床注册路径分析", "专利检索与分析报告",
                "同类产品测试方案、报告", "不良事件调研报告", "法规、标准清单").contains(categoryName)) {
            return DccFileCategoryLifecycleStageEnum.INPUT.getCode();
        }
        if (Set.of("风险管理报告", "产品技术要求", "生产用设备清单", "检验用设备清单", "BOM表",
                "产品说明书", "成品图纸", "零配件图纸", "包装设计", "标签、合格证", "物资采购清单",
                "采购技术要求", "工艺流程图", "工序卡/作业指导书", "项目间通用工序卡/作业指导书",
                "来料检验规程", "过程检验规程", "成品检验规程", "检验记录表单", "生产记录表单",
                "标准测试方法", "设备采购技术要求", "生产/检验用工装模具采购技术要求及设计图纸",
                "生产/检验用工装模具维护保养规范", "生产/检验用工装模具维护保养记录表").contains(categoryName)) {
            return DccFileCategoryLifecycleStageEnum.OUTPUT.getCode();
        }
        if (Set.of("验证主计划", "设计验证方案", "设计验证报告", "通用验证方案", "通用验证报告").contains(categoryName)) {
            return DccFileCategoryLifecycleStageEnum.VERIFICATION.getCode();
        }
        if (Set.of("运输包装验证方案/报告", "货架寿命验证方案/报告", "性能评价方案和报告", "产品过程确认主计划",
                "设备安装确认（IQ）方案", "设备安装确认（IQ）报告", "过程运行确认（OQ）方案",
                "过程运行确认（OQ）报告", "过程性能确认（PQ）方案", "过程性能确认（PQ）报告",
                "过程确认总结报告", "灭菌确认方案/报告").contains(categoryName)) {
            return DccFileCategoryLifecycleStageEnum.VALIDATION.getCode();
        }
        if (Set.of("设计转移方案/报告", "首次注册资料汇编", "延续注册资料汇编", "变更注册资料汇编",
                "首次备案资料汇编", "变更备案资料汇编", "生产许可/备案资料汇编").contains(categoryName)) {
            return DccFileCategoryLifecycleStageEnum.TRANSFER.getCode();
        }
        throw exception(FILE_CATEGORY_LIFECYCLE_STAGE_INVALID, code + "/" + name);
    }

    private void applyRequirementDefaults(DccFileCategoryDO category) {
        if (category.getDistributionRequired() == null) {
            category.setDistributionRequired(Boolean.TRUE);
        }
        if (category.getTrainingRequired() == null) {
            category.setTrainingRequired(Boolean.TRUE);
        }
    }

    private void applyLocalSourceDefault(DccFileCategoryDO category) {
        if (!StringUtils.hasText(category.getSource())) {
            category.setSource(LOCAL_SOURCE);
        }
    }

    private Long parseIntAuthSourceId(String source) {
        if (source == null || !source.startsWith(INTAUTH_SOURCE_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(source.substring(INTAUTH_SOURCE_PREFIX.length()));
        } catch (NumberFormatException ex) {
            throw exception(INTAUTH_FILE_CATEGORY_SYNC_AMBIGUOUS, source);
        }
    }

    private String buildIntAuthSource(Long sourceId) {
        return INTAUTH_SOURCE_PREFIX + sourceId;
    }

    private String buildIntAuthCode(Long sourceId) {
        return INTAUTH_CODE_PREFIX + sourceId;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
