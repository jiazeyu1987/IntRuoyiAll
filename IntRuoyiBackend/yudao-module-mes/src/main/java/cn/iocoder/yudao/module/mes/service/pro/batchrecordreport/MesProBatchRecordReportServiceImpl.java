package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportDeleteAllRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkerVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkersRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionApprovalEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionApprovalEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class MesProBatchRecordReportServiceImpl implements MesProBatchRecordReportService {

    private static final String IMPORT_ACTION_REBUILD_V1 = "REBUILD_V1";
    private static final String IMPORT_ACTION_UPGRADE = "UPGRADE";
    private static final String ROUTE_GOVERNANCE_CREATE_REQUIRED = "CREATE_REQUIRED";
    private static final String ROUTE_GOVERNANCE_UPGRADE_REQUIRED = "UPGRADE_REQUIRED";
    private static final String ROUTE_GOVERNANCE_DUPLICATE_BLOCKED = "DUPLICATE_BLOCKED";
    public static final String BATCH_RECORD_VERSION_APPROVAL_PROCESS_DEFINITION_KEY =
            "mes-batch-record-version-approval-v1";
    private static final String DATA_DOMAIN = "MES";
    private static final String SYSTEM_CODE = "MES";
    private static final String BATCH_RECORD_VERSION_OBJECT_TYPE = "BATCH_RECORD_VERSION";
    private static final String PUBLISH_ACTION_CODE = "PUBLISH";

    static final String FIXED_SAMPLE_KEY = "FIXED_DOC";
    static final int JIMU_REPORT_NAME_MAX_LENGTH = 50;
    static final String DEFAULT_BATCH_RECORD_NAME = "棘突球囊";
    static final int BATCH_RECORD_NAME_MAX_LENGTH = 100;
    static final String DELETE_ALL_CONFIRM_CODE = "PROD";
    private static final Set<String> SIGNATURE_ACTION_TYPES = Set.of("FORM_REVIEW", "SUBMIT", "APPROVE");
    private static final Set<String> REVIEW_SOURCE_TYPES = Set.of("POST", "ROLE", "USER", "ROLES", "USERS");
    private static final Set<String> MULTI_REVIEW_SOURCE_TYPES = Set.of("ROLES", "USERS");
    private static final String DEFAULT_SIGNATURE_DISPLAY_FORMAT = "ACTOR_SIGNED_AT";
    private enum GeneratedReportSource {
        IMPORTED_DOC,
        IMAGE,
        UPLOADED_DOC
    }

    @Resource
    private MesProBatchRecordDocParser docParser;
    @Resource
    private MesProBatchRecordImageParser imageParser;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;
    @Resource
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Resource
    private MesProBatchRecordVersionMapper versionMapper;
    @Resource
    private MesProBatchRecordVersionMigrationItemMapper migrationItemMapper;
    @Resource
    private MesProBatchRecordVersionApprovalEventMapper approvalEventMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProBatchRecordExecutionMapper batchRecordExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProBatchRecordRouteGenerationService routeGenerationService;
    @Resource
    private MesProBatchRecordJimuReportGateway jimuReportGateway;
    @Resource
    private MesProBatchRecordFormProfileRegistry formProfileRegistry;
    @Resource
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;
    @Resource
    private MesProBatchRecordVersionBusinessApprovalEffectExecutor batchRecordVersionApprovalEffectExecutor;
    @Autowired(required = false)
    private List<MesProBatchRecordRouteRecognizer> routeRecognizers = List.of();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult importPilotDoc(MultipartFile file) {
        validateImportedDoc(file);
        byte[] bytes = getBytes(file);
        String sourceHash = sha256(bytes);
        List<MesProBatchRecordParsedTable> parsedTables = docParser.parse(bytes);
        attachDocumentFrame(parsedTables, docParser.extractDocumentFrame(bytes));
        if (parsedTables.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID,
                    parsedTables.size());
        }
        return saveGeneratedReports(parsedTables, normalizeFileName(file.getOriginalFilename()), sourceHash,
                scopeSampleKeyByTenant(MesProBatchRecordReportConstants.SAMPLE_KEY_PREFIX + sourceHash.substring(0, 16)),
                MesProBatchRecordRecognitionRouteKeys.LEGACY, DEFAULT_BATCH_RECORD_NAME,
                GeneratedReportSource.IMPORTED_DOC, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult importImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EMPTY);
        }
        validateImageFile(file);
        byte[] bytes = getBytes(file);
        String sourceFileName = normalizeFileName(file.getOriginalFilename());
        String sha256 = sha256(bytes);
        List<MesProBatchRecordParsedTable> parsedTables = imageParser.parse(sourceFileName, bytes);
        if (parsedTables.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID);
        }
        return saveGeneratedReports(parsedTables, sourceFileName, sha256, scopeSampleKeyByTenant(buildImageSampleKey(sha256)),
                MesProBatchRecordRecognitionRouteKeys.LEGACY, DEFAULT_BATCH_RECORD_NAME,
                GeneratedReportSource.IMAGE, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult recognizeFixedRoute(String routeKey) {
        String normalizedRouteKey = MesProBatchRecordRecognitionRouteKeys.normalize(routeKey);
        if (!MesProBatchRecordRecognitionRouteKeys.isFixedRoute(normalizedRouteKey)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_INVALID, routeKey);
        }
        Path samplePath = Path.of(MesProBatchRecordReportConstants.FIXED_SAMPLE_PATH);
        if (!Files.exists(samplePath) || !Files.isRegularFile(samplePath)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FIXED_SAMPLE_MISSING,
                    MesProBatchRecordReportConstants.FIXED_SAMPLE_PATH);
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(samplePath);
        } catch (Exception ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FIXED_SAMPLE_READ_FAILED,
                    ex.getMessage());
        }
        MesProBatchRecordRouteRecognizer recognizer = routeRecognizerMap().get(normalizedRouteKey);
        if (recognizer == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_RECOGNIZER_MISSING,
                    normalizedRouteKey);
        }
        List<MesProBatchRecordParsedTable> parsedTables = recognizer.recognize(
                samplePath, bytes, samplePath.getFileName().toString());
        List<MesProBatchRecordParsedTable> sourceTables = docParser.parse(bytes);
        MesProBatchRecordDocumentFrame documentFrame = docParser.extractDocumentFrame(bytes);
        attachDocumentFrame(parsedTables, documentFrame);
        int expectedReportCount = sourceTables.size();
        if (expectedReportCount == 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID,
                    expectedReportCount);
        }
        if (parsedTables.size() != expectedReportCount) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_RESULT_COUNT_INVALID,
                    normalizedRouteKey, expectedReportCount, parsedTables.size());
        }
        return saveGeneratedReports(parsedTables, samplePath.getFileName().toString(), sha256(bytes),
                scopeSampleKeyByTenant(FIXED_SAMPLE_KEY), normalizedRouteKey, DEFAULT_BATCH_RECORD_NAME,
                GeneratedReportSource.IMPORTED_DOC, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                                String batchRecordName, Boolean upgrade,
                                                                List<String> productNames) {
        return recognizeUploadedRoute(file, routeKey, batchRecordName,
                Boolean.TRUE.equals(upgrade) ? IMPORT_ACTION_UPGRADE : IMPORT_ACTION_REBUILD_V1,
                null, null, productNames,
                true, List.of(), productNames);
    }

    @Override
    public MesProBatchRecordImportPreflightResult preflightUploadedRoute(String routeKey, String batchRecordName,
                                                                         List<String> productNames) {
        String normalizedRouteKey = MesProBatchRecordRecognitionRouteKeys.normalize(routeKey);
        if (!MesProBatchRecordRecognitionRouteKeys.isFixedRoute(normalizedRouteKey)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_INVALID, routeKey);
        }
        String normalizedBatchRecordName = normalizeBatchRecordName(batchRecordName);
        List<String> normalizedProductNames = normalizeRouteProductNames(productNames);
        validateDccProjectNameMatchesBatchRecordName(normalizedBatchRecordName, normalizedProductNames);
        MesProBatchRecordDefinitionDO definition = definitionMapper.selectByNameAndRouteKey(
                normalizedBatchRecordName, normalizedRouteKey);
        List<MesProBatchRecordVersionDO> definitionVersions = definition == null
                ? List.of() : versionMapper.selectListByDefinitionId(definition.getId());
        MesProBatchRecordVersionDO currentVersion = definition == null || definition.getCurrentVersionId() == null
                ? null : definitionVersions.stream()
                .filter(version -> Objects.equals(version.getId(), definition.getCurrentVersionId()))
                .findFirst()
                .orElse(null);
        MesProBatchRecordVersionDO pendingApprovalVersion = findPendingApprovalVersion(definitionVersions);
        boolean pendingApprovalLocked = pendingApprovalVersion != null;
        boolean currentBatchRecordHasMainReports = false;
        List<VersionResetBlocker> blockers = List.of();
        if (currentVersion != null) {
            currentBatchRecordHasMainReports = hasVisibleMainReport(definition.getId(), currentVersion.getId());
            blockers = findVersionResetBlockers(List.of(currentVersion));
            if (!currentBatchRecordHasMainReports && blockers.isEmpty()) {
                currentVersion = null;
            }
        }
        MesProBatchRecordVersionDO latestVersion = currentVersion == null && !pendingApprovalLocked
                ? null : latestBatchRecordVersion(definitionVersions);
        List<String> allowedActions = buildAllowedImportActions(currentVersion, currentBatchRecordHasMainReports,
                blockers);
        String recommendedAction = allowedActions.contains(IMPORT_ACTION_UPGRADE)
                && (currentBatchRecordHasMainReports || !blockers.isEmpty())
                ? IMPORT_ACTION_UPGRADE : IMPORT_ACTION_REBUILD_V1;
        List<MesProRouteDO> governedRoutes = routeMapper.selectListByName(normalizedBatchRecordName);
        MesProRouteDO route = governedRoutes.size() == 1 ? governedRoutes.get(0) : null;
        MesProRouteVersionDO routeVersion = route == null ? null : routeVersionMapper.selectActiveByRouteId(route.getId());
        String routeGovernanceStatus = resolveRouteGovernanceStatus(governedRoutes);
        boolean importActionLocked = pendingApprovalLocked || ROUTE_GOVERNANCE_DUPLICATE_BLOCKED.equals(routeGovernanceStatus);
        List<String> effectiveAllowedActions = importActionLocked
                ? List.of() : allowedActions;

        return MesProBatchRecordImportPreflightResult.builder()
                .routeKey(normalizedRouteKey)
                .batchRecordName(normalizedBatchRecordName)
                .batchRecordDefinitionId(definition == null ? null : definition.getId())
                .currentBatchRecordVersionId(currentVersion == null ? null : currentVersion.getId())
                .currentBatchRecordVersionNo(currentVersion == null ? null : currentVersion.getVersionNo())
                .currentBatchRecordVersionStatus(currentVersion == null ? null : currentVersion.getStatus())
                .latestBatchRecordVersionId(latestVersion == null ? null : latestVersion.getId())
                .latestBatchRecordVersionNo(latestVersion == null ? null : latestVersion.getVersionNo())
                .latestBatchRecordVersionStatus(latestVersion == null ? null : latestVersion.getStatus())
                .currentBatchRecordHasMainReports(currentVersion != null && currentBatchRecordHasMainReports)
                .routeGovernanceStatus(routeGovernanceStatus)
                .routeUpgradeRequired(ROUTE_GOVERNANCE_UPGRADE_REQUIRED.equals(routeGovernanceStatus))
                .duplicateRoutes(toDuplicateRoutes(governedRoutes))
                .currentRouteId(route == null ? null : route.getId())
                .currentRouteCode(route == null ? null : route.getCode())
                .currentRouteName(route == null ? null : route.getName())
                .currentRouteVersionId(routeVersion == null ? null : routeVersion.getId())
                .currentRouteVersionNo(routeVersion == null ? null : routeVersion.getVersionNo())
                .currentRouteVersionActive(routeVersion == null ? null : routeVersion.getActive())
                .hasHistoricalReferences(!blockers.isEmpty())
                .referenceBlockers(toReferenceBlockers(blockers))
                .allowedActions(effectiveAllowedActions)
                .recommendedAction(importActionLocked
                        ? null : recommendedAction)
                .nextVersionNo(currentVersion == null ? "V1.0" : nextVersionNo(definitionVersions))
                .routeProductOptions(importActionLocked
                        ? List.of() : buildRouteProductOptions(route, routeVersion, normalizedProductNames))
                .build();
    }

    private String resolveRouteGovernanceStatus(List<MesProRouteDO> routes) {
        if (routes.isEmpty()) {
            return ROUTE_GOVERNANCE_CREATE_REQUIRED;
        }
        if (routes.size() == 1) {
            return ROUTE_GOVERNANCE_UPGRADE_REQUIRED;
        }
        return ROUTE_GOVERNANCE_DUPLICATE_BLOCKED;
    }

    private void ensureNoDuplicateRouteForProjectName(String projectName) {
        List<MesProRouteDO> routes = routeMapper.selectListByName(projectName);
        if (routes.size() <= 1) {
            return;
        }
        String routeCodes = routes.stream()
                .map(route -> StrUtil.blankToDefault(route.getCode(), String.valueOf(route.getId()))
                        + "/" + route.getId())
                .collect(java.util.stream.Collectors.joining("、"));
        throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_DUPLICATE,
                projectName, routeCodes);
    }

    private void ensureRouteUpgradeConfirmedIfNeeded(String projectName, boolean routeRebuildRequested,
                                                     Boolean routeUpgradeConfirmed, Long expectedRouteId,
                                                     Long expectedRouteVersionId) {
        if (!routeRebuildRequested) {
            return;
        }
        List<MesProRouteDO> routes = routeMapper.selectListByName(projectName);
        if (routes.isEmpty()) {
            if (expectedRouteId != null || expectedRouteVersionId != null) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED,
                        expectedRouteId, expectedRouteVersionId, null, null);
            }
            return;
        }
        MesProRouteDO route = routes.get(0);
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(route.getId());
        if (!Boolean.TRUE.equals(routeUpgradeConfirmed)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_CONFIRM_REQUIRED,
                    projectName);
        }
        Long currentVersionId = activeVersion == null ? null : activeVersion.getId();
        if (!Objects.equals(expectedRouteId, route.getId())
                || !Objects.equals(expectedRouteVersionId, currentVersionId)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED,
                    expectedRouteId, expectedRouteVersionId, route.getId(), currentVersionId);
        }
    }

    private List<MesProBatchRecordImportPreflightResult.DuplicateRoute> toDuplicateRoutes(List<MesProRouteDO> routes) {
        if (routes.size() <= 1) {
            return List.of();
        }
        return routes.stream()
                .map(route -> {
                    MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(route.getId());
                    return MesProBatchRecordImportPreflightResult.DuplicateRoute.builder()
                            .routeId(route.getId())
                            .routeCode(route.getCode())
                            .routeName(route.getName())
                            .routeVersionId(activeVersion == null ? null : activeVersion.getId())
                            .routeVersionNo(activeVersion == null ? null : activeVersion.getVersionNo())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                                String batchRecordName, Boolean upgrade,
                                                                List<String> productNames,
                                                                Boolean rebuildBatchRecord,
                                                                List<Long> selectedRouteProductIds,
                                                                List<String> selectedProductNames) {
        return recognizeUploadedRoute(file, routeKey, batchRecordName,
                Boolean.TRUE.equals(upgrade) ? IMPORT_ACTION_UPGRADE : IMPORT_ACTION_REBUILD_V1,
                null, null, productNames, rebuildBatchRecord, selectedRouteProductIds, selectedProductNames);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                                String batchRecordName, String importAction,
                                                                Long expectedSourceVersionId,
                                                                List<String> productNames,
                                                                Boolean rebuildBatchRecord,
                                                                List<Long> selectedRouteProductIds,
                                                                List<String> selectedProductNames) {
        return recognizeUploadedRoute(file, routeKey, batchRecordName, importAction, expectedSourceVersionId,
                null, productNames, rebuildBatchRecord, selectedRouteProductIds, selectedProductNames);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                                String batchRecordName, String importAction,
                                                                Long expectedSourceVersionId,
                                                                String expectedTargetVersionNo,
                                                                List<String> productNames,
                                                                Boolean rebuildBatchRecord,
                                                                List<Long> selectedRouteProductIds,
                                                                List<String> selectedProductNames) {
        return recognizeUploadedRoute(file, routeKey, batchRecordName, importAction, expectedSourceVersionId,
                expectedTargetVersionNo, productNames, rebuildBatchRecord, selectedRouteProductIds,
                selectedProductNames, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                                String batchRecordName, String importAction,
                                                                Long expectedSourceVersionId,
                                                                String expectedTargetVersionNo,
                                                                List<String> productNames,
                                                                Boolean rebuildBatchRecord,
                                                                List<Long> selectedRouteProductIds,
                                                                List<String> selectedProductNames,
                                                                Long approvalSubmitterUserId) {
        return recognizeUploadedRoute(file, routeKey, batchRecordName, importAction, expectedSourceVersionId,
                expectedTargetVersionNo, productNames, rebuildBatchRecord, selectedRouteProductIds,
                selectedProductNames, false, null, null, approvalSubmitterUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult recognizeUploadedRoute(MultipartFile file, String routeKey,
                                                                String batchRecordName, String importAction,
                                                                Long expectedSourceVersionId,
                                                                String expectedTargetVersionNo,
                                                                List<String> productNames,
                                                                Boolean rebuildBatchRecord,
                                                                List<Long> selectedRouteProductIds,
                                                                List<String> selectedProductNames,
                                                                Boolean routeUpgradeConfirmed,
                                                                Long expectedRouteId,
                                                                Long expectedRouteVersionId,
                                                                Long approvalSubmitterUserId) {
        String normalizedRouteKey = MesProBatchRecordRecognitionRouteKeys.normalize(routeKey);
        if (!MesProBatchRecordRecognitionRouteKeys.isFixedRoute(normalizedRouteKey)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_INVALID, routeKey);
        }
        String normalizedBatchRecordName = normalizeBatchRecordName(batchRecordName);
        ensureNoDuplicateRouteForProjectName(normalizedBatchRecordName);
        List<String> normalizedProductNames = normalizeRouteProductNames(productNames);
        validateDccProjectNameMatchesBatchRecordName(normalizedBatchRecordName, normalizedProductNames);
        String normalizedImportAction = normalizeImportAction(importAction);
        String normalizedExpectedTargetVersionNo = normalizeExpectedTargetVersionNo(expectedTargetVersionNo);
        boolean upgradeImport = IMPORT_ACTION_UPGRADE.equals(normalizedImportAction);
        boolean rebuildRecord = Boolean.TRUE.equals(rebuildBatchRecord);
        List<Long> normalizedRouteProductIds = normalizeSelectedRouteProductIds(selectedRouteProductIds);
        List<String> normalizedSelectedProductNames = normalizeOptionalRouteProductNames(selectedProductNames);
        boolean routeRebuildRequested = !normalizedRouteProductIds.isEmpty() || !normalizedSelectedProductNames.isEmpty();
        ensureRouteUpgradeConfirmedIfNeeded(normalizedBatchRecordName, routeRebuildRequested || rebuildRecord,
                routeUpgradeConfirmed, expectedRouteId, expectedRouteVersionId);
        if (!rebuildRecord && !routeRebuildRequested) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMPORT_SCOPE_EMPTY);
        }
        if (IMPORT_ACTION_REBUILD_V1.equals(normalizedImportAction) && rebuildRecord) {
            ensureV1ImportAllowed(normalizedBatchRecordName, normalizedRouteKey);
        }
        validateUploadedRouteDoc(file);
        byte[] bytes = getBytes(file);
        String sourceFileName = normalizeFileName(file.getOriginalFilename());
        String sha256 = sha256(bytes);
        MesProBatchRecordDefinitionDO definition = rebuildRecord && !upgradeImport
                ? getOrCreateDefinition(normalizedBatchRecordName, normalizedRouteKey)
                : definitionMapper.selectByNameAndRouteKey(normalizedBatchRecordName, normalizedRouteKey);
        MesProBatchRecordVersionDO sourceVersion = definition == null || definition.getCurrentVersionId() == null
                ? null : versionMapper.selectById(definition.getCurrentVersionId());
        boolean routeOnlyWithoutBatchRecordVersion = routeRebuildRequested && !rebuildRecord && !upgradeImport
                && sourceVersion == null;
        if (definition == null) {
            if (!routeOnlyWithoutBatchRecordVersion) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_REBUILD_VERSION_REQUIRED,
                        normalizedBatchRecordName);
            }
        }
        if (!rebuildRecord && sourceVersion == null) {
            if (!routeOnlyWithoutBatchRecordVersion) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_REBUILD_VERSION_REQUIRED,
                        normalizedBatchRecordName);
            }
        }
        ensureNoPendingApprovalImport(definition);
        if (upgradeImport) {
            validateUpgradeImportAllowed(definition, sourceVersion, expectedSourceVersionId);
        }
        if (definition != null) {
            validateImportActionAllowed(definition, sourceVersion, normalizedImportAction);
        }
        MesProBatchRecordVersionDO targetSourceVersion = sourceVersion;
        String targetVersionNo = rebuildRecord ? resolveTargetVersionNo(definition, normalizedExpectedTargetVersionNo)
                : null;
        MesProBatchRecordVersionDO reusablePendingVersion = upgradeImport && rebuildRecord
                ? versionMapper.selectReusablePendingByHash(definition.getId(), sha256) : null;
        if (reusablePendingVersion != null && normalizedExpectedTargetVersionNo != null
                && !Objects.equals(normalizedExpectedTargetVersionNo, reusablePendingVersion.getVersionNo())) {
            if (canSupersedeReusablePendingVersion(reusablePendingVersion)) {
                voidSupersededReusablePendingVersion(reusablePendingVersion, normalizedExpectedTargetVersionNo);
                targetSourceVersion = reusablePendingVersion;
                reusablePendingVersion = null;
            } else {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_TARGET_CHANGED,
                        normalizedExpectedTargetVersionNo, reusablePendingVersion.getVersionNo());
            }
        }
        if (reusablePendingVersion != null && hasGeneratedReports(definition.getId(), reusablePendingVersion.getId())) {
            if (Objects.equals(sourceVersion == null ? null : sourceVersion.getSourceFileSha256(), sha256)) {
                return buildReusableImportResult(definition, reusablePendingVersion,
                        resolveReportProductName(normalizedProductNames), approvalSubmitterUserId);
            }
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FORM_SLOT_EXISTS,
                    normalizedBatchRecordName, "主批记录");
        }
        Long routeProductScopeRouteId = resolveRouteProductScopeRouteId(sourceVersion, normalizedBatchRecordName,
                expectedRouteId, expectedRouteVersionId);
        List<String> routeProductNames = routeRebuildRequested
                ? resolveSelectedRouteProductNames(routeProductScopeRouteId, normalizedRouteProductIds,
                normalizedSelectedProductNames)
                : List.of();
        if (routeRebuildRequested) {
            validateDccProjectNameMatchesBatchRecordName(normalizedBatchRecordName, routeProductNames);
        }

        MesProBatchRecordRouteRecognizer recognizer = routeRecognizerMap().get(normalizedRouteKey);
        if (recognizer == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_RECOGNIZER_MISSING,
                    normalizedRouteKey);
        }
        List<MesProBatchRecordParsedTable> parsedTables = recognizer.recognize(null, bytes, sourceFileName);
        attachDocumentFrame(parsedTables, docParser.extractDocumentFrame(bytes));
        routeGenerationService.validateUploadedWordRoute(parsedTables);

        MesProBatchRecordVersionDO targetVersion = rebuildRecord ? createPrecheckVersion(
                definition, upgradeImport ? targetSourceVersion : null, sourceFileName, sha256, targetVersionNo)
                : sourceVersion;
        if (upgradeImport && rebuildRecord && hasGeneratedReports(definition.getId(), targetVersion.getId())) {
            if (Objects.equals(targetSourceVersion == null ? null : targetSourceVersion.getSourceFileSha256(), sha256)) {
                return buildReusableImportResult(definition, targetVersion,
                        resolveReportProductName(normalizedProductNames), approvalSubmitterUserId);
            }
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FORM_SLOT_EXISTS,
                    normalizedBatchRecordName, "主批记录");
        }
        MesProBatchRecordImportResult importResult = rebuildRecord
                ? saveGeneratedReports(parsedTables, sourceFileName, sha256,
                scopeSampleKeyByTenant(buildBatchRecordVersionSampleKey(normalizedBatchRecordName, targetVersion.getId())),
                normalizedRouteKey, normalizedBatchRecordName, GeneratedReportSource.UPLOADED_DOC, false,
                MesProBatchRecordFormSlotType.MAIN.getType(), definition.getId(), targetVersion.getId(), true,
                resolveReportProductName(normalizedProductNames))
                : targetVersion == null ? emptyImportResult() : buildCurrentVersionImportResult(definition, targetVersion);
        if (upgradeImport && rebuildRecord) {
            copySourceVersionReportFillRules(definition.getId(), targetSourceVersion, targetVersion,
                    importResult.reports());
        }
        MesProBatchRecordRouteGenerationResult routeResult = null;
        if (routeRebuildRequested) {
            if (routeOnlyWithoutBatchRecordVersion) {
                routeResult = routeGenerationService.generateRouteOnlyForUploadedWord(
                        normalizedBatchRecordName, parsedTables, routeProductNames,
                        expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed);
            } else {
                List<MesProBatchRecordReportView> reportsForRoute = rebuildRecord
                        ? importResult.reports()
                        : loadCurrentVersionReports(definition.getId(), targetVersion.getId());
                routeResult = routeGenerationService.generateForUploadedWord(
                        normalizedBatchRecordName, parsedTables, reportsForRoute, routeProductNames,
                        definition.getId(), targetVersion.getId(),
                        expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed, rebuildRecord);
                targetVersion.setRouteId(routeResult.routeId());
                targetVersion.setSourceRouteId(targetSourceVersion == null ? null : targetSourceVersion.getRouteId());
                versionMapper.updateById(targetVersion);
            }
        } else if (rebuildRecord && expectedRouteId != null) {
            routeResult = routeGenerationService.generateBatchRecordBindingCandidateForUploadedWord(
                    normalizedBatchRecordName, parsedTables, importResult.reports(),
                    definition.getId(), targetVersion.getId(),
                    expectedRouteId, expectedRouteVersionId, routeUpgradeConfirmed);
            targetVersion.setRouteId(routeResult.routeId());
            targetVersion.setSourceRouteId(targetSourceVersion == null ? null : targetSourceVersion.getRouteId());
            versionMapper.updateById(targetVersion);
        }
        if (rebuildRecord) {
            writePhaseOneMigrationEvidence(definition.getId(), targetVersion.getId(),
                    upgradeImport ? targetSourceVersion.getId() : null,
                    upgradeImport ? targetSourceVersion.getSourceFileSha256() : null, sha256,
                    importResult.importedCount());
            if (migrationItemMapper.countBlockingItems(targetVersion.getId()) > 0) {
                targetVersion.setStatus("PRECHECK_FAILED");
                versionMapper.updateById(targetVersion);
            } else if (!upgradeImport) {
                activateInitialVersionWithoutApproval(targetVersion);
            } else if (approvalSubmitterUserId != null) {
                targetVersion = submitPrecheckVersionForApproval(targetVersion, approvalSubmitterUserId);
            }
        }
        RouteDisplay routeDisplay = routeResult == null && targetVersion != null
                ? loadRouteDisplay(targetVersion.getRouteId()) : RouteDisplay.empty();
        return MesProBatchRecordImportResult.builder()
                .importedCount(importResult.importedCount())
                .createdCount(importResult.createdCount())
                .updatedCount(importResult.updatedCount())
                .batchRecordDefinitionId(definition == null ? null : definition.getId())
                .batchRecordVersionId(targetVersion == null ? null : targetVersion.getId())
                .sourceBatchRecordVersionId(targetVersion == null ? null : targetVersion.getSourceVersionId())
                .versionNo(targetVersion == null ? null : targetVersion.getVersionNo())
                .versionStatus(targetVersion == null ? null : targetVersion.getStatus())
                .approvalInstanceId(targetVersion == null ? null : targetVersion.getApprovalInstanceId())
                .routeId(routeResult == null ? routeDisplay.routeId() : routeResult.routeId())
                .routeCode(routeResult == null ? routeDisplay.routeCode() : routeResult.routeCode())
                .routeName(routeResult == null ? routeDisplay.routeName() : routeResult.routeName())
                .routeVersionId(routeResult == null ? routeDisplay.routeVersionId() : routeResult.routeVersionId())
                .routeVersionNo(routeResult == null ? routeDisplay.routeVersionNo() : routeResult.routeVersionNo())
                .routeProcessCount(routeResult == null ? null : routeResult.routeProcessCount())
                .batchRecordRouteBindingCount(routeResult == null ? null : routeResult.batchRecordRouteBindingCount())
                .boundProductNameCount(routeResult == null ? null : routeResult.boundProductNameCount())
                .boundProductCodeCount(routeResult == null ? null : routeResult.boundProductCodeCount())
                .skippedProductNames(routeResult == null ? List.of() : routeResult.skippedProductNames())
                .reports(importResult.reports())
                .build();
    }

    private void activateInitialVersionWithoutApproval(MesProBatchRecordVersionDO version) {
        int updated = definitionMapper.updateCurrentVersionIfMatch(version.getDefinitionId(), null, version.getId());
        if (updated != 1) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_CURRENT_CHANGED,
                    version.getDefinitionId());
        }
        version.setStatus("APPROVED");
        version.setApprovedAt(LocalDateTime.now());
        versionMapper.updateById(version);
    }

    private String normalizeImportAction(String importAction) {
        String normalized = StrUtil.blankToDefault(importAction, IMPORT_ACTION_REBUILD_V1).trim().toUpperCase();
        if (!IMPORT_ACTION_REBUILD_V1.equals(normalized) && !IMPORT_ACTION_UPGRADE.equals(normalized)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMPORT_ACTION_INVALID,
                    importAction);
        }
        return normalized;
    }

    private String normalizeExpectedTargetVersionNo(String expectedTargetVersionNo) {
        String normalized = StrUtil.trimToNull(expectedTargetVersionNo);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!upper.matches("V\\d+(\\.0)?")) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_NO_INVALID,
                    expectedTargetVersionNo);
        }
        return upper.contains(".") ? upper : upper + ".0";
    }

    private String resolveTargetVersionNo(MesProBatchRecordDefinitionDO definition, String expectedTargetVersionNo) {
        String currentTargetVersionNo = nextVersionNo(definition.getId());
        if (expectedTargetVersionNo != null && !Objects.equals(expectedTargetVersionNo, currentTargetVersionNo)) {
            MesProBatchRecordVersionDO reusableExpectedVersion =
                    versionMapper.selectByDefinitionIdAndVersionNo(definition.getId(), expectedTargetVersionNo);
            if (reusableExpectedVersion == null
                    || !List.of("DRAFT", "PRECHECK_PASSED", "PENDING_APPROVAL")
                    .contains(reusableExpectedVersion.getStatus())) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_TARGET_CHANGED,
                        expectedTargetVersionNo, currentTargetVersionNo);
            }
            return expectedTargetVersionNo;
        }
        return expectedTargetVersionNo == null ? currentTargetVersionNo : expectedTargetVersionNo;
    }

    private boolean canSupersedeReusablePendingVersion(MesProBatchRecordVersionDO version) {
        return version != null && List.of("DRAFT", "PRECHECK_PASSED").contains(version.getStatus());
    }

    private void voidSupersededReusablePendingVersion(MesProBatchRecordVersionDO version, String expectedTargetVersionNo) {
        version.setStatus("VOIDED");
        version.setRemark("同文件重新导入生成 " + expectedTargetVersionNo + "，自动作废未提交预检快照");
        versionMapper.updateById(version);
    }

    private void validateUpgradeImportAllowed(MesProBatchRecordDefinitionDO definition,
                                               MesProBatchRecordVersionDO sourceVersion,
                                               Long expectedSourceVersionId) {
        if (sourceVersion == null || expectedSourceVersionId == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                    .PRO_BATCH_RECORD_REPORT_VERSION_UPGRADE_SOURCE_REQUIRED, definition.getBatchRecordName());
        }
        if (expectedSourceVersionId != null && !Objects.equals(expectedSourceVersionId, sourceVersion.getId())) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_CURRENT_CHANGED,
                    definition.getId());
        }
    }

    private void validateImportActionAllowed(MesProBatchRecordDefinitionDO definition,
                                             MesProBatchRecordVersionDO sourceVersion,
                                             String importAction) {
        boolean hasMainReports = sourceVersion != null
                && hasVisibleMainReport(definition.getId(), sourceVersion.getId());
        List<VersionResetBlocker> blockers = sourceVersion == null ? List.of()
                : findVersionResetBlockers(List.of(sourceVersion));
        List<String> allowedActions = buildAllowedImportActions(sourceVersion, hasMainReports, blockers);
        if (!allowedActions.contains(importAction)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                    .PRO_BATCH_RECORD_REPORT_IMPORT_ACTION_NOT_ALLOWED, importAction);
        }
    }

    private List<String> buildAllowedImportActions(MesProBatchRecordVersionDO currentVersion,
                                                   boolean currentBatchRecordHasMainReports,
                                                   List<VersionResetBlocker> blockers) {
        if (currentVersion == null) {
            return List.of(IMPORT_ACTION_REBUILD_V1);
        }
        if (!currentBatchRecordHasMainReports && blockers.isEmpty()) {
            return List.of(IMPORT_ACTION_REBUILD_V1);
        }
        if (currentBatchRecordHasMainReports || !blockers.isEmpty()) {
            return List.of(IMPORT_ACTION_UPGRADE);
        }
        return List.of(IMPORT_ACTION_REBUILD_V1, IMPORT_ACTION_UPGRADE);
    }

    private MesProBatchRecordVersionDO findPendingApprovalVersion(List<MesProBatchRecordVersionDO> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        return versions.stream()
                .filter(version -> Objects.equals("PENDING_APPROVAL", version.getStatus()))
                .max(this::compareBatchRecordVersion)
                .orElse(null);
    }

    private void ensureNoPendingApprovalImport(MesProBatchRecordDefinitionDO definition) {
        if (definition == null) {
            return;
        }
        MesProBatchRecordVersionDO pendingApprovalVersion =
                versionMapper.selectPendingApprovalByDefinitionIdForUpdate(definition.getId());
        if (pendingApprovalVersion != null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                            .PRO_BATCH_RECORD_REPORT_VERSION_PENDING_APPROVAL_EXISTS,
                    definition.getBatchRecordName());
        }
    }

    private List<MesProBatchRecordImportPreflightResult.ReferenceBlocker> toReferenceBlockers(
            List<VersionResetBlocker> blockers) {
        if (blockers == null || blockers.isEmpty()) {
            return List.of();
        }
        return blockers.stream()
                .map(blocker -> MesProBatchRecordImportPreflightResult.ReferenceBlocker.builder()
                        .versionNo(blocker.versionNo())
                        .referenceName(blocker.referenceName())
                        .count(blocker.count())
                        .cleanupEntrance(blocker.cleanupEntrance())
                        .cleanupAction(blocker.cleanupAction())
                        .build())
                .toList();
    }

    private List<String> normalizeRouteProductNames(List<String> productNames) {
        List<String> normalized = productNames == null ? List.of() : productNames.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_NAME_REQUIRED);
        }
        return normalized;
    }

    private void validateDccProjectNameMatchesBatchRecordName(String batchRecordName, List<String> dccProjectNames) {
        if (dccProjectNames == null || dccProjectNames.size() != 1
                || !Objects.equals(batchRecordName, dccProjectNames.get(0))) {
            String projectNameText = dccProjectNames == null || dccProjectNames.isEmpty()
                    ? "" : String.join("、", dccProjectNames);
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_DCC_PROJECT_NAME_REQUIRED,
                    batchRecordName, projectNameText);
        }
    }

    private List<String> normalizeOptionalRouteProductNames(List<String> productNames) {
        return productNames == null ? List.of() : productNames.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private String resolveReportProductName(List<String> productNames) {
        return productNames == null || productNames.isEmpty() ? null : productNames.get(0);
    }

    private List<Long> normalizeSelectedRouteProductIds(List<Long> routeProductIds) {
        return routeProductIds == null ? List.of() : routeProductIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<MesProBatchRecordImportRouteProductOption> buildRouteProductOptions(
            MesProRouteDO route, MesProRouteVersionDO routeVersion, List<String> requestedProductNames) {
        Map<String, MesProBatchRecordImportRouteProductOption> options = new LinkedHashMap<>();
        if (route != null) {
            List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByRouteId(route.getId());
            Map<Long, cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO> itemById =
                    itemMapper.selectListByIds(routeProducts.stream()
                                    .map(MesProRouteProductDO::getItemId)
                                    .filter(Objects::nonNull)
                                    .toList())
                            .stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO::getId,
                                    item -> item,
                                    (left, right) -> left,
                                    LinkedHashMap::new));
            for (MesProRouteProductDO routeProduct : routeProducts) {
                cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO item = itemById.get(routeProduct.getItemId());
                String productName = item == null ? null : item.getName();
                if (StrUtil.isBlank(productName)) {
                    continue;
                }
                options.put(productName, MesProBatchRecordImportRouteProductOption.builder()
                        .optionKey("ROUTE_PRODUCT:" + routeProduct.getId())
                        .routeProductId(routeProduct.getId())
                        .routeId(route.getId())
                        .routeCode(route.getCode())
                        .routeName(route.getName())
                        .routeVersionId(routeVersion == null ? null : routeVersion.getId())
                        .routeVersionNo(routeVersion == null ? null : routeVersion.getVersionNo())
                        .productId(item.getId())
                        .productCode(item.getCode())
                        .productName(productName)
                        .existing(true)
                        .build());
            }
        }
        for (String productName : requestedProductNames) {
            options.putIfAbsent(productName, MesProBatchRecordImportRouteProductOption.builder()
                    .optionKey("PRODUCT_NAME:" + productName)
                    .productName(productName)
                    .existing(false)
                    .build());
        }
        return new ArrayList<>(options.values());
    }

    private Long resolveRouteProductScopeRouteId(MesProBatchRecordVersionDO sourceVersion, String projectName,
                                                 Long expectedRouteId, Long expectedRouteVersionId) {
        if (expectedRouteId != null) {
            List<MesProRouteDO> routes = routeMapper.selectListByName(projectName);
            if (routes.size() != 1 || !Objects.equals(routes.get(0).getId(), expectedRouteId)) {
                Long currentRouteId = routes.size() == 1 ? routes.get(0).getId() : null;
                MesProRouteVersionDO activeVersion = currentRouteId == null
                        ? null : routeVersionMapper.selectActiveByRouteId(currentRouteId);
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED,
                        expectedRouteId, expectedRouteVersionId, currentRouteId,
                        activeVersion == null ? null : activeVersion.getId());
            }
            MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(expectedRouteId);
            Long currentVersionId = activeVersion == null ? null : activeVersion.getId();
            if (!Objects.equals(expectedRouteVersionId, currentVersionId)) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED,
                        expectedRouteId, expectedRouteVersionId, expectedRouteId, currentVersionId);
            }
            return expectedRouteId;
        }
        if (sourceVersion != null && sourceVersion.getRouteId() != null) {
            return sourceVersion.getRouteId();
        }
        return null;
    }

    private List<String> resolveSelectedRouteProductNames(Long routeProductScopeRouteId,
                                                          List<Long> selectedRouteProductIds,
                                                          List<String> selectedProductNames) {
        LinkedHashSet<String> productNames = new LinkedHashSet<>(selectedProductNames);
        if (!selectedRouteProductIds.isEmpty()) {
            if (routeProductScopeRouteId == null) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_SCOPE_INVALID,
                        selectedRouteProductIds);
            }
            Map<Long, MesProRouteProductDO> routeProductById = new LinkedHashMap<>();
            List<Long> invalidRouteProductIds = new ArrayList<>();
            for (Long selectedRouteProductId : selectedRouteProductIds) {
                MesProRouteProductDO routeProduct = routeProductMapper.selectById(selectedRouteProductId);
                if (routeProduct != null && Objects.equals(routeProduct.getRouteId(), routeProductScopeRouteId)) {
                    routeProductById.putIfAbsent(routeProduct.getId(), routeProduct);
                    continue;
                }
                MesProRouteProductDO routeProductByItem = routeProductMapper.selectByRouteIdAndItemId(
                        routeProductScopeRouteId, selectedRouteProductId);
                if (routeProductByItem == null && routeProduct != null && routeProduct.getItemId() != null) {
                    routeProductByItem = routeProductMapper.selectByRouteIdAndItemId(
                            routeProductScopeRouteId, routeProduct.getItemId());
                }
                if (routeProductByItem == null) {
                    invalidRouteProductIds.add(selectedRouteProductId);
                } else {
                    routeProductById.putIfAbsent(routeProductByItem.getId(), routeProductByItem);
                }
            }
            if (!invalidRouteProductIds.isEmpty()) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_SCOPE_INVALID,
                        invalidRouteProductIds);
            }
            List<MesProRouteProductDO> routeProducts = new ArrayList<>(routeProductById.values());
            Map<Long, cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO> itemById =
                    itemMapper.selectListByIds(routeProducts.stream()
                                    .map(MesProRouteProductDO::getItemId)
                                    .filter(Objects::nonNull)
                                    .toList())
                            .stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO::getId,
                                    item -> item,
                                    (left, right) -> left,
                                    LinkedHashMap::new));
            for (MesProRouteProductDO routeProduct : routeProducts) {
                cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO item = itemById.get(routeProduct.getItemId());
                if (item == null || StrUtil.isBlank(item.getName())) {
                    throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_SCOPE_INVALID,
                            routeProduct.getId());
                }
                productNames.add(item.getName());
            }
        }
        if (productNames.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMPORT_SCOPE_EMPTY);
        }
        return new ArrayList<>(productNames);
    }

    private MesProBatchRecordImportResult buildCurrentVersionImportResult(MesProBatchRecordDefinitionDO definition,
                                                                           MesProBatchRecordVersionDO version) {
        List<MesProBatchRecordReportView> reports = loadCurrentVersionReports(definition.getId(), version.getId());
        return MesProBatchRecordImportResult.builder()
                .importedCount(reports.size())
                .createdCount(0)
                .updatedCount(0)
                .reports(reports)
                .build();
    }

    private MesProBatchRecordImportResult emptyImportResult() {
        return MesProBatchRecordImportResult.builder()
                .importedCount(0)
                .createdCount(0)
                .updatedCount(0)
                .reports(List.of())
                .build();
    }

    private List<MesProBatchRecordReportView> loadCurrentVersionReports(Long definitionId, Long versionId) {
        return reportMapper.selectListByDefinitionIdAndVersionId(definitionId, versionId).stream()
                .filter(report -> MesProBatchRecordFormSlotType.MAIN.getType().equals(report.getFormSlotType())
                        || report.getFormSlotType() == null)
                .map(this::toReportViewFromMetadata)
                .toList();
    }

    private RouteDisplay loadRouteDisplay(Long routeId) {
        if (routeId == null) {
            return RouteDisplay.empty();
        }
        MesProRouteDO route = routeMapper.selectById(routeId);
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectActiveByRouteId(routeId);
        return new RouteDisplay(routeId,
                route == null ? null : route.getCode(),
                route == null ? null : route.getName(),
                routeVersion == null ? null : routeVersion.getId(),
                routeVersion == null ? null : routeVersion.getVersionNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordVersionApprovalResult submitBatchRecordVersionApproval(Long versionId, Long actorUserId) {
        MesProBatchRecordVersionDO version = versionMapper.selectByIdForUpdate(versionId);
        if (version == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_NOT_EXISTS,
                    versionId);
        }
        MesProBatchRecordVersionDO submittedVersion = submitPrecheckVersionForApproval(version, actorUserId);
        return approvalResult(submittedVersion, null, null, "SUBMITTED");
    }

    private MesProBatchRecordVersionDO submitPrecheckVersionForApproval(MesProBatchRecordVersionDO version, Long actorUserId) {
        Long submitterUserId = Objects.requireNonNull(actorUserId,
                "PRO_BATCH_RECORD_REPORT_VERSION_APPROVAL_SUBMITTER_REQUIRED");
        if (!Objects.equals("PRECHECK_PASSED", version.getStatus())) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_STATUS_INVALID,
                    version.getId(), version.getStatus());
        }
        MesProBatchRecordDefinitionDO definition = definitionMapper.selectByIdForUpdate(version.getDefinitionId());
        if (definition == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_NOT_EXISTS);
        }
        MesProBatchRecordVersionDO pendingApprovalVersion =
                versionMapper.selectPendingApprovalByDefinitionIdForUpdate(version.getDefinitionId());
        if (pendingApprovalVersion != null && !Objects.equals(pendingApprovalVersion.getId(), version.getId())) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                            .PRO_BATCH_RECORD_REPORT_VERSION_PENDING_APPROVAL_EXISTS,
                    definition.getBatchRecordName());
        }
        if (migrationItemMapper.countBlockingItems(version.getId()) > 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_MIGRATION_BLOCKED,
                    version.getId());
        }
        businessApprovalOrchestrator.submit(buildBatchRecordVersionPublishContext(version, submitterUserId));
        MesProBatchRecordVersionDO submittedVersion = versionMapper.selectById(version.getId());
        if (submittedVersion == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_NOT_EXISTS,
                    version.getId());
        }
        return submittedVersion;
    }

    private BusinessApprovalContext buildBatchRecordVersionPublishContext(MesProBatchRecordVersionDO version,
                                                                          Long submitterUserId) {
        return BusinessApprovalContext.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .dataDomain(DATA_DOMAIN)
                .systemCode(SYSTEM_CODE)
                .objectType(BATCH_RECORD_VERSION_OBJECT_TYPE)
                .objectId(String.valueOf(version.getId()))
                .objectVersion(version.getVersionNo())
                .actionCode(PUBLISH_ACTION_CODE)
                .objectState(version.getStatus())
                .applicantUserId(submitterUserId)
                .reason("publish batch record version")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordVersionApprovalResult handleBatchRecordVersionApprovalCallback(String approvalInstanceId,
                                                                                          String approvalEventId,
                                                                                          String approvalResult,
                                                                                          Long actorUserId) {
        return handleBatchRecordVersionApprovalCallback(approvalInstanceId, approvalEventId, approvalResult,
                null, actorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordVersionApprovalResult handleBatchRecordVersionApprovalCallback(String approvalInstanceId,
                                                                                          String approvalEventId,
                                                                                          String approvalResult,
                                                                                          String rejectReason,
                                                                                          Long actorUserId) {
        MesProBatchRecordVersionApprovalEventDO existingEvent =
                approvalEventMapper.selectByApprovalEvent(approvalInstanceId, approvalEventId);
        if (existingEvent != null) {
            MesProBatchRecordVersionDO existingVersion = versionMapper.selectById(existingEvent.getVersionId());
            return approvalResult(existingVersion, existingEvent.getApprovalEventId(),
                    existingEvent.getApprovalResult(), "DUPLICATE");
        }
        String normalizedApprovalResult = validateApprovalResult(approvalResult);
        MesProBatchRecordVersionDO version = versionMapper.selectByApprovalInstanceId(approvalInstanceId);
        if (version == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                    .PRO_BATCH_RECORD_REPORT_VERSION_APPROVAL_NOT_EXISTS, approvalInstanceId);
        }
        validatePendingApprovalReviewer(version, actorUserId);
        return processBatchRecordVersionApproval(version, approvalInstanceId, approvalEventId,
                normalizedApprovalResult, rejectReason, actorUserId);
    }

    private String validateApprovalResult(String approvalResult) {
        String normalizedApprovalResult = StrUtil.trimToEmpty(approvalResult).toUpperCase(Locale.ROOT);
        if (!Objects.equals("APPROVED", normalizedApprovalResult)
                && !Objects.equals("REJECTED", normalizedApprovalResult)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants
                    .PRO_BATCH_RECORD_REPORT_VERSION_APPROVAL_RESULT_INVALID, approvalResult);
        }
        return normalizedApprovalResult;
    }

    private void validatePendingApprovalReviewer(MesProBatchRecordVersionDO version, Long actorUserId) {
        if (!Objects.equals("PENDING_APPROVAL", version.getStatus())) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_STATUS_INVALID,
                    version.getId(), version.getStatus());
        }
    }

    private MesProBatchRecordVersionApprovalResult processBatchRecordVersionApproval(MesProBatchRecordVersionDO version,
                                                                                    String approvalInstanceId,
                                                                                    String approvalEventId,
                                                                                    String approvalResult,
                                                                                    String rejectReason,
                                                                                    Long actorUserId) {
        String normalizedRejectReason = StrUtil.trimToNull(rejectReason);
        if (Objects.equals("APPROVED", approvalResult)) {
            approveVersion(version, actorUserId);
        } else {
            version.setStatus("REJECTED");
            version.setApprovedBy(actorUserId);
            version.setApprovedAt(LocalDateTime.now());
            version.setRejectReason(normalizedRejectReason);
            versionMapper.updateById(version);
        }
        approvalEventMapper.insert(MesProBatchRecordVersionApprovalEventDO.builder()
                .definitionId(version.getDefinitionId())
                .versionId(version.getId())
                .approvalInstanceId(approvalInstanceId)
                .approvalEventId(approvalEventId)
                .approvalResult(approvalResult)
                .processedResult("PROCESSED")
                .actorUserId(actorUserId)
                .processedAt(LocalDateTime.now())
                .remark(normalizedRejectReason)
                .build());
        return approvalResult(version, approvalEventId, approvalResult, "PROCESSED");
    }

    private void approveVersion(MesProBatchRecordVersionDO version, Long actorUserId) {
        int updated = definitionMapper.updateCurrentVersionIfMatch(
                version.getDefinitionId(), resolveExpectedCurrentVersionId(version), version.getId());
        if (updated != 1) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_CURRENT_CHANGED,
                    version.getDefinitionId());
        }
        versionMapper.obsoleteApprovedVersionsExcept(version.getDefinitionId(), version.getId());
        version.setStatus("APPROVED");
        version.setApprovedBy(actorUserId);
        version.setApprovedAt(LocalDateTime.now());
        versionMapper.updateById(version);
    }

    private Long resolveExpectedCurrentVersionId(MesProBatchRecordVersionDO version) {
        if (version == null || version.getSourceVersionId() == null) {
            return null;
        }
        MesProBatchRecordVersionDO sourceVersion = versionMapper.selectById(version.getSourceVersionId());
        if (sourceVersion != null
                && Objects.equals("VOIDED", sourceVersion.getStatus())
                && sourceVersion.getSourceVersionId() != null) {
            return sourceVersion.getSourceVersionId();
        }
        return version.getSourceVersionId();
    }

    private MesProBatchRecordVersionApprovalResult approvalResult(MesProBatchRecordVersionDO version,
                                                                  String approvalEventId,
                                                                  String approvalResult,
                                                                  String processedResult) {
        return MesProBatchRecordVersionApprovalResult.builder()
                .definitionId(version == null ? null : version.getDefinitionId())
                .versionId(version == null ? null : version.getId())
                .versionStatus(version == null ? null : version.getStatus())
                .approvalInstanceId(version == null ? null : version.getApprovalInstanceId())
                .approvalEventId(approvalEventId)
                .approvalResult(approvalResult)
                .processedResult(processedResult)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult uploadExtraFormSlot(MultipartFile file, String batchRecordName,
                                                             String formSlotType) {
        return uploadExtraFormSlot(file, batchRecordName, formSlotType, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordImportResult uploadExtraFormSlot(MultipartFile file, String batchRecordName,
                                                             String formSlotType,
                                                             Long approvalSubmitterUserId) {
        String normalizedBatchRecordName = normalizeBatchRecordName(batchRecordName);
        String normalizedFormSlotType = normalizeExtraFormSlotType(formSlotType);
        validateExtraSlotWordFile(file);
        byte[] bytes = getBytes(file);
        String sourceFileName = normalizeFileName(file.getOriginalFilename());
        List<MesProBatchRecordParsedTable> parsedTables = formProfileRegistry.normalizeSourceTables(
                normalizedFormSlotType, parseWordByFileName(bytes, sourceFileName));
        if (parsedTables.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID,
                    parsedTables.size());
        }
        String sha256 = sha256(bytes);
        ExtraFormSlotVersionContext versionContext = prepareExtraFormSlotVersion(
                normalizedBatchRecordName, normalizedFormSlotType, approvalSubmitterUserId);
        MesProBatchRecordDefinitionDO definition = versionContext.definition();
        MesProBatchRecordVersionDO sourceVersion = versionContext.currentVersion();
        MesProBatchRecordVersionDO reusablePendingVersion =
                sourceVersion == null ? null : versionMapper.selectReusablePendingByHash(definition.getId(), sha256);
        if (reusablePendingVersion != null && hasGeneratedReports(definition.getId(), reusablePendingVersion.getId())) {
            return buildReusableImportResult(definition, reusablePendingVersion,
                    normalizedBatchRecordName, approvalSubmitterUserId);
        }
        MesProBatchRecordVersionDO targetVersion = reusablePendingVersion == null
                ? createPrecheckVersion(definition, sourceVersion, sourceFileName, sha256, null)
                : reusablePendingVersion;
        MesProBatchRecordImportResult importResult = saveGeneratedReports(parsedTables, sourceFileName, sha256,
                scopeSampleKeyByTenant(buildExtraFormSlotVersionSampleKey(
                        normalizedBatchRecordName, normalizedFormSlotType, targetVersion.getId())),
                normalizedFormSlotType, normalizedBatchRecordName, GeneratedReportSource.UPLOADED_DOC, false,
                normalizedFormSlotType, definition.getId(), targetVersion.getId(), true, normalizedBatchRecordName);
        if (sourceVersion != null) {
            copySourceVersionReportFillRules(definition.getId(), sourceVersion, targetVersion,
                    importResult.reports());
        }
        if (sourceVersion == null) {
            activateInitialVersionWithoutApproval(targetVersion);
        } else if (approvalSubmitterUserId != null && Objects.equals("PRECHECK_PASSED", targetVersion.getStatus())) {
            targetVersion = submitPrecheckVersionForApproval(targetVersion, approvalSubmitterUserId);
        }
        return MesProBatchRecordImportResult.builder()
                .importedCount(importResult.importedCount())
                .createdCount(importResult.createdCount())
                .updatedCount(importResult.updatedCount())
                .batchRecordDefinitionId(definition.getId())
                .batchRecordVersionId(targetVersion.getId())
                .sourceBatchRecordVersionId(targetVersion.getSourceVersionId())
                .versionNo(targetVersion.getVersionNo())
                .versionStatus(targetVersion.getStatus())
                .approvalInstanceId(targetVersion.getApprovalInstanceId())
                .reports(importResult.reports())
                .build();
    }

    @Override
    public Boolean existsBatchRecordName(String routeKey, String batchRecordName) {
        String normalizedRouteKey = MesProBatchRecordRecognitionRouteKeys.normalize(routeKey);
        if (!MesProBatchRecordRecognitionRouteKeys.isFixedRoute(normalizedRouteKey)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_INVALID, routeKey);
        }
        String normalizedBatchRecordName = normalizeBatchRecordName(batchRecordName);
        return reportMapper.countMainByBatchRecordNameAndRouteKey(normalizedBatchRecordName, normalizedRouteKey,
                MesProBatchRecordFormSlotType.MAIN.getType()) > 0;
    }

    @Override
    public List<String> getBatchRecordNameOptions() {
        return reportMapper.selectList()
                .stream()
                .map(MesProBatchRecordReportDO::getBatchRecordName)
                .map(name -> StrUtil.blankToDefault(name, DEFAULT_BATCH_RECORD_NAME))
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public PageResult<MesProBatchRecordReportView> getGeneratedReportPage(BatchRecordReportPageReqVO pageReqVO) {
        List<MesProBatchRecordReportView> baseReports = reportMapper.selectList()
                .stream()
                .map(this::toVisibleReportView)
                .filter(Objects::nonNull)
                .filter(report -> filterByReportId(report, pageReqVO.getReportId()))
                .filter(report -> filterByRouteKey(report, pageReqVO.getRouteKey()))
                .filter(report -> filterByBatchRecordName(report, pageReqVO.getBatchRecordName()))
                .filter(report -> filterByFormSlotType(report, pageReqVO.getFormSlotType()))
                .filter(report -> filterByName(report, pageReqVO.getName()))
                .sorted(Comparator.comparing(MesProBatchRecordReportView::lastImportTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MesProBatchRecordReportView::sourceTableIndex))
                .toList();
        List<MesProBatchRecordReportView> latestScopedReports = Boolean.TRUE.equals(pageReqVO.getLatestVersionOnly())
                ? filterLatestBatchRecordVersions(baseReports)
                : baseReports;
        List<MesProBatchRecordReportView> allReports = expandReportsByVersionProducts(latestScopedReports)
                .stream()
                .filter(report -> filterByProductName(report, pageReqVO.getProductName()))
                .filter(report -> filterByVersionNo(report, pageReqVO.getVersionNo()))
                .toList();
        if (Boolean.TRUE.equals(pageReqVO.getLatestVersionOnly())) {
            allReports = filterLatestVisibleBatchRecordVersions(allReports);
        }
        int fromIndex = Math.max((pageReqVO.getPageNo() - 1) * pageReqVO.getPageSize(), 0);
        int toIndex = Math.min(fromIndex + pageReqVO.getPageSize(), allReports.size());
        List<MesProBatchRecordReportView> pageList = fromIndex >= allReports.size()
                ? List.of()
                : allReports.subList(fromIndex, toIndex);
        return new PageResult<>(pageList, (long) allReports.size());
    }

    @Override
    public String getDesignerPath(String reportId) {
        requireMetadata(reportId);
        return jimuReportGateway.buildPreviewPath(reportId);
    }

    @Override
    public String getEditPath(String reportId) {
        requireMetadata(reportId);
        return jimuReportGateway.buildDesignerPath(reportId);
    }

    @Override
    public BatchRecordReportSignatureCellMarkersRespVO getSignatureCellMarkers(String reportId) {
        requireMetadata(reportId);
        JSONObject root = parseReportJson(reportId);
        return toSignatureCellMarkersRespVO(reportId, root);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordReportSignatureCellMarkersRespVO saveSignatureCellMarkers(
            BatchRecordReportSignatureCellMarkersReqVO reqVO) {
        requireMetadata(reqVO.getReportId());
        JSONObject root = parseReportJson(reqVO.getReportId());
        clearSignatureMarkers(root);
        Set<String> signatureCellKeys = new LinkedHashSet<>();
        for (BatchRecordReportSignatureCellMarkerVO marker : reqVO.getMarkers()) {
            if (!Boolean.TRUE.equals(marker.getEnabled())) {
                continue;
            }
            validateSignatureMarker(marker);
            JSONObject cell = requireCell(root, marker.getRowIndex(), marker.getColumnIndex());
            String signatureCellKey = buildSignatureCellKey(marker);
            if (!signatureCellKeys.add(signatureCellKey)) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_CELL_DUPLICATE,
                        signatureCellKey);
            }
            JSONObject signature = new JSONObject(true);
            signature.put("enabled", true);
            signature.put("signatureCellKey", signatureCellKey);
            signature.put("actionType", marker.getActionType());
            signature.put("label", StrUtil.blankToDefault(marker.getLabel(), marker.getActionType()));
            signature.put("displayFormat", StrUtil.blankToDefault(marker.getDisplayFormat(),
                    DEFAULT_SIGNATURE_DISPLAY_FORMAT));
            if (Objects.equals("APPROVE", marker.getActionType())) {
                signature.put("reviewSourceType", marker.getReviewSourceType());
                if (isMultipleReviewSourceType(marker.getReviewSourceType())) {
                    signature.put("reviewSourceIds", normalizeReviewSourceIds(marker));
                } else {
                    signature.put("reviewSourceId", marker.getReviewSourceId());
                }
                signature.put("reviewSourceName", StrUtil.blankToDefault(StrUtil.trim(marker.getReviewSourceName()),
                        defaultReviewSourceName(marker)));
            }
            cell.put("edhrSignature", signature);
        }
        jimuReportGateway.updateReportJson(reqVO.getReportId(), root.toJSONString());
        return toSignatureCellMarkersRespVO(reqVO.getReportId(), root);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordReportCellRulesRespVO getCellRules(String reportId) {
        MesProBatchRecordReportDO metadata = requireMetadata(reportId);
        JSONObject root = parseReportJson(reportId);
        ensureNoLegacyFormProfileLayoutOnRead(metadata, root);
        int normalizedCount = MesProBatchRecordCellRuleSupport.normalizeAutomaticRulesAsUnreviewed(root);
        int refreshedCount = MesProBatchRecordCellRuleSupport.refreshUnreviewedAutomaticSuggestions(
                root, metadata.getReportCode());
        if (normalizedCount > 0 || refreshedCount > 0) {
            jimuReportGateway.updateReportJson(metadata.getReportId(), root.toJSONString());
        }
        return toCellRulesRespVO(reportId, root);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordReportCellRulesRespVO saveCellRules(BatchRecordReportCellRulesReqVO reqVO) {
        MesProBatchRecordReportDO metadata = requireMetadata(reqVO.getReportId());
        JSONObject root = parseReportJson(reqVO.getReportId());
        clearCellRules(root);
        for (BatchRecordReportCellRuleVO rule : reqVO.getRules()) {
            JSONObject cell = MesProBatchRecordCellRuleSupport.requireCell(root, rule.getRowIndex(), rule.getColumnIndex());
            if (cell == null) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_CELL_RULE_CELL_MISSING,
                        rule.getRowIndex(), rule.getColumnIndex());
            }
            try {
                MesProBatchRecordCellRuleSupport.ensureManualFillForm(rule, cell, metadata.getReportCode());
                MesProBatchRecordCellRuleSupport.validateRule(rule, cell);
            } catch (IllegalArgumentException ex) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_CELL_RULE_INVALID,
                        ex.getMessage());
            }
            cell.put(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY,
                    MesProBatchRecordCellRuleSupport.toRuleJson(rule));
        }
        jimuReportGateway.updateReportJson(reqVO.getReportId(), root.toJSONString());
        return toCellRulesRespVO(reqVO.getReportId(), root);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameGeneratedReport(String reportId, String reportName) {
        MesProBatchRecordReportDO report = requireMetadata(reportId);
        String normalizedName = normalizeReportName(reportName);
        jimuReportGateway.renameReportName(report.getReportId(), normalizedName);
        report.setReportName(normalizedName);
        reportMapper.updateById(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGeneratedReport(String reportId) {
        deleteGeneratedReports(List.of(reportId), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordReportDeleteAllRespVO deleteGeneratedReports(List<String> reportIds, Boolean forceUnbind) {
        List<MesProBatchRecordReportDO> reports = normalizeReportIds(reportIds).stream()
                .map(this::requireMetadata)
                .toList();
        return deleteGeneratedReports(reports, Boolean.TRUE.equals(forceUnbind), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGeneratedReportByBatchRecordNameAndFormSlotType(String batchRecordName, String formSlotType) {
        String normalizedBatchRecordName = normalizeBatchRecordName(batchRecordName);
        String normalizedFormSlotType = normalizeExtraFormSlotType(formSlotType);
        List<MesProBatchRecordReportDO> reports = reportMapper.selectListByBatchRecordNameAndFormSlotType(
                normalizedBatchRecordName, normalizedFormSlotType);
        for (MesProBatchRecordReportDO report : reports) {
            validateReportNotBound(report.getReportId());
        }
        for (MesProBatchRecordReportDO report : reports) {
            jimuReportGateway.deleteReport(report.getReportId());
            reportMapper.deleteHardByReportId(report.getReportId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordReportDeleteAllRespVO deleteGeneratedReportsByBatchRecordName(String batchRecordName) {
        return deleteGeneratedReportsByBatchRecordName(batchRecordName, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordReportDeleteAllRespVO deleteGeneratedReportsByBatchRecordName(String batchRecordName,
                                                                                   Boolean forceUnbind) {
        String normalizedBatchRecordName = normalizeBatchRecordName(batchRecordName);
        List<MesProBatchRecordReportDO> reports = reportMapper.selectList(MesProBatchRecordReportDO::getBatchRecordName,
                normalizedBatchRecordName);
        return deleteGeneratedReports(reports, Boolean.TRUE.equals(forceUnbind), true);
    }

    private BatchRecordReportDeleteAllRespVO deleteGeneratedReports(List<MesProBatchRecordReportDO> reports,
                                                                    boolean forceUnbindEnabled,
                                                                    boolean skipBoundReports) {
        int deletedReportCount = 0;
        int deletedMetadataCount = 0;
        int skippedBoundReportCount = 0;
        int unboundRouteProcessCount = 0;
        int deletedRouteFlowBindingCount = 0;
        int unboundRouteFlowProcessConfigCount = 0;
        Set<Long> affectedDefinitionIds = reports.stream()
                .map(MesProBatchRecordReportDO::getBatchRecordDefinitionId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!forceUnbindEnabled && !skipBoundReports) {
            for (MesProBatchRecordReportDO report : reports) {
                validateReportNotBound(report.getReportId());
            }
        }
        if (forceUnbindEnabled && !reports.isEmpty()) {
            List<String> reportIds = reports.stream()
                    .map(MesProBatchRecordReportDO::getReportId)
                    .filter(StrUtil::isNotBlank)
                    .toList();
            if (!reportIds.isEmpty()) {
                unboundRouteProcessCount = routeProcessMapper.unbindBatchRecordReportIds(reportIds);
                deletedRouteFlowBindingCount = routeFlowProcessBatchRecordMapper.deleteByBatchRecordReportIds(reportIds);
                unboundRouteFlowProcessConfigCount = routeFlowProcessConfigMapper.unbindBatchRecordReportIds(reportIds);
            }
        }
        for (MesProBatchRecordReportDO report : reports) {
            String reportId = report.getReportId();
            if (!forceUnbindEnabled && isReportBound(reportId)) {
                skippedBoundReportCount++;
                continue;
            }
            jimuReportGateway.deleteReport(reportId);
            deletedReportCount++;
            deletedMetadataCount += reportMapper.deleteHardByReportId(reportId);
        }
        cleanupDeletedBatchRecordDefinitionsAfterReportDelete(affectedDefinitionIds);
        return new BatchRecordReportDeleteAllRespVO()
                .setDeletedMetadataCount(deletedMetadataCount)
                .setDeletedReportCount(deletedReportCount)
                .setSkippedBoundReportCount(skippedBoundReportCount)
                .setUnboundRouteProcessCount(unboundRouteProcessCount)
                .setDeletedRouteFlowBindingCount(deletedRouteFlowBindingCount)
                .setUnboundRouteFlowProcessConfigCount(unboundRouteFlowProcessConfigCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordReportDeleteAllRespVO deleteAllGeneratedReports(String confirm) {
        validateDeleteAllConfirm(confirm);
        String categoryId = jimuReportGateway.findElectronicBatchRecordCategoryId();
        if (StrUtil.isBlank(categoryId)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_CATEGORY_NOT_EXISTS);
        }
        int deletedReportCount = 0;
        int deletedMetadataCount = 0;
        int skippedBoundReportCount = 0;
        List<MesProBatchRecordReportDO> reports = reportMapper.selectListByReportCategoryId(categoryId);
        for (MesProBatchRecordReportDO report : reports) {
            String reportId = report.getReportId();
            if (isReportBound(reportId)) {
                skippedBoundReportCount++;
                continue;
            }
            jimuReportGateway.deleteReport(reportId);
            deletedReportCount++;
            deletedMetadataCount += reportMapper.deleteHardByReportId(reportId);
        }
        return new BatchRecordReportDeleteAllRespVO()
                .setDeletedMetadataCount(deletedMetadataCount)
                .setDeletedReportCount(deletedReportCount)
                .setSkippedBoundReportCount(skippedBoundReportCount);
    }

    private MesProBatchRecordImportResult saveGeneratedReports(List<MesProBatchRecordParsedTable> parsedTables,
                                                               String sourceFileName,
                                                               String sha256,
                                                               String sampleKey,
                                                               String routeKey,
                                                               String batchRecordName,
                                                               GeneratedReportSource source,
                                                               boolean matchByBatchRecordName) {
        return saveGeneratedReports(parsedTables, sourceFileName, sha256, sampleKey, routeKey, batchRecordName,
                source, matchByBatchRecordName, MesProBatchRecordFormSlotType.MAIN.getType());
    }

    private MesProBatchRecordImportResult saveGeneratedReports(List<MesProBatchRecordParsedTable> parsedTables,
                                                               String sourceFileName,
                                                               String sha256,
                                                               String sampleKey,
                                                               String routeKey,
                                                               String batchRecordName,
                                                               GeneratedReportSource source,
                                                               boolean matchByBatchRecordName,
                                                               String formSlotType) {
        return saveGeneratedReports(parsedTables, sourceFileName, sha256, sampleKey, routeKey, batchRecordName,
                source, matchByBatchRecordName, formSlotType, null, null, false);
    }

    private MesProBatchRecordImportResult saveGeneratedReports(List<MesProBatchRecordParsedTable> parsedTables,
                                                               String sourceFileName,
                                                               String sha256,
                                                               String sampleKey,
                                                               String routeKey,
                                                               String batchRecordName,
                                                               GeneratedReportSource source,
                                                               boolean matchByBatchRecordName,
                                                               String formSlotType,
                                                               Long batchRecordDefinitionId,
                                                               Long batchRecordVersionId,
                                                               boolean forceCreateSnapshot) {
        return saveGeneratedReports(parsedTables, sourceFileName, sha256, sampleKey, routeKey, batchRecordName,
                source, matchByBatchRecordName, formSlotType, batchRecordDefinitionId, batchRecordVersionId,
                forceCreateSnapshot, null);
    }

    private MesProBatchRecordImportResult saveGeneratedReports(List<MesProBatchRecordParsedTable> parsedTables,
                                                               String sourceFileName,
                                                               String sha256,
                                                               String sampleKey,
                                                               String routeKey,
                                                               String batchRecordName,
                                                               GeneratedReportSource source,
                                                               boolean matchByBatchRecordName,
                                                               String formSlotType,
                                                               Long batchRecordDefinitionId,
                                                               Long batchRecordVersionId,
                                                               boolean forceCreateSnapshot,
                                                               String productName) {
        String normalizedRouteKey = MesProBatchRecordRecognitionRouteKeys.normalize(routeKey);
        String normalizedFormSlotType = normalizeFormSlotType(formSlotType);
        String normalizedProductName = StrUtil.trim(productName);
        if (StrUtil.isBlank(normalizedProductName)) {
            normalizedProductName = null;
        }
        String categoryId = jimuReportGateway.ensureElectronicBatchRecordCategoryId();
        LocalDateTime now = LocalDateTime.now();
        int createdCount = 0;
        int updatedCount = 0;
        List<MesProBatchRecordReportView> reports = new ArrayList<>();

        String reportCodeHash = forceCreateSnapshot && batchRecordVersionId == null
                ? sha256((sha256 + "|" + sampleKey).getBytes(StandardCharsets.UTF_8)) : sha256;
        for (MesProBatchRecordParsedTable parsedTable : parsedTables) {
            String reportCode = buildReportCode(source, reportCodeHash, normalizedRouteKey, parsedTable.getSourceTableIndex(),
                    batchRecordVersionId);
            String reportName = buildReportName(source, normalizedRouteKey, sourceFileName, parsedTable,
                    batchRecordName, normalizedFormSlotType);
            logT06SourceSnapshot(reportCode, parsedTable);
            MesProBatchRecordReportDO existing = forceCreateSnapshot ? null : findExistingGeneratedReport(
                    matchByBatchRecordName, sampleKey, normalizedRouteKey, batchRecordName,
                    parsedTable.getSourceTableIndex(), sha256, reportCode);
            MesProBatchRecordGeneratedReport generatedReport = jimuReportGateway.saveOrUpdateReport(
                    MesProBatchRecordJimuReportSaveReq.builder()
                            .existingReportId(existing == null ? null : existing.getReportId())
                            .categoryId(categoryId)
                            .reportCode(reportCode)
                            .reportName(reportName)
                            .parsedTable(parsedTable)
                            .build());
            if (existing == null) {
                MesProBatchRecordReportDO created = new MesProBatchRecordReportDO();
                created.setSampleKey(sampleKey);
                created.setBatchRecordName(batchRecordName);
                created.setProductName(normalizedProductName);
                created.setFormSlotType(normalizedFormSlotType);
                created.setRouteKey(normalizedRouteKey);
                created.setBatchRecordDefinitionId(batchRecordDefinitionId);
                created.setBatchRecordVersionId(batchRecordVersionId);
                created.setSourceFileName(sourceFileName);
                created.setSourceFileSha256(sha256);
                created.setSourceTableIndex(parsedTable.getSourceTableIndex());
                created.setTableTitle(parsedTable.getTableTitle());
                created.setReportId(generatedReport.reportId());
                created.setReportCode(generatedReport.reportCode());
                created.setReportName(generatedReport.reportName());
                created.setReportCategoryId(categoryId);
                created.setLastImportTime(now);
                reportMapper.insert(created);
                createdCount++;
            } else {
                existing.setSampleKey(sampleKey);
                existing.setBatchRecordName(batchRecordName);
                existing.setProductName(normalizedProductName);
                existing.setFormSlotType(normalizedFormSlotType);
                existing.setRouteKey(normalizedRouteKey);
                existing.setBatchRecordDefinitionId(batchRecordDefinitionId);
                existing.setBatchRecordVersionId(batchRecordVersionId);
                existing.setSourceFileName(sourceFileName);
                existing.setSourceFileSha256(sha256);
                existing.setTableTitle(parsedTable.getTableTitle());
                existing.setReportId(generatedReport.reportId());
                existing.setReportCode(generatedReport.reportCode());
                existing.setReportName(generatedReport.reportName());
                existing.setReportCategoryId(categoryId);
                existing.setLastImportTime(now);
                reportMapper.updateById(existing);
                updatedCount++;
            }
            reports.add(MesProBatchRecordReportView.builder()
                    .batchRecordName(batchRecordName)
                    .batchRecordDefinitionId(batchRecordDefinitionId)
                    .batchRecordVersionId(batchRecordVersionId)
                    .productName(normalizedProductName)
                    .formSlotType(normalizedFormSlotType)
                    .routeKey(normalizedRouteKey)
                    .sourceTableIndex(parsedTable.getSourceTableIndex())
                    .tableTitle(parsedTable.getTableTitle())
                    .reportId(generatedReport.reportId())
                    .reportCode(generatedReport.reportCode())
                    .reportName(generatedReport.reportName())
                    .sourceFileName(sourceFileName)
                    .lastImportTime(now)
                    .updateTime(now)
                    .build());
        }

        reports.sort(Comparator.comparing(MesProBatchRecordReportView::sourceTableIndex));
        return MesProBatchRecordImportResult.builder()
                .importedCount(reports.size())
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .reports(reports)
                .build();
    }

    private void copySourceVersionReportFillRules(Long definitionId,
                                                  MesProBatchRecordVersionDO sourceVersion,
                                                  MesProBatchRecordVersionDO targetVersion,
                                                  List<MesProBatchRecordReportView> targetReports) {
        if (definitionId == null || sourceVersion == null || targetVersion == null || targetReports.isEmpty()) {
            return;
        }
        Map<String, MesProBatchRecordReportDO> sourceReports = reportMapper
                .selectListByDefinitionIdAndVersionId(definitionId, sourceVersion.getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        report -> buildVersionReportMatchKey(report.getFormSlotType(), report.getSourceTableIndex()),
                        report -> report,
                        (left, right) -> left,
                        LinkedHashMap::new));
        for (MesProBatchRecordReportView targetReport : targetReports) {
            MesProBatchRecordReportDO sourceReport = sourceReports.get(
                    buildVersionReportMatchKey(targetReport.formSlotType(), targetReport.sourceTableIndex()));
            if (sourceReport == null || StrUtil.isBlank(sourceReport.getReportId())
                    || StrUtil.isBlank(targetReport.reportId())) {
                continue;
            }
            List<MesProEdhrProcessFormPermissionRuleDO> sourceFillRules = processFormPermissionRuleMapper
                    .selectListByRouteProcessAndReport(
                            MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID,
                            sourceReport.getReportId())
                    .stream()
                    .filter(rule -> Objects.equals("FILL", rule.getRuleType()))
                    .toList();
            if (sourceFillRules.isEmpty()) {
                continue;
            }
            processFormPermissionRuleMapper.physicalDeleteByRouteProcessAndReport(
                    MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID,
                    targetReport.reportId());
            for (MesProEdhrProcessFormPermissionRuleDO sourceFillRule : sourceFillRules) {
                processFormPermissionRuleMapper.insert(copyReportFillRule(
                        sourceFillRule, targetReport.reportId(), definitionId, targetVersion.getId()));
            }
        }
    }

    private String buildVersionReportMatchKey(String formSlotType, Integer sourceTableIndex) {
        return StrUtil.blankToDefault(formSlotType, MesProBatchRecordFormSlotType.MAIN.getType())
                + "|" + sourceTableIndex;
    }

    private MesProEdhrProcessFormPermissionRuleDO copyReportFillRule(
            MesProEdhrProcessFormPermissionRuleDO sourceRule,
            String targetReportId,
            Long targetDefinitionId,
            Long targetVersionId) {
        return new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(MesProEdhrProcessFormPermissionRuleMapper.FORM_LEVEL_ROUTE_PROCESS_ID)
                .setBatchRecordReportId(targetReportId)
                .setBatchRecordDefinitionId(targetDefinitionId)
                .setBatchRecordVersionId(targetVersionId)
                .setRuleType(sourceRule.getRuleType())
                .setSignatureCellKey(StrUtil.blankToDefault(sourceRule.getSignatureCellKey(), ""))
                .setSignatureRole(sourceRule.getSignatureRole())
                .setCandidateSourceType(sourceRule.getCandidateSourceType())
                .setCandidateSourceIds(sourceRule.getCandidateSourceIds())
                .setCompletionPolicy(sourceRule.getCompletionPolicy())
                .setDueMinutes(sourceRule.getDueMinutes())
                .setEnabled(Boolean.TRUE.equals(sourceRule.getEnabled()))
                .setRemark(sourceRule.getRemark());
    }

    private ExtraFormSlotVersionContext prepareExtraFormSlotVersion(String batchRecordName, String formSlotType,
                                                                    Long approvalSubmitterUserId) {
        MesProBatchRecordDefinitionDO definition = getOrCreateDefinition(batchRecordName, formSlotType);
        MesProBatchRecordVersionDO currentVersion = definition.getCurrentVersionId() == null
                ? null : versionMapper.selectById(definition.getCurrentVersionId());
        if (currentVersion != null) {
            return new ExtraFormSlotVersionContext(definition, currentVersion);
        }
        List<MesProBatchRecordReportDO> legacyReports = reportMapper
                .selectListByBatchRecordNameAndFormSlotType(batchRecordName, formSlotType)
                .stream()
                .filter(report -> report.getBatchRecordVersionId() == null)
                .toList();
        if (legacyReports.isEmpty()) {
            return new ExtraFormSlotVersionContext(definition, null);
        }
        MesProBatchRecordVersionDO initialVersion = createPrecheckVersion(
                definition, null,
                resolveLegacyExtraSlotSourceFileName(legacyReports),
                resolveLegacyExtraSlotSourceFileSha256(legacyReports),
                null);
        for (MesProBatchRecordReportDO legacyReport : legacyReports) {
            legacyReport.setBatchRecordDefinitionId(definition.getId());
            legacyReport.setBatchRecordVersionId(initialVersion.getId());
            legacyReport.setBatchRecordName(batchRecordName);
            legacyReport.setProductName(StrUtil.blankToDefault(legacyReport.getProductName(), batchRecordName));
            legacyReport.setFormSlotType(formSlotType);
            legacyReport.setRouteKey(formSlotType);
            reportMapper.updateById(legacyReport);
        }
        initialVersion = activateInitialVersionWithDirectApproval(initialVersion, approvalSubmitterUserId);
        return new ExtraFormSlotVersionContext(definition, initialVersion);
    }

    private MesProBatchRecordVersionDO activateInitialVersionWithDirectApproval(MesProBatchRecordVersionDO version,
                                                                                Long actorUserId) {
        Long submitterUserId = Objects.requireNonNull(actorUserId,
                "PRO_BATCH_RECORD_REPORT_VERSION_APPROVAL_SUBMITTER_REQUIRED");
        BusinessApprovalContext context = buildBatchRecordVersionPublishContext(version, submitterUserId);
        BusinessApprovalRequest request = BusinessApprovalRequest.builder()
                .requestId(version.getId())
                .tenantId(context.getTenantId())
                .effectExecutorCode(MesProBatchRecordVersionBusinessApprovalEffectExecutor.EXECUTOR_CODE)
                .status(BusinessApprovalRequestStatus.DIRECT_EXECUTING)
                .context(context)
                .build();
        batchRecordVersionApprovalEffectExecutor.executeDirect(context, request);
        MesProBatchRecordVersionDO activatedVersion = versionMapper.selectById(version.getId());
        if (activatedVersion == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_NOT_EXISTS,
                    version.getId());
        }
        return activatedVersion;
    }

    private String resolveLegacyExtraSlotSourceFileName(List<MesProBatchRecordReportDO> legacyReports) {
        return legacyReports.stream()
                .map(MesProBatchRecordReportDO::getSourceFileName)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse("legacy-extra-slot.doc");
    }

    private String resolveLegacyExtraSlotSourceFileSha256(List<MesProBatchRecordReportDO> legacyReports) {
        return legacyReports.stream()
                .map(MesProBatchRecordReportDO::getSourceFileSha256)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElseGet(() -> sha256(("legacy-extra-slot|" + legacyReports.size()).getBytes(StandardCharsets.UTF_8)));
    }

    private record ExtraFormSlotVersionContext(MesProBatchRecordDefinitionDO definition,
                                               MesProBatchRecordVersionDO currentVersion) {
    }

    private MesProBatchRecordDefinitionDO getOrCreateDefinition(String batchRecordName, String routeKey) {
        MesProBatchRecordDefinitionDO existing = definitionMapper.selectByNameAndRouteKey(batchRecordName, routeKey);
        if (existing != null) {
            return existing;
        }
        MesProBatchRecordDefinitionDO created = MesProBatchRecordDefinitionDO.builder()
                .batchRecordName(batchRecordName)
                .routeKey(routeKey)
                .build();
        try {
            definitionMapper.insert(created);
        } catch (DuplicateKeyException duplicateKeyException) {
            MesProBatchRecordDefinitionDO concurrent = definitionMapper.selectByNameAndRouteKey(batchRecordName, routeKey);
            if (concurrent == null) {
                throw duplicateKeyException;
            }
            return concurrent;
        }
        return created;
    }

    private void cleanupDeletedBatchRecordDefinitionsAfterReportDelete(Set<Long> definitionIds) {
        if (definitionIds == null || definitionIds.isEmpty()) {
            return;
        }
        for (Long definitionId : definitionIds) {
            MesProBatchRecordDefinitionDO definition = definitionMapper.selectById(definitionId);
            if (definition == null || hasVisibleMainReports(definition.getId())) {
                continue;
            }
            cleanupDefinitionVersionsIfNoReports(definition, false);
        }
    }

    private void cleanupOrphanDefinitionBeforeImport(String batchRecordName, String routeKey) {
        MesProBatchRecordDefinitionDO definition = definitionMapper.selectByNameAndRouteKey(batchRecordName, routeKey);
        if (definition == null || hasVisibleMainReports(definition.getId())) {
            return;
        }
        cleanupDefinitionVersionsIfNoReports(definition, !hasAnyVersionResetBlocker(definition));
    }

    private void ensureV1ImportAllowed(String batchRecordName, String routeKey) {
        MesProBatchRecordDefinitionDO definition = definitionMapper.selectByNameAndRouteKey(batchRecordName, routeKey);
        if (definition == null) {
            if (Boolean.TRUE.equals(existsBatchRecordName(routeKey, batchRecordName))) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_BATCH_NAME_EXISTS,
                        batchRecordName);
            }
            return;
        }
        if (hasVisibleMainReports(definition.getId())) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FORM_SLOT_EXISTS,
                    batchRecordName, "主批记录");
        }
        String blocker = buildVersionResetBlockerMessage(versionMapper.selectListByDefinitionId(definition.getId()));
        if (StrUtil.isNotBlank(blocker)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_RESET_BLOCKED,
                    blocker);
        }
        cleanupDefinitionVersionsIfNoReports(definition, true);
    }

    private boolean cleanupDefinitionVersionsIfNoReports(MesProBatchRecordDefinitionDO definition, boolean failOnBlocker) {
        if (reportMapper.countByDefinitionId(definition.getId()) > 0) {
            return false;
        }
        List<MesProBatchRecordVersionDO> versions = versionMapper.selectListByDefinitionId(definition.getId());
        String blocker = buildVersionResetBlockerMessage(versions);
        if (StrUtil.isNotBlank(blocker)) {
            if (failOnBlocker) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_RESET_BLOCKED,
                        blocker);
            }
            return false;
        }
        approvalEventMapper.deleteHardByDefinitionId(definition.getId());
        migrationItemMapper.deleteHardByDefinitionId(definition.getId());
        versionMapper.deleteHardByDefinitionId(definition.getId());
        definitionMapper.deleteHardById(definition.getId());
        return true;
    }

    private boolean hasVisibleMainReports(Long definitionId) {
        return definitionId != null
                && reportMapper.countMainByDefinitionId(definitionId, MesProBatchRecordFormSlotType.MAIN.getType()) > 0;
    }

    private String buildVersionResetBlockerMessage(List<MesProBatchRecordVersionDO> versions) {
        List<VersionResetBlocker> blockers = findVersionResetBlockers(versions);
        if (blockers.isEmpty()) {
            return null;
        }
        StringBuilder message = new StringBuilder("请先处理以下历史引用后再重新导入：");
        for (int i = 0; i < blockers.size(); i++) {
            VersionResetBlocker blocker = blockers.get(i);
            message.append(i + 1)
                    .append(". ")
                    .append(blocker.versionNo())
                    .append(" ")
                    .append(blocker.referenceName())
                    .append(" ")
                    .append(blocker.count())
                    .append(" 条；位置：")
                    .append(blocker.cleanupEntrance())
                    .append("；处理：")
                    .append(blocker.cleanupAction())
                    .append("。");
        }
        return message.toString();
    }

    private List<VersionResetBlocker> findVersionResetBlockers(List<MesProBatchRecordVersionDO> versions) {
        List<VersionResetBlocker> blockers = new ArrayList<>();
        for (MesProBatchRecordVersionDO version : versions) {
            Long versionId = version.getId();
            Long executionCount = batchRecordExecutionMapper.countByBatchRecordVersionId(versionId);
            if (executionCount > 0) {
                blockers.add(new VersionResetBlocker(version.getVersionNo(), "存在批记录执行", executionCount,
                        "eDHR 批记录 > 批次执行",
                        "删除或作废执行记录，并确认执行列表不再引用这个批记录版本"));
            }
            Long taskCount = batchExecutionTaskMapper.countByBatchRecordVersionId(versionId);
            if (taskCount > 0) {
                blockers.add(new VersionResetBlocker(version.getVersionNo(), "存在批记录任务", taskCount,
                        "eDHR 批记录 > 批次执行任务",
                        "删除任务或解除任务中的表单"));
            }
            Long permissionRuleCount = processFormPermissionRuleMapper.countByBatchRecordVersionId(versionId);
            if (permissionRuleCount > 0) {
                blockers.add(new VersionResetBlocker(version.getVersionNo(), "存在工序表单权限规则",
                        permissionRuleCount, "eDHR 批记录 > 工序表单权限设置",
                        "删除该工序的表单权限规则"));
            }
            Long routeBindingCount = routeFlowProcessBatchRecordMapper.countByBatchRecordVersionId(versionId);
            if (routeBindingCount > 0) {
                blockers.add(new VersionResetBlocker(version.getVersionNo(), "存在工艺流程批记录绑定",
                        routeBindingCount, "MES 系统 > 工艺路线/工序配置",
                        "删除该工序上的批记录表单绑定"));
            }
        }
        return blockers;
    }

    private boolean hasAnyVersionResetBlocker(MesProBatchRecordDefinitionDO definition) {
        return definition != null && !findVersionResetBlockers(
                versionMapper.selectListByDefinitionId(definition.getId())).isEmpty();
    }

    private record VersionResetBlocker(String versionNo, String referenceName, Long count,
                                      String cleanupEntrance, String cleanupAction) {
    }

    private boolean existsBatchRecordImportContext(String routeKey, String batchRecordName) {
        if (existsBatchRecordName(routeKey, batchRecordName)) {
            return true;
        }
        MesProBatchRecordDefinitionDO definition = definitionMapper.selectByNameAndRouteKey(batchRecordName, routeKey);
        return hasAnyVersionResetBlocker(definition);
    }

    private MesProBatchRecordVersionDO createPrecheckVersion(MesProBatchRecordDefinitionDO definition,
                                                             MesProBatchRecordVersionDO sourceVersion,
                                                             String sourceFileName,
                                                             String sha256,
                                                             String targetVersionNo) {
        String versionNo = StrUtil.blankToDefault(targetVersionNo, nextVersionNo(definition.getId()));
        MesProBatchRecordVersionDO version = MesProBatchRecordVersionDO.builder()
                .definitionId(definition.getId())
                .versionNo(versionNo)
                .status("PRECHECK_PASSED")
                .sourceVersionId(sourceVersion == null ? null : sourceVersion.getId())
                .sourceFileName(sourceFileName)
                .sourceFileSha256(sha256)
                .routeId(sourceVersion == null ? null : sourceVersion.getRouteId())
                .sourceRouteId(sourceVersion == null ? null : sourceVersion.getRouteId())
                .build();
        try {
            versionMapper.insert(version);
        } catch (DuplicateKeyException duplicateKeyException) {
            MesProBatchRecordVersionDO concurrent =
                    versionMapper.selectReusablePendingByHash(definition.getId(), sha256);
            if (concurrent == null || !Objects.equals(versionNo, concurrent.getVersionNo())) {
                throw duplicateKeyException;
            }
            return concurrent;
        }
        return version;
    }

    private MesProBatchRecordImportResult buildReusableImportResult(MesProBatchRecordDefinitionDO definition,
                                                                    MesProBatchRecordVersionDO reusableVersion,
                                                                    String productName) {
        return buildReusableImportResult(definition, reusableVersion, productName, null);
    }

    private MesProBatchRecordImportResult buildReusableImportResult(MesProBatchRecordDefinitionDO definition,
                                                                    MesProBatchRecordVersionDO reusableVersion,
                                                                    String productName,
                                                                    Long approvalSubmitterUserId) {
        if (approvalSubmitterUserId != null && Objects.equals("PRECHECK_PASSED", reusableVersion.getStatus())) {
            reusableVersion = submitPrecheckVersionForApproval(reusableVersion, approvalSubmitterUserId);
        }
        List<MesProBatchRecordReportDO> reusableReports = reportMapper.selectListByDefinitionIdAndVersionId(
                definition.getId(), reusableVersion.getId());
        String normalizedProductName = StrUtil.trim(productName);
        if (StrUtil.isNotBlank(normalizedProductName)) {
            LocalDateTime now = LocalDateTime.now();
            for (MesProBatchRecordReportDO reusableReport : reusableReports) {
                if (!Objects.equals(normalizedProductName, reusableReport.getProductName())) {
                    reusableReport.setProductName(normalizedProductName);
                    reusableReport.setLastImportTime(now);
                    reportMapper.updateById(reusableReport);
                }
            }
        }
        RouteDisplay routeDisplay = loadRouteDisplay(reusableVersion.getRouteId());
        return MesProBatchRecordImportResult.builder()
                .importedCount(reusableReports.size())
                .createdCount(0)
                .updatedCount(0)
                .batchRecordDefinitionId(definition.getId())
                .batchRecordVersionId(reusableVersion.getId())
                .sourceBatchRecordVersionId(reusableVersion.getSourceVersionId())
                .versionNo(reusableVersion.getVersionNo())
                .versionStatus(reusableVersion.getStatus())
                .approvalInstanceId(reusableVersion.getApprovalInstanceId())
                .routeId(reusableVersion.getRouteId())
                .routeCode(routeDisplay.routeCode())
                .routeName(routeDisplay.routeName())
                .routeVersionId(routeDisplay.routeVersionId())
                .routeVersionNo(routeDisplay.routeVersionNo())
                .reports(reusableReports.stream().map(this::toReportViewFromMetadata).toList())
                .build();
    }

    private boolean hasGeneratedReports(Long definitionId, Long versionId) {
        if (definitionId == null || versionId == null) {
            return false;
        }
        return !reportMapper.selectListByDefinitionIdAndVersionId(definitionId, versionId).isEmpty();
    }

    private String nextVersionNo(Long definitionId) {
        return nextVersionNo(versionMapper.selectListByDefinitionId(definitionId));
    }

    private String nextVersionNo(List<MesProBatchRecordVersionDO> versions) {
        int nextMajor = versions.stream()
                .mapToInt(this::parseBatchRecordVersionMajor)
                .max()
                .orElse(0) + 1;
        return "V" + nextMajor + ".0";
    }

    private MesProBatchRecordVersionDO latestBatchRecordVersion(List<MesProBatchRecordVersionDO> versions) {
        return versions.stream()
                .max(this::compareBatchRecordVersion)
                .orElse(null);
    }

    private int compareBatchRecordVersion(MesProBatchRecordVersionDO left, MesProBatchRecordVersionDO right) {
        int versionCompare = Integer.compare(parseBatchRecordVersionMajor(left), parseBatchRecordVersionMajor(right));
        if (versionCompare != 0) {
            return versionCompare;
        }
        return Long.compare(left.getId() == null ? 0L : left.getId(), right.getId() == null ? 0L : right.getId());
    }

    private int parseBatchRecordVersionMajor(MesProBatchRecordVersionDO version) {
        String versionNo = version == null ? null : version.getVersionNo();
        String normalized = StrUtil.trimToEmpty(versionNo).toUpperCase(Locale.ROOT);
        if (!normalized.matches("V\\d+(\\.0)?")) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_NO_INVALID,
                    versionNo);
        }
        String numericPart = normalized.substring(1);
        int dotIndex = numericPart.indexOf('.');
        if (dotIndex >= 0) {
            numericPart = numericPart.substring(0, dotIndex);
        }
        return Integer.parseInt(numericPart);
    }

    private void writePhaseOneMigrationEvidence(Long definitionId, Long versionId, Long sourceVersionId,
                                                String sourceFileSha256, String targetFileSha256, int reportCount) {
        if (sourceVersionId == null) {
            migrationItemMapper.insert(MesProBatchRecordVersionMigrationItemDO.builder()
                    .definitionId(definitionId)
                    .versionId(versionId)
                    .sourceVersionId(null)
                    .itemType("PHASE_ONE_PRECHECK")
                    .diffGroup("TABLE")
                    .diffType("FIRST_IMPORT")
                    .sourceLogicalKey("FIRST_IMPORT")
                    .targetLogicalKey("VERSION:" + versionId)
                    .matchConfidence(BigDecimal.ONE)
                    .matchEvidenceJson(migrationEvidenceJson("FIRST_IMPORT", reportCount, "首次导入无源版本"))
                    .riskLevel("INFO")
                    .ruleType("VERSION_BASELINE")
                    .businessOwnerType("SYSTEM")
                    .confirmed(false)
                    .message("一期安全升版最小闭环预检通过")
                    .build());
            return;
        }

        boolean sameSourceFile = StrUtil.isNotBlank(sourceFileSha256)
                && Objects.equals(sourceFileSha256, targetFileSha256);
        insertStructuredMigrationItem(definitionId, versionId, sourceVersionId, reportCount,
                "TABLE", "TABLE_STRUCTURE_RECONCILED", "VERSION:" + sourceVersionId + ":TABLES",
                "VERSION:" + versionId + ":TABLES", "INFO", "TABLE_COUNT", "SYSTEM",
                "批记录表单结构已生成版本化快照");
        insertStructuredMigrationItem(definitionId, versionId, sourceVersionId, reportCount,
                "PROCESS", "PROCESS_ROUTE_REBOUND", "VERSION:" + sourceVersionId + ":ROUTE",
                "VERSION:" + versionId + ":ROUTE", "INFO", "ROUTE_BINDING", "PROCESS_OWNER",
                "工艺路线和批记录绑定已按新版本重建");
        insertStructuredMigrationItem(definitionId, versionId, sourceVersionId, reportCount,
                "FIELD", "FIELD_MAPPING_REVIEWED", "VERSION:" + sourceVersionId + ":FIELDS",
                "VERSION:" + versionId + ":FIELDS", "INFO", "FIELD_MAPPING", "QUALITY_OWNER",
                "字段映射和填写单元格已生成可审计证据");
        insertStructuredMigrationItem(definitionId, versionId, sourceVersionId, reportCount,
                "SIGNATURE_CELL", "SIGNATURE_CELL_REVIEW_REQUIRED",
                "VERSION:" + sourceVersionId + ":SIGNATURE_CELLS",
                "VERSION:" + versionId + ":SIGNATURE_CELLS", sameSourceFile ? "INFO" : "CONFIRM_REQUIRED",
                "SIGNATURE_REVIEW", "DCC_OWNER",
                sameSourceFile ? "文件 hash 未变化，签名位继承无需人工确认" : "签名位迁移需 DCC 负责人授权确认");
        insertStructuredMigrationItem(definitionId, versionId, sourceVersionId, reportCount,
                "ATTACHMENT_RULE", "ATTACHMENT_RULE_RECONCILED",
                "VERSION:" + sourceVersionId + ":ATTACHMENT_RULES",
                "VERSION:" + versionId + ":ATTACHMENT_RULES", "INFO",
                "ATTACHMENT_RULE", "QUALITY_OWNER", "附件规则已纳入版本迁移证据");
        insertStructuredMigrationItem(definitionId, versionId, sourceVersionId, reportCount,
                "CELL_RULE", "CELL_RULE_RECONCILED", "VERSION:" + sourceVersionId + ":CELL_RULES",
                "VERSION:" + versionId + ":CELL_RULES", "INFO", "CELL_RULE", "PROCESS_OWNER",
                "单元格约束已纳入版本迁移证据");
    }

    private void insertStructuredMigrationItem(Long definitionId, Long versionId, Long sourceVersionId, int reportCount,
                                               String diffGroup, String diffType, String sourceLogicalKey,
                                               String targetLogicalKey, String riskLevel, String ruleType,
                                               String businessOwnerType, String message) {
        migrationItemMapper.insert(MesProBatchRecordVersionMigrationItemDO.builder()
                .definitionId(definitionId)
                .versionId(versionId)
                .sourceVersionId(sourceVersionId)
                .itemType(diffGroup)
                .diffGroup(diffGroup)
                .diffType(diffType)
                .sourceLogicalKey(sourceLogicalKey)
                .targetLogicalKey(targetLogicalKey)
                .matchConfidence(BigDecimal.ONE)
                .matchEvidenceJson(migrationEvidenceJson(diffGroup, reportCount, message))
                .riskLevel(riskLevel)
                .ruleType(ruleType)
                .businessOwnerType(businessOwnerType)
                .confirmed(false)
                .message(message)
                .build());
    }

    private String migrationEvidenceJson(String diffGroup, int reportCount, String message) {
        JSONObject evidence = new JSONObject(true);
        evidence.put("diffGroup", diffGroup);
        evidence.put("reportCount", reportCount);
        evidence.put("source", "real_word_import");
        evidence.put("message", message);
        return evidence.toJSONString();
    }

    private MesProBatchRecordReportView toReportViewFromMetadata(MesProBatchRecordReportDO metadata) {
        return MesProBatchRecordReportView.builder()
                .batchRecordName(metadata.getBatchRecordName())
                .batchRecordDefinitionId(metadata.getBatchRecordDefinitionId())
                .batchRecordVersionId(metadata.getBatchRecordVersionId())
                .productName(metadata.getProductName())
                .formSlotType(metadata.getFormSlotType())
                .routeKey(metadata.getRouteKey())
                .sourceTableIndex(metadata.getSourceTableIndex())
                .tableTitle(metadata.getTableTitle())
                .reportId(metadata.getReportId())
                .reportCode(metadata.getReportCode())
                .reportName(metadata.getReportName())
                .sourceFileName(metadata.getSourceFileName())
                .lastImportTime(metadata.getLastImportTime())
                .updateTime(metadata.getUpdateTime())
                .build();
    }

    private MesProBatchRecordReportDO findExistingGeneratedReport(boolean matchByBatchRecordName, String sampleKey,
                                                                  String routeKey, String batchRecordName,
                                                                  Integer sourceTableIndex, String sourceFileSha256,
                                                                  String reportCode) {
        MesProBatchRecordReportDO existing = findExistingGeneratedReportByStableIdentity(
                sourceFileSha256, routeKey, sourceTableIndex, reportCode);
        if (existing != null) {
            return existing;
        }
        if (!matchByBatchRecordName) {
            return reportMapper.selectBySampleKeyAndRouteKeyAndSourceTableIndex(sampleKey, routeKey, sourceTableIndex);
        }
        List<MesProBatchRecordReportDO> candidates = reportMapper
                .selectListByBatchRecordNameAndRouteKeyAndSourceTableIndex(batchRecordName, routeKey, sourceTableIndex);
        if (candidates.size() > 1) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_BATCH_NAME_DUPLICATE,
                    batchRecordName, routeKey, sourceTableIndex);
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private MesProBatchRecordReportDO findExistingGeneratedReportByStableIdentity(String sourceFileSha256,
                                                                                  String routeKey,
                                                                                  Integer sourceTableIndex,
                                                                                  String reportCode) {
        MesProBatchRecordReportDO existing = reportMapper
                .selectBySourceFileSha256AndRouteKeyAndSourceTableIndex(sourceFileSha256, routeKey, sourceTableIndex);
        if (existing != null) {
            return existing;
        }
        existing = reportMapper.selectByReportCode(reportCode);
        if (existing != null) {
            return existing;
        }
        MesProBatchRecordReportInfo linkedReport = jimuReportGateway.getReportInfoByCode(reportCode);
        if (linkedReport == null) {
            return null;
        }
        MesProBatchRecordReportDO linkedMetadata = reportMapper.selectByReportId(linkedReport.reportId());
        if (linkedMetadata != null) {
            return linkedMetadata;
        }
        MesProBatchRecordReportDO recreated = new MesProBatchRecordReportDO();
        recreated.setReportId(linkedReport.reportId());
        recreated.setReportCode(linkedReport.reportCode());
        recreated.setReportName(linkedReport.reportName());
        return recreated;
    }

    private MesProBatchRecordReportView toVisibleReportView(MesProBatchRecordReportDO metadata) {
        MesProBatchRecordReportInfo reportInfo = jimuReportGateway.getReportInfo(metadata.getReportId());
        if (reportInfo == null) {
            return null;
        }
        return MesProBatchRecordReportView.builder()
                .batchRecordName(StrUtil.blankToDefault(metadata.getBatchRecordName(), DEFAULT_BATCH_RECORD_NAME))
                .batchRecordDefinitionId(metadata.getBatchRecordDefinitionId())
                .batchRecordVersionId(metadata.getBatchRecordVersionId())
                .productName(metadata.getProductName())
                .formSlotType(normalizeFormSlotType(metadata.getFormSlotType()))
                .routeKey(metadata.getRouteKey())
                .sourceTableIndex(metadata.getSourceTableIndex())
                .tableTitle(metadata.getTableTitle())
                .reportId(reportInfo.reportId())
                .reportCode(reportInfo.reportCode())
                .reportName(reportInfo.reportName())
                .sourceFileName(metadata.getSourceFileName())
                .lastImportTime(metadata.getLastImportTime())
                .updateTime(reportInfo.updateTime())
                .build();
    }

    private boolean hasVisibleMainReport(Long definitionId, Long versionId) {
        if (definitionId == null || versionId == null) {
            return false;
        }
        return reportMapper.selectList().stream()
                .filter(report -> Objects.equals(definitionId, report.getBatchRecordDefinitionId()))
                .filter(report -> Objects.equals(versionId, report.getBatchRecordVersionId()))
                .filter(report -> Objects.equals(MesProBatchRecordFormSlotType.MAIN.getType(),
                        normalizeFormSlotType(report.getFormSlotType())))
                .anyMatch(report -> jimuReportGateway.getReportInfo(report.getReportId()) != null);
    }

    private List<MesProBatchRecordReportView> expandReportsByVersionProducts(List<MesProBatchRecordReportView> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }
        Set<Long> versionIds = reports.stream()
                .map(MesProBatchRecordReportView::batchRecordVersionId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Map<Long, MesProBatchRecordVersionDO> versionMap = versionIds.isEmpty() ? Map.of()
                : versionMapper.selectBatchIds(versionIds)
                .stream()
                .collect(LinkedHashMap::new, (map, version) -> map.put(version.getId(), version), Map::putAll);
        Set<Long> routeIds = versionMap.values().stream()
                .map(MesProBatchRecordVersionDO::getRouteId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByRouteIds(routeIds);
        Set<Long> itemIds = routeProducts.stream()
                .map(MesProRouteProductDO::getItemId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Map<Long, String> itemNameMap = itemIds.isEmpty() ? Map.of()
                : itemMapper.selectListByIds(itemIds)
                .stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item.getName()), Map::putAll);
        Map<Long, List<String>> productNamesByRouteId = new LinkedHashMap<>();
        for (MesProRouteProductDO routeProduct : routeProducts) {
            String productName = itemNameMap.get(routeProduct.getItemId());
            if (StrUtil.isBlank(productName)) {
                continue;
            }
            productNamesByRouteId
                    .computeIfAbsent(routeProduct.getRouteId(), key -> new ArrayList<>())
                    .add(productName);
        }
        List<MesProBatchRecordReportView> expandedReports = new ArrayList<>();
        for (MesProBatchRecordReportView report : reports) {
            MesProBatchRecordVersionDO version = report.batchRecordVersionId() == null
                    ? null : versionMap.get(report.batchRecordVersionId());
            List<String> productNames = version == null ? List.of() : productNamesByRouteId.get(version.getRouteId());
            if (productNames == null || productNames.isEmpty()) {
                productNames = StrUtil.isBlank(report.productName()) ? List.of() : List.of(report.productName());
            }
            if (productNames.isEmpty()) {
                expandedReports.add(copyReportWithVersionProduct(report, version, null));
                continue;
            }
            productNames.stream()
                    .distinct()
                    .forEach(productName -> expandedReports.add(copyReportWithVersionProduct(report, version, productName)));
        }
        return expandedReports;
    }

    private List<MesProBatchRecordReportView> filterLatestBatchRecordVersions(List<MesProBatchRecordReportView> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }
        Set<Long> definitionIds = reports.stream()
                .map(MesProBatchRecordReportView::batchRecordDefinitionId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        if (definitionIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> latestVersionIdByDefinitionId = new LinkedHashMap<>();
        for (Long definitionId : definitionIds) {
            MesProBatchRecordVersionDO latestVersion =
                    latestBatchRecordVersion(versionMapper.selectListByDefinitionId(definitionId));
            if (latestVersion != null && latestVersion.getId() != null) {
                latestVersionIdByDefinitionId.put(definitionId, latestVersion.getId());
            }
        }
        return reports.stream()
                .filter(report -> {
                    Long latestVersionId = latestVersionIdByDefinitionId.get(report.batchRecordDefinitionId());
                    return latestVersionId != null && Objects.equals(report.batchRecordVersionId(), latestVersionId);
                })
                .toList();
    }

    private List<MesProBatchRecordReportView> filterLatestVisibleBatchRecordVersions(
            List<MesProBatchRecordReportView> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }
        Set<Long> versionIds = reports.stream()
                .map(MesProBatchRecordReportView::batchRecordVersionId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        if (versionIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MesProBatchRecordVersionDO> versionById = versionMapper.selectBatchIds(versionIds)
                .stream()
                .collect(LinkedHashMap::new, (map, version) -> map.put(version.getId(), version), Map::putAll);
        Map<String, MesProBatchRecordVersionDO> latestVersionByVisibleGroup = new LinkedHashMap<>();
        for (MesProBatchRecordReportView report : reports) {
            MesProBatchRecordVersionDO version = versionById.get(report.batchRecordVersionId());
            if (version == null) {
                continue;
            }
            String visibleGroupKey = latestVisibleBatchRecordGroupKey(report);
            MesProBatchRecordVersionDO currentLatest = latestVersionByVisibleGroup.get(visibleGroupKey);
            if (currentLatest == null || compareBatchRecordVersion(currentLatest, version) < 0) {
                latestVersionByVisibleGroup.put(visibleGroupKey, version);
            }
        }
        return reports.stream()
                .filter(report -> {
                    MesProBatchRecordVersionDO latestVersion =
                            latestVersionByVisibleGroup.get(latestVisibleBatchRecordGroupKey(report));
                    return latestVersion != null && Objects.equals(report.batchRecordVersionId(), latestVersion.getId());
                })
                .toList();
    }

    private String latestVisibleBatchRecordGroupKey(MesProBatchRecordReportView report) {
        return normalizeLatestVisibleGroupPart(StrUtil.blankToDefault(report.productName(), report.batchRecordName()))
                + "|"
                + normalizeLatestVisibleGroupPart(report.batchRecordName())
                + "|"
                + normalizeLatestVisibleGroupPart(report.formSlotType());
    }

    private String normalizeLatestVisibleGroupPart(String value) {
        return StrUtil.trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private MesProBatchRecordReportView copyReportWithVersionProduct(MesProBatchRecordReportView report,
                                                                     MesProBatchRecordVersionDO version,
                                                                     String productName) {
        return MesProBatchRecordReportView.builder()
                .batchRecordName(report.batchRecordName())
                .batchRecordDefinitionId(report.batchRecordDefinitionId())
                .batchRecordVersionId(report.batchRecordVersionId())
                .productName(productName)
                .versionNo(version == null ? null : version.getVersionNo())
                .versionStatus(version == null ? null : version.getStatus())
                .formSlotType(report.formSlotType())
                .routeKey(report.routeKey())
                .sourceTableIndex(report.sourceTableIndex())
                .tableTitle(report.tableTitle())
                .reportId(report.reportId())
                .reportCode(report.reportCode())
                .reportName(report.reportName())
                .sourceFileName(report.sourceFileName())
                .lastImportTime(report.lastImportTime())
                .updateTime(report.updateTime())
                .build();
    }

    private boolean filterByName(MesProBatchRecordReportView report, String name) {
        if (StrUtil.isBlank(name)) {
            return true;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return report.batchRecordName().toLowerCase(Locale.ROOT).contains(normalized)
                || report.reportName().toLowerCase(Locale.ROOT).contains(normalized)
                || report.reportCode().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private boolean filterByReportId(MesProBatchRecordReportView report, String reportId) {
        if (StrUtil.isBlank(reportId)) {
            return true;
        }
        return Objects.equals(report.reportId(), reportId.trim());
    }

    private boolean filterByBatchRecordName(MesProBatchRecordReportView report, String batchRecordName) {
        if (StrUtil.isBlank(batchRecordName)) {
            return true;
        }
        return Objects.equals(report.batchRecordName(), StrUtil.trim(batchRecordName));
    }

    private boolean filterByFormSlotType(MesProBatchRecordReportView report, String formSlotType) {
        if (StrUtil.isBlank(formSlotType)) {
            return true;
        }
        String normalized = MesProBatchRecordFormSlotType.normalize(formSlotType);
        return normalized != null && Objects.equals(report.formSlotType(), normalized);
    }

    private boolean filterByRouteKey(MesProBatchRecordReportView report, String routeKey) {
        if (StrUtil.isBlank(routeKey)) {
            return true;
        }
        return Objects.equals(report.routeKey(), MesProBatchRecordRecognitionRouteKeys.normalize(routeKey));
    }

    private boolean filterByProductName(MesProBatchRecordReportView report, String productName) {
        if (StrUtil.isBlank(productName)) {
            return true;
        }
        return StrUtil.containsIgnoreCase(report.productName(), StrUtil.trim(productName));
    }

    private boolean filterByVersionNo(MesProBatchRecordReportView report, String versionNo) {
        if (StrUtil.isBlank(versionNo)) {
            return true;
        }
        return Objects.equals(report.versionNo(), StrUtil.trim(versionNo));
    }

    private String normalizeFormSlotType(String formSlotType) {
        String normalized = MesProBatchRecordFormSlotType.normalize(formSlotType);
        return normalized == null ? MesProBatchRecordFormSlotType.MAIN.getType() : normalized;
    }

    private String normalizeExtraFormSlotType(String formSlotType) {
        String normalized = MesProBatchRecordFormSlotType.normalize(formSlotType);
        if (!MesProBatchRecordFormSlotType.isExtraSlot(normalized)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FORM_SLOT_INVALID);
        }
        return normalized;
    }

    private void validateExtraSlotWordFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EMPTY);
        }
        String fileName = normalizeFileName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!(fileName.endsWith(".doc") || fileName.endsWith(".docx"))) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID);
        }
    }

    private List<MesProBatchRecordParsedTable> parseWordByFileName(byte[] bytes, String sourceFileName) {
        String lowerFileName = normalizeFileName(sourceFileName).toLowerCase(Locale.ROOT);
        if (lowerFileName.endsWith(".docx")) {
            return docParser.parseDocx(bytes);
        }
        return docParser.parse(bytes);
    }

    private void attachDocumentFrame(List<MesProBatchRecordParsedTable> parsedTables,
                                     MesProBatchRecordDocumentFrame documentFrame) {
        if (parsedTables == null || parsedTables.isEmpty() || documentFrame == null) {
            return;
        }
        for (MesProBatchRecordParsedTable parsedTable : parsedTables) {
            parsedTable.setDocumentFrame(documentFrame);
        }
    }

    private void validateImportedDoc(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EMPTY);
        }
        String fileName = normalizeFileName(file.getOriginalFilename());
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".doc")) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID);
        }
    }

    private void validateUploadedRouteDoc(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EMPTY);
        }
        String fileName = normalizeFileName(file.getOriginalFilename());
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".doc")) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID);
        }
    }

    private void validateImageFile(MultipartFile file) {
        String fileName = normalizeFileName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!(fileName.endsWith(".png")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".bmp"))) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_FILE_EXTENSION_INVALID);
        }
    }

    private byte[] getBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED,
                    ex.getMessage());
        }
    }

    private String normalizeFileName(String originalFilename) {
        if (StrUtil.isBlank(originalFilename)) {
            return "";
        }
        try {
            return Path.of(originalFilename).getFileName().toString();
        } catch (InvalidPathException ignored) {
            return originalFilename;
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("sha256_unavailable", ex);
        }
    }

    private String buildImageSampleKey(String sha256) {
        return "IMG_" + sha256.substring(0, Math.min(32, sha256.length()));
    }

    private String buildUploadedRouteSampleKey(String sha256) {
        return "UPLOAD_" + sha256.substring(0, Math.min(32, sha256.length()));
    }

    private String buildBatchRecordNameSampleKey(String batchRecordName) {
        String hash = sha256(batchRecordName.getBytes(StandardCharsets.UTF_8));
        return "BATCH_" + hash.substring(0, Math.min(32, hash.length()));
    }

    private String buildBatchRecordVersionSampleKey(String batchRecordName, Long batchRecordVersionId) {
        if (batchRecordVersionId == null) {
            return buildBatchRecordNameSampleKey(batchRecordName);
        }
        String hash = sha256((batchRecordName + "|VERSION|" + batchRecordVersionId).getBytes(StandardCharsets.UTF_8));
        return "BATCH_VERSION_" + hash.substring(0, Math.min(32, hash.length()));
    }

    private String buildExtraFormSlotSampleKey(String batchRecordName, String formSlotType) {
        String hash = sha256((batchRecordName + "|" + formSlotType).getBytes(StandardCharsets.UTF_8));
        return "BATCH_SLOT_" + hash.substring(0, Math.min(32, hash.length()));
    }

    private String buildExtraFormSlotVersionSampleKey(String batchRecordName, String formSlotType,
                                                      Long batchRecordVersionId) {
        if (batchRecordVersionId == null) {
            return buildExtraFormSlotSampleKey(batchRecordName, formSlotType);
        }
        String hash = sha256((batchRecordName + "|" + formSlotType + "|VERSION|" + batchRecordVersionId)
                .getBytes(StandardCharsets.UTF_8));
        return "BATCH_SLOT_VERSION_" + hash.substring(0, Math.min(32, hash.length()));
    }

    private String scopeSampleKeyByTenant(String baseSampleKey) {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? baseSampleKey : baseSampleKey + "_TN" + tenantId;
    }

    private String buildImportedDocReportCode(String sha256, String routeKey, Integer sourceTableIndex) {
        String tenantCodeSegment = buildTenantCodeSegment();
        if (Objects.equals(routeKey, MesProBatchRecordRecognitionRouteKeys.LEGACY)) {
            return String.format("EBR_%sDOC_%s_T%02d", tenantCodeSegment,
                    sha256.substring(0, Math.min(8, sha256.length())), sourceTableIndex);
        }
        return String.format("EBR_%s%s_DOC_%s_T%02d", tenantCodeSegment, routeKey,
                sha256.substring(0, Math.min(8, sha256.length())), sourceTableIndex);
    }

    private String buildImageReportCode(String sha256, String routeKey, Integer sourceTableIndex) {
        String tenantCodeSegment = buildTenantCodeSegment();
        if (Objects.equals(routeKey, MesProBatchRecordRecognitionRouteKeys.LEGACY)) {
            return String.format("EBR_%sIMG_%s_T%02d", tenantCodeSegment,
                    sha256.substring(0, Math.min(8, sha256.length())),
                    sourceTableIndex);
        }
        return String.format("EBR_%s%s_IMG_%s_T%02d", tenantCodeSegment, routeKey,
                sha256.substring(0, Math.min(8, sha256.length())), sourceTableIndex);
    }

    private String buildUploadedDocReportCode(String sha256, String routeKey, Integer sourceTableIndex,
                                              Long batchRecordVersionId) {
        String tenantCodeSegment = buildTenantCodeSegment();
        if (batchRecordVersionId != null) {
            String hashSegment = sha256.substring(0, Math.min(8, sha256.length()));
            String decimalVersionCode = String.valueOf(batchRecordVersionId);
            String reportCode = String.format("EBR_%s%s_DOC_%s_V%s_T%02d", tenantCodeSegment, routeKey,
                    hashSegment, decimalVersionCode, sourceTableIndex);
            if (reportCode.length() <= 50) {
                return reportCode;
            }
            String compactVersionCode = Long.toUnsignedString(batchRecordVersionId, 36).toUpperCase(Locale.ROOT);
            return String.format("EBR_%s%s_DOC_%s_V%s_T%02d", tenantCodeSegment, routeKey,
                    hashSegment, compactVersionCode, sourceTableIndex);
        }
        return String.format("EBR_%s%s_DOC_%s_T%02d", tenantCodeSegment, routeKey,
                sha256.substring(0, Math.min(8, sha256.length())), sourceTableIndex);
    }

    private String buildReportCode(GeneratedReportSource source, String sha256, String routeKey,
                                   Integer sourceTableIndex, Long batchRecordVersionId) {
        if (source == GeneratedReportSource.IMAGE) {
            return buildImageReportCode(sha256, routeKey, sourceTableIndex);
        }
        if (source == GeneratedReportSource.UPLOADED_DOC) {
            return buildUploadedDocReportCode(sha256, routeKey, sourceTableIndex, batchRecordVersionId);
        }
        return buildImportedDocReportCode(sha256, routeKey, sourceTableIndex);
    }

    private String buildTenantCodeSegment() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? "" : "TN" + tenantId + "_";
    }

    private String buildImportedDocReportName(String routeKey, MesProBatchRecordParsedTable parsedTable) {
        String title = StrUtil.blankToDefault(parsedTable.getTableTitle(), "表" + parsedTable.getSourceTableIndex());
        String normalizedTitle = title.replaceAll("\\s+", " ").trim();
        String prefix = Objects.equals(routeKey, MesProBatchRecordRecognitionRouteKeys.LEGACY)
                ? "电子批记录"
                : "电子批记录[" + routeKey + "]";
        String reportName = prefix + "-表" + parsedTable.getSourceTableIndex() + "-" + normalizedTitle;
        return trimReportName(reportName);
    }

    private String buildImageReportName(String routeKey, String sourceFileName, MesProBatchRecordParsedTable parsedTable) {
        String title = StrUtil.blankToDefault(parsedTable.getTableTitle(), "图片表" + parsedTable.getSourceTableIndex());
        String safeFileName = normalizeFileName(sourceFileName).replaceAll("\\s+", " ").trim();
        String prefix = Objects.equals(routeKey, MesProBatchRecordRecognitionRouteKeys.LEGACY)
                ? "图片电子批记录-"
                : "图片电子批记录[" + routeKey + "]-";
        String reportName = prefix + title + "-" + safeFileName;
        return trimReportName(reportName);
    }

    private String buildUploadedDocReportName(MesProBatchRecordParsedTable parsedTable) {
        String title = StrUtil.blankToDefault(parsedTable.getTableTitle(), "表" + parsedTable.getSourceTableIndex());
        return trimReportName(title.replaceAll("\\s+", " ").trim());
    }

    private String buildReportName(GeneratedReportSource source, String routeKey, String sourceFileName,
                                   MesProBatchRecordParsedTable parsedTable, String batchRecordName,
                                   String formSlotType) {
        if (source == GeneratedReportSource.IMAGE) {
            return buildImageReportName(routeKey, sourceFileName, parsedTable);
        }
        if (source == GeneratedReportSource.UPLOADED_DOC) {
            if (MesProBatchRecordFormSlotType.isExtraSlot(formSlotType)) {
                return MesProBatchRecordFormSlotType.displayName(formSlotType);
            }
            return buildUploadedDocReportName(parsedTable);
        }
        return buildImportedDocReportName(routeKey, parsedTable);
    }

    private String normalizeBatchRecordName(String batchRecordName) {
        String normalized = StrUtil.trim(batchRecordName);
        if (StrUtil.isBlank(normalized)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_BATCH_NAME_EMPTY);
        }
        if (normalized.length() > BATCH_RECORD_NAME_MAX_LENGTH) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_BATCH_NAME_TOO_LONG);
        }
        return normalized;
    }

    private String trimReportName(String reportName) {
        if (reportName.length() <= JIMU_REPORT_NAME_MAX_LENGTH) {
            return reportName;
        }
        return reportName.substring(0, JIMU_REPORT_NAME_MAX_LENGTH);
    }

    private String normalizeReportName(String reportName) {
        String normalized = StrUtil.trim(reportName);
        if (StrUtil.isBlank(normalized)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_NAME_EMPTY);
        }
        if (normalized.length() > JIMU_REPORT_NAME_MAX_LENGTH) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_NAME_TOO_LONG);
        }
        return normalized;
    }

    private void logT06SourceSnapshot(String reportCode, MesProBatchRecordParsedTable parsedTable) {
        if (!StrUtil.contains(reportCode, "_T06") || parsedTable == null || parsedTable.getRows() == null) {
            return;
        }
        System.out.println("[EDHR-T06-SOURCE] reportCode=" + reportCode
                + " title=" + parsedTable.getTableTitle()
                + " rows=" + summarizeRows(parsedTable, 5, 18));
    }

    private String summarizeRows(MesProBatchRecordParsedTable parsedTable, int startRowIndex, int endRowIndex) {
        StringBuilder summary = new StringBuilder();
        int lastRowIndex = Math.min(endRowIndex, parsedTable.getRows().size() - 1);
        for (int rowIndex = Math.max(0, startRowIndex); rowIndex <= lastRowIndex; rowIndex++) {
            if (summary.length() > 0) {
                summary.append(" || ");
            }
            summary.append("row").append(rowIndex).append('=');
            List<MesProBatchRecordParsedCell> row = parsedTable.getRows().get(rowIndex);
            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                if (cellIndex > 0) {
                    summary.append(" | ");
                }
                MesProBatchRecordParsedCell cell = row.get(cellIndex);
                summary.append('[')
                        .append(Math.max(1, cell.getRowSpan()))
                        .append('x')
                        .append(Math.max(1, cell.getColSpan()))
                        .append(' ')
                        .append(textOf(cell))
                        .append(']');
            }
        }
        return summary.toString();
    }

    private String textOf(MesProBatchRecordParsedCell cell) {
        String text = cell == null ? "" : cell.getText();
        return text == null ? "" : text.replace("\r", "").replace("\n", "/").replaceAll("\\s+", "");
    }

    private JSONObject parseReportJson(String reportId) {
        String reportJson = jimuReportGateway.getReportJson(reportId);
        if (StrUtil.isBlank(reportJson)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING,
                    reportId);
        }
        try {
            JSONObject root = JSON.parseObject(reportJson);
            JSONObject rows = root == null ? null : root.getJSONObject("rows");
            if (rows == null || !hasRenderableRows(rows)) {
                throw new IllegalArgumentException("missing renderable rows");
            }
            return root;
        } catch (Exception ex) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_JSON_INVALID,
                    ex.getMessage());
        }
    }

    private BatchRecordReportSignatureCellMarkersRespVO toSignatureCellMarkersRespVO(String reportId,
                                                                                    JSONObject root) {
        JSONObject layout = new JSONObject(true);
        layout.put("rows", root.getJSONObject("rows"));
        layout.put("cols", root.getJSONObject("cols"));
        layout.put("merges", root.getJSONArray("merges"));
        return new BatchRecordReportSignatureCellMarkersRespVO()
                .setReportId(reportId)
                .setSheetLayoutJson(layout.toJSONString())
                .setMarkers(extractSignatureMarkers(root));
    }

    private BatchRecordReportCellRulesRespVO toCellRulesRespVO(String reportId, JSONObject root) {
        JSONObject layout = new JSONObject(true);
        layout.put("rows", root.getJSONObject("rows"));
        layout.put("cols", root.getJSONObject("cols"));
        layout.put("merges", root.getJSONArray("merges"));
        return new BatchRecordReportCellRulesRespVO()
                .setReportId(reportId)
                .setSheetLayoutJson(layout.toJSONString())
                .setRules(MesProBatchRecordCellRuleSupport.extractReviewedRules(root))
                .setSuggestions(MesProBatchRecordCellRuleSupport.buildSuggestions(root))
                .setUnreviewedFillableCellCount(MesProBatchRecordCellRuleSupport.countUnreviewedFillableCells(root));
    }

    private void ensureNoLegacyFormProfileLayoutOnRead(MesProBatchRecordReportDO metadata, JSONObject root) {
        if (formProfileRegistry.findLegacyLayoutProfile(metadata, root).isPresent()) {
            throw exception(
                    MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_LEGACY_LAYOUT_MIGRATION_REQUIRED,
                    metadata.getReportId());
        }
    }

    private boolean hasRenderableRows(JSONObject rows) {
        return rows.keySet().stream()
                .filter(StrUtil::isNumeric)
                .map(rows::getJSONObject)
                .anyMatch(row -> row != null && row.getJSONObject("cells") != null
                        && !row.getJSONObject("cells").isEmpty());
    }

    private List<BatchRecordReportSignatureCellMarkerVO> extractSignatureMarkers(JSONObject root) {
        List<BatchRecordReportSignatureCellMarkerVO> markers = new ArrayList<>();
        JSONObject rows = root.getJSONObject("rows");
        for (String rowKey : rows.keySet()) {
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            for (String columnKey : cells.keySet()) {
                JSONObject cell = cells.getJSONObject(columnKey);
                JSONObject signature = cell == null ? null : cell.getJSONObject("edhrSignature");
                if (signature == null || !Boolean.TRUE.equals(signature.getBoolean("enabled"))) {
                    continue;
                }
                markers.add(new BatchRecordReportSignatureCellMarkerVO()
                        .setRowIndex(Integer.valueOf(rowKey))
                        .setColumnIndex(Integer.valueOf(columnKey))
                        .setEnabled(true)
                        .setSignatureCellKey(StrUtil.blankToDefault(signature.getString("signatureCellKey"),
                                buildSignatureCellKey(Integer.valueOf(rowKey), Integer.valueOf(columnKey))))
                        .setActionType(signature.getString("actionType"))
                        .setLabel(signature.getString("label"))
                        .setDisplayFormat(StrUtil.blankToDefault(signature.getString("displayFormat"),
                                DEFAULT_SIGNATURE_DISPLAY_FORMAT))
                        .setReviewSourceType(signature.getString("reviewSourceType"))
                        .setReviewSourceId(signature.getLong("reviewSourceId"))
                        .setReviewSourceIds(readReviewSourceIds(signature))
                        .setReviewSourceName(signature.getString("reviewSourceName")));
            }
        }
        markers.sort(Comparator.comparing(BatchRecordReportSignatureCellMarkerVO::getRowIndex)
                .thenComparing(BatchRecordReportSignatureCellMarkerVO::getColumnIndex));
        return markers;
    }

    private void clearSignatureMarkers(JSONObject root) {
        JSONObject rows = root.getJSONObject("rows");
        for (String rowKey : rows.keySet()) {
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            for (String columnKey : cells.keySet()) {
                JSONObject cell = cells.getJSONObject(columnKey);
                if (cell != null) {
                    cell.remove("edhrSignature");
                }
            }
        }
    }

    private void clearCellRules(JSONObject root) {
        JSONObject rows = root.getJSONObject("rows");
        for (String rowKey : rows.keySet()) {
            JSONObject row = rows.getJSONObject(rowKey);
            JSONObject cells = row == null ? null : row.getJSONObject("cells");
            if (cells == null) {
                continue;
            }
            for (String columnKey : cells.keySet()) {
                JSONObject cell = cells.getJSONObject(columnKey);
                if (cell != null) {
                    cell.remove(MesProBatchRecordCellRuleSupport.CELL_RULE_KEY);
                    MesProBatchRecordCellRuleSupport.removeManualFillForm(cell);
                }
            }
        }
    }

    private void validateSignatureMarker(BatchRecordReportSignatureCellMarkerVO marker) {
        if (!SIGNATURE_ACTION_TYPES.contains(marker.getActionType())) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_ACTION_INVALID,
                    marker.getActionType());
        }
        if (marker.getRowIndex() == null || marker.getColumnIndex() == null
                || marker.getRowIndex() < 0 || marker.getColumnIndex() < 0) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_CELL_MISSING,
                    marker.getRowIndex(), marker.getColumnIndex());
        }
        if (Objects.equals("APPROVE", marker.getActionType())) {
            if (StrUtil.isBlank(marker.getReviewSourceType())) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_REQUIRED,
                        buildSignatureCellKey(marker));
            }
            if (!REVIEW_SOURCE_TYPES.contains(marker.getReviewSourceType())) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_INVALID,
                        marker.getReviewSourceType());
            }
            if (isMultipleReviewSourceType(marker.getReviewSourceType())) {
                normalizeReviewSourceIds(marker);
            } else if (marker.getReviewSourceId() == null) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_REQUIRED,
                        buildSignatureCellKey(marker));
            }
        }
    }

    private boolean isMultipleReviewSourceType(String reviewSourceType) {
        return MULTI_REVIEW_SOURCE_TYPES.contains(reviewSourceType);
    }

    private List<Long> normalizeReviewSourceIds(BatchRecordReportSignatureCellMarkerVO marker) {
        if (marker.getReviewSourceIds() == null || marker.getReviewSourceIds().isEmpty()) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_REQUIRED,
                    buildSignatureCellKey(marker));
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : marker.getReviewSourceIds()) {
            if (id == null || !uniqueIds.add(id)) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_INVALID,
                        marker.getReviewSourceType());
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private List<Long> readReviewSourceIds(JSONObject signature) {
        JSONArray ids = signature.getJSONArray("reviewSourceIds");
        if (ids == null) {
            return null;
        }
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.getLong(i);
            if (id == null) {
                throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_INVALID,
                        ids.toJSONString());
            }
            result.add(id);
        }
        return result;
    }

    private String defaultReviewSourceName(BatchRecordReportSignatureCellMarkerVO marker) {
        if (isMultipleReviewSourceType(marker.getReviewSourceType())) {
            return marker.getReviewSourceType() + ":" + normalizeReviewSourceIds(marker);
        }
        return marker.getReviewSourceType() + ":" + marker.getReviewSourceId();
    }

    private String buildSignatureCellKey(BatchRecordReportSignatureCellMarkerVO marker) {
        String requestKey = StrUtil.trim(marker.getSignatureCellKey());
        if (StrUtil.isNotBlank(requestKey)) {
            return requestKey;
        }
        return buildSignatureCellKey(marker.getRowIndex(), marker.getColumnIndex());
    }

    private String buildSignatureCellKey(Integer rowIndex, Integer columnIndex) {
        return "R" + rowIndex + "C" + columnIndex;
    }

    private JSONObject requireCell(JSONObject root, Integer rowIndex, Integer columnIndex) {
        JSONObject row = root.getJSONObject("rows").getJSONObject(String.valueOf(rowIndex));
        JSONObject cells = row == null ? null : row.getJSONObject("cells");
        JSONObject cell = cells == null ? null : cells.getJSONObject(String.valueOf(columnIndex));
        if (cell == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_CELL_MISSING,
                    rowIndex, columnIndex);
        }
        return cell;
    }

    private MesProBatchRecordReportDO requireMetadata(String reportId) {
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(reportId);
        if (report == null) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_NOT_EXISTS);
        }
        return report;
    }

    private List<String> normalizeReportIds(List<String> reportIds) {
        return reportIds.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private void validateReportNotBound(String reportId) {
        if (isReportBound(reportId)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_BOUND_BY_ROUTE_PROCESS,
                    reportId);
        }
    }

    private boolean isReportBound(String reportId) {
        return routeProcessMapper.countByBatchRecordReportId(reportId) > 0
                || routeFlowProcessBatchRecordMapper.countByBatchRecordReportId(reportId) > 0
                || routeFlowProcessConfigMapper.countByBatchRecordReportId(reportId) > 0;
    }

    private void validateDeleteAllConfirm(String confirm) {
        if (!StrUtil.equals(confirm, DELETE_ALL_CONFIRM_CODE)) {
            throw exception(MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_DELETE_CONFIRM_INVALID);
        }
    }

    private Map<String, MesProBatchRecordRouteRecognizer> routeRecognizerMap() {
        Map<String, MesProBatchRecordRouteRecognizer> map = new LinkedHashMap<>();
        for (MesProBatchRecordRouteRecognizer recognizer : routeRecognizers) {
            map.put(MesProBatchRecordRecognitionRouteKeys.normalize(recognizer.routeKey()), recognizer);
        }
        return map;
    }

    private record RouteDisplay(Long routeId, String routeCode, String routeName,
                                Long routeVersionId, String routeVersionNo) {
        private static RouteDisplay empty() {
            return new RouteDisplay(null, null, null, null, null);
        }
    }
}
