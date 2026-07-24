package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccAdminFullConfigPackageImportRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryDirectoryBindingSaveReqVO;
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
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccAdminFullConfigManagedScopeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@Import(DccAdminFullConfigPackageServiceImpl.class)
class DccAdminFullConfigPackageServiceTest extends BaseDbUnitTest {

    @Resource
    private DccAdminFullConfigPackageServiceImpl service;
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
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @MockitoBean
    private DccDirectoryAdminService directoryAdminService;
    @MockitoBean
    private DccFileCategoryAdminService fileCategoryAdminService;
    @MockitoBean
    private DccCategoryPermissionAdminService permissionAdminService;
    @MockitoBean
    private DccCategoryDistributionRuleAdminService distributionRuleAdminService;
    @MockitoBean
    private DccCategoryTrainingRuleAdminService trainingRuleAdminService;
    @MockitoBean
    private DccCategoryApprovalMatrixAdminService approvalMatrixAdminService;
    @MockitoBean
    private DccCategoryViewMatrixAdminService viewMatrixAdminService;
    @MockitoBean
    private DccApprovalPositionAdminService approvalPositionAdminService;
    @MockitoBean
    private AdminUserService adminUserService;
    @MockitoBean
    private AdminUserMapper adminUserMapper;
    @MockitoBean
    private RoleMapper roleMapper;
    @MockitoBean
    private PostMapper postMapper;
    @MockitoBean
    private DeptMapper deptMapper;

    private final Map<Long, AdminUserDO> usersById = new LinkedHashMap<>();
    private final Map<String, AdminUserDO> usersByUsername = new LinkedHashMap<>();
    private final Map<Long, RoleDO> rolesById = new LinkedHashMap<>();
    private final Map<String, RoleDO> rolesByCode = new LinkedHashMap<>();
    private final Map<Long, PostDO> postsById = new LinkedHashMap<>();
    private final Map<String, PostDO> postsByCode = new LinkedHashMap<>();
    private final Map<Long, DeptDO> deptsById = new LinkedHashMap<>();

    @BeforeEach
    void setUpMocks() {
        usersById.clear();
        usersByUsername.clear();
        rolesById.clear();
        rolesByCode.clear();
        postsById.clear();
        postsByCode.clear();
        deptsById.clear();

        when(adminUserService.getUserByUsername(any())).thenAnswer(invocation ->
                usersByUsername.get(invocation.getArgument(0, String.class)));
        when(adminUserMapper.selectById(anyLong())).thenAnswer(invocation ->
                usersById.get(invocation.getArgument(0, Long.class)));
        when(postMapper.selectById(anyLong())).thenAnswer(invocation ->
                postsById.get(invocation.getArgument(0, Long.class)));
        when(postMapper.selectByCode(any())).thenAnswer(invocation ->
                postsByCode.get(invocation.getArgument(0, String.class)));
        when(roleMapper.selectById(anyLong())).thenAnswer(invocation ->
                rolesById.get(invocation.getArgument(0, Long.class)));
        when(roleMapper.selectByCode(any())).thenAnswer(invocation ->
                rolesByCode.get(invocation.getArgument(0, String.class)));
        when(deptMapper.selectList()).thenAnswer(invocation -> new ArrayList<>(deptsById.values()));

        doAnswer(invocation -> {
            DccDirectorySaveReqVO reqVO = invocation.getArgument(0);
            DccFileDirectoryDO directory = DccFileDirectoryDO.builder()
                    .code(reqVO.getCode())
                    .name(reqVO.getName())
                    .parentId(reqVO.getParentId())
                    .active(reqVO.getActive())
                    .sort(reqVO.getSort())
                    .remark(reqVO.getRemark())
                    .accessRuleManuallyBound(Boolean.FALSE)
                    .build();
            directoryMapper.insert(directory);
            return directory.getId();
        }).when(directoryAdminService).createDirectory(any(DccDirectorySaveReqVO.class));
        doAnswer(invocation -> {
            DccDirectorySaveReqVO reqVO = invocation.getArgument(0);
            DccFileDirectoryDO directory = directoryMapper.selectById(reqVO.getId());
            directory.setCode(reqVO.getCode());
            directory.setName(reqVO.getName());
            directory.setParentId(reqVO.getParentId());
            directory.setActive(reqVO.getActive());
            directory.setSort(reqVO.getSort());
            directory.setRemark(reqVO.getRemark());
            directoryMapper.updateById(directory);
            return null;
        }).when(directoryAdminService).updateDirectory(any(DccDirectorySaveReqVO.class));
        doAnswer(invocation -> {
            Long directoryId = invocation.getArgument(0);
            List<DccDirectoryAccessRuleSaveReqVO> rules = invocation.getArgument(1);
            directoryAccessRuleMapper.delete(DccDirectoryAccessRuleDO::getDirectoryId, directoryId);
            for (DccDirectoryAccessRuleSaveReqVO reqVO : rules) {
                directoryAccessRuleMapper.insert(DccDirectoryAccessRuleDO.builder()
                        .directoryId(directoryId)
                        .subjectType(reqVO.getSubjectType())
                        .subjectId(reqVO.getSubjectId())
                        .canQuery(reqVO.getCanQuery())
                        .canPreview(reqVO.getCanPreview())
                        .canDownload(reqVO.getCanDownload())
                        .active(reqVO.getActive())
                        .changeReason(reqVO.getChangeReason())
                        .build());
            }
            DccFileDirectoryDO directory = directoryMapper.selectById(directoryId);
            directory.setAccessRuleManuallyBound(!rules.isEmpty());
            directoryMapper.updateById(directory);
            return null;
        }).when(directoryAdminService).replaceAccessRules(anyLong(), any());
        doAnswer(invocation -> {
            Long directoryId = invocation.getArgument(0);
            directoryAccessRuleMapper.delete(DccDirectoryAccessRuleDO::getDirectoryId, directoryId);
            DccFileDirectoryDO directory = directoryMapper.selectById(directoryId);
            if (directory != null) {
                directory.setAccessRuleManuallyBound(Boolean.FALSE);
                directoryMapper.updateById(directory);
            }
            return null;
        }).when(directoryAdminService).deleteAccessRules(anyLong());

        doAnswer(invocation -> {
            DccFileCategorySaveReqVO reqVO = invocation.getArgument(0);
            DccFileCategoryDO category = DccFileCategoryDO.builder()
                    .code(reqVO.getCode())
                    .name(reqVO.getName())
                    .parentId(reqVO.getParentId())
                    .active(reqVO.getActive())
                    .sort(reqVO.getSort())
                    .source(reqVO.getSource())
                    .remark(reqVO.getRemark())
                    .description(reqVO.getDescription())
                    .lifecycleStage(reqVO.getLifecycleStage())
                    .distributionRequired(reqVO.getDistributionRequired())
                    .trainingRequired(reqVO.getTrainingRequired())
                    .build();
            categoryMapper.insert(category);
            return category.getId();
        }).when(fileCategoryAdminService).createCategory(any(DccFileCategorySaveReqVO.class));
        doAnswer(invocation -> {
            DccFileCategorySaveReqVO reqVO = invocation.getArgument(0);
            DccFileCategoryDO category = categoryMapper.selectById(reqVO.getId());
            category.setCode(reqVO.getCode());
            category.setName(reqVO.getName());
            category.setParentId(reqVO.getParentId());
            category.setActive(reqVO.getActive());
            category.setSort(reqVO.getSort());
            category.setSource(reqVO.getSource());
            category.setRemark(reqVO.getRemark());
            category.setDescription(reqVO.getDescription());
            category.setLifecycleStage(reqVO.getLifecycleStage());
            category.setDistributionRequired(reqVO.getDistributionRequired());
            category.setTrainingRequired(reqVO.getTrainingRequired());
            categoryMapper.updateById(category);
            return null;
        }).when(fileCategoryAdminService).updateCategory(any(DccFileCategorySaveReqVO.class));
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            DccCategoryDirectoryBindingSaveReqVO reqVO = invocation.getArgument(1);
            categoryDirectoryBindingMapper.delete(DccCategoryDirectoryBindingDO::getCategoryId, categoryId);
            DccCategoryDirectoryBindingDO binding = DccCategoryDirectoryBindingDO.builder()
                    .categoryId(categoryId)
                    .directoryId(reqVO.getDirectoryId())
                    .active(reqVO.getActive())
                    .build();
            categoryDirectoryBindingMapper.insert(binding);
            return binding;
        }).when(fileCategoryAdminService).bindDirectory(anyLong(), any(DccCategoryDirectoryBindingSaveReqVO.class));
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            categoryDirectoryBindingMapper.delete(DccCategoryDirectoryBindingDO::getCategoryId, categoryId);
            permissionRuleMapper.delete(DccFileCategoryPermissionRuleDO::getCategoryId, categoryId);
            viewMatrixRuleMapper.delete(DccCategoryViewMatrixRuleDO::getCategoryId, categoryId);
            distributionRuleMapper.delete(DccFileCategoryDistributionRuleDO::getCategoryId, categoryId);
            trainingRuleMapper.delete(DccFileCategoryTrainingRuleDO::getCategoryId, categoryId);
            DccCategoryApprovalRouteDO route = approvalRouteMapper.selectLatestActiveByCategoryId(categoryId);
            if (route != null) {
                approvalRouteNodeMapper.delete(DccCategoryApprovalRouteNodeDO::getRouteId, route.getId());
                approvalRouteMapper.deleteById(route.getId());
            }
            categoryMapper.deleteById(categoryId);
            return null;
        }).when(fileCategoryAdminService).deleteCategory(anyLong());

        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            List<DccCategoryPermissionRuleSaveReqVO> rules = invocation.getArgument(1);
            permissionRuleMapper.delete(DccFileCategoryPermissionRuleDO::getCategoryId, categoryId);
            List<DccFileCategoryPermissionRuleDO> result = new ArrayList<>();
            for (DccCategoryPermissionRuleSaveReqVO reqVO : rules) {
                DccFileCategoryPermissionRuleDO rule = DccFileCategoryPermissionRuleDO.builder()
                        .categoryId(categoryId)
                        .actionType(reqVO.getActionType())
                        .subjectType(reqVO.getSubjectType())
                        .subjectId(reqVO.getSubjectId())
                        .scopeType(reqVO.getScopeType())
                        .active(reqVO.getActive())
                        .remark(reqVO.getRemark())
                        .build();
                permissionRuleMapper.insert(rule);
                result.add(rule);
            }
            return result;
        }).when(permissionAdminService).replacePermissionRules(anyLong(), any());
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            List<DccCategoryDistributionRuleSaveReqVO> rules = invocation.getArgument(1);
            distributionRuleMapper.delete(DccFileCategoryDistributionRuleDO::getCategoryId, categoryId);
            List<DccFileCategoryDistributionRuleDO> result = new ArrayList<>();
            for (DccCategoryDistributionRuleSaveReqVO reqVO : rules) {
                DccFileCategoryDistributionRuleDO rule = DccFileCategoryDistributionRuleDO.builder()
                        .categoryId(categoryId)
                        .departmentId(reqVO.getDepartmentId())
                        .distributionMedium(reqVO.getDistributionMedium())
                        .active(reqVO.getActive())
                        .build();
                distributionRuleMapper.insert(rule);
                result.add(rule);
            }
            return result;
        }).when(distributionRuleAdminService).replaceDistributionRules(anyLong(), any());
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            List<DccCategoryDistributionRuleSaveReqVO> rules = invocation.getArgument(1);
            distributionRuleMapper.delete(DccFileCategoryDistributionRuleDO::getCategoryId, categoryId);
            List<DccFileCategoryDistributionRuleDO> result = new ArrayList<>();
            for (DccCategoryDistributionRuleSaveReqVO reqVO : rules) {
                DccFileCategoryDistributionRuleDO rule = DccFileCategoryDistributionRuleDO.builder()
                        .categoryId(categoryId)
                        .departmentId(reqVO.getDepartmentId())
                        .distributionMedium(reqVO.getDistributionMedium())
                        .active(reqVO.getActive())
                        .build();
                distributionRuleMapper.insert(rule);
                result.add(rule);
            }
            return result;
        }).when(distributionRuleAdminService).importDistributionRules(anyLong(), any());
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            List<DccCategoryTrainingRuleSaveReqVO> rules = invocation.getArgument(1);
            trainingRuleMapper.delete(DccFileCategoryTrainingRuleDO::getCategoryId, categoryId);
            List<DccFileCategoryTrainingRuleDO> result = new ArrayList<>();
            for (DccCategoryTrainingRuleSaveReqVO reqVO : rules) {
                DccFileCategoryTrainingRuleDO rule = DccFileCategoryTrainingRuleDO.builder()
                        .categoryId(categoryId)
                        .departmentId(reqVO.getDepartmentId())
                        .active(reqVO.getActive())
                        .build();
                trainingRuleMapper.insert(rule);
                result.add(rule);
            }
            return result;
        }).when(trainingRuleAdminService).replaceTrainingRules(anyLong(), any());
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            List<DccCategoryTrainingRuleSaveReqVO> rules = invocation.getArgument(1);
            trainingRuleMapper.delete(DccFileCategoryTrainingRuleDO::getCategoryId, categoryId);
            List<DccFileCategoryTrainingRuleDO> result = new ArrayList<>();
            for (DccCategoryTrainingRuleSaveReqVO reqVO : rules) {
                DccFileCategoryTrainingRuleDO rule = DccFileCategoryTrainingRuleDO.builder()
                        .categoryId(categoryId)
                        .departmentId(reqVO.getDepartmentId())
                        .active(reqVO.getActive())
                        .build();
                trainingRuleMapper.insert(rule);
                result.add(rule);
            }
            return result;
        }).when(trainingRuleAdminService).importTrainingRules(anyLong(), any());
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            DccCategoryViewMatrixSaveReqVO reqVO = invocation.getArgument(1);
            viewMatrixRuleMapper.delete(DccCategoryViewMatrixRuleDO::getCategoryId, categoryId);
            List<DccCategoryViewMatrixSaveReqVO.Rule> rules = reqVO.getRules() == null ? List.of() : reqVO.getRules();
            for (DccCategoryViewMatrixSaveReqVO.Rule ruleReq : rules) {
                viewMatrixRuleMapper.insert(DccCategoryViewMatrixRuleDO.builder()
                        .categoryId(categoryId)
                        .excelFileName(ruleReq.getExcelFileName())
                        .excelRowNo(ruleReq.getExcelRowNo())
                        .excelColumnLetter(ruleReq.getExcelColumnLetter())
                        .subjectLabel(ruleReq.getSubjectLabel())
                        .subjectTopHeader(ruleReq.getSubjectTopHeader())
                        .subjectSubHeader(ruleReq.getSubjectSubHeader())
                        .marker(ruleReq.getMarker())
                        .scopeType(ruleReq.getScopeType())
                        .subjectType(ruleReq.getSubjectType())
                        .subjectId(ruleReq.getSubjectId())
                        .active(ruleReq.getActive())
                        .remark(ruleReq.getRemark())
                        .build());
            }
            return rules;
        }).when(viewMatrixAdminService).saveViewMatrix(anyLong(), any(DccCategoryViewMatrixSaveReqVO.class));
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            DccCategoryViewMatrixSaveReqVO reqVO = invocation.getArgument(1);
            viewMatrixRuleMapper.delete(DccCategoryViewMatrixRuleDO::getCategoryId, categoryId);
            List<DccCategoryViewMatrixSaveReqVO.Rule> rules = reqVO.getRules() == null ? List.of() : reqVO.getRules();
            for (DccCategoryViewMatrixSaveReqVO.Rule ruleReq : rules) {
                viewMatrixRuleMapper.insert(DccCategoryViewMatrixRuleDO.builder()
                        .categoryId(categoryId)
                        .excelFileName(ruleReq.getExcelFileName())
                        .excelRowNo(ruleReq.getExcelRowNo())
                        .excelColumnLetter(ruleReq.getExcelColumnLetter())
                        .subjectLabel(ruleReq.getSubjectLabel())
                        .subjectTopHeader(ruleReq.getSubjectTopHeader())
                        .subjectSubHeader(ruleReq.getSubjectSubHeader())
                        .marker(ruleReq.getMarker())
                        .scopeType(ruleReq.getScopeType())
                        .subjectType(ruleReq.getSubjectType())
                        .subjectId(ruleReq.getSubjectId())
                        .active(ruleReq.getActive())
                        .remark(ruleReq.getRemark())
                        .build());
            }
            return rules;
        }).when(viewMatrixAdminService).importViewMatrix(anyLong(), any(DccCategoryViewMatrixSaveReqVO.class));
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            DccCategoryApprovalMatrixSaveReqVO reqVO = invocation.getArgument(1);
            DccCategoryApprovalRouteDO existing = approvalRouteMapper.selectLatestActiveByCategoryId(categoryId);
            if (existing != null) {
                approvalRouteNodeMapper.delete(DccCategoryApprovalRouteNodeDO::getRouteId, existing.getId());
                approvalRouteMapper.deleteById(existing.getId());
            }
            DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                    .categoryId(categoryId)
                    .versionNo(1)
                    .active(Boolean.TRUE)
                    .effectiveTime(reqVO.getEffectiveTime())
                    .remark(reqVO.getRemark())
                    .build();
            approvalRouteMapper.insert(route);
            List<DccCategoryApprovalMatrixSaveReqVO.Rule> rules = reqVO.getRules() == null ? List.of() : reqVO.getRules();
            int stageOrder = 1;
            for (DccCategoryApprovalMatrixSaveReqVO.Rule ruleReq : rules) {
                approvalRouteNodeMapper.insert(DccCategoryApprovalRouteNodeDO.builder()
                        .routeId(route.getId())
                        .stageNo("SIGNOFF".equalsIgnoreCase(ruleReq.getStageType()) ? 2 : 3)
                        .stageCode(ruleReq.getStageType())
                        .stageName(ruleReq.getSubjectLabel())
                        .stageOrder(stageOrder++)
                        .candidateSourceType("DCC_POSITION".equalsIgnoreCase(ruleReq.getSubjectType()) ? "POSITION"
                                : ruleReq.getSubjectType())
                        .candidateSourceId(ruleReq.getSubjectId())
                        .candidateSourceIds(ruleReq.getSubjectId() == null ? null : String.valueOf(ruleReq.getSubjectId()))
                        .approveMethod("ALL")
                        .requireAllApprovals(Boolean.TRUE)
                        .required(Boolean.TRUE)
                        .sort(stageOrder)
                        .subjectType("DCC_POSITION".equalsIgnoreCase(ruleReq.getSubjectType()) ? "POSITION"
                                : ruleReq.getSubjectType())
                        .subjectLabel(ruleReq.getSubjectLabel())
                        .subjectId(ruleReq.getSubjectId())
                        .subjectName(ruleReq.getSubjectName())
                        .subjectDepartmentPath(ruleReq.getSubjectDepartmentPath())
                        .marker(ruleReq.getMarker())
                        .ruleRemark(ruleReq.getRemark())
                        .build());
            }
            return route;
        }).when(approvalMatrixAdminService).saveApprovalMatrix(anyLong(), any(DccCategoryApprovalMatrixSaveReqVO.class));
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            DccCategoryApprovalRouteDO route = approvalRouteMapper.selectLatestActiveByCategoryId(categoryId);
            if (route != null) {
                approvalRouteNodeMapper.delete(DccCategoryApprovalRouteNodeDO::getRouteId, route.getId());
                approvalRouteMapper.deleteById(route.getId());
            }
            return null;
        }).when(approvalMatrixAdminService).deleteApprovalMatrix(anyLong());
        doAnswer(invocation -> {
            Long categoryId = invocation.getArgument(0);
            DccCategoryApprovalMatrixSaveReqVO reqVO = invocation.getArgument(1);
            DccCategoryApprovalRouteDO existing = approvalRouteMapper.selectLatestActiveByCategoryId(categoryId);
            if (existing != null) {
                approvalRouteNodeMapper.delete(DccCategoryApprovalRouteNodeDO::getRouteId, existing.getId());
                approvalRouteMapper.deleteById(existing.getId());
            }
            DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                    .categoryId(categoryId)
                    .versionNo(1)
                    .active(Boolean.TRUE)
                    .effectiveTime(reqVO.getEffectiveTime())
                    .remark(reqVO.getRemark())
                    .build();
            approvalRouteMapper.insert(route);
            List<DccCategoryApprovalMatrixSaveReqVO.Rule> rules = reqVO.getRules() == null ? List.of() : reqVO.getRules();
            int stageOrder = 1;
            for (DccCategoryApprovalMatrixSaveReqVO.Rule ruleReq : rules) {
                approvalRouteNodeMapper.insert(DccCategoryApprovalRouteNodeDO.builder()
                        .routeId(route.getId())
                        .stageNo("SIGNOFF".equalsIgnoreCase(ruleReq.getStageType()) ? 2 : 3)
                        .stageCode(ruleReq.getStageType())
                        .stageName(ruleReq.getSubjectLabel())
                        .stageOrder(stageOrder++)
                        .candidateSourceType("DCC_POSITION".equalsIgnoreCase(ruleReq.getSubjectType()) ? "POSITION"
                                : ruleReq.getSubjectType())
                        .candidateSourceId(ruleReq.getSubjectId())
                        .candidateSourceIds(ruleReq.getSubjectId() == null ? null : String.valueOf(ruleReq.getSubjectId()))
                        .approveMethod("ALL")
                        .requireAllApprovals(Boolean.TRUE)
                        .required(Boolean.TRUE)
                        .sort(stageOrder)
                        .subjectType("DCC_POSITION".equalsIgnoreCase(ruleReq.getSubjectType()) ? "POSITION"
                                : ruleReq.getSubjectType())
                        .subjectLabel(ruleReq.getSubjectLabel())
                        .subjectId(ruleReq.getSubjectId())
                        .subjectName(ruleReq.getSubjectName())
                        .subjectDepartmentPath(ruleReq.getSubjectDepartmentPath())
                        .marker(ruleReq.getMarker())
                        .ruleRemark(ruleReq.getRemark())
                        .build());
            }
            return route;
        }).when(approvalMatrixAdminService).importApprovalMatrix(anyLong(), any(DccCategoryApprovalMatrixSaveReqVO.class));
        doAnswer(invocation -> {
            Long positionId = invocation.getArgument(0);
            List<DccPositionAssignmentSaveReqVO> assignments = invocation.getArgument(1);
            DccApprovalPositionDO position = approvalPositionMapper.selectById(positionId);
            if (DccUploaderDerivedPositionSupport.isUploaderDerivedPosition(position) && !assignments.isEmpty()) {
                throw exception(APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED, position.getName());
            }
            positionAssignmentMapper.delete(DccPositionAssignmentDO::getPositionId, positionId);
            if (DccUploaderDerivedPositionSupport.isUploaderDerivedPosition(position)) {
                return List.of();
            }
            List<DccPositionAssignmentDO> result = new ArrayList<>();
            for (DccPositionAssignmentSaveReqVO reqVO : assignments) {
                DccPositionAssignmentDO assignment = DccPositionAssignmentDO.builder()
                        .positionId(positionId)
                        .assignmentType(reqVO.getAssignmentType())
                        .systemPostId(reqVO.getSystemPostId())
                        .userId(reqVO.getUserId())
                        .active(reqVO.getActive())
                        .changeReason(reqVO.getChangeReason())
                        .build();
                positionAssignmentMapper.insert(assignment);
                result.add(assignment);
            }
            return result;
        }).when(approvalPositionAdminService).replaceAssignments(anyLong(), any());
    }

    @Test
    void exportPackage_shouldContainOwnedScopeAndStableBusinessKeys() throws Exception {
        seedSystemSubjects();
        DccApprovalPositionDO position = insertPosition("DOC_CTRL", "文控");
        positionAssignmentMapper.insert(DccPositionAssignmentDO.builder()
                .positionId(position.getId())
                .assignmentType("POST")
                .systemPostId(20L)
                .active(Boolean.TRUE)
                .changeReason("seed")
                .build());
        DccFileDirectoryDO root = insertDirectory("ROOT", "文控中心", null);
        DccFileDirectoryDO child = insertDirectory("QA", "质量体系", root.getId());
        directoryAccessRuleMapper.insert(DccDirectoryAccessRuleDO.builder()
                .directoryId(child.getId())
                .subjectType("ROLE")
                .subjectId(30L)
                .canQuery(Boolean.TRUE)
                .canPreview(Boolean.TRUE)
                .canDownload(Boolean.FALSE)
                .active(Boolean.TRUE)
                .changeReason("seed")
                .build());
        DccFileCategoryDO category = insertCategory("SOP", "程序文件");
        categoryDirectoryBindingMapper.insert(DccCategoryDirectoryBindingDO.builder()
                .categoryId(category.getId())
                .directoryId(child.getId())
                .active(Boolean.TRUE)
                .build());
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .categoryId(category.getId())
                .actionType("DOWNLOAD")
                .subjectType("USER")
                .subjectId(10L)
                .scopeType(DccFileCategoryPermissionScopeEnum.PRODUCT_GROUP.getCode())
                .active(Boolean.TRUE)
                .remark("seed")
                .build());
        approvalRouteMapper.insert(DccCategoryApprovalRouteDO.builder()
                .categoryId(category.getId())
                .versionNo(1)
                .active(Boolean.TRUE)
                .effectiveTime(LocalDateTime.of(2026, 6, 30, 12, 0))
                .remark("route-seed")
                .build());
        DccCategoryApprovalRouteDO route = approvalRouteMapper.selectLatestActiveByCategoryId(category.getId());
        approvalRouteNodeMapper.insert(DccCategoryApprovalRouteNodeDO.builder()
                .routeId(route.getId())
                .stageNo(2)
                .stageCode("MATRIX_REVIEW")
                .stageName("审核会签")
                .stageOrder(2)
                .candidateSourceType("POSITION")
                .candidateSourceIds(String.valueOf(position.getId()))
                .approveMethod("ALL")
                .requireAllApprovals(Boolean.TRUE)
                .required(Boolean.TRUE)
                .sort(1)
                .subjectType("POSITION")
                .subjectLabel("文控")
                .subjectId(position.getId())
                .build());
        viewMatrixRuleMapper.insert(DccCategoryViewMatrixRuleDO.builder()
                .categoryId(category.getId())
                .excelFileName("权限矩阵.xlsx")
                .excelRowNo(2)
                .excelColumnLetter("B")
                .subjectLabel("质量岗位")
                .marker("●")
                .scopeType("ALL_MEMBERS")
                .subjectType("POST")
                .subjectId(20L)
                .active(Boolean.TRUE)
                .remark("view")
                .build());
        distributionRuleMapper.insert(DccFileCategoryDistributionRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(40L)
                .distributionMedium("PUBLIC_FOLDER")
                .active(Boolean.TRUE)
                .build());
        trainingRuleMapper.insert(DccFileCategoryTrainingRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(40L)
                .active(Boolean.TRUE)
                .build());

        byte[] exported = service.exportPackage();
        DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage payload = JsonUtils.parseObject(
                new String(exported, StandardCharsets.UTF_8),
                DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage.class);

        assertEquals(DccAdminFullConfigPackageServiceImpl.PACKAGE_VERSION, payload.getPackageVersion());
        assertEquals(1, payload.getApprovalPositions().size());
        assertEquals("DOC_CTRL", payload.getApprovalPositions().get(0).getCode());
        assertEquals("quality_manager", payload.getApprovalPositions().get(0).getAssignments().get(0).getPostCode());
        assertEquals(2, payload.getDirectories().size());
        assertEquals("文控中心/质量体系", payload.getDirectories().get(1).getPath());
        assertEquals("quality_role", payload.getDirectories().get(1).getAccessRules().get(0).getRoleCode());
        assertEquals(1, payload.getCategories().size());
        assertEquals("文控中心/质量体系", payload.getCategories().get(0).getDirectoryBinding().getDirectoryPath());
        assertEquals(DccFileCategoryPermissionScopeEnum.PRODUCT_GROUP.getCode(),
                payload.getCategories().get(0).getPermissionRules().get(0).getScopeType());
        assertEquals("DOC_CTRL", payload.getCategories().get(0).getApprovalMatrix().getRules().get(0).getDccPositionCode());
        assertEquals("quality_manager",
                payload.getCategories().get(0).getViewMatrix().getRules().get(0).getPostCode());
        assertEquals("测试租户/质量中心", payload.getCategories().get(0).getDistributionRules().get(0).getDepartmentPath());
    }

    @Test
    void exportPackage_shouldExcludeMatrixManagedReviewApprovePermissionRules() {
        seedSystemSubjects();
        DccFileCategoryDO category = insertCategory("MATRIX-GOV", "矩阵治理");
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .categoryId(category.getId())
                .actionType("VIEW")
                .subjectType("USER")
                .subjectId(10L)
                .scopeType(DccFileCategoryPermissionScopeEnum.GLOBAL.getCode())
                .active(Boolean.TRUE)
                .remark("keep-view")
                .build());
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .categoryId(category.getId())
                .actionType("REVIEW")
                .subjectType("USER")
                .subjectId(10L)
                .scopeType(DccFileCategoryPermissionScopeEnum.GLOBAL.getCode())
                .active(Boolean.TRUE)
                .remark("legacy-review")
                .build());
        permissionRuleMapper.insert(DccFileCategoryPermissionRuleDO.builder()
                .categoryId(category.getId())
                .actionType("APPROVE")
                .subjectType("USER")
                .subjectId(10L)
                .scopeType(DccFileCategoryPermissionScopeEnum.GLOBAL.getCode())
                .active(Boolean.TRUE)
                .remark("legacy-approve")
                .build());

        byte[] exported = service.exportPackage();
        DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage payload = JsonUtils.parseObject(
                new String(exported, StandardCharsets.UTF_8),
                DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage.class);

        var exportedCategory = payload.getCategories().stream()
                .filter(item -> "MATRIX-GOV".equals(item.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("VIEW"),
                exportedCategory.getPermissionRules().stream().map(rule -> rule.getActionType()).toList());
    }

    @Test
    void exportPackage_shouldExcludeRulesWhoseDepartmentPathCannotBeResolved() {
        seedSystemSubjects();
        DccFileCategoryDO category = insertCategory("DEPT-ORPHAN", "部门孤儿规则");
        distributionRuleMapper.insert(DccFileCategoryDistributionRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(40L)
                .distributionMedium("PUBLIC_FOLDER")
                .active(Boolean.TRUE)
                .build());
        distributionRuleMapper.insert(DccFileCategoryDistributionRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(999999L)
                .distributionMedium("PUBLIC_FOLDER")
                .active(Boolean.TRUE)
                .build());
        trainingRuleMapper.insert(DccFileCategoryTrainingRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(40L)
                .active(Boolean.TRUE)
                .build());
        trainingRuleMapper.insert(DccFileCategoryTrainingRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(999999L)
                .active(Boolean.TRUE)
                .build());

        byte[] exported = service.exportPackage();
        DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage payload = JsonUtils.parseObject(
                new String(exported, StandardCharsets.UTF_8),
                DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage.class);

        var exportedCategory = payload.getCategories().stream()
                .filter(item -> "DEPT-ORPHAN".equals(item.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("测试租户/质量中心"),
                exportedCategory.getDistributionRules().stream().map(rule -> rule.getDepartmentPath()).toList());
        assertEquals(List.of("测试租户/质量中心"),
                exportedCategory.getTrainingRules().stream().map(rule -> rule.getDepartmentPath()).toList());
    }

    @Test
    void exportPackage_shouldNormalizeUploaderDerivedPositionAssignmentsForRoundTrip() {
        seedSystemSubjects();
        DccApprovalPositionDO position = insertPosition("LOCAL-ROLE-APPROVER-DEPT", "部门负责人");
        positionAssignmentMapper.insert(DccPositionAssignmentDO.builder()
                .positionId(position.getId())
                .assignmentType("POST")
                .systemPostId(20L)
                .active(Boolean.TRUE)
                .changeReason("legacy")
                .build());

        byte[] exported = service.exportPackage();
        DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage payload = JsonUtils.parseObject(
                new String(exported, StandardCharsets.UTF_8),
                DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage.class);

        assertEquals(1, payload.getApprovalPositions().size());
        assertEquals("LOCAL-ROLE-APPROVER-DEPT", payload.getApprovalPositions().get(0).getCode());
        assertTrue(payload.getApprovalPositions().get(0).getAssignments().isEmpty());

        DccAdminFullConfigPackageImportRespVO result = service.importPackage(exported);

        assertEquals(1, result.getApprovalPositionCount());
        assertEquals(0, positionAssignmentMapper.selectList(DccPositionAssignmentDO::getPositionId, position.getId()).size());
    }

    @Test
    void importPackage_shouldUpsertByBusinessKeyAndReplaceOwnedScope() {
        seedSystemSubjects();
        DccApprovalPositionDO oldPosition = insertPosition("OLD_POSITION", "旧岗位");
        DccFileDirectoryDO oldDirectory = insertDirectory("OLD", "旧目录", null);
        DccFileCategoryDO oldCategory = insertCategory("OLD-CAT", "旧类别");
        saveManagedScope(List.of("OLD-CAT"), List.of("旧目录"), List.of("OLD_POSITION"));

        DccAdminFullConfigPackageImportRespVO result = service.importPackage(
                standardManagedPackagePayload().getBytes(StandardCharsets.UTF_8));

        assertEquals(1, result.getApprovalPositionCount());
        assertEquals(2, result.getDirectoryCount());
        assertEquals(1, result.getCategoryCount());
        assertEquals(1, result.getRemovedApprovalPositionCount());
        assertEquals(1, result.getRemovedDirectoryCount());
        assertEquals(1, result.getRemovedCategoryCount());
        assertEquals(0, approvalPositionMapper.selectCount(DccApprovalPositionDO::getCode, "OLD_POSITION"));
        assertEquals(0, directoryMapper.selectCount(DccFileDirectoryDO::getCode, "OLD"));
        assertEquals(0, categoryMapper.selectCount(DccFileCategoryDO::getCode, "OLD-CAT"));
        assertEquals(1, approvalPositionMapper.selectCount(DccApprovalPositionDO::getCode, "DOC_CTRL"));
        assertEquals(1, categoryMapper.selectCount(DccFileCategoryDO::getCode, "SOP"));
        verify(approvalMatrixAdminService).importApprovalMatrix(anyLong(), any(DccCategoryApprovalMatrixSaveReqVO.class));
        verify(viewMatrixAdminService).importViewMatrix(anyLong(), any(DccCategoryViewMatrixSaveReqVO.class));
        verify(viewMatrixAdminService, never()).saveViewMatrix(anyLong(), any(DccCategoryViewMatrixSaveReqVO.class));
        DccFileCategoryDO category = categoryMapper.selectOne(DccFileCategoryDO::getCode, "SOP");
        assertEquals(1, permissionRuleMapper.selectCount(DccFileCategoryPermissionRuleDO::getCategoryId, category.getId()));
        assertEquals(DccFileCategoryPermissionScopeEnum.PRODUCT_GROUP.getCode(),
                permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId, category.getId())
                        .get(0).getScopeType());
        DccAdminFullConfigManagedScopeDO scope = managedScopeMapper.selectCurrentScope();
        assertEquals(List.of("SOP"), JsonUtils.parseArray(scope.getCategoryCodesJson(), String.class));
        assertEquals(List.of("文控中心", "文控中心/质量体系"),
                JsonUtils.parseArray(scope.getDirectoryPathsJson(), String.class));
        assertEquals(List.of("DOC_CTRL"), JsonUtils.parseArray(scope.getApprovalPositionCodesJson(), String.class));
    }

    @Test
    void importPackage_shouldAllowRequiredDistributionAndTrainingRulesToBeClearedWhenSourcePackageHasNone() {
        seedSystemSubjects();
        DccFileCategoryDO category = insertCategory("EMPTY-RULES", "空规则覆盖");
        category.setDistributionRequired(Boolean.TRUE);
        category.setTrainingRequired(Boolean.TRUE);
        categoryMapper.updateById(category);
        distributionRuleMapper.insert(DccFileCategoryDistributionRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(40L)
                .distributionMedium("PUBLIC_FOLDER")
                .active(Boolean.TRUE)
                .build());
        trainingRuleMapper.insert(DccFileCategoryTrainingRuleDO.builder()
                .categoryId(category.getId())
                .departmentId(40L)
                .active(Boolean.TRUE)
                .build());

        String payload = """
                {
                  "packageVersion":"dcc-admin-full-config-package.v1",
                  "approvalPositions":[],
                  "directories":[],
                  "categories":[
                    {
                      "code":"EMPTY-RULES",
                      "name":"空规则覆盖",
                      "active":true,
                      "sort":1,
                      "source":"LOCAL",
                      "remark":"empty",
                      "description":"empty",
                      "lifecycleStage":"INPUT",
                      "distributionRequired":true,
                      "trainingRequired":true,
                      "permissionRules":[],
                      "approvalMatrix":{"rules":[]},
                      "viewMatrix":{"rules":[]},
                      "distributionRules":[],
                      "trainingRules":[]
                    }
                  ]
                }
                """;

        DccAdminFullConfigPackageImportRespVO result = service.importPackage(payload.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, result.getCategoryCount());
        assertEquals(0, distributionRuleMapper.selectCount(DccFileCategoryDistributionRuleDO::getCategoryId, category.getId()));
        assertEquals(0, trainingRuleMapper.selectCount(DccFileCategoryTrainingRuleDO::getCategoryId, category.getId()));
    }

    @Test
    void importPackage_shouldDeleteOldCategoriesBeforeRemovingObsoleteApprovalPositions() {
        seedSystemSubjects();
        DccApprovalPositionDO obsoletePosition = insertPosition("INTAUTH-386423", "旧审批岗位");
        DccFileCategoryDO obsoleteCategory = insertCategory("OBSOLETE-CAT", "旧类别");
        approvalRouteMapper.insert(DccCategoryApprovalRouteDO.builder()
                .categoryId(obsoleteCategory.getId())
                .versionNo(1)
                .active(Boolean.TRUE)
                .effectiveTime(LocalDateTime.of(2026, 6, 30, 12, 0))
                .remark("obsolete-route")
                .build());
        DccCategoryApprovalRouteDO obsoleteRoute = approvalRouteMapper.selectLatestActiveByCategoryId(obsoleteCategory.getId());
        approvalRouteNodeMapper.insert(DccCategoryApprovalRouteNodeDO.builder()
                .routeId(obsoleteRoute.getId())
                .stageNo(2)
                .stageCode("MATRIX_REVIEW")
                .stageName("旧审核")
                .stageOrder(1)
                .candidateSourceType("POSITION")
                .candidateSourceIds(String.valueOf(obsoletePosition.getId()))
                .approveMethod("ALL")
                .requireAllApprovals(Boolean.TRUE)
                .required(Boolean.TRUE)
                .sort(1)
                .subjectType("POSITION")
                .subjectLabel(obsoletePosition.getName())
                .subjectId(obsoletePosition.getId())
                .build());
        saveManagedScope(List.of("OBSOLETE-CAT"), List.of(), List.of("INTAUTH-386423"));

        String payload = """
                {
                  "packageVersion":"dcc-admin-full-config-package.v1",
                  "approvalPositions":[],
                  "directories":[],
                  "categories":[]
                }
                """;

        DccAdminFullConfigPackageImportRespVO result = service.importPackage(payload.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, result.getRemovedCategoryCount());
        assertEquals(1, result.getRemovedApprovalPositionCount());
        assertEquals(0, categoryMapper.selectCount(DccFileCategoryDO::getCode, "OBSOLETE-CAT"));
        assertEquals(0, approvalPositionMapper.selectCount(DccApprovalPositionDO::getCode, "INTAUTH-386423"));
    }

    @Test
    void importPackage_shouldIgnoreReferencedPackageExternalCategoryAndExportOnlyManagedScope() {
        seedSystemSubjects();
        DccFileDirectoryDO externalDirectory = insertDirectory("EXT", "外部目录", null);
        DccFileCategoryDO externalCategory = insertCategory("CODEX_EXT", "历史测试类别");
        controlledFileMasterMapper.insert(DccControlledFileMasterDO.builder()
                .categoryId(externalCategory.getId())
                .directoryId(externalDirectory.getId())
                .fileName("legacy.docx")
                .fileNumber("LEG-001")
                .status("ACTIVE")
                .build());

        DccAdminFullConfigPackageImportRespVO result = service.importPackage(
                standardManagedPackagePayload().getBytes(StandardCharsets.UTF_8));

        assertEquals(1, result.getCategoryCount());
        assertEquals(0, result.getRemovedCategoryCount());
        assertEquals(1, categoryMapper.selectCount(DccFileCategoryDO::getCode, "CODEX_EXT"));

        byte[] exported = service.exportPackage();
        DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage payload = parsePackage(exported);

        assertEquals(List.of("SOP"),
                payload.getCategories().stream().map(DccAdminFullConfigPackageServiceImpl.CategoryItem::getCode).toList());
        assertEquals(List.of("文控中心", "文控中心/质量体系"),
                payload.getDirectories().stream().map(DccAdminFullConfigPackageServiceImpl.DirectoryItem::getPath).toList());
        assertEquals(List.of("DOC_CTRL"),
                payload.getApprovalPositions().stream()
                        .map(DccAdminFullConfigPackageServiceImpl.ApprovalPositionItem::getCode)
                        .toList());
    }

    @Test
    void importPackage_shouldPreserveDirectoryManualBindingAndRoundTripNormalizedMatrixFields() {
        seedSystemSubjects();

        DccAdminFullConfigPackageImportRespVO result = service.importPackage(
                standardManagedPackagePayload().getBytes(StandardCharsets.UTF_8));

        assertEquals(2, result.getDirectoryCount());
        byte[] exported = service.exportPackage();
        DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage payload = parsePackage(exported);

        Map<String, DccAdminFullConfigPackageServiceImpl.DirectoryItem> directoriesByPath = payload.getDirectories().stream()
                .collect(LinkedHashMap::new,
                        (map, item) -> map.put(item.getPath(), item),
                        LinkedHashMap::putAll);
        assertEquals(Boolean.FALSE, directoriesByPath.get("文控中心").getAccessRuleManuallyBound());
        assertEquals(Boolean.TRUE, directoriesByPath.get("文控中心/质量体系").getAccessRuleManuallyBound());

        DccAdminFullConfigPackageServiceImpl.DirectoryAccessRuleItem accessRule =
                directoriesByPath.get("文控中心/质量体系").getAccessRules().get(0);
        assertEquals(Boolean.TRUE, accessRule.getCanQuery());
        assertEquals(Boolean.TRUE, accessRule.getCanPreview());

        DccAdminFullConfigPackageServiceImpl.CategoryItem category = payload.getCategories().stream()
                .filter(item -> "SOP".equals(item.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("▲", "▲"),
                category.getApprovalMatrix().getRules().stream()
                        .sorted(Comparator.comparing(DccAdminFullConfigPackageServiceImpl.ApprovalMatrixRuleItem::getStageType))
                        .map(DccAdminFullConfigPackageServiceImpl.ApprovalMatrixRuleItem::getMarker)
                        .toList());
    }

    @Test
    void importPackage_shouldFailFastWhenVersionUnsupported() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importPackage("""
                        {"packageVersion":"bad-version","approvalPositions":[],"directories":[],"categories":[]}
                        """.getBytes(StandardCharsets.UTF_8)));

        assertEquals("Unsupported DCC admin full config package version: bad-version", ex.getMessage());
    }

    @Test
    void importPackage_shouldFailFastWhenUploaderDerivedPositionContainsAssignments() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.importPackage("""
                        {
                          "packageVersion":"dcc-admin-full-config-package.v1",
                          "approvalPositions":[
                            {
                              "code":"LOCAL-ROLE-APPROVER-DEPT",
                              "name":"部门负责人",
                              "active":true,
                              "source":"LOCAL",
                              "remark":"seed",
                              "assignments":[
                                {
                                  "assignmentType":"POST",
                                  "postCode":"quality_manager",
                                  "active":true,
                                  "changeReason":"legacy"
                                }
                              ]
                            }
                          ],
                          "directories":[],
                          "categories":[]
                        }
                        """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED.getCode(), ex.getCode());
    }

    private void seedSystemSubjects() {
        AdminUserDO user = AdminUserDO.builder()
                .id(10L)
                .username("alice")
                .password("x")
                .nickname("Alice")
                .deptId(40L)
                .status(0)
                .build();
        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUsername(), user);

        RoleDO role = role(30L, "quality_role");
        rolesById.put(role.getId(), role);
        rolesByCode.put(role.getCode(), role);

        PostDO post = post(20L, "quality_manager");
        postsById.put(post.getId(), post);
        postsByCode.put(post.getCode(), post);

        DeptDO rootDept = dept(1L, "测试租户", 0L);
        DeptDO childDept = dept(40L, "质量中心", 1L);
        deptsById.put(rootDept.getId(), rootDept);
        deptsById.put(childDept.getId(), childDept);
    }

    private DccApprovalPositionDO insertPosition(String code, String name) {
        DccApprovalPositionDO position = DccApprovalPositionDO.builder()
                .code(code)
                .name(name)
                .active(Boolean.TRUE)
                .source("LOCAL")
                .remark("seed")
                .build();
        approvalPositionMapper.insert(position);
        return position;
    }

    private DccFileDirectoryDO insertDirectory(String code, String name, Long parentId) {
        DccFileDirectoryDO directory = DccFileDirectoryDO.builder()
                .code(code)
                .name(name)
                .parentId(parentId)
                .active(Boolean.TRUE)
                .sort(1)
                .remark("seed")
                .accessRuleManuallyBound(Boolean.FALSE)
                .build();
        directoryMapper.insert(directory);
        return directory;
    }

    private DccFileCategoryDO insertCategory(String code, String name) {
        DccFileCategoryDO category = DccFileCategoryDO.builder()
                .code(code)
                .name(name)
                .active(Boolean.TRUE)
                .sort(1)
                .source("LOCAL")
                .description("seed")
                .lifecycleStage("PLAN")
                .distributionRequired(Boolean.TRUE)
                .trainingRequired(Boolean.TRUE)
                .build();
        categoryMapper.insert(category);
        return category;
    }

    private void saveManagedScope(List<String> categoryCodes, List<String> directoryPaths, List<String> approvalPositionCodes) {
        DccAdminFullConfigManagedScopeDO scope = managedScopeMapper.selectCurrentScope();
        if (scope == null) {
            scope = DccAdminFullConfigManagedScopeDO.builder().build();
            scope.setTenantId(1L);
            scope.setCategoryCodesJson(JsonUtils.toJsonString(categoryCodes));
            scope.setDirectoryPathsJson(JsonUtils.toJsonString(directoryPaths));
            scope.setApprovalPositionCodesJson(JsonUtils.toJsonString(approvalPositionCodes));
            managedScopeMapper.insert(scope);
            return;
        }
        scope.setCategoryCodesJson(JsonUtils.toJsonString(categoryCodes));
        scope.setDirectoryPathsJson(JsonUtils.toJsonString(directoryPaths));
        scope.setApprovalPositionCodesJson(JsonUtils.toJsonString(approvalPositionCodes));
        managedScopeMapper.updateById(scope);
    }

    private DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage parsePackage(byte[] content) {
        return JsonUtils.parseObject(new String(content, StandardCharsets.UTF_8),
                DccAdminFullConfigPackageServiceImpl.DccAdminFullConfigPackage.class);
    }

    private String standardManagedPackagePayload() {
        return """
                {
                  "packageVersion":"dcc-admin-full-config-package.v1",
                  "approvalPositions":[
                    {
                      "code":"DOC_CTRL",
                      "name":"文控",
                      "active":true,
                      "source":"LOCAL",
                      "remark":"seed",
                      "assignments":[
                        {
                          "assignmentType":"POST",
                          "postCode":"quality_manager",
                          "active":true,
                          "changeReason":"seed"
                        }
                      ]
                    }
                  ],
                  "directories":[
                    {
                      "path":"文控中心",
                      "code":"ROOT",
                      "name":"文控中心",
                      "active":true,
                      "sort":1,
                      "remark":"root",
                      "accessRuleManuallyBound":false,
                      "accessRules":[]
                    },
                    {
                      "path":"文控中心/质量体系",
                      "parentPath":"文控中心",
                      "code":"QA",
                      "name":"质量体系",
                      "active":true,
                      "sort":2,
                      "remark":"child",
                      "accessRuleManuallyBound":true,
                      "accessRules":[
                        {
                          "subjectType":"ROLE",
                          "roleCode":"quality_role",
                          "canQuery":true,
                          "canPreview":true,
                          "canDownload":false,
                          "active":true,
                          "changeReason":"seed"
                        }
                      ]
                    }
                  ],
                  "categories":[
                    {
                      "code":"SOP",
                      "name":"程序文件",
                      "active":true,
                      "sort":10,
                      "source":"LOCAL",
                      "remark":"seed",
                      "description":"desc",
                      "lifecycleStage":"PLAN",
                      "distributionRequired":true,
                      "trainingRequired":true,
                      "directoryBinding":{
                        "directoryPath":"文控中心/质量体系",
                        "active":true
                      },
                      "permissionRules":[
                        {
                          "actionType":"DOWNLOAD",
                          "subjectType":"USER",
                          "username":"alice",
                          "scopeType":"PRODUCT_GROUP",
                          "active":true,
                          "remark":"download"
                        }
                      ],
                      "approvalMatrix":{
                        "effectiveTime":"2026-06-30T12:00:00",
                        "remark":"route",
                        "rules":[
                          {
                            "stageType":"SIGNOFF",
                            "active":true,
                            "subjectLabel":"文控审核",
                            "marker":"▲",
                            "subjectType":"DCC_POSITION",
                            "dccPositionCode":"DOC_CTRL"
                          },
                          {
                            "stageType":"APPROVAL",
                            "active":true,
                            "subjectLabel":"文控批准",
                            "marker":"▲",
                            "subjectType":"DCC_POSITION",
                            "dccPositionCode":"DOC_CTRL"
                          }
                        ]
                      },
                      "viewMatrix":{
                        "rules":[
                          {
                            "excelFileName":"矩阵.xlsx",
                            "excelRowNo":2,
                            "excelColumnLetter":"B",
                            "subjectLabel":"质量岗位",
                            "marker":"●",
                            "scopeType":"ALL_MEMBERS",
                            "subjectType":"POST",
                            "postCode":"quality_manager",
                            "active":true,
                            "remark":"view"
                          }
                        ]
                      },
                      "distributionRules":[
                        {
                          "departmentPath":"测试租户/质量中心",
                          "distributionMedium":"PUBLIC_FOLDER",
                          "active":true
                        }
                      ],
                      "trainingRules":[
                        {
                          "departmentPath":"测试租户/质量中心",
                          "active":true
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private RoleDO role(Long id, String code) {
        RoleDO role = new RoleDO();
        role.setId(id);
        role.setCode(code);
        role.setName(code);
        role.setStatus(0);
        role.setType(1);
        role.setTenantId(1L);
        return role;
    }

    private PostDO post(Long id, String code) {
        PostDO post = new PostDO();
        post.setId(id);
        post.setCode(code);
        post.setName(code);
        post.setStatus(0);
        return post;
    }

    private DeptDO dept(Long id, String name, Long parentId) {
        DeptDO dept = new DeptDO();
        dept.setId(id);
        dept.setName(name);
        dept.setParentId(parentId);
        dept.setStatus(0);
        dept.setTenantId(1L);
        return dept;
    }
}
