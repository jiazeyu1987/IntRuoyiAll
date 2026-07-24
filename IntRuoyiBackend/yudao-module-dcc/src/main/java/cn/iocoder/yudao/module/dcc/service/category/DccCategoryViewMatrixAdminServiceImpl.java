package cn.iocoder.yudao.module.dcc.service.category;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixEffectivePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixUserLookupRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileCategoryPermissionSupport;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileViewMatrixAccessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_VIEW_MATRIX_EFFECTIVE_ACCESS_BLOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;

@Service
@Validated
public class DccCategoryViewMatrixAdminServiceImpl implements DccCategoryViewMatrixAdminService {

    public static final ErrorCode CATEGORY_VIEW_MATRIX_BLOCKED = CATEGORY_VIEW_MATRIX_EFFECTIVE_ACCESS_BLOCKED;

    private static final String VIEW_RULE_SUMMARY =
            "当前查看矩阵参与人可浏览、查看详情和预览已发布受控副本";
    private static final String PENDING_PREVIEW_RULE_SUMMARY =
            "进行中文件待审原件预览继续按提交时 route snapshot 参与人放行";
    private static final String DOWNLOAD_RULE_SUMMARY =
            "下载规则单独治理，仅展示类别 DOWNLOAD 规则，不改变下载判定逻辑";

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccCategoryViewMatrixRuleMapper ruleMapper;
    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Resource
    private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Resource
    private DeptApi deptApi;

    @Override
    public List<DccCategoryViewMatrixRowRespVO> getViewMatrixRows(String code, String name, Boolean active,
                                                                  Boolean configured) {
        Map<Long, List<DccCategoryViewMatrixRuleDO>> rulesByCategoryId = ruleMapper.selectList().stream()
                .collect(Collectors.groupingBy(DccCategoryViewMatrixRuleDO::getCategoryId, LinkedHashMap::new,
                        Collectors.toList()));
        Map<Long, List<DccFileCategoryPermissionRuleDO>> permissionRulesByCategoryId =
                permissionRuleMapper.selectList().stream()
                        .collect(Collectors.groupingBy(DccFileCategoryPermissionRuleDO::getCategoryId,
                                LinkedHashMap::new, Collectors.toList()));
        ViewMatrixRowBuildContext context = new ViewMatrixRowBuildContext(viewMatrixAccessService.newResolutionContext());
        return categoryMapper.selectList().stream()
                .filter(item -> matchesKeyword(item.getCode(), code))
                .filter(item -> matchesKeyword(item.getName(), name))
                .filter(item -> active == null || Objects.equals(item.getActive(), active))
                .filter(item -> configured == null || Objects.equals(isConfigured(rulesByCategoryId.get(item.getId())),
                        configured))
                .map(item -> buildRow(item, rulesByCategoryId.getOrDefault(item.getId(), List.of()),
                        permissionRulesByCategoryId.getOrDefault(item.getId(), List.of()), context))
                .filter(item -> configured == null || Objects.equals(item.getConfigured(), configured))
                .sorted(java.util.Comparator.comparing(DccCategoryViewMatrixRowRespVO::getCode,
                                java.util.Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccCategoryViewMatrixRowRespVO::getCategoryId,
                                java.util.Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    public DccCategoryViewMatrixEffectivePreviewRespVO previewViewMatrix(Long categoryId,
                                                                         DccCategoryViewMatrixSaveReqVO reqVO) {
        validateCategoryExists(categoryId);
        List<DccControlledFileViewMatrixAccessService.ViewMatrixRuleInput> inputs = toInputs(categoryId, reqVO);
        DccControlledFileViewMatrixAccessService.ViewMatrixAccessResolution resolution =
                viewMatrixAccessService.previewViewMatrixAccessDetails(categoryId, inputs);
        return buildPreview(categoryId, toRuleVOs(reqVO), resolution);
    }

    @Override
    public List<DccCategoryViewMatrixRowRespVO.Rule> saveViewMatrix(Long categoryId,
                                                                    DccCategoryViewMatrixSaveReqVO reqVO) {
        DccCategoryViewMatrixEffectivePreviewRespVO preview = previewViewMatrix(categoryId, reqVO);
        if (Boolean.TRUE.equals(preview.getBlocking())) {
            throw exception(CATEGORY_VIEW_MATRIX_BLOCKED, preview.getRisks().stream()
                    .filter(risk -> Boolean.TRUE.equals(risk.getBlocking()))
                    .map(DccCategoryViewMatrixRowRespVO.Risk::getMessage)
                    .distinct()
                    .collect(Collectors.joining("；")));
        }
        return persistViewMatrix(categoryId, reqVO);
    }

    @Override
    public List<DccCategoryViewMatrixRowRespVO.Rule> importViewMatrix(Long categoryId,
                                                                      DccCategoryViewMatrixSaveReqVO reqVO) {
        validateCategoryExists(categoryId);
        return persistViewMatrix(categoryId, reqVO);
    }

    private List<DccCategoryViewMatrixRowRespVO.Rule> persistViewMatrix(Long categoryId,
                                                                        DccCategoryViewMatrixSaveReqVO reqVO) {
        ruleMapper.delete(DccCategoryViewMatrixRuleDO::getCategoryId, categoryId);
        List<DccCategoryViewMatrixSaveReqVO.Rule> rules = reqVO == null || reqVO.getRules() == null
                ? List.of() : reqVO.getRules();
        for (DccCategoryViewMatrixSaveReqVO.Rule rule : rules) {
            DccCategoryViewMatrixRuleDO ruleDO = toDO(categoryId, rule);
            ruleMapper.insert(ruleDO);
        }
        return ruleMapper.selectList(DccCategoryViewMatrixRuleDO::getCategoryId, categoryId).stream()
                .map(this::toRuleVO)
                .toList();
    }

    @Override
    public List<DccCategoryViewMatrixUserLookupRespVO> getUserViewMatrixAccess(Long userId) {
        if (userId == null) {
            return List.of();
        }
        boolean directoryAdmin = directoryAccessPermissionService.hasDirectoryManagementPermission(userId);
        return categoryMapper.selectList().stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .map(category -> buildUserLookup(category, userId, directoryAdmin))
                .filter(Objects::nonNull)
                .toList();
    }

    private DccCategoryViewMatrixRowRespVO buildRow(DccFileCategoryDO category) {
        return buildRow(category, ruleMapper.selectList(DccCategoryViewMatrixRuleDO::getCategoryId, category.getId()),
                permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId, category.getId()),
                new ViewMatrixRowBuildContext(viewMatrixAccessService.newResolutionContext()));
    }

    private DccCategoryViewMatrixRowRespVO buildRow(DccFileCategoryDO category,
                                                    List<DccCategoryViewMatrixRuleDO> rules,
                                                    List<DccFileCategoryPermissionRuleDO> permissionRules,
                                                    ViewMatrixRowBuildContext context) {
        DccControlledFileViewMatrixAccessService.ViewMatrixAccessResolution resolution =
                viewMatrixAccessService.resolveCurrentViewMatrixAccessDetails(category.getId(),
                        rules.stream().map(viewMatrixAccessService::toInput).toList(), context.matrixContext);
        List<DccCategoryViewMatrixRowRespVO.Rule> ruleVOs = rules.stream()
                .map(rule -> toRuleVO(rule, context))
                .toList();
        DccCategoryViewMatrixRowRespVO row = new DccCategoryViewMatrixRowRespVO();
        row.setCategoryId(category.getId());
        row.setCode(category.getCode());
        row.setName(category.getName());
        row.setActive(category.getActive());
        row.setSort(category.getSort());
        row.setConfigured(isConfigured(rules));
        row.setViewRuleSummary(VIEW_RULE_SUMMARY);
        row.setRules(ruleVOs);
        row.setViewSubjects(toSubjectVOs(resolution.subjects()));
        row.setPendingPreviewRuleSummary(PENDING_PREVIEW_RULE_SUMMARY);
        row.setDownloadRuleSubjects(buildDownloadRuleSubjects(permissionRules));
        row.setDownloadRuleSummary(row.getDownloadRuleSubjects().isEmpty()
                ? DOWNLOAD_RULE_SUMMARY
                : "类别 DOWNLOAD 规则单独治理，当前配置 " + row.getDownloadRuleSubjects().size() + " 条主体");
        row.setRisks(combineRisks(resolution.risks(), buildDownloadRisks(permissionRules, resolution.subjects())));
        return row;
    }

    private DccCategoryViewMatrixEffectivePreviewRespVO buildPreview(Long categoryId,
                                                                     List<DccCategoryViewMatrixRowRespVO.Rule> rules,
                                                                     DccControlledFileViewMatrixAccessService.ViewMatrixAccessResolution resolution) {
        DccCategoryViewMatrixEffectivePreviewRespVO respVO = new DccCategoryViewMatrixEffectivePreviewRespVO();
        respVO.setCategoryId(categoryId);
        respVO.setViewRuleSummary(VIEW_RULE_SUMMARY);
        respVO.setPendingPreviewRuleSummary(PENDING_PREVIEW_RULE_SUMMARY);
        respVO.setDownloadRuleSubjects(buildDownloadRuleSubjects(categoryId));
        respVO.setDownloadRuleSummary(respVO.getDownloadRuleSubjects().isEmpty()
                ? DOWNLOAD_RULE_SUMMARY
                : "类别 DOWNLOAD 规则单独治理，当前配置 " + respVO.getDownloadRuleSubjects().size() + " 条主体");
        respVO.setRules(rules);
        respVO.setViewSubjects(toSubjectVOs(resolution.subjects()));
        List<DccCategoryViewMatrixRowRespVO.Risk> risks = combineRisks(resolution.risks(),
                buildDownloadRisks(categoryId, resolution.subjects()));
        respVO.setRisks(risks);
        respVO.setBlocking(risks.stream().anyMatch(risk -> Boolean.TRUE.equals(risk.getBlocking())));
        return respVO;
    }

    private DccCategoryViewMatrixUserLookupRespVO buildUserLookup(DccFileCategoryDO category, Long userId,
                                                                  boolean directoryAdmin) {
        DccControlledFileViewMatrixAccessService.ViewMatrixAccessResolution resolution =
                viewMatrixAccessService.resolveCurrentViewMatrixAccessDetails(category.getId());
        List<DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject> matchedSubjects =
                resolution.subjects().stream()
                        .filter(subject -> userId.equals(subject.userId()))
                        .toList();
        boolean viewMatched = !matchedSubjects.isEmpty();
        boolean downloadMatched = directoryAdmin || permissionSupport.evaluateCategoryPermission(category.getId(), userId,
                DccFileCategoryPermissionActionEnum.DOWNLOAD).allowed();
        if (!directoryAdmin && !viewMatched && !downloadMatched) {
            return null;
        }
        DccCategoryViewMatrixUserLookupRespVO respVO = new DccCategoryViewMatrixUserLookupRespVO();
        respVO.setCategoryId(category.getId());
        respVO.setCode(category.getCode());
        respVO.setName(category.getName());
        if (directoryAdmin) {
            setViewAbility(respVO, "YES", "DIRECTORY_ADMIN", "目录管理员");
        } else if (viewMatched) {
            setViewAbility(respVO, "YES", DccControlledFileViewMatrixAccessService.SOURCE_CURRENT_VIEW_MATRIX,
                    "当前查看矩阵参与人");
        } else {
            setViewAbility(respVO, "NO", "DENIED", "不在当前文件类型查看矩阵解析主体内");
        }
        respVO.setPendingPreviewStatus("CONDITIONAL");
        respVO.setPendingPreviewSource("ROUTE_SNAPSHOT");
        respVO.setPendingPreviewReason("仅对命中 route snapshot 的进行中文件实例生效");
        if (downloadMatched) {
            respVO.setDownloadStatus("CONDITIONAL");
            respVO.setDownloadSource(directoryAdmin ? "DIRECTORY_ADMIN" : "DOWNLOAD_RULE");
            respVO.setDownloadReason(directoryAdmin
                    ? "目录管理员进入独立下载判定，实际下载仍取决于下载策略与文件状态"
                    : "命中类别 DOWNLOAD 规则，实际下载仍取决于独立下载策略与目标文件状态");
        } else {
            respVO.setDownloadStatus("NO");
            respVO.setDownloadSource("DOWNLOAD_POLICY");
            respVO.setDownloadReason("未命中类别 DOWNLOAD 规则");
        }
        respVO.setViewSources(toSubjectVOs(matchedSubjects));
        respVO.setRisks(toRiskVOs(resolution.risks()));
        return respVO;
    }

    private void setViewAbility(DccCategoryViewMatrixUserLookupRespVO respVO, String status,
                                String source, String reason) {
        respVO.setBrowseStatus(status);
        respVO.setBrowseSource(source);
        respVO.setBrowseReason(reason);
        respVO.setDetailStatus(status);
        respVO.setDetailSource(source);
        respVO.setDetailReason(reason);
        respVO.setPublishedPreviewStatus(status);
        respVO.setPublishedPreviewSource(source);
        respVO.setPublishedPreviewReason(reason);
    }

    private List<DccControlledFileViewMatrixAccessService.ViewMatrixRuleInput> toInputs(
            Long categoryId, DccCategoryViewMatrixSaveReqVO reqVO) {
        List<DccCategoryViewMatrixSaveReqVO.Rule> rules = reqVO == null || reqVO.getRules() == null
                ? List.of() : reqVO.getRules();
        return rules.stream()
                .map(rule -> new DccControlledFileViewMatrixAccessService.ViewMatrixRuleInput(
                        rule.getId(), categoryId, rule.getExcelFileName(), rule.getExcelRowNo(),
                        rule.getExcelColumnLetter(), rule.getSubjectLabel(), rule.getSubjectTopHeader(),
                        rule.getSubjectSubHeader(), rule.getMarker(), rule.getScopeType(), rule.getSubjectType(),
                        rule.getSubjectId(), Boolean.TRUE.equals(rule.getActive()), rule.getRemark()))
                .toList();
    }

    private DccCategoryViewMatrixRuleDO toDO(Long categoryId, DccCategoryViewMatrixSaveReqVO.Rule rule) {
        return DccCategoryViewMatrixRuleDO.builder()
                // 保存采用“删除旧规则后重建规则集”，新记录必须生成新主键，不能复用编辑态旧 ID。
                .id(null)
                .categoryId(categoryId)
                .excelFileName(rule.getExcelFileName())
                .excelRowNo(rule.getExcelRowNo())
                .excelColumnLetter(rule.getExcelColumnLetter())
                .subjectLabel(rule.getSubjectLabel())
                .subjectTopHeader(rule.getSubjectTopHeader())
                .subjectSubHeader(rule.getSubjectSubHeader())
                .marker(rule.getMarker())
                .scopeType(deriveScopeType(rule))
                .subjectType(rule.getSubjectType())
                .subjectId(rule.getSubjectId())
                .active(Boolean.TRUE.equals(rule.getActive()))
                .remark(rule.getRemark())
                .build();
    }

    private String deriveScopeType(DccCategoryViewMatrixSaveReqVO.Rule rule) {
        if (StrUtil.isNotBlank(rule.getScopeType())) {
            return rule.getScopeType();
        }
        if ("●".equals(rule.getMarker())) {
            return DccControlledFileViewMatrixAccessService.SCOPE_ALL_MEMBERS;
        }
        if ("▲".equals(rule.getMarker())) {
            return DccControlledFileViewMatrixAccessService.SCOPE_MANAGER_AND_ABOVE;
        }
        return rule.getScopeType();
    }

    private List<DccCategoryViewMatrixRowRespVO.Rule> toRuleVOs(DccCategoryViewMatrixSaveReqVO reqVO) {
        if (reqVO == null || reqVO.getRules() == null) {
            return List.of();
        }
        return reqVO.getRules().stream().map(this::toRuleVO).toList();
    }

    private DccCategoryViewMatrixRowRespVO.Rule toRuleVO(DccCategoryViewMatrixSaveReqVO.Rule rule) {
        DccCategoryViewMatrixRowRespVO.Rule vo = new DccCategoryViewMatrixRowRespVO.Rule();
        vo.setId(rule.getId());
        vo.setExcelFileName(rule.getExcelFileName());
        vo.setExcelRowNo(rule.getExcelRowNo());
        vo.setExcelColumnLetter(rule.getExcelColumnLetter());
        vo.setSubjectLabel(rule.getSubjectLabel());
        vo.setSubjectTopHeader(rule.getSubjectTopHeader());
        vo.setSubjectSubHeader(rule.getSubjectSubHeader());
        vo.setMarker(rule.getMarker());
        vo.setScopeType(rule.getScopeType());
        vo.setSubjectType(rule.getSubjectType());
        vo.setSubjectId(rule.getSubjectId());
        vo.setSubjectDepartmentPath(resolveSubjectDepartmentPath(rule.getSubjectType(), rule.getSubjectId()));
        vo.setActive(Boolean.TRUE.equals(rule.getActive()));
        vo.setRemark(rule.getRemark());
        return vo;
    }

    private DccCategoryViewMatrixRowRespVO.Rule toRuleVO(DccCategoryViewMatrixRuleDO rule) {
        return toRuleVO(rule, new ViewMatrixRowBuildContext(viewMatrixAccessService.newResolutionContext()));
    }

    private DccCategoryViewMatrixRowRespVO.Rule toRuleVO(DccCategoryViewMatrixRuleDO rule,
                                                        ViewMatrixRowBuildContext context) {
        DccCategoryViewMatrixRowRespVO.Rule vo = new DccCategoryViewMatrixRowRespVO.Rule();
        vo.setId(rule.getId());
        vo.setExcelFileName(rule.getExcelFileName());
        vo.setExcelRowNo(rule.getExcelRowNo());
        vo.setExcelColumnLetter(rule.getExcelColumnLetter());
        vo.setSubjectLabel(rule.getSubjectLabel());
        vo.setSubjectTopHeader(rule.getSubjectTopHeader());
        vo.setSubjectSubHeader(rule.getSubjectSubHeader());
        vo.setMarker(rule.getMarker());
        vo.setScopeType(rule.getScopeType());
        vo.setSubjectType(rule.getSubjectType());
        vo.setSubjectId(rule.getSubjectId());
        vo.setSubjectName(resolveSubjectName(rule));
        vo.setSubjectDepartmentPath(resolveSubjectDepartmentPath(rule.getSubjectType(), rule.getSubjectId(), context));
        vo.setActive(rule.getActive());
        vo.setRemark(rule.getRemark());
        return vo;
    }

    private String resolveSubjectName(DccCategoryViewMatrixRuleDO rule) {
        if (StrUtil.isNotBlank(rule.getSubjectLabel())) {
            return rule.getSubjectLabel();
        }
        if (StrUtil.isBlank(rule.getSubjectType()) || rule.getSubjectId() == null) {
            return null;
        }
        return rule.getSubjectType() + "#" + rule.getSubjectId();
    }

    private String resolveSubjectDepartmentPath(String subjectType, Long subjectId) {
        return resolveSubjectDepartmentPath(subjectType, subjectId,
                new ViewMatrixRowBuildContext(viewMatrixAccessService.newResolutionContext()));
    }

    private String resolveSubjectDepartmentPath(String subjectType, Long subjectId, ViewMatrixRowBuildContext context) {
        if (!DccControlledFileViewMatrixAccessService.SUBJECT_DEPT.equalsIgnoreCase(StrUtil.blankToDefault(subjectType, ""))
                || subjectId == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        Set<Long> visited = new LinkedHashSet<>();
        Long currentId = subjectId;
        while (currentId != null && currentId > 0 && visited.add(currentId)) {
            DeptRespDTO dept = getDept(currentId, context);
            if (dept == null || StrUtil.isBlank(dept.getName())) {
                return "部门不存在（" + currentId + "）";
            }
            names.add(dept.getName());
            Long parentId = dept.getParentId();
            if (parentId == null || Objects.equals(parentId, currentId)) {
                break;
            }
            currentId = parentId;
        }
        Collections.reverse(names);
        return String.join("-", names);
    }

    private List<DccCategoryViewMatrixRowRespVO.Subject> toSubjectVOs(
            List<DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject> subjects) {
        if (subjects == null) {
            return List.of();
        }
        return subjects.stream().map(this::toSubjectVO).toList();
    }

    private DccCategoryViewMatrixRowRespVO.Subject toSubjectVO(
            DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject subject) {
        DccCategoryViewMatrixRowRespVO.Subject vo = new DccCategoryViewMatrixRowRespVO.Subject();
        vo.setUserId(subject.userId());
        vo.setUserName(subject.userName());
        vo.setSource(subject.source());
        vo.setExcelFileName(subject.excelFileName());
        vo.setExcelRowNo(subject.excelRowNo());
        vo.setExcelColumnLetter(subject.excelColumnLetter());
        vo.setSubjectLabel(subject.subjectLabel());
        vo.setMarker(subject.marker());
        vo.setScopeType(subject.scopeType());
        vo.setSubjectType(subject.subjectType());
        vo.setSubjectId(subject.subjectId());
        vo.setReason(subject.reason());
        return vo;
    }

    private List<DccCategoryViewMatrixRowRespVO.Risk> combineRisks(
            List<DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk> matrixRisks,
            List<DccCategoryViewMatrixRowRespVO.Risk> downloadRisks) {
        List<DccCategoryViewMatrixRowRespVO.Risk> risks = new java.util.ArrayList<>(toRiskVOs(matrixRisks));
        risks.addAll(downloadRisks);
        return risks;
    }

    private List<DccCategoryViewMatrixRowRespVO.Risk> toRiskVOs(
            List<DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk> risks) {
        if (risks == null) {
            return List.of();
        }
        return risks.stream().map(this::toRiskVO).toList();
    }

    private DccCategoryViewMatrixRowRespVO.Risk toRiskVO(
            DccControlledFileViewMatrixAccessService.ViewMatrixAccessRisk risk) {
        DccCategoryViewMatrixRowRespVO.Risk vo = new DccCategoryViewMatrixRowRespVO.Risk();
        vo.setCode(risk.code());
        vo.setMessage(risk.message());
        vo.setSeverity(risk.severity());
        vo.setBlocking(risk.blocking());
        return vo;
    }

    private List<DccCategoryViewMatrixRowRespVO.Risk> buildDownloadRisks(
            Long categoryId, List<DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject> viewSubjects) {
        return buildDownloadRisks(permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId,
                categoryId), viewSubjects);
    }

    private List<DccCategoryViewMatrixRowRespVO.Risk> buildDownloadRisks(
            List<DccFileCategoryPermissionRuleDO> permissionRules,
            List<DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject> viewSubjects) {
        List<DccFileCategoryPermissionRuleDO> downloadRules = permissionRules.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .filter(rule -> DccFileCategoryPermissionActionEnum.DOWNLOAD.getCode().equalsIgnoreCase(rule.getActionType()))
                .toList();
        if (downloadRules.isEmpty()) {
            return List.of();
        }
        Set<Long> viewUserIds = viewSubjects == null ? Set.of() : viewSubjects.stream()
                .map(DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<DccCategoryViewMatrixRowRespVO.Risk> risks = new java.util.ArrayList<>();
        boolean userRuleOutsideView = downloadRules.stream()
                .filter(rule -> "USER".equalsIgnoreCase(rule.getSubjectType()))
                .map(DccFileCategoryPermissionRuleDO::getSubjectId)
                .anyMatch(subjectId -> subjectId != null && !viewUserIds.contains(subjectId));
        if (userRuleOutsideView) {
            risks.add(riskVO("DOWNLOAD_RULE_DIVERGENCE",
                    "存在 DOWNLOAD 用户主体不在当前查看矩阵解析主体内，下载与查阅口径存在差异，请管理员确认是否符合业务规则。",
                    "WARNING", false));
        }
        boolean nonUserRule = downloadRules.stream()
                .anyMatch(rule -> !"USER".equalsIgnoreCase(rule.getSubjectType()));
        if (nonUserRule) {
            risks.add(riskVO("DOWNLOAD_RULE_MANUAL_REVIEW_REQUIRED",
                    "当前 DOWNLOAD 规则包含非 USER 主体，系统仅展示原始规则，请管理员人工复核其与查看矩阵的一致性。",
                    "WARNING", false));
        }
        return risks;
    }

    private DccCategoryViewMatrixRowRespVO.Risk riskVO(String code, String message, String severity, boolean blocking) {
        DccCategoryViewMatrixRowRespVO.Risk risk = new DccCategoryViewMatrixRowRespVO.Risk();
        risk.setCode(code);
        risk.setMessage(message);
        risk.setSeverity(severity);
        risk.setBlocking(blocking);
        return risk;
    }

    private List<String> buildDownloadRuleSubjects(Long categoryId) {
        return buildDownloadRuleSubjects(permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId,
                categoryId));
    }

    private List<String> buildDownloadRuleSubjects(List<DccFileCategoryPermissionRuleDO> permissionRules) {
        return permissionRules.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .filter(rule -> DccFileCategoryPermissionActionEnum.DOWNLOAD.getCode().equalsIgnoreCase(rule.getActionType()))
                .map(rule -> rule.getSubjectType() + "#" + rule.getSubjectId())
                .toList();
    }

    private boolean isConfigured(List<DccCategoryViewMatrixRuleDO> rules) {
        return rules != null && rules.stream().anyMatch(rule -> Boolean.TRUE.equals(rule.getActive()));
    }

    private DeptRespDTO getDept(Long deptId, ViewMatrixRowBuildContext context) {
        if (!context.deptById.containsKey(deptId)) {
            context.deptById.put(deptId, deptApi.getDept(deptId));
        }
        return context.deptById.get(deptId);
    }

    private boolean matchesKeyword(String value, String keyword) {
        return StrUtil.isBlank(keyword) || StrUtil.containsIgnoreCase(StrUtil.blankToDefault(value, ""), keyword.trim());
    }

    private void validateCategoryExists(Long categoryId) {
        if (categoryId == null || categoryMapper.selectById(categoryId) == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
    }

    private static class ViewMatrixRowBuildContext {

        private final DccControlledFileViewMatrixAccessService.ViewMatrixResolutionContext matrixContext;
        private final Map<Long, DeptRespDTO> deptById = new LinkedHashMap<>();

        private ViewMatrixRowBuildContext(
                DccControlledFileViewMatrixAccessService.ViewMatrixResolutionContext matrixContext) {
            this.matrixContext = matrixContext;
        }
    }
}
