package cn.iocoder.yudao.module.dcc.service.category;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixEffectivePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixUserLookupRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileCategoryPermissionSupport;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileReviewMatrixAccessService;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_APPROVAL_COUNT_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_DOC_CONTROL_POSITION_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_EFFECTIVE_ACCESS_BLOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_POSITION_INACTIVE_OR_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_SIGNOFF_EMPTY;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;

@Service
@Validated
public class DccCategoryApprovalMatrixAdminServiceImpl implements DccCategoryApprovalMatrixAdminService {

    public static final ErrorCode CATEGORY_APPROVAL_MATRIX_SIGNOFF_EMPTY =
            cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_SIGNOFF_EMPTY;
    public static final ErrorCode CATEGORY_APPROVAL_MATRIX_APPROVAL_COUNT_INVALID =
            cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_APPROVAL_COUNT_INVALID;
    public static final ErrorCode CATEGORY_APPROVAL_MATRIX_DOC_CONTROL_POSITION_MISSING =
            cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_DOC_CONTROL_POSITION_MISSING;
    public static final ErrorCode CATEGORY_APPROVAL_MATRIX_EFFECTIVE_ACCESS_BLOCKED =
            cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CATEGORY_APPROVAL_MATRIX_EFFECTIVE_ACCESS_BLOCKED;

    private static final String DOC_CONTROL_POSITION_NAME = "文控";
    private static final String VIEW_RULE_SUMMARY = "当前审阅矩阵参与人可浏览、查看详情和预览已发布受控副本";
    private static final String PENDING_PREVIEW_RULE_SUMMARY =
            "进行中文件待审原件预览继续按提交时 route snapshot 参与人放行";
    private static final String DOWNLOAD_RULE_SUMMARY =
            "下载规则单独治理，仅展示类别 DOWNLOAD 规则，不改变下载判定逻辑";

    private static final Map<Integer, FixedStageDefinition> FIXED_STAGE_MAP = List.of(
            new FixedStageDefinition(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "文控审核", 1,
                    "ANY", false),
            new FixedStageDefinition(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), "审核会签", 2,
                    "ALL", true),
            new FixedStageDefinition(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), "批准", 3,
                    "ANY", false),
            new FixedStageDefinition(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(), "文控批准", 4,
                    "ANY", false)
    ).stream().collect(Collectors.toMap(FixedStageDefinition::stageNo, Function.identity()));

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccApprovalPositionMapper positionMapper;
    @Resource
    private DccCategoryApprovalRouteMapper routeMapper;
    @Resource
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Resource
    private DccControlledFileReviewMatrixAccessService reviewMatrixAccessService;
    @Resource
    private DccApprovalPositionRuntimeResolver positionRuntimeResolver;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;

    @Override
    public DccCategoryApprovalMatrixRespVO getApprovalMatrix(Long categoryId) {
        validateCategoryExists(categoryId);
        DccCategoryApprovalRouteDO route = routeMapper.selectLatestActiveByCategoryId(categoryId);
        DccCategoryApprovalMatrixRespVO respVO = new DccCategoryApprovalMatrixRespVO();
        respVO.setCategoryId(categoryId);
        respVO.setRules(List.of());
        if (route == null) {
            return respVO;
        }
        List<DccCategoryApprovalRouteNodeDO> nodes = routeNodeMapper.selectListByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageOrder,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getStageNo))
                .toList();
        respVO.setRouteVersionNo(route.getVersionNo());
        respVO.setEffectiveTime(route.getEffectiveTime());
        respVO.setRemark(route.getRemark());
        Map<Long, String> positionNameMap = buildActivePositionNameMap();
        respVO.setRules(buildEditableRules(nodes, positionNameMap));
        return respVO;
    }

    @Override
    public List<DccCategoryReviewMatrixRowRespVO> getReviewMatrixRows(String code, String name, Boolean active,
                                                                      Boolean configured) {
        Map<Long, String> positionNameMap = positionMapper.selectList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .collect(Collectors.toMap(DccApprovalPositionDO::getId, DccApprovalPositionDO::getName,
                        (left, right) -> left));
        return categoryMapper.selectList().stream()
                .filter(item -> matchesKeyword(item.getCode(), code))
                .filter(item -> matchesKeyword(item.getName(), name))
                .filter(item -> active == null || Objects.equals(item.getActive(), active))
                .map(item -> buildReviewMatrixRow(item, positionNameMap))
                .filter(item -> configured == null || Objects.equals(item.getConfigured(), configured))
                .sorted(Comparator.comparing(DccCategoryReviewMatrixRowRespVO::getCode, Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccCategoryReviewMatrixRowRespVO::getCategoryId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    public DccCategoryReviewMatrixEffectivePreviewRespVO previewApprovalMatrix(Long categoryId,
                                                                               DccCategoryApprovalMatrixSaveReqVO reqVO) {
        validateCategoryExists(categoryId);
        List<DccCategoryApprovalMatrixSaveReqVO.Rule> signoffRules = getStageRules(reqVO, "SIGNOFF");
        List<DccCategoryApprovalMatrixSaveReqVO.Rule> approvalRules = getStageRules(reqVO, "APPROVAL");
        if (signoffRules.isEmpty()) {
            throw exception(CATEGORY_APPROVAL_MATRIX_SIGNOFF_EMPTY);
        }
        if (approvalRules.isEmpty()) {
            throw exception(CATEGORY_APPROVAL_MATRIX_APPROVAL_COUNT_INVALID);
        }
        DccApprovalPositionDO docControlPosition = resolveDocControlPosition();
        Map<Long, String> positionNameMap = buildActivePositionNameMap();
        Integer nextRouteVersionNo = routeMapper.selectList(DccCategoryApprovalRouteDO::getCategoryId, categoryId).stream()
                .map(DccCategoryApprovalRouteDO::getVersionNo)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        return buildMatrixPreview(categoryId, nextRouteVersionNo,
                buildDerivedNodes(docControlPosition.getId(), signoffRules, approvalRules),
                positionNameMap);
    }

    @Override
    public List<DccCategoryReviewMatrixUserLookupRespVO> getUserReviewMatrixAccess(Long userId) {
        if (userId == null) {
            return List.of();
        }
        boolean directoryAdmin = directoryAccessPermissionService.hasDirectoryManagementPermission(userId);
        return categoryMapper.selectList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .map(item -> buildUserLookup(item, userId, directoryAdmin))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(DccCategoryReviewMatrixUserLookupRespVO::getCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(DccCategoryReviewMatrixUserLookupRespVO::getCategoryId,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccCategoryApprovalRouteDO saveApprovalMatrix(Long categoryId, DccCategoryApprovalMatrixSaveReqVO reqVO) {
        MatrixSaveContext context = prepareMatrixSaveContext(categoryId, reqVO);
        DccCategoryReviewMatrixEffectivePreviewRespVO preview = buildMatrixPreview(categoryId,
                context.nextRouteVersionNo(),
                context.nodes(),
                context.positionNameMap());
        if (Boolean.TRUE.equals(preview.getBlocking())) {
            throw exception(CATEGORY_APPROVAL_MATRIX_EFFECTIVE_ACCESS_BLOCKED,
                    preview.getRisks().stream()
                            .filter(risk -> Boolean.TRUE.equals(risk.getBlocking()))
                            .map(DccCategoryReviewMatrixEffectivePreviewRespVO.Risk::getMessage)
                            .distinct()
                            .collect(Collectors.joining("；")));
        }
        return persistApprovalMatrix(context);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccCategoryApprovalRouteDO importApprovalMatrix(Long categoryId, DccCategoryApprovalMatrixSaveReqVO reqVO) {
        return persistApprovalMatrix(prepareMatrixSaveContext(categoryId, reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApprovalMatrix(Long categoryId) {
        validateCategoryExists(categoryId);
        routeMapper.selectList(DccCategoryApprovalRouteDO::getCategoryId, categoryId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .forEach(item -> routeMapper.updateById(DccCategoryApprovalRouteDO.builder()
                        .id(item.getId())
                        .active(Boolean.FALSE)
                        .build()));
        removeMatrixManagedPermissionRules(categoryId);
    }

    private DccCategoryReviewMatrixRowRespVO buildReviewMatrixRow(DccFileCategoryDO category,
                                                                  Map<Long, String> positionNameMap) {
        DccCategoryApprovalRouteDO route = routeMapper.selectLatestActiveByCategoryId(category.getId());
        DccCategoryReviewMatrixRowRespVO row = new DccCategoryReviewMatrixRowRespVO();
        row.setCategoryId(category.getId());
        row.setCode(category.getCode());
        row.setName(category.getName());
        row.setLifecycleStage(category.getLifecycleStage());
        row.setActive(category.getActive());
        row.setConfigured(route != null);
        row.setRules(List.of());
        row.setViewRuleSummary(VIEW_RULE_SUMMARY);
        row.setPendingPreviewRuleSummary(PENDING_PREVIEW_RULE_SUMMARY);
        row.setViewSubjects(List.of());
        row.setDownloadRuleSubjects(buildDownloadRuleSubjects(category.getId()));
        row.setDownloadRuleSummary(row.getDownloadRuleSubjects().isEmpty()
                ? DOWNLOAD_RULE_SUMMARY
                : "类别 DOWNLOAD 规则单独治理，当前配置 " + row.getDownloadRuleSubjects().size() + " 条主体");
        row.setRisks(List.of());
        if (route == null) {
            DccCategoryReviewMatrixRowRespVO.Risk risk = new DccCategoryReviewMatrixRowRespVO.Risk();
            risk.setCode("MATRIX_NOT_CONFIGURED");
            risk.setMessage("当前文件类型未配置生效审阅矩阵，普通查阅权限无法解析。");
            risk.setSeverity("BLOCKING");
            risk.setBlocking(Boolean.TRUE);
            row.setRisks(List.of(risk));
            return row;
        }
        List<DccCategoryApprovalRouteNodeDO> nodes = routeNodeMapper.selectListByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageOrder,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getStageNo))
                .toList();
        row.setRouteVersionNo(route.getVersionNo());
        row.setEffectiveTime(route.getEffectiveTime());
        row.setRemark(route.getRemark());
        row.setRules(buildRowRules(nodes, positionNameMap));
        DccCategoryReviewMatrixEffectivePreviewRespVO preview = buildMatrixPreview(category.getId(), route.getVersionNo(),
                nodes, positionNameMap);
        row.setViewRuleSummary(preview.getViewRuleSummary());
        row.setPendingPreviewRuleSummary(preview.getPendingPreviewRuleSummary());
        row.setViewSubjects(convertList(preview.getViewSubjects(), this::toRowSubject));
        row.setDownloadRuleSubjects(preview.getDownloadRuleSubjects());
        row.setDownloadRuleSummary(preview.getDownloadRuleSummary());
        row.setRisks(convertList(preview.getRisks(), this::toRowRisk));
        return row;
    }

    private List<String> buildDownloadRuleSubjects(Long categoryId) {
        return permissionRuleMapper.selectList(DccFileCategoryPermissionRuleDO::getCategoryId, categoryId).stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .filter(rule -> "DOWNLOAD".equals(rule.getActionType()))
                .map(rule -> rule.getSubjectType() + "#" + rule.getSubjectId())
                .toList();
    }

    private Map<Long, String> buildActivePositionNameMap() {
        return positionMapper.selectList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .collect(Collectors.toMap(DccApprovalPositionDO::getId, DccApprovalPositionDO::getName,
                        (left, right) -> left));
    }

    private DccCategoryReviewMatrixEffectivePreviewRespVO buildMatrixPreview(Long categoryId, Integer nextRouteVersionNo,
                                                                             List<DccCategoryApprovalRouteNodeDO> nodes,
                                                                             Map<Long, String> positionNameMap) {
        DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessResolution accessResolution =
                reviewMatrixAccessService.previewReviewMatrixAccessDetails(null, nodes);
        DccCategoryReviewMatrixEffectivePreviewRespVO respVO = new DccCategoryReviewMatrixEffectivePreviewRespVO();
        respVO.setCategoryId(categoryId);
        respVO.setNextRouteVersionNo(nextRouteVersionNo);
        respVO.setViewRuleSummary(VIEW_RULE_SUMMARY);
        respVO.setPendingPreviewRuleSummary(PENDING_PREVIEW_RULE_SUMMARY);
        respVO.setDownloadRuleSubjects(buildDownloadRuleSubjects(categoryId));
        respVO.setDownloadRuleSummary(respVO.getDownloadRuleSubjects().isEmpty()
                ? DOWNLOAD_RULE_SUMMARY
                : "类别 DOWNLOAD 规则单独治理，当前配置 " + respVO.getDownloadRuleSubjects().size() + " 条主体");
        respVO.setRules(buildRowRules(nodes, positionNameMap));
        respVO.setStages(buildStagePreviews(nodes, accessResolution.subjects(), positionNameMap));
        respVO.setViewSubjects(deduplicatePreviewSubjects(accessResolution.subjects()));
        List<DccCategoryReviewMatrixEffectivePreviewRespVO.Risk> risks = new ArrayList<>();
        risks.addAll(convertList(accessResolution.risks(), this::toPreviewRisk));
        risks.addAll(buildDuplicateSubjectRisks(accessResolution.subjects()));
        risks.addAll(buildSnapshotDriftRisks(categoryId, accessResolution.subjects()));
        risks.addAll(buildDownloadRisks(categoryId, accessResolution.subjects()));
        respVO.setRisks(deduplicateRisks(risks));
        respVO.setBlocking(respVO.getRisks().stream().anyMatch(risk -> Boolean.TRUE.equals(risk.getBlocking())));
        return respVO;
    }

    private List<DccCategoryReviewMatrixEffectivePreviewRespVO.StagePreview> buildStagePreviews(
            List<DccCategoryApprovalRouteNodeDO> nodes,
            List<DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessSubject> subjects,
            Map<Long, String> positionNameMap) {
        if (nodes == null) {
            return List.of();
        }
        Map<Integer, List<DccCategoryApprovalRouteNodeDO>> stageNodeMap = nodes.stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageOrder,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getStageNo))
                .collect(Collectors.groupingBy(DccCategoryApprovalRouteNodeDO::getStageNo, LinkedHashMap::new,
                        Collectors.toList()));
        List<DccCategoryReviewMatrixEffectivePreviewRespVO.StagePreview> previews = new ArrayList<>();
        stageNodeMap.forEach((stageNo, stageNodes) -> {
            if (stageNodes == null || stageNodes.isEmpty()) {
                return;
            }
            DccCategoryApprovalRouteNodeDO firstNode = stageNodes.get(0);
            List<NormalizedRuleInput> normalizedRules = stageNodes.stream()
                    .flatMap(node -> buildNormalizedRuleInputs(node, positionNameMap).stream())
                    .toList();
            DccCategoryReviewMatrixEffectivePreviewRespVO.StagePreview preview =
                    new DccCategoryReviewMatrixEffectivePreviewRespVO.StagePreview();
            preview.setStageNo(stageNo);
            preview.setStageName(firstNode.getStageName());
            preview.setStageType(normalizedRules.stream()
                    .map(NormalizedRuleInput::stageType)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse(null));
            preview.setApproveMethod(firstNode.getApproveMethod());
            List<Long> candidateIds = normalizedRules.stream()
                    .map(NormalizedRuleInput::subjectId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            preview.setPositionIds(candidateIds);
            preview.setPositionNames(normalizedRules.stream()
                    .map(this::resolveNormalizedRuleDisplayName)
                    .filter(StrUtil::isNotBlank)
                    .map(name -> name + " ▲")
                    .distinct()
                    .toList());
            preview.setSourceRule(resolveStageSourceRule(stageNodes));
            preview.setResolvedSubjects(subjects.stream()
                    .filter(subject -> Objects.equals(subject.stageNo(), stageNo))
                    .collect(Collectors.toMap(subject -> subject.userId() + "#" + subject.positionId(),
                            this::toPreviewSubject, (left, right) -> left, LinkedHashMap::new))
                    .values().stream()
                    .toList());
            previews.add(preview);
        });
        return previews;
    }

    private List<DccCategoryReviewMatrixEffectivePreviewRespVO.Subject> deduplicatePreviewSubjects(
            List<DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessSubject> subjects) {
        Map<Long, DccCategoryReviewMatrixEffectivePreviewRespVO.Subject> subjectMap = new LinkedHashMap<>();
        subjects.stream()
                .filter(subject -> subject.userId() != null)
                .forEach(subject -> subjectMap.putIfAbsent(subject.userId(), toPreviewSubject(subject)));
        return new ArrayList<>(subjectMap.values());
    }

    private DccCategoryReviewMatrixRowRespVO.Subject toRowSubject(
            DccCategoryReviewMatrixEffectivePreviewRespVO.Subject subject) {
        DccCategoryReviewMatrixRowRespVO.Subject respVO = new DccCategoryReviewMatrixRowRespVO.Subject();
        respVO.setUserId(subject.getUserId());
        respVO.setUserName(subject.getUserName());
        respVO.setSource(subject.getSource());
        respVO.setStageNo(subject.getStageNo());
        respVO.setStageName(subject.getStageName());
        respVO.setStageType(subject.getStageType());
        respVO.setPositionId(subject.getPositionId());
        respVO.setPositionName(subject.getPositionName());
        respVO.setSubjectLabel(subject.getSubjectLabel());
        respVO.setMarker(subject.getMarker());
        respVO.setSubjectType(subject.getSubjectType());
        respVO.setSubjectId(subject.getSubjectId());
        respVO.setReason(subject.getReason());
        return respVO;
    }

    private DccCategoryReviewMatrixRowRespVO.Risk toRowRisk(
            DccCategoryReviewMatrixEffectivePreviewRespVO.Risk risk) {
        DccCategoryReviewMatrixRowRespVO.Risk respVO = new DccCategoryReviewMatrixRowRespVO.Risk();
        respVO.setCode(risk.getCode());
        respVO.setMessage(risk.getMessage());
        respVO.setSeverity(risk.getSeverity());
        respVO.setBlocking(risk.getBlocking());
        return respVO;
    }

    private DccCategoryReviewMatrixEffectivePreviewRespVO.Subject toPreviewSubject(
            DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessSubject subject) {
        DccCategoryReviewMatrixEffectivePreviewRespVO.Subject respVO = new DccCategoryReviewMatrixEffectivePreviewRespVO.Subject();
        respVO.setUserId(subject.userId());
        respVO.setUserName(subject.userName());
        respVO.setSource(subject.source());
        respVO.setStageNo(subject.stageNo());
        respVO.setStageName(subject.stageName());
        respVO.setStageType(subject.stageType());
        respVO.setPositionId(subject.positionId());
        respVO.setPositionName(subject.positionName());
        respVO.setSubjectLabel(subject.subjectLabel());
        respVO.setMarker(subject.marker());
        respVO.setSubjectType(subject.subjectType());
        respVO.setSubjectId(subject.subjectId());
        respVO.setReason(subject.reason());
        return respVO;
    }

    private DccCategoryReviewMatrixEffectivePreviewRespVO.Risk toPreviewRisk(
            DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessRisk risk) {
        return createRisk(risk.code(), risk.message(), isBlockingRisk(risk.code()) ? "BLOCKING" : "WARNING",
                isBlockingRisk(risk.code()));
    }

    private List<DccCategoryReviewMatrixEffectivePreviewRespVO.Risk> buildDuplicateSubjectRisks(
            List<DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessSubject> subjects) {
        Map<Long, Set<Integer>> stageMap = new LinkedHashMap<>();
        subjects.stream()
                .filter(subject -> subject.userId() != null)
                .forEach(subject -> stageMap.computeIfAbsent(subject.userId(), key -> new LinkedHashSet<>())
                        .add(subject.stageNo()));
        List<DccCategoryReviewMatrixEffectivePreviewRespVO.Risk> risks = new ArrayList<>();
        stageMap.forEach((userId, stageNos) -> {
            if (stageNos.size() <= 1) {
                return;
            }
            if (stageNos.equals(Set.of(1, 4))) {
                return;
            }
            risks.add(createRisk("DUPLICATE_SUBJECT",
                    "用户#" + userId + " 同时出现在多个查阅阶段 " + stageNos + "，请确认是否符合岗位职责分离。",
                    "WARNING", false));
        });
        return risks;
    }

    private List<DccCategoryReviewMatrixEffectivePreviewRespVO.Risk> buildSnapshotDriftRisks(
            Long categoryId,
            List<DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessSubject> subjects) {
        if (categoryId == null) {
            return List.of();
        }
        Set<Long> currentUserIds = subjects.stream()
                .map(DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessSubject::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        long driftCount = controlledFileMapper.selectList(DccControlledFileDO::getCategoryId, categoryId).stream()
                .filter(file -> isSnapshotSensitiveStatus(file.getStatus()))
                .filter(file -> !currentUserIds.equals(routeSnapshotMapper.selectListByControlledFileId(file.getId()).stream()
                        .flatMap(snapshot -> parseResolvedUserIds(snapshot).stream())
                        .collect(Collectors.toCollection(LinkedHashSet::new))))
                .count();
        if (driftCount == 0) {
            return List.of();
        }
        return List.of(createRisk("SNAPSHOT_DRIFT",
                "当前类别有 " + driftCount + " 个进行中文件的 route snapshot 与当前矩阵解析结果不同，待审原件预览和审批仍将按历史 snapshot 执行。",
                "WARNING", false));
    }

    private List<DccCategoryReviewMatrixEffectivePreviewRespVO.Risk> buildDownloadRisks(
            Long categoryId,
            List<DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessSubject> subjects) {
        List<DccFileCategoryPermissionRuleDO> downloadRules = permissionRuleMapper
                .selectList(DccFileCategoryPermissionRuleDO::getCategoryId, categoryId).stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .filter(rule -> DccFileCategoryPermissionActionEnum.DOWNLOAD.getCode().equalsIgnoreCase(rule.getActionType()))
                .toList();
        if (downloadRules.isEmpty()) {
            return List.of();
        }
        Set<Long> viewUserIds = subjects.stream()
                .map(DccControlledFileReviewMatrixAccessService.ReviewMatrixAccessSubject::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean hasUserRuleOutsideView = downloadRules.stream()
                .filter(rule -> "USER".equalsIgnoreCase(rule.getSubjectType()))
                .map(DccFileCategoryPermissionRuleDO::getSubjectId)
                .filter(Objects::nonNull)
                .anyMatch(userId -> !viewUserIds.contains(userId));
        List<DccCategoryReviewMatrixEffectivePreviewRespVO.Risk> risks = new ArrayList<>();
        if (hasUserRuleOutsideView) {
            risks.add(createRisk("DOWNLOAD_RULE_DIVERGENCE",
                    "存在 DOWNLOAD 用户主体不在当前查阅矩阵参与人内，下载与查阅口径存在差异，请管理员确认是否符合业务规则。",
                    "WARNING", false));
        }
        boolean hasNonUserRule = downloadRules.stream()
                .anyMatch(rule -> !"USER".equalsIgnoreCase(rule.getSubjectType()));
        if (hasNonUserRule) {
            risks.add(createRisk("DOWNLOAD_RULE_MANUAL_REVIEW_REQUIRED",
                    "当前 DOWNLOAD 规则包含非 USER 主体，系统仅展示原始规则，请管理员人工复核其与查阅矩阵的一致性。",
                    "WARNING", false));
        }
        return risks;
    }

    private List<DccCategoryReviewMatrixEffectivePreviewRespVO.Risk> deduplicateRisks(
            List<DccCategoryReviewMatrixEffectivePreviewRespVO.Risk> risks) {
        Map<String, DccCategoryReviewMatrixEffectivePreviewRespVO.Risk> riskMap = new LinkedHashMap<>();
        risks.forEach(risk -> riskMap.putIfAbsent(risk.getCode() + "#" + risk.getMessage(), risk));
        return new ArrayList<>(riskMap.values());
    }

    private DccCategoryReviewMatrixEffectivePreviewRespVO.Risk createRisk(String code, String message,
                                                                          String severity, boolean blocking) {
        DccCategoryReviewMatrixEffectivePreviewRespVO.Risk risk = new DccCategoryReviewMatrixEffectivePreviewRespVO.Risk();
        risk.setCode(code);
        risk.setMessage(message);
        risk.setSeverity(severity);
        risk.setBlocking(blocking);
        return risk;
    }

    private boolean isBlockingRisk(String code) {
        return Set.of("MATRIX_NOT_CONFIGURED", "MATRIX_NO_RESOLVED_USER", "POSITION_EMPTY", "DEPT_LEADER_MISSING", "DEPT_LEADER_USER_NOT_FOUND", "ROLE_EMPTY").contains(code);
    }

    private MatrixSaveContext prepareMatrixSaveContext(Long categoryId, DccCategoryApprovalMatrixSaveReqVO reqVO) {
        validateCategoryExists(categoryId);
        List<DccCategoryApprovalMatrixSaveReqVO.Rule> signoffRules = getStageRules(reqVO, "SIGNOFF");
        List<DccCategoryApprovalMatrixSaveReqVO.Rule> approvalRules = getStageRules(reqVO, "APPROVAL");
        if (signoffRules.isEmpty()) {
            throw exception(CATEGORY_APPROVAL_MATRIX_SIGNOFF_EMPTY);
        }
        if (approvalRules.isEmpty()) {
            throw exception(CATEGORY_APPROVAL_MATRIX_APPROVAL_COUNT_INVALID);
        }
        DccApprovalPositionDO docControlPosition = resolveDocControlPosition();
        validatePositionsExist(List.of(docControlPosition.getId()));
        List<DccCategoryApprovalRouteNodeDO> nodes = buildDerivedNodes(docControlPosition.getId(), signoffRules, approvalRules);
        validatePositionsExist(nodes.stream()
                .filter(node -> "DCC_POSITION".equalsIgnoreCase(node.getSubjectType()))
                .map(DccCategoryApprovalRouteNodeDO::getSubjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Integer nextRouteVersionNo = routeMapper.selectList(DccCategoryApprovalRouteDO::getCategoryId, categoryId).stream()
                .map(DccCategoryApprovalRouteDO::getVersionNo)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        return new MatrixSaveContext(categoryId, reqVO, nodes, buildActivePositionNameMap(), nextRouteVersionNo);
    }

    private List<DccCategoryApprovalRouteNodeDO> buildDerivedNodes(Long docControlPositionId,
                                                                   List<DccCategoryApprovalMatrixSaveReqVO.Rule> signoffRules,
                                                                   List<DccCategoryApprovalMatrixSaveReqVO.Rule> approvalRules) {
        List<DccCategoryApprovalRouteNodeDO> nodes = new ArrayList<>();
        nodes.add(createDerivedNode(1, "DOC_CONTROL", docControlPositionId, null, 1));
        nodes.addAll(buildRuleStageNodes(2, "SIGNOFF", signoffRules));
        nodes.addAll(buildRuleStageNodes(3, "APPROVAL", approvalRules));
        nodes.add(createDerivedNode(4, "DOC_CONTROL", docControlPositionId, null, 1));
        return nodes;
    }

    private List<DccCategoryApprovalRouteNodeDO> buildRuleStageNodes(Integer stageNo, String stageType,
                                                                     List<DccCategoryApprovalMatrixSaveReqVO.Rule> rules) {
        List<DccCategoryApprovalRouteNodeDO> nodes = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            DccCategoryApprovalMatrixSaveReqVO.Rule rule = rules.get(i);
            nodes.add(createDerivedNode(stageNo, stageType, resolveRuleSubjectId(rule), rule, i + 1));
        }
        return nodes;
    }

    private DccCategoryApprovalRouteNodeDO createDerivedNode(Integer stageNo, String stageType, Long candidateSourceId,
                                                             DccCategoryApprovalMatrixSaveReqVO.Rule rule, int sort) {
        FixedStageDefinition stage = FIXED_STAGE_MAP.get(stageNo);
        return DccCategoryApprovalRouteNodeDO.builder()
                .stageNo(stage.stageNo())
                .stageCode(stage.stageCode())
                .stageName(stage.stageName())
                .stageOrder(stage.stageOrder())
                .candidateSourceType(rule == null ? "POSITION" : normalizeSourceType(rule.getSubjectType()))
                .candidateSourceId(candidateSourceId)
                .candidateSourceIds(candidateSourceId == null ? null : String.valueOf(candidateSourceId))
                .approveMethod(stage.approveMethod())
                .approveRatio("ALL".equals(stage.approveMethod()) ? 100 : null)
                .requireAllApprovals(stage.requireAllApprovals())
                .required(Boolean.TRUE)
                .sort(sort)
                .stageType(stageType)
                .subjectLabel(rule == null ? "文控" : rule.getSubjectLabel())
                .marker("▲")
                .subjectType(rule == null ? "DCC_POSITION" : normalizeRuleValue(rule.getSubjectType()))
                .subjectId(candidateSourceId)
                .subjectName(rule == null ? "文控" : rule.getSubjectName())
                .subjectDepartmentPath(rule == null ? null : rule.getSubjectDepartmentPath())
                .ruleRemark(null)
                .build();
    }

    private List<DccCategoryApprovalMatrixSaveReqVO.Rule> getStageRules(DccCategoryApprovalMatrixSaveReqVO reqVO,
                                                                        String stageType) {
        if (reqVO == null || reqVO.getRules() == null) {
            return List.of();
        }
        return reqVO.getRules().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .filter(rule -> stageType.equalsIgnoreCase(StrUtil.blankToDefault(rule.getStageType(), "")))
                .filter(rule -> rule.getSubjectId() != null)
                .toList();
    }

    private Long resolveRuleSubjectId(DccCategoryApprovalMatrixSaveReqVO.Rule rule) {
        return rule == null ? null : rule.getSubjectId();
    }

    private String normalizeSourceType(String subjectType) {
        if ("DCC_POSITION".equalsIgnoreCase(subjectType)) {
            return "POSITION";
        }
        if (StrUtil.isBlank(subjectType)) {
            return "POSITION";
        }
        return normalizeRuleValue(subjectType);
    }

    private String normalizeRuleValue(String value) {
        return StrUtil.blankToDefault(value, "").trim().toUpperCase();
    }

    private List<DccCategoryApprovalMatrixRespVO.Rule> buildEditableRules(List<DccCategoryApprovalRouteNodeDO> nodes,
                                                                          Map<Long, String> positionNameMap) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .filter(node -> node.getStageNo() != null && (node.getStageNo() == 2 || node.getStageNo() == 3))
                .flatMap(node -> buildNormalizedRuleInputs(node, positionNameMap).stream())
                .map(input -> {
                    DccCategoryApprovalMatrixRespVO.Rule rule = new DccCategoryApprovalMatrixRespVO.Rule();
                    rule.setStageType(input.stageType());
                    rule.setActive(Boolean.TRUE);
                    rule.setSubjectLabel(input.subjectLabel());
                    rule.setMarker(input.marker());
                    rule.setSubjectType(input.subjectType());
                    rule.setSubjectId(input.subjectId());
                    rule.setSubjectName(input.subjectName());
                    rule.setSubjectDepartmentPath(input.subjectDepartmentPath());
                    rule.setRemark(input.remark());
                    return rule;
                })
                .toList();
    }

    private List<DccCategoryReviewMatrixRowRespVO.Rule> buildRowRules(List<DccCategoryApprovalRouteNodeDO> nodes,
                                                                      Map<Long, String> positionNameMap) {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .filter(node -> node.getStageNo() != null && (node.getStageNo() == 2 || node.getStageNo() == 3))
                .flatMap(node -> buildNormalizedRuleInputs(node, positionNameMap).stream())
                .map(input -> {
                    DccCategoryReviewMatrixRowRespVO.Rule rule = new DccCategoryReviewMatrixRowRespVO.Rule();
                    rule.setStageType(input.stageType());
                    rule.setActive(Boolean.TRUE);
                    rule.setSubjectLabel(input.subjectLabel());
                    rule.setMarker(input.marker());
                    rule.setSubjectType(input.subjectType());
                    rule.setSubjectId(input.subjectId());
                    rule.setSubjectName(input.subjectName());
                    rule.setSubjectDepartmentPath(input.subjectDepartmentPath());
                    rule.setRemark(input.remark());
                    return rule;
                })
                .toList();
    }

    private List<NormalizedRuleInput> buildNormalizedRuleInputs(DccCategoryApprovalRouteNodeDO node,
                                                               Map<Long, String> positionNameMap) {
        String stageType = node.getStageNo() == 2 ? "SIGNOFF" : "APPROVAL";
        String subjectType = normalizeLegacySubjectType(node);
        List<Long> subjectIds = readCandidateSourceIds(node);
        if (subjectIds.isEmpty() && node.getSubjectId() != null) {
            subjectIds = List.of(node.getSubjectId());
        }
        if (subjectIds.isEmpty()) {
            return List.of(new NormalizedRuleInput(stageType, normalizeSubjectLabel(node, null, positionNameMap),
                    "▲", subjectType, node.getSubjectId(),
                    resolveLegacySubjectName(node, null, positionNameMap), node.getSubjectDepartmentPath(),
                    null));
        }
        return subjectIds.stream()
                .map(subjectId -> new NormalizedRuleInput(stageType,
                        normalizeSubjectLabel(node, subjectId, positionNameMap),
                        "▲",
                        subjectType,
                        subjectId,
                        resolveLegacySubjectName(node, subjectId, positionNameMap),
                        node.getSubjectDepartmentPath(),
                        null))
                .toList();
    }

    private String normalizeLegacySubjectType(DccCategoryApprovalRouteNodeDO node) {
        String subjectType = normalizeRuleValue(node.getSubjectType());
        if (StrUtil.isNotBlank(subjectType)) {
            return "POSITION".equals(subjectType) ? "DCC_POSITION" : subjectType;
        }
        String candidateSourceType = normalizeRuleValue(node.getCandidateSourceType());
        return "POSITION".equals(candidateSourceType) ? "DCC_POSITION" : candidateSourceType;
    }

    private String normalizeSubjectLabel(DccCategoryApprovalRouteNodeDO node, Long subjectId,
                                         Map<Long, String> positionNameMap) {
        if (StrUtil.isNotBlank(node.getSubjectLabel())) {
            return node.getSubjectLabel();
        }
        return resolveLegacySubjectName(node, subjectId, positionNameMap);
    }

    private String resolveLegacySubjectName(DccCategoryApprovalRouteNodeDO node, Long subjectId,
                                            Map<Long, String> positionNameMap) {
        if (subjectId == null) {
            return resolveNodeDisplayName(node);
        }
        String subjectType = normalizeLegacySubjectType(node);
        if ("DCC_POSITION".equals(subjectType)) {
            return positionNameMap.getOrDefault(subjectId, "岗位#" + subjectId);
        }
        if (StrUtil.isNotBlank(node.getSubjectName())) {
            return node.getSubjectName();
        }
        return "主体#" + subjectId;
    }

    private String resolveStageSourceRule(List<DccCategoryApprovalRouteNodeDO> nodes) {
        Set<String> subjectTypes = nodes.stream()
                .map(this::normalizeLegacySubjectType)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (subjectTypes.contains("DEPT")) {
            return "按部门负责人解析";
        }
        if (subjectTypes.contains("ROLE")) {
            return "按系统角色解析";
        }
        if (subjectTypes.contains("POST")) {
            return "按系统岗位解析";
        }
        if (subjectTypes.contains("DCC_POSITION") || subjectTypes.contains("POSITION")) {
            return "按 DCC 岗位解析";
        }
        if (subjectTypes.contains("USER")) {
            return "按显式人员解析";
        }
        return "按规则解析";
    }

    private String resolveNodeDisplayName(DccCategoryApprovalRouteNodeDO node) {
        if (node == null) {
            return null;
        }
        if (StrUtil.isNotBlank(node.getSubjectLabel())) {
            return node.getSubjectLabel();
        }
        if (StrUtil.isNotBlank(node.getSubjectName())) {
            return node.getSubjectName();
        }
        if (StrUtil.isNotBlank(node.getSubjectDepartmentPath())) {
            String path = node.getSubjectDepartmentPath();
            int separator = Math.max(path.lastIndexOf('-'), path.lastIndexOf('/'));
            return separator >= 0 && separator < path.length() - 1 ? path.substring(separator + 1) : path;
        }
        return node.getSubjectId() == null ? null : "主体#" + node.getSubjectId();
    }

    private String resolveNormalizedRuleDisplayName(NormalizedRuleInput input) {
        if (input == null) {
            return null;
        }
        if (StrUtil.isNotBlank(input.subjectLabel())) {
            return input.subjectLabel();
        }
        if (StrUtil.isNotBlank(input.subjectName())) {
            return input.subjectName();
        }
        if (StrUtil.isNotBlank(input.subjectDepartmentPath())) {
            String path = input.subjectDepartmentPath();
            int separator = Math.max(path.lastIndexOf('-'), path.lastIndexOf('/'));
            return separator >= 0 && separator < path.length() - 1 ? path.substring(separator + 1) : path;
        }
        return input.subjectId() == null ? null : "主体#" + input.subjectId();
    }

    private DccCategoryReviewMatrixUserLookupRespVO buildUserLookup(DccFileCategoryDO category, Long userId,
                                                                    boolean directoryAdmin) {
        Map<Long, String> positionNameMap = buildActivePositionNameMap();
        DccCategoryApprovalRouteDO route = routeMapper.selectLatestActiveByCategoryId(category.getId());
        DccCategoryReviewMatrixEffectivePreviewRespVO preview = null;
        boolean matrixParticipant = false;
        if (route != null) {
            List<DccCategoryApprovalRouteNodeDO> nodes = routeNodeMapper.selectListByRouteId(route.getId()).stream()
                    .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageOrder,
                                    Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(DccCategoryApprovalRouteNodeDO::getSort, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(DccCategoryApprovalRouteNodeDO::getStageNo))
                    .toList();
            preview = buildMatrixPreview(category.getId(), route.getVersionNo(), nodes, positionNameMap);
            matrixParticipant = preview.getViewSubjects().stream()
                    .anyMatch(subject -> Objects.equals(subject.getUserId(), userId));
        }
        boolean downloadMatched = directoryAdmin || permissionSupport.evaluateCategoryPermission(category.getId(), userId,
                DccFileCategoryPermissionActionEnum.DOWNLOAD).allowed();
        if (!directoryAdmin && !matrixParticipant && !downloadMatched) {
            return null;
        }
        DccCategoryReviewMatrixUserLookupRespVO respVO = new DccCategoryReviewMatrixUserLookupRespVO();
        respVO.setCategoryId(category.getId());
        respVO.setCode(category.getCode());
        respVO.setName(category.getName());
        if (directoryAdmin) {
            respVO.setBrowseStatus("YES");
            respVO.setBrowseSource("DIRECTORY_ADMIN");
            respVO.setBrowseReason("目录管理员");
            respVO.setDetailStatus("YES");
            respVO.setDetailSource("DIRECTORY_ADMIN");
            respVO.setDetailReason("目录管理员");
            respVO.setPublishedPreviewStatus("YES");
            respVO.setPublishedPreviewSource("DIRECTORY_ADMIN");
            respVO.setPublishedPreviewReason("目录管理员");
            respVO.setPendingPreviewStatus("YES");
            respVO.setPendingPreviewSource("DIRECTORY_ADMIN");
            respVO.setPendingPreviewReason("目录管理员");
        } else if (matrixParticipant) {
            respVO.setBrowseStatus("YES");
            respVO.setBrowseSource("CURRENT_REVIEW_MATRIX");
            respVO.setBrowseReason("当前审阅矩阵参与人");
            respVO.setDetailStatus("YES");
            respVO.setDetailSource("CURRENT_REVIEW_MATRIX");
            respVO.setDetailReason("当前审阅矩阵参与人");
            respVO.setPublishedPreviewStatus("YES");
            respVO.setPublishedPreviewSource("CURRENT_REVIEW_MATRIX");
            respVO.setPublishedPreviewReason("当前审阅矩阵参与人");
            respVO.setPendingPreviewStatus("CONDITIONAL");
            respVO.setPendingPreviewSource("ROUTE_SNAPSHOT");
            respVO.setPendingPreviewReason("仅对命中 route snapshot 的进行中文件实例生效");
        } else {
            respVO.setBrowseStatus("NO");
            respVO.setDetailStatus("NO");
            respVO.setPublishedPreviewStatus("NO");
            respVO.setPendingPreviewStatus("NO");
        }
        if (downloadMatched) {
            respVO.setDownloadStatus("CONDITIONAL");
            if (directoryAdmin) {
                respVO.setDownloadSource("DIRECTORY_ADMIN");
                respVO.setDownloadReason("目录管理员进入独立下载判定，实际下载仍取决于下载策略与文件状态");
            } else {
                respVO.setDownloadSource("DOWNLOAD_RULE");
                respVO.setDownloadReason("命中类别 DOWNLOAD 规则，实际下载仍取决于独立下载策略与目标文件状态");
            }
        } else {
            respVO.setDownloadStatus("NO");
        }
        respVO.setViewSources(preview == null ? List.of() : preview.getStages().stream()
                .flatMap(stage -> stage.getResolvedSubjects().stream()
                        .filter(subject -> Objects.equals(subject.getUserId(), userId))
                        .map(subject -> {
                            DccCategoryReviewMatrixUserLookupRespVO.ViewSource source = new DccCategoryReviewMatrixUserLookupRespVO.ViewSource();
                            source.setSource(subject.getSource());
                            source.setReason(subject.getReason());
                            source.setStageNo(stage.getStageNo());
                            source.setStageName(stage.getStageName());
                            source.setPositionId(subject.getPositionId());
                            source.setPositionName(subject.getPositionName());
                            return source;
                        }))
                .toList());
        respVO.setRisks(preview == null ? List.of(createRisk("MATRIX_NOT_CONFIGURED",
                        "当前文件类型未配置生效审阅矩阵，普通查阅权限无法解析。", "BLOCKING", true))
                : preview.getRisks());
        return respVO;
    }

    private DccCategoryApprovalRouteNodeDO findNode(List<DccCategoryApprovalRouteNodeDO> nodes, Integer stageNo) {
        return nodes.stream()
                .filter(item -> stageNo.equals(item.getStageNo()))
                .findFirst()
                .orElse(null);
    }

    private Set<Long> parseResolvedUserIds(DccControlledFileRouteSnapshotDO snapshot) {
        if (snapshot == null || StrUtil.isBlank(snapshot.getResolvedUserIds())) {
            return Set.of();
        }
        return StrUtil.split(snapshot.getResolvedUserIds(), ',').stream()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isSnapshotSensitiveStatus(String status) {
        return DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus().equals(status)
                || DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus().equals(status);
    }

    private List<Long> readCandidateSourceIds(DccCategoryApprovalRouteNodeDO node) {
        if (node == null || StrUtil.isBlank(node.getCandidateSourceIds())) {
            return List.of();
        }
        return StrUtil.split(node.getCandidateSourceIds(), ',').stream()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(Long::valueOf)
                .toList();
    }

    private List<Long> normalizePositionIds(List<Long> positionIds) {
        if (positionIds == null) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(positionIds));
    }

    private List<String> resolvePositionNames(List<Long> positionIds, Map<Long, String> positionNameMap) {
        return positionIds.stream()
                .map(id -> positionNameMap.getOrDefault(id, "岗位#" + id))
                .toList();
    }

    private String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private boolean matchesKeyword(String actual, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        return StrUtil.containsIgnoreCase(StrUtil.blankToDefault(actual, ""), StrUtil.trim(keyword));
    }

    private void validatePositionsExist(List<Long> positionIds) {
        Map<Long, DccApprovalPositionDO> positionMap = positionMapper.selectList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .collect(Collectors.toMap(DccApprovalPositionDO::getId, Function.identity(), (left, right) -> left));
        boolean missingPosition = positionIds.stream().anyMatch(id -> !positionMap.containsKey(id));
        if (missingPosition) {
            throw exception(CATEGORY_APPROVAL_MATRIX_POSITION_INACTIVE_OR_MISSING);
        }
    }

    private DccApprovalPositionDO resolveDocControlPosition() {
        return positionMapper.selectList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .filter(item -> DOC_CONTROL_POSITION_NAME.equals(item.getName()))
                .min(Comparator.comparing(DccApprovalPositionDO::getId))
                .orElseThrow(() -> exception(CATEGORY_APPROVAL_MATRIX_DOC_CONTROL_POSITION_MISSING));
    }

    private DccFileCategoryDO validateCategoryExists(Long categoryId) {
        DccFileCategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private void removeMatrixManagedPermissionRules(Long categoryId) {
        permissionRuleMapper.delete(new LambdaQueryWrapperX<DccFileCategoryPermissionRuleDO>()
                .eq(DccFileCategoryPermissionRuleDO::getCategoryId, categoryId)
                .in(DccFileCategoryPermissionRuleDO::getActionType, Set.of("REVIEW", "APPROVE")));
    }

    private DccCategoryApprovalRouteDO persistApprovalMatrix(MatrixSaveContext context) {
        removeMatrixManagedPermissionRules(context.categoryId());

        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .categoryId(context.categoryId())
                .versionNo(context.nextRouteVersionNo())
                .active(Boolean.TRUE)
                .effectiveTime(context.reqVO().getEffectiveTime())
                .remark(context.reqVO().getRemark())
                .build();
        routeMapper.insert(route);

        routeMapper.selectList(DccCategoryApprovalRouteDO::getCategoryId, context.categoryId()).stream()
                .filter(item -> !item.getId().equals(route.getId()) && Boolean.TRUE.equals(item.getActive()))
                .forEach(item -> routeMapper.updateById(DccCategoryApprovalRouteDO.builder()
                        .id(item.getId())
                        .active(Boolean.FALSE)
                        .build()));

        context.nodes().forEach(node -> {
            node.setRouteId(route.getId());
            routeNodeMapper.insert(node);
        });
        return route;
    }

    private record FixedStageDefinition(Integer stageNo, String stageCode, String stageName, Integer stageOrder,
                                        String approveMethod, boolean requireAllApprovals) {
    }

    private record MatrixSaveContext(Long categoryId,
                                     DccCategoryApprovalMatrixSaveReqVO reqVO,
                                     List<DccCategoryApprovalRouteNodeDO> nodes,
                                     Map<Long, String> positionNameMap,
                                     Integer nextRouteVersionNo) {
    }

    private record NormalizedRuleInput(String stageType, String subjectLabel, String marker, String subjectType,
                                       Long subjectId, String subjectName, String subjectDepartmentPath,
                                       String remark) {
    }
}
