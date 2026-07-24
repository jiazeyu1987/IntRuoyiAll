package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccAdminFullConfigPackageImportRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDistributionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryTrainingRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileCategorySaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccPositionAssignmentSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccAdminFullConfigManagedScopeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccAdminFullConfigManagedScopeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionScopeEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAdminService;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionAdminService;
import cn.iocoder.yudao.module.dcc.service.position.DccUploaderDerivedPositionSupport;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.PostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.Data;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED;

@Service
public class DccAdminFullConfigPackageServiceImpl implements DccAdminFullConfigPackageService {

    static final String PACKAGE_VERSION = "dcc-admin-full-config-package.v1";
    private static final Set<String> MATRIX_MANAGED_PERMISSION_ACTIONS = Set.of("REVIEW", "APPROVE");

    @Resource
    private DccApprovalPositionMapper approvalPositionMapper;
    @Resource
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccDirectoryAccessRuleMapper directoryAccessRuleMapper;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccAdminFullConfigManagedScopeMapper managedScopeMapper;
    @Resource
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Resource
    private DccCategoryApprovalRouteMapper approvalRouteMapper;
    @Resource
    private DccCategoryApprovalRouteNodeMapper approvalRouteNodeMapper;
    @Resource
    private DccCategoryViewMatrixRuleMapper viewMatrixRuleMapper;
    @Resource
    private DccFileCategoryDistributionRuleMapper distributionRuleMapper;
    @Resource
    private DccFileCategoryTrainingRuleMapper trainingRuleMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private DccDirectoryAdminService directoryAdminService;
    @Resource
    private DccFileCategoryAdminService fileCategoryAdminService;
    @Resource
    private DccCategoryPermissionAdminService permissionAdminService;
    @Resource
    private DccCategoryDistributionRuleAdminService distributionRuleAdminService;
    @Resource
    private DccCategoryTrainingRuleAdminService trainingRuleAdminService;
    @Resource
    private DccCategoryApprovalMatrixAdminService approvalMatrixAdminService;
    @Resource
    private DccCategoryViewMatrixAdminService viewMatrixAdminService;
    @Resource
    private DccApprovalPositionAdminService approvalPositionAdminService;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private DeptMapper deptMapper;

    @Override
    public byte[] exportPackage() {
        LookupCache lookupCache = new LookupCache();
        ManagedScope managedScope = loadManagedScope();
        DccAdminFullConfigPackage payload = new DccAdminFullConfigPackage();
        payload.setPackageVersion(PACKAGE_VERSION);
        payload.setApprovalPositions(exportApprovalPositions(lookupCache, managedScope));
        payload.setDirectories(exportDirectories(lookupCache, managedScope));
        payload.setCategories(exportCategories(lookupCache, managedScope));
        return JsonUtils.toJsonByte(payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccAdminFullConfigPackageImportRespVO importPackage(byte[] content) {
        DccAdminFullConfigPackage payload = JsonUtils.parseObject(content, DccAdminFullConfigPackage.class);
        validatePayload(payload);
        ManagedScope previousScope = loadManagedScope();
        ImportContext context = new ImportContext();
        importApprovalPositions(payload.getApprovalPositions(), context, new LookupCache());
        importDirectories(payload.getDirectories(), context, new LookupCache());
        importCategories(payload.getCategories(), context, new LookupCache());
        cleanupRemovedManagedScope(previousScope, context, new LookupCache());
        persistManagedScope(context);
        return buildImportResp(context);
    }

    private List<ApprovalPositionItem> exportApprovalPositions(LookupCache lookupCache, ManagedScope managedScope) {
        return approvalPositionMapper.selectList().stream()
                .filter(position -> managedScope.includeApprovalPosition(position.getCode()))
                .sorted(Comparator.comparing(DccApprovalPositionDO::getCode, Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccApprovalPositionDO::getId))
                .map(position -> {
                    ApprovalPositionItem item = new ApprovalPositionItem();
                    item.setCode(position.getCode());
                    item.setName(position.getName());
                    item.setActive(position.getActive());
                    item.setSource(position.getSource());
                    item.setRemark(position.getRemark());
                    item.setAssignments(exportApprovalPositionAssignments(position, lookupCache));
                    return item;
                })
                .toList();
    }

    private List<PositionAssignmentItem> exportApprovalPositionAssignments(DccApprovalPositionDO position,
                                                                           LookupCache lookupCache) {
        if (DccUploaderDerivedPositionSupport.isUploaderDerivedPosition(position)) {
            return List.of();
        }
        return positionAssignmentMapper.selectList(DccPositionAssignmentDO::getPositionId, position.getId()).stream()
                .sorted(Comparator.comparing(DccPositionAssignmentDO::getAssignmentType,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccPositionAssignmentDO::getId))
                .map(assignment -> toAssignmentItem(assignment, lookupCache))
                .toList();
    }

    private PositionAssignmentItem toAssignmentItem(DccPositionAssignmentDO assignment, LookupCache lookupCache) {
        PositionAssignmentItem item = new PositionAssignmentItem();
        item.setAssignmentType(assignment.getAssignmentType());
        item.setActive(assignment.getActive());
        item.setChangeReason(assignment.getChangeReason());
        item.setUsername(lookupCache.resolveUsername(assignment.getUserId()));
        item.setPostCode(lookupCache.resolvePostCode(assignment.getSystemPostId()));
        return item;
    }

    private List<DirectoryItem> exportDirectories(LookupCache lookupCache, ManagedScope managedScope) {
        List<DccFileDirectoryDO> directories = lookupCache.getDirectories().stream()
                .filter(directory -> managedScope.includeDirectory(lookupCache.resolveDirectoryPath(directory.getId())))
                .sorted(Comparator.comparing(DccFileDirectoryDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccFileDirectoryDO::getId))
                .toList();
        Map<Long, DccFileDirectoryDO> directoryMap = lookupCache.getDirectoryById();
        return directories.stream()
                .map(directory -> {
                    DirectoryItem item = new DirectoryItem();
                    item.setPath(buildDirectoryPath(directory, directoryMap));
                    item.setCode(directory.getCode());
                    item.setName(directory.getName());
                    item.setParentPath(buildParentPath(directory, directoryMap));
                    item.setActive(directory.getActive());
                    item.setSort(directory.getSort());
                    item.setRemark(directory.getRemark());
                    item.setAccessRuleManuallyBound(directory.getAccessRuleManuallyBound());
                    item.setAccessRules(directoryAccessRuleMapper.selectListByDirectoryId(directory.getId()).stream()
                            .sorted(Comparator.comparing(DccDirectoryAccessRuleDO::getSubjectType,
                                            Comparator.nullsLast(String::compareTo))
                                    .thenComparing(DccDirectoryAccessRuleDO::getSubjectId))
                            .map(rule -> toDirectoryAccessRuleItem(rule, lookupCache))
                            .toList());
                    return item;
                })
                .toList();
    }

    private DirectoryAccessRuleItem toDirectoryAccessRuleItem(DccDirectoryAccessRuleDO rule, LookupCache lookupCache) {
        DirectoryAccessRuleItem item = new DirectoryAccessRuleItem();
        item.setSubjectType(rule.getSubjectType());
        boolean mergedReadAllowed = Boolean.TRUE.equals(rule.getCanQuery()) || Boolean.TRUE.equals(rule.getCanPreview());
        item.setCanQuery(mergedReadAllowed);
        item.setCanPreview(mergedReadAllowed);
        item.setCanDownload(rule.getCanDownload());
        item.setActive(rule.getActive());
        item.setChangeReason(rule.getChangeReason());
        fillSystemSubjectKey(item, rule.getSubjectType(), rule.getSubjectId(), lookupCache);
        return item;
    }

    private List<CategoryItem> exportCategories(LookupCache lookupCache, ManagedScope managedScope) {
        List<DccFileCategoryDO> categories = categoryMapper.selectList().stream()
                .filter(category -> managedScope.includeCategory(category.getCode()))
                .sorted(Comparator.comparing(DccFileCategoryDO::getCode, Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccFileCategoryDO::getId))
                .toList();
        Map<Long, DccFileCategoryDO> categoryMap = convertMap(categories, DccFileCategoryDO::getId);
        Map<Long, DccCategoryDirectoryBindingDO> bindingMap = categoryDirectoryBindingMapper.selectList().stream()
                .collect(Collectors.toMap(DccCategoryDirectoryBindingDO::getCategoryId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, DccCategoryApprovalRouteDO> activeRouteMap = new LinkedHashMap<>();
        for (DccFileCategoryDO category : categories) {
            DccCategoryApprovalRouteDO route = approvalRouteMapper.selectLatestActiveByCategoryId(category.getId());
            if (route != null) {
                activeRouteMap.put(category.getId(), route);
            }
        }
        return categories.stream()
                .map(category -> exportCategory(category, categoryMap, bindingMap.get(category.getId()),
                        activeRouteMap.get(category.getId()), lookupCache))
                .toList();
    }

    private CategoryItem exportCategory(DccFileCategoryDO category,
                                        Map<Long, DccFileCategoryDO> categoryMap,
                                        DccCategoryDirectoryBindingDO binding,
                                        DccCategoryApprovalRouteDO route,
                                        LookupCache lookupCache) {
        CategoryItem item = new CategoryItem();
        item.setCode(category.getCode());
        item.setName(category.getName());
        item.setParentCode(resolveParentCategoryCode(category, categoryMap));
        item.setActive(category.getActive());
        item.setSort(category.getSort());
        item.setSource(category.getSource());
        item.setRemark(category.getRemark());
        item.setDescription(category.getDescription());
        item.setLifecycleStage(category.getLifecycleStage());
        item.setDistributionRequired(category.getDistributionRequired());
        item.setTrainingRequired(category.getTrainingRequired());
        if (binding != null) {
            DirectoryBindingItem bindingItem = new DirectoryBindingItem();
            bindingItem.setDirectoryPath(lookupCache.resolveDirectoryPath(binding.getDirectoryId()));
            bindingItem.setActive(binding.getActive());
            item.setDirectoryBinding(bindingItem);
        }
        item.setPermissionRules(permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId,
                        category.getId()).stream()
                .filter(rule -> !isMatrixManagedPermissionAction(rule.getActionType()))
                .sorted(Comparator.comparing(DccFileCategoryPermissionRuleDO::getActionType,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccFileCategoryPermissionRuleDO::getSubjectType,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccFileCategoryPermissionRuleDO::getSubjectId,
                                Comparator.nullsLast(Long::compareTo)))
                .map(rule -> toPermissionRuleItem(rule, lookupCache))
                .toList());
        item.setViewMatrix(buildViewMatrix(category.getId(), lookupCache));
        item.setApprovalMatrix(buildApprovalMatrix(route, lookupCache));
        item.setDistributionRules(distributionRuleMapper.selectList(
                        DccFileCategoryDistributionRuleDO::getCategoryId, category.getId()).stream()
                .sorted(Comparator.comparing(DccFileCategoryDistributionRuleDO::getDepartmentId))
                .map(rule -> toDistributionRuleItem(rule, lookupCache))
                .filter(rule -> StringUtils.hasText(rule.getDepartmentPath()))
                .toList());
        item.setTrainingRules(trainingRuleMapper.selectList(
                        DccFileCategoryTrainingRuleDO::getCategoryId, category.getId()).stream()
                .sorted(Comparator.comparing(DccFileCategoryTrainingRuleDO::getDepartmentId))
                .map(rule -> toTrainingRuleItem(rule, lookupCache))
                .filter(rule -> StringUtils.hasText(rule.getDepartmentPath()))
                .toList());
        return item;
    }

    private PermissionRuleItem toPermissionRuleItem(DccFileCategoryPermissionRuleDO rule, LookupCache lookupCache) {
        PermissionRuleItem item = new PermissionRuleItem();
        item.setActionType(rule.getActionType());
        item.setSubjectType(rule.getSubjectType());
        item.setScopeType(StringUtils.hasText(rule.getScopeType())
                ? rule.getScopeType() : DccFileCategoryPermissionScopeEnum.GLOBAL.getCode());
        item.setActive(rule.getActive());
        item.setRemark(rule.getRemark());
        fillSystemSubjectKey(item, rule.getSubjectType(), rule.getSubjectId(), lookupCache);
        return item;
    }

    private boolean isMatrixManagedPermissionAction(String actionType) {
        return actionType != null && MATRIX_MANAGED_PERMISSION_ACTIONS.contains(actionType.trim().toUpperCase());
    }

    private ViewMatrixItem buildViewMatrix(Long categoryId, LookupCache lookupCache) {
        ViewMatrixItem item = new ViewMatrixItem();
        item.setRules(viewMatrixRuleMapper.selectList(DccCategoryViewMatrixRuleDO::getCategoryId, categoryId).stream()
                .sorted(Comparator.comparing(DccCategoryViewMatrixRuleDO::getExcelFileName,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccCategoryViewMatrixRuleDO::getExcelRowNo,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryViewMatrixRuleDO::getExcelColumnLetter,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccCategoryViewMatrixRuleDO::getId))
                .map(rule -> toViewMatrixRuleItem(rule, lookupCache))
                .toList());
        return item;
    }

    private ViewMatrixRuleItem toViewMatrixRuleItem(DccCategoryViewMatrixRuleDO rule, LookupCache lookupCache) {
        ViewMatrixRuleItem item = new ViewMatrixRuleItem();
        item.setExcelFileName(rule.getExcelFileName());
        item.setExcelRowNo(rule.getExcelRowNo());
        item.setExcelColumnLetter(rule.getExcelColumnLetter());
        item.setSubjectLabel(rule.getSubjectLabel());
        item.setSubjectTopHeader(rule.getSubjectTopHeader());
        item.setSubjectSubHeader(rule.getSubjectSubHeader());
        item.setMarker(rule.getMarker());
        item.setScopeType(rule.getScopeType());
        item.setSubjectType(rule.getSubjectType());
        item.setActive(rule.getActive());
        item.setRemark(rule.getRemark());
        fillSystemSubjectKey(item, rule.getSubjectType(), rule.getSubjectId(), lookupCache);
        if ("DCC_POSITION".equalsIgnoreCase(rule.getSubjectType())) {
            item.setDccPositionCode(lookupCache.resolvePositionCode(rule.getSubjectId()));
        }
        return item;
    }

    private ApprovalMatrixItem buildApprovalMatrix(DccCategoryApprovalRouteDO route, LookupCache lookupCache) {
        ApprovalMatrixItem item = new ApprovalMatrixItem();
        if (route == null) {
            item.setEffectiveTime(null);
            item.setRemark(null);
            item.setRules(List.of());
            return item;
        }
        item.setEffectiveTime(route.getEffectiveTime());
        item.setRemark(route.getRemark());
        item.setRules(approvalRouteNodeMapper.selectListByRouteId(route.getId()).stream()
                .filter(node -> node.getStageNo() != null && (node.getStageNo() == 2 || node.getStageNo() == 3))
                .flatMap(node -> toApprovalMatrixRules(node, lookupCache).stream())
                .toList());
        return item;
    }

    private List<ApprovalMatrixRuleItem> toApprovalMatrixRules(DccCategoryApprovalRouteNodeDO node,
                                                               LookupCache lookupCache) {
        List<Long> sourceIds = readLongList(node.getCandidateSourceIds(), node.getCandidateSourceId());
        if (sourceIds.isEmpty()) {
            sourceIds = node.getSubjectId() == null ? List.of() : List.of(node.getSubjectId());
        }
        if (sourceIds.isEmpty()) {
            return List.of();
        }
        String subjectType = normalizeApprovalMatrixSubjectType(node);
        String stageType = Objects.equals(node.getStageNo(), 2) ? "SIGNOFF" : "APPROVAL";
        List<ApprovalMatrixRuleItem> items = new ArrayList<>();
        for (Long sourceId : sourceIds) {
            ApprovalMatrixRuleItem item = new ApprovalMatrixRuleItem();
            item.setStageType(stageType);
            item.setActive(Boolean.TRUE);
            item.setSubjectLabel(node.getSubjectLabel());
            item.setMarker(StringUtils.hasText(node.getMarker()) ? node.getMarker() : "▲");
            item.setSubjectType(subjectType);
            item.setSubjectName(node.getSubjectName());
            item.setSubjectDepartmentPath(node.getSubjectDepartmentPath());
            item.setRemark(node.getRuleRemark());
            if ("DCC_POSITION".equals(subjectType)) {
                item.setDccPositionCode(lookupCache.resolvePositionCode(sourceId));
            } else {
                fillSystemSubjectKey(item, subjectType, sourceId, lookupCache);
            }
            items.add(item);
        }
        return items;
    }

    private DistributionRuleItem toDistributionRuleItem(DccFileCategoryDistributionRuleDO rule,
                                                        LookupCache lookupCache) {
        DistributionRuleItem item = new DistributionRuleItem();
        item.setDepartmentPath(lookupCache.resolveDepartmentPath(rule.getDepartmentId()));
        item.setDistributionMedium(rule.getDistributionMedium());
        item.setActive(rule.getActive());
        return item;
    }

    private TrainingRuleItem toTrainingRuleItem(DccFileCategoryTrainingRuleDO rule, LookupCache lookupCache) {
        TrainingRuleItem item = new TrainingRuleItem();
        item.setDepartmentPath(lookupCache.resolveDepartmentPath(rule.getDepartmentId()));
        item.setActive(rule.getActive());
        return item;
    }

    private void validatePayload(DccAdminFullConfigPackage payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Invalid DCC admin full config package JSON");
        }
        if (!PACKAGE_VERSION.equals(payload.getPackageVersion())) {
            throw new IllegalArgumentException("Unsupported DCC admin full config package version: "
                    + payload.getPackageVersion());
        }
        if (payload.getApprovalPositions() == null) {
            throw new IllegalArgumentException("DCC admin full config package approvalPositions cannot be null");
        }
        if (payload.getDirectories() == null) {
            throw new IllegalArgumentException("DCC admin full config package directories cannot be null");
        }
        if (payload.getCategories() == null) {
            throw new IllegalArgumentException("DCC admin full config package categories cannot be null");
        }
    }

    private void importApprovalPositions(List<ApprovalPositionItem> positions, ImportContext context,
                                         LookupCache lookupCache) {
        for (ApprovalPositionItem item : positions) {
            validateApprovalPositionItem(item);
            DccApprovalPositionDO existing = approvalPositionMapper.selectOne(DccApprovalPositionDO::getCode, item.getCode());
            if (existing == null) {
                DccApprovalPositionDO position = DccApprovalPositionDO.builder()
                        .code(item.getCode())
                        .name(item.getName())
                        .active(Boolean.TRUE.equals(item.getActive()))
                        .source(item.getSource())
                        .remark(item.getRemark())
                        .build();
                approvalPositionMapper.insert(position);
                existing = position;
            } else {
                existing.setName(item.getName());
                existing.setActive(Boolean.TRUE.equals(item.getActive()));
                existing.setSource(item.getSource());
                existing.setRemark(item.getRemark());
                approvalPositionMapper.updateById(existing);
            }
            List<DccPositionAssignmentSaveReqVO> assignments = new ArrayList<>();
            for (PositionAssignmentItem assignmentItem : item.getAssignments()) {
                DccPositionAssignmentSaveReqVO reqVO = new DccPositionAssignmentSaveReqVO();
                reqVO.setAssignmentType(assignmentItem.getAssignmentType());
                reqVO.setActive(Boolean.TRUE.equals(assignmentItem.getActive()));
                reqVO.setChangeReason(assignmentItem.getChangeReason());
                if (StringUtils.hasText(assignmentItem.getUsername())) {
                    Long userId = lookupCache.resolveUserIdByUsername(assignmentItem.getUsername());
                    if (userId == null) {
                        throw new IllegalArgumentException("DCC admin full config package user not found: "
                                + assignmentItem.getUsername());
                    }
                    reqVO.setUserId(userId);
                }
                if (StringUtils.hasText(assignmentItem.getPostCode())) {
                    Long postId = lookupCache.resolvePostIdByCode(assignmentItem.getPostCode());
                    if (postId == null) {
                        throw new IllegalArgumentException("DCC admin full config package post code not found: "
                                + assignmentItem.getPostCode());
                    }
                    reqVO.setSystemPostId(postId);
                }
                assignments.add(reqVO);
            }
            approvalPositionAdminService.replaceAssignments(existing.getId(), assignments);
            context.importedApprovalPositionCodes.add(existing.getCode());
            context.approvalPositionCount = context.importedApprovalPositionCodes.size();
        }
    }

    private void importDirectories(List<DirectoryItem> directories, ImportContext context, LookupCache lookupCache) {
        List<DirectoryItem> ordered = directories.stream()
                .sorted(Comparator.comparing((DirectoryItem item) -> pathDepth(item.getPath()))
                        .thenComparing(DirectoryItem::getPath, Comparator.nullsLast(String::compareTo)))
                .toList();
        Map<String, Long> directoryIdByPath = new LinkedHashMap<>();
        for (DirectoryItem item : ordered) {
            validateDirectoryItem(item);
            DccFileDirectoryDO existing = lookupCache.resolveDirectoryByPath(item.getPath());
            DccDirectorySaveReqVO reqVO = new DccDirectorySaveReqVO();
            reqVO.setId(existing == null ? null : existing.getId());
            reqVO.setCode(item.getCode());
            reqVO.setName(item.getName());
            reqVO.setParentId(StringUtils.hasText(item.getParentPath())
                    ? directoryIdByPath.get(item.getParentPath()) : null);
            reqVO.setActive(Boolean.TRUE.equals(item.getActive()));
            reqVO.setSort(item.getSort());
            reqVO.setRemark(item.getRemark());
            Long directoryId;
            if (existing == null) {
                directoryId = directoryAdminService.createDirectory(reqVO);
            } else {
                directoryAdminService.updateDirectory(reqVO);
                directoryId = existing.getId();
            }
            directoryIdByPath.put(item.getPath(), directoryId);
            context.importedDirectoryPaths.add(item.getPath());
            context.directoryCount = context.importedDirectoryPaths.size();

            List<DccDirectoryAccessRuleSaveReqVO> accessRules = new ArrayList<>();
            for (DirectoryAccessRuleItem ruleItem : item.getAccessRules()) {
                DccDirectoryAccessRuleSaveReqVO ruleReq = new DccDirectoryAccessRuleSaveReqVO();
                ruleReq.setDirectoryId(directoryId);
                ruleReq.setSubjectType(ruleItem.getSubjectType());
                ruleReq.setSubjectId(resolveSystemSubjectId(ruleItem.getSubjectType(), ruleItem, lookupCache));
                ruleReq.setCanQuery(Boolean.TRUE.equals(ruleItem.getCanQuery()));
                ruleReq.setCanPreview(Boolean.TRUE.equals(ruleItem.getCanPreview()));
                ruleReq.setCanDownload(Boolean.TRUE.equals(ruleItem.getCanDownload()));
                ruleReq.setActive(Boolean.TRUE.equals(ruleItem.getActive()));
                ruleReq.setChangeReason(ruleItem.getChangeReason());
                accessRules.add(ruleReq);
            }
            directoryAdminService.replaceAccessRules(directoryId, accessRules);
            boolean accessRuleManuallyBound = Boolean.TRUE.equals(item.getAccessRuleManuallyBound());
            DccFileDirectoryDO importedDirectory = directoryMapper.selectById(directoryId);
            if (importedDirectory != null
                    && !Objects.equals(importedDirectory.getAccessRuleManuallyBound(), accessRuleManuallyBound)) {
                importedDirectory.setAccessRuleManuallyBound(accessRuleManuallyBound);
                directoryMapper.updateById(importedDirectory);
            }
            context.directoryAccessRuleCount += accessRules.size();
        }
    }

    private void importCategories(List<CategoryItem> categories, ImportContext context, LookupCache lookupCache) {
        Map<String, Long> categoryIdByCode = new LinkedHashMap<>();
        Map<String, Long> directoryIdByPath = lookupCache.getDirectoryIdByPath();
        List<CategoryItem> ordered = categories.stream()
                .sorted(Comparator.comparing((CategoryItem item) -> categoryDepth(item.getParentCode(), categories))
                        .thenComparing(CategoryItem::getCode, Comparator.nullsLast(String::compareTo)))
                .toList();
        for (CategoryItem item : ordered) {
            validateCategoryItem(item);
            DccFileCategoryDO existing = categoryMapper.selectOne(DccFileCategoryDO::getCode, item.getCode());
            DccFileCategorySaveReqVO categoryReq = new DccFileCategorySaveReqVO();
            categoryReq.setId(existing == null ? null : existing.getId());
            categoryReq.setCode(item.getCode());
            categoryReq.setName(item.getName());
            categoryReq.setParentId(StringUtils.hasText(item.getParentCode())
                    ? categoryIdByCode.get(item.getParentCode()) : null);
            categoryReq.setActive(Boolean.TRUE.equals(item.getActive()));
            categoryReq.setSort(item.getSort());
            categoryReq.setSource(item.getSource());
            categoryReq.setRemark(item.getRemark());
            categoryReq.setDescription(item.getDescription());
            categoryReq.setLifecycleStage(item.getLifecycleStage());
            categoryReq.setDistributionRequired(item.getDistributionRequired());
            categoryReq.setTrainingRequired(item.getTrainingRequired());
            Long categoryId;
            if (existing == null) {
                categoryId = fileCategoryAdminService.createCategory(categoryReq);
            } else {
                fileCategoryAdminService.updateCategory(categoryReq);
                categoryId = existing.getId();
            }
            categoryIdByCode.put(item.getCode(), categoryId);
            context.importedCategoryCodes.add(item.getCode());
            context.categoryCount = context.importedCategoryCodes.size();

            if (item.getDirectoryBinding() != null) {
                Long directoryId = directoryIdByPath.get(item.getDirectoryBinding().getDirectoryPath());
                if (directoryId == null) {
                    throw new IllegalArgumentException("DCC admin full config package directory path not found: "
                            + item.getDirectoryBinding().getDirectoryPath());
                }
                cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDirectoryBindingSaveReqVO bindingReq =
                        new cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDirectoryBindingSaveReqVO();
                bindingReq.setDirectoryId(directoryId);
                bindingReq.setActive(Boolean.TRUE.equals(item.getDirectoryBinding().getActive()));
                fileCategoryAdminService.bindDirectory(categoryId, bindingReq);
            }

            List<DccCategoryPermissionRuleSaveReqVO> permissionRules = new ArrayList<>();
            for (PermissionRuleItem ruleItem : item.getPermissionRules()) {
                DccCategoryPermissionRuleSaveReqVO reqVO = new DccCategoryPermissionRuleSaveReqVO();
                reqVO.setActionType(ruleItem.getActionType());
                reqVO.setSubjectType(ruleItem.getSubjectType());
                reqVO.setSubjectId(resolveSystemSubjectId(ruleItem.getSubjectType(), ruleItem, lookupCache));
                reqVO.setScopeType(ruleItem.getScopeType());
                reqVO.setActive(Boolean.TRUE.equals(ruleItem.getActive()));
                reqVO.setRemark(ruleItem.getRemark());
                permissionRules.add(reqVO);
            }
            permissionAdminService.replacePermissionRules(categoryId, permissionRules);
            context.permissionRuleCount += permissionRules.size();

            DccCategoryApprovalMatrixSaveReqVO approvalMatrixReq = new DccCategoryApprovalMatrixSaveReqVO();
            ApprovalMatrixItem approvalMatrix = item.getApprovalMatrix() == null ? new ApprovalMatrixItem()
                    : item.getApprovalMatrix();
            approvalMatrixReq.setEffectiveTime(approvalMatrix.getEffectiveTime() == null
                    ? LocalDateTime.now().plusSeconds(1) : approvalMatrix.getEffectiveTime());
            approvalMatrixReq.setRemark(approvalMatrix.getRemark());
            List<DccCategoryApprovalMatrixSaveReqVO.Rule> approvalRules = new ArrayList<>();
            for (ApprovalMatrixRuleItem ruleItem : approvalMatrix.getRules()) {
                DccCategoryApprovalMatrixSaveReqVO.Rule reqRule = new DccCategoryApprovalMatrixSaveReqVO.Rule();
                reqRule.setStageType(ruleItem.getStageType());
                reqRule.setActive(Boolean.TRUE.equals(ruleItem.getActive()));
                reqRule.setSubjectLabel(ruleItem.getSubjectLabel());
                reqRule.setMarker(ruleItem.getMarker());
                reqRule.setSubjectType(ruleItem.getSubjectType());
                reqRule.setSubjectName(ruleItem.getSubjectName());
                reqRule.setSubjectDepartmentPath(ruleItem.getSubjectDepartmentPath());
                reqRule.setRemark(ruleItem.getRemark());
                if ("DCC_POSITION".equalsIgnoreCase(ruleItem.getSubjectType())) {
                    Long positionId = lookupCache.resolvePositionIdByCode(ruleItem.getDccPositionCode());
                    if (positionId == null) {
                        throw new IllegalArgumentException("DCC admin full config package DCC position code not found: "
                                + ruleItem.getDccPositionCode());
                    }
                    reqRule.setSubjectId(positionId);
                } else {
                    reqRule.setSubjectId(resolveSystemSubjectId(ruleItem.getSubjectType(), ruleItem, lookupCache));
                }
                approvalRules.add(reqRule);
            }
            approvalMatrixReq.setRules(approvalRules);
            if (!approvalRules.isEmpty()) {
                approvalMatrixAdminService.importApprovalMatrix(categoryId, approvalMatrixReq);
            } else {
                approvalMatrixAdminService.deleteApprovalMatrix(categoryId);
            }
            context.approvalMatrixRuleCount += approvalRules.size();

            DccCategoryViewMatrixSaveReqVO viewMatrixReq = new DccCategoryViewMatrixSaveReqVO();
            List<DccCategoryViewMatrixSaveReqVO.Rule> viewRules = new ArrayList<>();
            ViewMatrixItem viewMatrix = item.getViewMatrix() == null ? new ViewMatrixItem() : item.getViewMatrix();
            for (ViewMatrixRuleItem ruleItem : viewMatrix.getRules()) {
                DccCategoryViewMatrixSaveReqVO.Rule reqRule = new DccCategoryViewMatrixSaveReqVO.Rule();
                reqRule.setExcelFileName(ruleItem.getExcelFileName());
                reqRule.setExcelRowNo(ruleItem.getExcelRowNo());
                reqRule.setExcelColumnLetter(ruleItem.getExcelColumnLetter());
                reqRule.setSubjectLabel(ruleItem.getSubjectLabel());
                reqRule.setSubjectTopHeader(ruleItem.getSubjectTopHeader());
                reqRule.setSubjectSubHeader(ruleItem.getSubjectSubHeader());
                reqRule.setMarker(ruleItem.getMarker());
                reqRule.setScopeType(ruleItem.getScopeType());
                reqRule.setSubjectType(ruleItem.getSubjectType());
                reqRule.setActive(Boolean.TRUE.equals(ruleItem.getActive()));
                reqRule.setRemark(ruleItem.getRemark());
                if ("DCC_POSITION".equalsIgnoreCase(ruleItem.getSubjectType())) {
                    Long positionId = lookupCache.resolvePositionIdByCode(ruleItem.getDccPositionCode());
                    if (positionId == null) {
                        throw new IllegalArgumentException("DCC admin full config package DCC position code not found: "
                                + ruleItem.getDccPositionCode());
                    }
                    reqRule.setSubjectId(positionId);
                } else {
                    reqRule.setSubjectId(resolveSystemSubjectId(ruleItem.getSubjectType(), ruleItem, lookupCache));
                }
                viewRules.add(reqRule);
            }
            viewMatrixReq.setRules(viewRules);
            viewMatrixAdminService.importViewMatrix(categoryId, viewMatrixReq);
            context.viewMatrixRuleCount += viewRules.size();

            List<DccCategoryDistributionRuleSaveReqVO> distributionRules = new ArrayList<>();
            for (DistributionRuleItem ruleItem : item.getDistributionRules()) {
                DccCategoryDistributionRuleSaveReqVO reqVO = new DccCategoryDistributionRuleSaveReqVO();
                reqVO.setDepartmentId(resolveDepartmentIdByPath(ruleItem.getDepartmentPath(), lookupCache));
                reqVO.setDistributionMedium(ruleItem.getDistributionMedium());
                reqVO.setActive(Boolean.TRUE.equals(ruleItem.getActive()));
                distributionRules.add(reqVO);
            }
            distributionRuleAdminService.importDistributionRules(categoryId, distributionRules);
            context.distributionRuleCount += distributionRules.size();

            List<DccCategoryTrainingRuleSaveReqVO> trainingRules = new ArrayList<>();
            for (TrainingRuleItem ruleItem : item.getTrainingRules()) {
                DccCategoryTrainingRuleSaveReqVO reqVO = new DccCategoryTrainingRuleSaveReqVO();
                reqVO.setDepartmentId(resolveDepartmentIdByPath(ruleItem.getDepartmentPath(), lookupCache));
                reqVO.setActive(Boolean.TRUE.equals(ruleItem.getActive()));
                trainingRules.add(reqVO);
            }
            trainingRuleAdminService.importTrainingRules(categoryId, trainingRules);
            context.trainingRuleCount += trainingRules.size();
        }
    }

    private void cleanupRemovedManagedScope(ManagedScope previousScope, ImportContext context,
                                            LookupCache lookupCache) {
        if (!previousScope.scoped()) {
            return;
        }
        for (String categoryCode : previousScope.categoryCodes()) {
            if (context.importedCategoryCodes.contains(categoryCode)) {
                continue;
            }
            DccFileCategoryDO category = categoryMapper.selectOne(DccFileCategoryDO::getCode, categoryCode);
            if (category == null || isCategoryReferenced(category.getId())) {
                continue;
            }
            fileCategoryAdminService.deleteCategory(category.getId());
            context.removedCategoryCount++;
        }

        for (String positionCode : previousScope.approvalPositionCodes()) {
            if (context.importedApprovalPositionCodes.contains(positionCode)) {
                continue;
            }
            DccApprovalPositionDO position = approvalPositionMapper.selectOne(DccApprovalPositionDO::getCode, positionCode);
            if (position == null || isApprovalPositionReferenced(position.getId())) {
                continue;
            }
            positionAssignmentMapper.delete(DccPositionAssignmentDO::getPositionId, position.getId());
            approvalPositionMapper.deleteById(position.getId());
            context.removedApprovalPositionCount++;
        }

        List<String> removableDirectoryPaths = previousScope.directoryPaths().stream()
                .filter(path -> !context.importedDirectoryPaths.contains(path))
                .sorted(Comparator.comparingInt(this::pathDepth).reversed())
                .toList();
        for (String directoryPath : removableDirectoryPaths) {
            DccFileDirectoryDO directory = resolveDirectoryByPath(directoryPath);
            if (directory == null || isDirectoryReferenced(directory.getId())
                    || categoryDirectoryBindingMapper.selectCount(DccCategoryDirectoryBindingDO::getDirectoryId,
                    directory.getId()) > 0) {
                continue;
            }
            directoryAdminService.deleteAccessRules(directory.getId());
            directoryMapper.deleteById(directory.getId());
            context.removedDirectoryCount++;
        }
    }

    private void persistManagedScope(ImportContext context) {
        DccAdminFullConfigManagedScopeDO existing = managedScopeMapper.selectCurrentScope();
        if (existing == null) {
            DccAdminFullConfigManagedScopeDO scope = DccAdminFullConfigManagedScopeDO.builder()
                    .categoryCodesJson(JsonUtils.toJsonString(new ArrayList<>(context.importedCategoryCodes)))
                    .directoryPathsJson(JsonUtils.toJsonString(new ArrayList<>(context.importedDirectoryPaths)))
                    .approvalPositionCodesJson(JsonUtils.toJsonString(new ArrayList<>(context.importedApprovalPositionCodes)))
                    .build();
            scope.setTenantId(TenantContextHolder.getRequiredTenantId());
            managedScopeMapper.insert(scope);
            return;
        }
        existing.setCategoryCodesJson(JsonUtils.toJsonString(new ArrayList<>(context.importedCategoryCodes)));
        existing.setDirectoryPathsJson(JsonUtils.toJsonString(new ArrayList<>(context.importedDirectoryPaths)));
        existing.setApprovalPositionCodesJson(JsonUtils.toJsonString(new ArrayList<>(context.importedApprovalPositionCodes)));
        managedScopeMapper.updateById(existing);
    }

    private ManagedScope loadManagedScope() {
        DccAdminFullConfigManagedScopeDO scope = managedScopeMapper.selectCurrentScope();
        if (scope == null) {
            return ManagedScope.unscoped();
        }
        return ManagedScope.scoped(
                JsonUtils.parseArray(scope.getCategoryCodesJson(), String.class),
                JsonUtils.parseArray(scope.getDirectoryPathsJson(), String.class),
                JsonUtils.parseArray(scope.getApprovalPositionCodesJson(), String.class));
    }

    private boolean isCategoryReferenced(Long categoryId) {
        return categoryId != null && (controlledFileMapper.selectCount(
                cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO::getCategoryId, categoryId) > 0
                || controlledFileMasterMapper.selectCount(
                cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO::getCategoryId,
                categoryId) > 0);
    }

    private boolean isDirectoryReferenced(Long directoryId) {
        return directoryId != null && (controlledFileMapper.selectCount(
                cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO::getDirectoryId, directoryId) > 0
                || controlledFileMasterMapper.selectCount(
                cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO::getDirectoryId,
                directoryId) > 0);
    }

    private boolean isApprovalPositionReferenced(Long positionId) {
        if (positionId == null) {
            return false;
        }
        boolean routeReferenced = approvalRouteNodeMapper.selectList().stream()
                .filter(node -> StringUtils.hasText(node.getCandidateSourceType()))
                .filter(node -> "POSITION".equalsIgnoreCase(node.getCandidateSourceType()))
                .anyMatch(node -> readLongList(node.getCandidateSourceIds(), node.getCandidateSourceId())
                        .contains(positionId));
        if (routeReferenced) {
            return true;
        }
        return routeSnapshotMapper.selectList().stream()
                .filter(snapshot -> "POSITION".equalsIgnoreCase(snapshot.getCandidateSourceType()))
                .anyMatch(snapshot -> readLongList(snapshot.getCandidateSourceIds(), snapshot.getCandidateSourceId())
                        .contains(positionId));
    }

    private DccAdminFullConfigPackageImportRespVO buildImportResp(ImportContext context) {
        DccAdminFullConfigPackageImportRespVO respVO = new DccAdminFullConfigPackageImportRespVO();
        respVO.setApprovalPositionCount(context.approvalPositionCount);
        respVO.setDirectoryCount(context.directoryCount);
        respVO.setDirectoryAccessRuleCount(context.directoryAccessRuleCount);
        respVO.setCategoryCount(context.categoryCount);
        respVO.setPermissionRuleCount(context.permissionRuleCount);
        respVO.setApprovalMatrixRuleCount(context.approvalMatrixRuleCount);
        respVO.setViewMatrixRuleCount(context.viewMatrixRuleCount);
        respVO.setDistributionRuleCount(context.distributionRuleCount);
        respVO.setTrainingRuleCount(context.trainingRuleCount);
        respVO.setRemovedApprovalPositionCount(context.removedApprovalPositionCount);
        respVO.setRemovedDirectoryCount(context.removedDirectoryCount);
        respVO.setRemovedCategoryCount(context.removedCategoryCount);
        return respVO;
    }

    private void validateApprovalPositionItem(ApprovalPositionItem item) {
        if (item == null || !StringUtils.hasText(item.getCode()) || !StringUtils.hasText(item.getName())) {
            throw new IllegalArgumentException("DCC admin full config package approval position is invalid");
        }
        if (item.getAssignments() == null) {
            throw new IllegalArgumentException("DCC admin full config package approval position assignments cannot be null: "
                    + item.getCode());
        }
        if (DccUploaderDerivedPositionSupport.isUploaderDerivedPositionName(item.getName()) && !item.getAssignments().isEmpty()) {
            throw exception(APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED, item.getName());
        }
    }

    private void validateDirectoryItem(DirectoryItem item) {
        if (item == null || !StringUtils.hasText(item.getPath()) || !StringUtils.hasText(item.getCode())
                || !StringUtils.hasText(item.getName())) {
            throw new IllegalArgumentException("DCC admin full config package directory is invalid");
        }
        if (item.getAccessRules() == null) {
            throw new IllegalArgumentException("DCC admin full config package directory accessRules cannot be null: "
                    + item.getPath());
        }
    }

    private void validateCategoryItem(CategoryItem item) {
        if (item == null || !StringUtils.hasText(item.getCode()) || !StringUtils.hasText(item.getName())
                || !StringUtils.hasText(item.getLifecycleStage())) {
            throw new IllegalArgumentException("DCC admin full config package category is invalid");
        }
        if (item.getPermissionRules() == null || item.getDistributionRules() == null || item.getTrainingRules() == null) {
            throw new IllegalArgumentException("DCC admin full config package category rules cannot be null: "
                    + item.getCode());
        }
    }

    private Long resolveSystemSubjectId(String subjectType, SubjectKeyCarrier subjectKey, LookupCache lookupCache) {
        if ("USER".equalsIgnoreCase(subjectType)) {
            if (!StringUtils.hasText(subjectKey.getUsername())) {
                throw new IllegalArgumentException("DCC admin full config package user subject username is required");
            }
            Long userId = lookupCache.resolveUserIdByUsername(subjectKey.getUsername());
            if (userId == null) {
                throw new IllegalArgumentException("DCC admin full config package user not found: "
                        + subjectKey.getUsername());
            }
            return userId;
        }
        if ("DEPT".equalsIgnoreCase(subjectType)) {
            return resolveDepartmentIdByPath(subjectKey.getDepartmentPath(), lookupCache);
        }
        if ("POSITION".equalsIgnoreCase(subjectType) || "POST".equalsIgnoreCase(subjectType)) {
            if (!StringUtils.hasText(subjectKey.getPostCode())) {
                throw new IllegalArgumentException("DCC admin full config package post code is required");
            }
            Long postId = lookupCache.resolvePostIdByCode(subjectKey.getPostCode());
            if (postId == null) {
                throw new IllegalArgumentException("DCC admin full config package post code not found: "
                        + subjectKey.getPostCode());
            }
            return postId;
        }
        if ("ROLE".equalsIgnoreCase(subjectType)) {
            if (!StringUtils.hasText(subjectKey.getRoleCode())) {
                throw new IllegalArgumentException("DCC admin full config package role code is required");
            }
            Long roleId = lookupCache.resolveRoleIdByCode(subjectKey.getRoleCode());
            if (roleId == null) {
                throw new IllegalArgumentException("DCC admin full config package role code not found: "
                        + subjectKey.getRoleCode());
            }
            return roleId;
        }
        throw new IllegalArgumentException("DCC admin full config package subject type unsupported: " + subjectType);
    }

    private Long resolveDepartmentIdByPath(String departmentPath, LookupCache lookupCache) {
        if (!StringUtils.hasText(departmentPath)) {
            throw new IllegalArgumentException("DCC admin full config package department path is required");
        }
        Long departmentId = lookupCache.resolveDepartmentIdByPath(departmentPath);
        if (departmentId == null) {
            throw new IllegalArgumentException("DCC admin full config package department path not found: " + departmentPath);
        }
        return departmentId;
    }

    private void fillSystemSubjectKey(DirectoryAccessRuleItem item, String subjectType, Long subjectId,
                                      LookupCache lookupCache) {
        SubjectKeyValue value = resolveSystemSubjectKeyValue(subjectType, subjectId, lookupCache);
        item.setUsername(value.username);
        item.setDepartmentPath(value.departmentPath);
        item.setPostCode(value.postCode);
        item.setRoleCode(value.roleCode);
    }

    private void fillSystemSubjectKey(PermissionRuleItem item, String subjectType, Long subjectId,
                                      LookupCache lookupCache) {
        SubjectKeyValue value = resolveSystemSubjectKeyValue(subjectType, subjectId, lookupCache);
        item.setUsername(value.username);
        item.setDepartmentPath(value.departmentPath);
        item.setPostCode(value.postCode);
        item.setRoleCode(value.roleCode);
    }

    private void fillSystemSubjectKey(ApprovalMatrixRuleItem item, String subjectType, Long subjectId,
                                      LookupCache lookupCache) {
        SubjectKeyValue value = resolveSystemSubjectKeyValue(subjectType, subjectId, lookupCache);
        item.setUsername(value.username);
        item.setDepartmentPath(value.departmentPath);
        item.setPostCode(value.postCode);
        item.setRoleCode(value.roleCode);
    }

    private void fillSystemSubjectKey(ViewMatrixRuleItem item, String subjectType, Long subjectId,
                                      LookupCache lookupCache) {
        SubjectKeyValue value = resolveSystemSubjectKeyValue(subjectType, subjectId, lookupCache);
        item.setUsername(value.username);
        item.setDepartmentPath(value.departmentPath);
        item.setPostCode(value.postCode);
        item.setRoleCode(value.roleCode);
    }

    private SubjectKeyValue resolveSystemSubjectKeyValue(String subjectType, Long subjectId,
                                                         LookupCache lookupCache) {
        if (!StringUtils.hasText(subjectType) || subjectId == null) {
            return SubjectKeyValue.empty();
        }
        if ("USER".equalsIgnoreCase(subjectType)) {
            return new SubjectKeyValue(lookupCache.resolveUsername(subjectId), null, null, null);
        }
        if ("DEPT".equalsIgnoreCase(subjectType)) {
            return new SubjectKeyValue(null, lookupCache.resolveDepartmentPath(subjectId), null, null);
        }
        if ("POSITION".equalsIgnoreCase(subjectType) || "POST".equalsIgnoreCase(subjectType)) {
            return new SubjectKeyValue(null, null, lookupCache.resolvePostCode(subjectId), null);
        }
        if ("ROLE".equalsIgnoreCase(subjectType)) {
            return new SubjectKeyValue(null, null, null, lookupCache.resolveRoleCode(subjectId));
        }
        return SubjectKeyValue.empty();
    }

    private String resolveUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserDO user = adminUserMapper.selectById(userId);
        return user == null ? null : user.getUsername();
    }

    private String resolvePostCode(Long postId) {
        if (postId == null) {
            return null;
        }
        PostDO post = postMapper.selectById(postId);
        return post == null ? null : post.getCode();
    }

    private String resolveRoleCode(Long roleId) {
        if (roleId == null) {
            return null;
        }
        RoleDO role = roleMapper.selectById(roleId);
        return role == null ? null : role.getCode();
    }

    private String resolvePositionCode(Long positionId) {
        if (positionId == null) {
            return null;
        }
        DccApprovalPositionDO position = approvalPositionMapper.selectById(positionId);
        return position == null ? null : position.getCode();
    }

    private String resolveDirectoryPath(Long directoryId) {
        if (directoryId == null) {
            return null;
        }
        return buildDirectoryPathMap().get(directoryId);
    }

    private String resolveDepartmentPath(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return buildDepartmentPathMap().entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().getId(), departmentId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private Map<String, DeptDO> buildDepartmentPathMap() {
        List<DeptDO> departments = deptMapper.selectList();
        Map<Long, DeptDO> deptById = convertMap(departments, DeptDO::getId);
        Map<String, DeptDO> result = new LinkedHashMap<>();
        for (DeptDO dept : departments) {
            result.put(buildDepartmentPath(dept, deptById), dept);
        }
        return result;
    }

    private String buildDepartmentPath(DeptDO dept, Map<Long, DeptDO> deptById) {
        List<String> segments = new ArrayList<>();
        DeptDO current = dept;
        while (current != null) {
            segments.add(current.getName());
            if (current.getParentId() == null || Objects.equals(current.getParentId(), DeptDO.PARENT_ID_ROOT)) {
                break;
            }
            current = deptById.get(current.getParentId());
        }
        java.util.Collections.reverse(segments);
        return String.join("/", segments);
    }

    private Map<Long, String> buildDirectoryPathMap() {
        List<DccFileDirectoryDO> directories = directoryMapper.selectList();
        Map<Long, DccFileDirectoryDO> directoryMap = convertMap(directories, DccFileDirectoryDO::getId);
        Map<Long, String> pathMap = new LinkedHashMap<>();
        for (DccFileDirectoryDO directory : directories) {
            pathMap.put(directory.getId(), buildDirectoryPath(directory, directoryMap));
        }
        return pathMap;
    }

    private Map<String, Long> buildDirectoryIdByPath() {
        Map<Long, String> pathMap = buildDirectoryPathMap();
        Map<String, Long> result = new LinkedHashMap<>();
        pathMap.forEach((id, path) -> result.put(path, id));
        return result;
    }

    private DccFileDirectoryDO resolveDirectoryByPath(String path) {
        Map<Long, String> pathMap = buildDirectoryPathMap();
        for (Map.Entry<Long, String> entry : pathMap.entrySet()) {
            if (Objects.equals(entry.getValue(), path)) {
                return directoryMapper.selectById(entry.getKey());
            }
        }
        return null;
    }

    private String buildDirectoryPath(DccFileDirectoryDO directory, Map<Long, DccFileDirectoryDO> directoryMap) {
        List<String> segments = new ArrayList<>();
        DccFileDirectoryDO current = directory;
        while (current != null) {
            segments.add(current.getName());
            if (current.getParentId() == null || Objects.equals(current.getParentId(), 0L)) {
                break;
            }
            current = directoryMap.get(current.getParentId());
        }
        java.util.Collections.reverse(segments);
        return String.join("/", segments);
    }

    private String buildParentPath(DccFileDirectoryDO directory, Map<Long, DccFileDirectoryDO> directoryMap) {
        if (directory.getParentId() == null || Objects.equals(directory.getParentId(), 0L)) {
            return null;
        }
        DccFileDirectoryDO parent = directoryMap.get(directory.getParentId());
        return parent == null ? null : buildDirectoryPath(parent, directoryMap);
    }

    private String resolveParentCategoryCode(DccFileCategoryDO category, Map<Long, DccFileCategoryDO> categoryMap) {
        if (category.getParentId() == null) {
            return null;
        }
        DccFileCategoryDO parent = categoryMap.get(category.getParentId());
        return parent == null ? null : parent.getCode();
    }

    private String normalizeApprovalMatrixSubjectType(DccCategoryApprovalRouteNodeDO node) {
        String subjectType = upper(node.getSubjectType());
        if ("POSITION".equals(subjectType)) {
            return "DCC_POSITION";
        }
        if (StringUtils.hasText(subjectType)) {
            return subjectType;
        }
        String candidateType = upper(node.getCandidateSourceType());
        return "POSITION".equals(candidateType) ? "DCC_POSITION" : candidateType;
    }

    private List<Long> readLongList(String values, Long fallbackId) {
        if (StringUtils.hasText(values)) {
            return java.util.Arrays.stream(values.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::valueOf)
                    .toList();
        }
        return fallbackId == null ? List.of() : List.of(fallbackId);
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private int pathDepth(String path) {
        if (!StringUtils.hasText(path)) {
            return 0;
        }
        return path.split("/").length;
    }

    private int categoryDepth(String parentCode, Collection<CategoryItem> categories) {
        if (!StringUtils.hasText(parentCode)) {
            return 0;
        }
        Map<String, String> parentByCode = categories.stream()
                .collect(Collectors.toMap(CategoryItem::getCode, CategoryItem::getParentCode, (left, right) -> left));
        int depth = 0;
        String cursor = parentCode;
        while (StringUtils.hasText(cursor)) {
            depth++;
            cursor = parentByCode.get(cursor);
        }
        return depth;
    }

    private final class LookupCache {
        private final Map<Long, String> usernameById = new HashMap<>();
        private final Map<String, Long> userIdByUsername = new HashMap<>();
        private final Map<Long, String> postCodeById = new HashMap<>();
        private final Map<String, Long> postIdByCode = new HashMap<>();
        private final Map<Long, String> roleCodeById = new HashMap<>();
        private final Map<String, Long> roleIdByCode = new HashMap<>();
        private final Map<Long, String> positionCodeById = new HashMap<>();
        private final Map<String, Long> positionIdByCode = new HashMap<>();
        private List<DccFileDirectoryDO> directories;
        private Map<Long, DccFileDirectoryDO> directoryById;
        private Map<Long, String> directoryPathById;
        private Map<String, Long> directoryIdByPath;
        private Map<String, DccFileDirectoryDO> directoryByPath;
        private Map<Long, String> departmentPathById;
        private Map<String, Long> departmentIdByPath;

        private List<DccFileDirectoryDO> getDirectories() {
            ensureDirectoryCache();
            return directories;
        }

        private Map<Long, DccFileDirectoryDO> getDirectoryById() {
            ensureDirectoryCache();
            return directoryById;
        }

        private Map<Long, String> getDirectoryPathById() {
            ensureDirectoryCache();
            return directoryPathById;
        }

        private Map<String, Long> getDirectoryIdByPath() {
            ensureDirectoryCache();
            return directoryIdByPath;
        }

        private DccFileDirectoryDO resolveDirectoryByPath(String path) {
            ensureDirectoryCache();
            return directoryByPath.get(path);
        }

        private String resolveDirectoryPath(Long directoryId) {
            if (directoryId == null) {
                return null;
            }
            ensureDirectoryCache();
            return directoryPathById.get(directoryId);
        }

        private String resolveDepartmentPath(Long departmentId) {
            if (departmentId == null) {
                return null;
            }
            ensureDepartmentCache();
            return departmentPathById.get(departmentId);
        }

        private Long resolveDepartmentIdByPath(String departmentPath) {
            ensureDepartmentCache();
            return departmentIdByPath.get(departmentPath);
        }

        private String resolveUsername(Long userId) {
            if (userId == null) {
                return null;
            }
            if (usernameById.containsKey(userId)) {
                return usernameById.get(userId);
            }
            AdminUserDO user = adminUserMapper.selectById(userId);
            String username = user == null ? null : user.getUsername();
            usernameById.put(userId, username);
            if (user != null) {
                userIdByUsername.putIfAbsent(user.getUsername(), user.getId());
            }
            return username;
        }

        private Long resolveUserIdByUsername(String username) {
            if (!StringUtils.hasText(username)) {
                return null;
            }
            if (userIdByUsername.containsKey(username)) {
                return userIdByUsername.get(username);
            }
            AdminUserDO user = adminUserService.getUserByUsername(username);
            Long userId = user == null ? null : user.getId();
            userIdByUsername.put(username, userId);
            if (user != null) {
                usernameById.putIfAbsent(user.getId(), user.getUsername());
            }
            return userId;
        }

        private String resolvePostCode(Long postId) {
            if (postId == null) {
                return null;
            }
            if (postCodeById.containsKey(postId)) {
                return postCodeById.get(postId);
            }
            PostDO post = postMapper.selectById(postId);
            String postCode = post == null ? null : post.getCode();
            postCodeById.put(postId, postCode);
            if (post != null) {
                postIdByCode.putIfAbsent(post.getCode(), post.getId());
            }
            return postCode;
        }

        private Long resolvePostIdByCode(String postCode) {
            if (!StringUtils.hasText(postCode)) {
                return null;
            }
            if (postIdByCode.containsKey(postCode)) {
                return postIdByCode.get(postCode);
            }
            PostDO post = postMapper.selectByCode(postCode);
            Long postId = post == null ? null : post.getId();
            postIdByCode.put(postCode, postId);
            if (post != null) {
                postCodeById.putIfAbsent(post.getId(), post.getCode());
            }
            return postId;
        }

        private String resolveRoleCode(Long roleId) {
            if (roleId == null) {
                return null;
            }
            if (roleCodeById.containsKey(roleId)) {
                return roleCodeById.get(roleId);
            }
            RoleDO role = roleMapper.selectById(roleId);
            String roleCode = role == null ? null : role.getCode();
            roleCodeById.put(roleId, roleCode);
            if (role != null) {
                roleIdByCode.putIfAbsent(role.getCode(), role.getId());
            }
            return roleCode;
        }

        private Long resolveRoleIdByCode(String roleCode) {
            if (!StringUtils.hasText(roleCode)) {
                return null;
            }
            if (roleIdByCode.containsKey(roleCode)) {
                return roleIdByCode.get(roleCode);
            }
            RoleDO role = roleMapper.selectByCode(roleCode);
            Long roleId = role == null ? null : role.getId();
            roleIdByCode.put(roleCode, roleId);
            if (role != null) {
                roleCodeById.putIfAbsent(role.getId(), role.getCode());
            }
            return roleId;
        }

        private String resolvePositionCode(Long positionId) {
            if (positionId == null) {
                return null;
            }
            if (positionCodeById.containsKey(positionId)) {
                return positionCodeById.get(positionId);
            }
            DccApprovalPositionDO position = approvalPositionMapper.selectById(positionId);
            String positionCode = position == null ? null : position.getCode();
            positionCodeById.put(positionId, positionCode);
            if (position != null) {
                positionIdByCode.putIfAbsent(position.getCode(), position.getId());
            }
            return positionCode;
        }

        private Long resolvePositionIdByCode(String positionCode) {
            if (!StringUtils.hasText(positionCode)) {
                return null;
            }
            if (positionIdByCode.containsKey(positionCode)) {
                return positionIdByCode.get(positionCode);
            }
            DccApprovalPositionDO position = approvalPositionMapper.selectOne(DccApprovalPositionDO::getCode, positionCode);
            Long positionId = position == null ? null : position.getId();
            positionIdByCode.put(positionCode, positionId);
            if (position != null) {
                positionCodeById.putIfAbsent(position.getId(), position.getCode());
            }
            return positionId;
        }

        private void ensureDirectoryCache() {
            if (directoryPathById != null) {
                return;
            }
            List<DccFileDirectoryDO> loadedDirectories = directoryMapper.selectList();
            Map<Long, DccFileDirectoryDO> loadedDirectoryById = convertMap(loadedDirectories, DccFileDirectoryDO::getId);
            Map<Long, String> loadedDirectoryPathById = new LinkedHashMap<>();
            Map<String, Long> loadedDirectoryIdByPath = new LinkedHashMap<>();
            Map<String, DccFileDirectoryDO> loadedDirectoryByPath = new LinkedHashMap<>();
            for (DccFileDirectoryDO directory : loadedDirectories) {
                String path = buildDirectoryPath(directory, loadedDirectoryById);
                loadedDirectoryPathById.put(directory.getId(), path);
                loadedDirectoryIdByPath.put(path, directory.getId());
                loadedDirectoryByPath.put(path, directory);
            }
            directories = loadedDirectories;
            directoryById = loadedDirectoryById;
            directoryPathById = loadedDirectoryPathById;
            directoryIdByPath = loadedDirectoryIdByPath;
            directoryByPath = loadedDirectoryByPath;
        }

        private void ensureDepartmentCache() {
            if (departmentPathById != null) {
                return;
            }
            List<DeptDO> departments = deptMapper.selectList();
            Map<Long, DeptDO> deptById = convertMap(departments, DeptDO::getId);
            Map<Long, String> loadedDepartmentPathById = new LinkedHashMap<>();
            Map<String, Long> loadedDepartmentIdByPath = new LinkedHashMap<>();
            for (DeptDO dept : departments) {
                String path = buildDepartmentPath(dept, deptById);
                loadedDepartmentPathById.put(dept.getId(), path);
                loadedDepartmentIdByPath.put(path, dept.getId());
            }
            departmentPathById = loadedDepartmentPathById;
            departmentIdByPath = loadedDepartmentIdByPath;
        }
    }

    private interface SubjectKeyCarrier {
        String getUsername();
        String getDepartmentPath();
        String getPostCode();
        String getRoleCode();
    }

    private static class SubjectKeyValue {
        private final String username;
        private final String departmentPath;
        private final String postCode;
        private final String roleCode;

        private SubjectKeyValue(String username, String departmentPath, String postCode, String roleCode) {
            this.username = username;
            this.departmentPath = departmentPath;
            this.postCode = postCode;
            this.roleCode = roleCode;
        }

        private static SubjectKeyValue empty() {
            return new SubjectKeyValue(null, null, null, null);
        }
    }

    private static class ImportContext {
        private final Set<String> importedApprovalPositionCodes = new LinkedHashSet<>();
        private final Set<String> importedDirectoryPaths = new LinkedHashSet<>();
        private final Set<String> importedCategoryCodes = new LinkedHashSet<>();
        private int approvalPositionCount;
        private int directoryCount;
        private int directoryAccessRuleCount;
        private int categoryCount;
        private int permissionRuleCount;
        private int approvalMatrixRuleCount;
        private int viewMatrixRuleCount;
        private int distributionRuleCount;
        private int trainingRuleCount;
        private int removedApprovalPositionCount;
        private int removedDirectoryCount;
        private int removedCategoryCount;
    }

    private record ManagedScope(boolean scoped, Set<String> categoryCodes, Set<String> directoryPaths,
                                Set<String> approvalPositionCodes) {

        private static ManagedScope unscoped() {
            return new ManagedScope(false, Set.of(), Set.of(), Set.of());
        }

        private static ManagedScope scoped(List<String> categoryCodes, List<String> directoryPaths,
                                           List<String> approvalPositionCodes) {
            return new ManagedScope(true,
                    new LinkedHashSet<>(categoryCodes == null ? List.of() : categoryCodes),
                    new LinkedHashSet<>(directoryPaths == null ? List.of() : directoryPaths),
                    new LinkedHashSet<>(approvalPositionCodes == null ? List.of() : approvalPositionCodes));
        }

        private boolean includeCategory(String categoryCode) {
            return !scoped || categoryCodes.contains(categoryCode);
        }

        private boolean includeDirectory(String directoryPath) {
            return !scoped || directoryPaths.contains(directoryPath);
        }

        private boolean includeApprovalPosition(String approvalPositionCode) {
            return !scoped || approvalPositionCodes.contains(approvalPositionCode);
        }
    }

    @Data
    public static class DccAdminFullConfigPackage {
        private String packageVersion;
        private List<ApprovalPositionItem> approvalPositions = new ArrayList<>();
        private List<DirectoryItem> directories = new ArrayList<>();
        private List<CategoryItem> categories = new ArrayList<>();
    }

    @Data
    public static class ApprovalPositionItem {
        private String code;
        private String name;
        private Boolean active;
        private String source;
        private String remark;
        private List<PositionAssignmentItem> assignments = new ArrayList<>();
    }

    @Data
    public static class PositionAssignmentItem {
        private String assignmentType;
        private String username;
        private String postCode;
        private Boolean active;
        private String changeReason;
    }

    @Data
    public static class DirectoryItem {
        private String path;
        private String parentPath;
        private String code;
        private String name;
        private Boolean active;
        private Integer sort;
        private String remark;
        private Boolean accessRuleManuallyBound;
        private List<DirectoryAccessRuleItem> accessRules = new ArrayList<>();
    }

    @Data
    public static class DirectoryAccessRuleItem implements SubjectKeyCarrier {
        private String subjectType;
        private String username;
        private String departmentPath;
        private String postCode;
        private String roleCode;
        private Boolean canQuery;
        private Boolean canPreview;
        private Boolean canDownload;
        private Boolean active;
        private String changeReason;
    }

    @Data
    public static class CategoryItem {
        private String code;
        private String name;
        private String parentCode;
        private Boolean active;
        private Integer sort;
        private String source;
        private String remark;
        private String description;
        private String lifecycleStage;
        private Boolean distributionRequired;
        private Boolean trainingRequired;
        private DirectoryBindingItem directoryBinding;
        private List<PermissionRuleItem> permissionRules = new ArrayList<>();
        private ApprovalMatrixItem approvalMatrix = new ApprovalMatrixItem();
        private ViewMatrixItem viewMatrix = new ViewMatrixItem();
        private List<DistributionRuleItem> distributionRules = new ArrayList<>();
        private List<TrainingRuleItem> trainingRules = new ArrayList<>();
    }

    @Data
    public static class DirectoryBindingItem {
        private String directoryPath;
        private Boolean active;
    }

    @Data
    public static class PermissionRuleItem implements SubjectKeyCarrier {
        private String actionType;
        private String subjectType;
        private String username;
        private String departmentPath;
        private String postCode;
        private String roleCode;
        private String scopeType;
        private Boolean active;
        private String remark;
    }

    @Data
    public static class ApprovalMatrixItem {
        private LocalDateTime effectiveTime;
        private String remark;
        private List<ApprovalMatrixRuleItem> rules = new ArrayList<>();
    }

    @Data
    public static class ApprovalMatrixRuleItem implements SubjectKeyCarrier {
        private String stageType;
        private Boolean active;
        private String subjectLabel;
        private String marker;
        private String subjectType;
        private String username;
        private String departmentPath;
        private String postCode;
        private String roleCode;
        private String dccPositionCode;
        private String subjectName;
        private String subjectDepartmentPath;
        private String remark;
    }

    @Data
    public static class ViewMatrixItem {
        private List<ViewMatrixRuleItem> rules = new ArrayList<>();
    }

    @Data
    public static class ViewMatrixRuleItem implements SubjectKeyCarrier {
        private String excelFileName;
        private Integer excelRowNo;
        private String excelColumnLetter;
        private String subjectLabel;
        private String subjectTopHeader;
        private String subjectSubHeader;
        private String marker;
        private String scopeType;
        private String subjectType;
        private String username;
        private String departmentPath;
        private String postCode;
        private String roleCode;
        private String dccPositionCode;
        private Boolean active;
        private String remark;
    }

    @Data
    public static class DistributionRuleItem {
        private String departmentPath;
        private String distributionMedium;
        private Boolean active;
    }

    @Data
    public static class TrainingRuleItem {
        private String departmentPath;
        private Boolean active;
    }
}
