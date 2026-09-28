package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogItem;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogRequest;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogResponse;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CategoryStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.ChainContext;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Condition;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceItem;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceRequest;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceResponse;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.QueryRequest;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.QueryResponse;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.TargetScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MesProEdhrReverseTraceServiceImpl implements MesProEdhrReverseTraceService {

    private static final String RELEASED_HISTORY = "RELEASED_HISTORY";
    private static final String AND = "AND";
    private static final String BLOCKED = "BLOCKED";
    private static final String COMPLETE = "COMPLETE";
    private static final String MATCHED = "MATCHED";
    private static final String NO_MATCH = "NO_MATCH";
    private static final String SOURCE_MISSING = "SOURCE_MISSING";
    private static final String CATALOG_STALE = "CATALOG_STALE";
    private static final String SOURCE_CONFLICT = "SOURCE_CONFLICT";
    private static final String RESULT_STALE = "RESULT_STALE";
    private static final String BATCH_SCOPE_INVALID = "BATCH_SCOPE_INVALID";
    private static final int INTERNAL_PAGE_SIZE = 100;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final List<Category> CATEGORIES = List.of(Category.values());
    private static final List<String> PARAMETER_SEMANTIC_IDENTITY_KEYS = List.of(
            "routeVersionId", "routeProcessId", "processId", "unit");

    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrBatchExecutionOriginMapper originMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    private final MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private final MesProEdhrBatchExecutionVisibilityService visibilityService;
    private final List<MesProEdhrReverseTraceSourceAdapter> sourceAdapters;

    @Override
    @Transactional(readOnly = true)
    public CatalogResponse getCatalog(CatalogRequest request) {
        validateCatalogRequest(request);
        Long anchorId = parsePositiveId(request.getAnchorBatchExecutionId(), "anchorBatchExecutionId");
        MesProEdhrBatchExecutionDO anchor;
        try {
            anchor = loadBatch(anchorId, Boolean.TRUE);
        } catch (IllegalArgumentException exception) {
            return blockedCatalog(request.getAnchorBatchExecutionId(), request.getCategory(),
                    "当前批次尚未进入上市放行历史，不能进行历史反查");
        }
        if (!isReleasedHistoryBatch(anchor)) {
            return blockedCatalog(request.getAnchorBatchExecutionId(), request.getCategory(),
                    "当前批次尚未进入上市放行历史，不能进行历史反查");
        }
        List<MesProEdhrBatchExecutionDO> candidates = loadReleasedCandidates(request.getTargetScope());
        try {
            validateCommonChains(anchor, candidates);
        } catch (IllegalStateException exception) {
            if (!isSourceFailure(exception)) throw exception;
            CatalogResponse blocked = new CatalogResponse().setAnchorBatchExecutionId(id(anchor.getId()));
            for (Category category : categories(request.getCategory())) {
                blocked.getCategories().add(blockedCategory(category, sourceFailureCode(exception), sourceFailureReason(exception)));
            }
            return blocked;
        }
        CatalogSnapshot snapshot = calculateCatalogSnapshot(anchor, candidates);
        CatalogResponse response = new CatalogResponse()
                .setAnchorBatchExecutionId(id(anchor.getId()))
                .setCatalogVersion(snapshot.version())
                .setChainContext(toChainContext(anchor, anchor.getId()))
                .setCategories(new ArrayList<>())
                .setItems(new ArrayList<>())
                .setTotal(0L);
        if (!isCompleteChainContext(response.getChainContext())) {
            addBlockedCategories(response, request.getCategory());
            return response.setTotal(null);
        }
        if (!hasCompleteAdapterCoverage()) {
            addBlockedCategories(response, request.getCategory());
            return response.setTotal(null);
        }
        for (Category category : categories(request.getCategory())) {
            MesProEdhrReverseTraceSourceAdapter adapter = adapterForCategory(category);
            MesProEdhrReverseTraceSourceAdapter.CatalogResult result = readCatalogSafely(adapter, anchor);
            if (result == null) {
                response.getCategories().add(blockedCategory(category,
                        SOURCE_MISSING, "正式来源目录缺失"));
                continue;
            }
            if ("NO_RECORDED_FACT".equals(result.status()) || "NOT_APPLICABLE".equals(result.status())) {
                response.getCategories().add(new CategoryStatus().setCategory(category).setStatus(result.status())
                        .setReasonCode(result.reasonCode()).setReason(result.reason()));
                continue;
            }
            if (!"AVAILABLE".equals(result.status())) {
                response.getCategories().add(blockedCategory(category, result.reasonCode(), result.reason()));
                continue;
            }
            response.getCategories().add(new CategoryStatus().setCategory(category).setStatus("AVAILABLE"));
            List<CatalogItem> items = result.items() == null ? List.of() : result.items();
            response.getItems().addAll(items);
        }
        if (response.getCategories().stream().allMatch(category -> BLOCKED.equals(category.getStatus()))) {
            return response.setTotal(null).setItems(new ArrayList<>());
        }
        int from = Math.min((request.getPageNo() - 1) * request.getPageSize(), response.getItems().size());
        int to = Math.min(from + request.getPageSize(), response.getItems().size());
        List<CatalogItem> page = new ArrayList<>(response.getItems().subList(from, to));
        response.setTotal((long) response.getItems().size()).setItems(page);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResponse query(QueryRequest request) {
        validateQueryRequest(request);
        Long anchorId = parsePositiveId(request.getAnchorBatchExecutionId(), "anchorBatchExecutionId");
        MesProEdhrBatchExecutionDO anchor = loadReleasedBatch(anchorId);
        List<MesProEdhrBatchExecutionDO> candidates = loadReleasedCandidates(request.getTargetScope());
        try {
            validateCommonChains(anchor, candidates);
        } catch (IllegalStateException exception) {
            if (!isSourceFailure(exception)) throw exception;
            return blockedQuery(new QueryResponse(), sourceFailureCode(exception), sourceFailureReason(exception));
        }
        CatalogSnapshot snapshot = calculateCatalogSnapshot(anchor, candidates);
        String currentCatalogVersion = snapshot.version();
        String queryHash = canonicalQueryHash(request, currentCatalogVersion, snapshot.sourceDigest());
        QueryResponse base = new QueryResponse()
                .setCatalogVersion(currentCatalogVersion)
                .setQueryHash(queryHash)
                .setEvaluatedAt(LocalDateTime.now())
                .setNormalizedQuery(normalizeQuery(request))
                .setCoverage(new MesProEdhrReverseTraceModels.Coverage()
                        .setCandidateBatchCount(candidates.size())
                        .setEvaluatedBatchCount(0)
                        .setNotApplicableBatchCount(0))
                .setList(new ArrayList<>());
        if (!Objects.equals(currentCatalogVersion, request.getCatalogVersion())) {
            return blockedQuery(base, CATALOG_STALE, "目录版本已过期或锚点资料已变化");
        }
        if (anchor == null) {
            return blockedQuery(base, BATCH_SCOPE_INVALID, "锚点批次不在已上市放行历史范围");
        }
        if (!isCompleteChainContext(toChainContext(anchor, anchorId))) {
            return blockedQuery(base, SOURCE_MISSING, "批次主链正式关系投影缺失");
        }
        if (!hasCompleteAdapterCoverage()) {
            return blockedQuery(base, SOURCE_MISSING, "六类正式来源适配器未完整接入");
        }
        AdapterResolution resolution = resolveAdapters(anchor, request.getConditions());
        if (!resolution.complete()) {
            return blockedQuery(base, resolution.reasonCode(), resolution.reason());
        }
        Evaluation evaluation = evaluateCandidates(candidates, request.getConditions(), resolution.byCategory());
        base.getCoverage().setEvaluatedBatchCount(candidates.size());
        if (evaluation.blockedReason() != null) {
            return blockedQuery(base, evaluation.blockedReason(), evaluation.reason());
        }
        List<MesProEdhrBatchExecutionDO> matched = evaluation.matched();
        int from = Math.min((request.getPageNo() - 1) * request.getPageSize(), matched.size());
        int to = Math.min(from + request.getPageSize(), matched.size());
        List<MesProEdhrReverseTraceModels.QueryItem> items = matched.subList(from, to).stream()
                .map(batch -> toQueryItem(batch, anchorId, request.getConditions()))
                .toList();
        if (matched.isEmpty()) {
            return base.setQueryStatus(NO_MATCH).setCoverageStatus(COMPLETE).setTotal(0L);
        }
        return base.setQueryStatus(MATCHED).setCoverageStatus(COMPLETE).setList(new ArrayList<>(items))
                .setTotal((long) matched.size());
    }

    @Override
    @Transactional(readOnly = true)
    public EvidenceResponse evidence(EvidenceRequest request) {
        validateEvidenceRequest(request);
        QueryRequest query = toQueryRequest(request);
        Long anchorId = parsePositiveId(request.getAnchorBatchExecutionId(), "anchorBatchExecutionId");
        Long targetId = parsePositiveId(request.getTargetBatchExecutionId(), "targetBatchExecutionId");
        MesProEdhrBatchExecutionDO anchor = loadReleasedBatch(anchorId);
        List<MesProEdhrBatchExecutionDO> candidates = loadReleasedCandidates(query.getTargetScope());
        try {
            validateCommonChains(anchor, candidates);
        } catch (IllegalStateException exception) {
            if (!isSourceFailure(exception)) throw exception;
            return blockedEvidence(new EvidenceResponse().setTargetBatchExecutionId(request.getTargetBatchExecutionId()),
                    sourceFailureCode(exception), sourceFailureReason(exception));
        }
        CatalogSnapshot snapshot = calculateCatalogSnapshot(anchor, candidates);
        String currentCatalogVersion = snapshot.version();
        String canonicalHash = canonicalQueryHash(query, currentCatalogVersion, snapshot.sourceDigest());
        EvidenceResponse base = new EvidenceResponse()
                .setCatalogVersion(currentCatalogVersion)
                .setQueryHash(canonicalHash)
                .setTargetBatchExecutionId(request.getTargetBatchExecutionId())
                .setItems(new ArrayList<>());
        if (!Objects.equals(currentCatalogVersion, request.getCatalogVersion())) {
            return blockedEvidence(base, CATALOG_STALE, "目录版本已过期或来源资料已变化");
        }
        if (anchor == null) {
            return blockedEvidence(base, BATCH_SCOPE_INVALID, "锚点批次不在已上市放行历史范围");
        }
        if (!Objects.equals(canonicalHash, request.getQueryHash())) {
            return blockedEvidence(base, RESULT_STALE, "queryHash与规范化查询不一致");
        }
        if (!isCompleteChainContext(toChainContext(anchor, anchorId))) {
            return blockedEvidence(base, SOURCE_MISSING, "批次主链正式关系投影缺失");
        }
        if (!hasCompleteAdapterCoverage()) {
            return blockedEvidence(base, SOURCE_MISSING, "六类正式来源适配器未完整接入");
        }
        AdapterResolution resolution = resolveAdapters(anchor, query.getConditions());
        if (!resolution.complete()) {
            return blockedEvidence(base, resolution.reasonCode(), resolution.reason());
        }
        Evaluation evaluation = evaluateCandidates(candidates, query.getConditions(), resolution.byCategory());
        if (evaluation.blockedReason() != null) {
            return blockedEvidence(base, evaluation.blockedReason(), evaluation.reason());
        }
        MesProEdhrBatchExecutionDO target = candidates.stream()
                .filter(batch -> Objects.equals(batch.getId(), targetId))
                .findFirst().orElse(null);
        if (target == null || evaluation.matched().stream().noneMatch(batch -> Objects.equals(batch.getId(), targetId))) {
            return blockedEvidence(base, BATCH_SCOPE_INVALID, "目标批次不在当前RELEASED查询命中范围");
        }
        List<EvidenceItem> items = new ArrayList<>();
        for (Map.Entry<Category, List<Condition>> entry : resolution.conditionsByCategory().entrySet()) {
            MesProEdhrReverseTraceSourceAdapter.CatalogResult targetCatalog =
                    readCatalogSafely(resolution.byCategory().get(entry.getKey()), target);
            if (targetCatalog == null || StrUtil.isBlank(targetCatalog.sourceVersion())
                    || StrUtil.isBlank(targetCatalog.sourceIdentity())) {
                return blockedEvidence(base, SOURCE_CONFLICT, "目标批次正式来源目录缺失");
            }
            MesProEdhrReverseTraceSourceAdapter.EvidenceResult evidence;
            try {
                evidence = resolution.byCategory().get(entry.getKey()).readEvidence(target, entry.getValue());
            } catch (IllegalStateException exception) {
                if (!isSourceFailure(exception)) throw exception;
                return blockedEvidence(base, sourceFailureCode(exception), sourceFailureReason(exception));
            }
            if (evidence == null || !Objects.equals(evidence.targetBatchExecutionId(), id(target.getId()))
                    || StrUtil.isBlank(evidence.sourceVersion()) || StrUtil.isBlank(evidence.sourceIdentity())
                    || !Objects.equals(evidence.sourceVersion(), targetCatalog.sourceVersion())
                    || !Objects.equals(evidence.sourceIdentity(), targetCatalog.sourceIdentity())) {
                return blockedEvidence(base, SOURCE_CONFLICT, "证据来源版本或身份不一致");
            }
            items.addAll(evidence.items() == null ? List.of() : evidence.items());
        }
        Map<String, EvidenceItem> uniqueEvidence = new LinkedHashMap<>();
        for (EvidenceItem item : items) {
            if (item == null || StrUtil.isBlank(item.getConditionId()) || StrUtil.isBlank(item.getSourceRef())
                    || StrUtil.isBlank(item.getRecordStatus()) || StrUtil.isBlank(item.getTargetBatchExecutionId())
                    || !Objects.equals(item.getTargetBatchExecutionId(), id(target.getId()))
                    || StrUtil.isBlank(item.getSourceIdentity()) || StrUtil.isBlank(item.getSourceStage())
                    || StrUtil.isBlank(item.getSourceAction())) {
                return blockedEvidence(base, SOURCE_CONFLICT, "证据缺少条件、来源引用或记录状态");
            }
            String key = String.join("|", item.getConditionId(), item.getSourceRef(), id(target.getId()),
                    item.getSourceStage(), item.getSourceAction(), item.getRecordStatus(),
                    time(item.getRecordedAt()), trim(item.getActualValue()));
            uniqueEvidence.putIfAbsent(key, item);
        }
        for (Condition condition : query.getConditions()) {
            if (uniqueEvidence.values().stream().noneMatch(item -> Objects.equals(item.getConditionId(), condition.getConditionId()))) {
                return blockedEvidence(base, RESULT_STALE, "证据未覆盖条件=" + condition.getConditionId());
            }
        }
        if (uniqueEvidence.values().stream().anyMatch(item -> query.getConditions().stream()
                .noneMatch(condition -> Objects.equals(condition.getConditionId(), item.getConditionId())))) {
            return blockedEvidence(base, SOURCE_CONFLICT, "证据包含额外条件");
        }
        items = new ArrayList<>(uniqueEvidence.values());
        int from = Math.min((request.getPageNo() - 1) * request.getPageSize(), items.size());
        int to = Math.min(from + request.getPageSize(), items.size());
        return base.setEvidenceStatus("MATCHED").setChainContext(toChainContext(target, anchorId))
                .setItems(new ArrayList<>(items.subList(from, to))).setTotal((long) items.size());
    }

    static String canonicalQueryHash(QueryRequest request) {
        return canonicalQueryHash(request, request.getCatalogVersion(), request.getSourceVersionDigest());
    }

    static String canonicalQueryHash(QueryRequest request, String catalogVersion, String sourceVersionDigest) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, "catalogVersion", catalogVersion);
        append(canonical, "sourceVersionDigest", sourceVersionDigest);
        append(canonical, "anchor", trim(request.getAnchorBatchExecutionId()));
        TargetScope scope = request.getTargetScope();
        append(canonical, "scope.kind", upper(scope == null ? null : scope.getKind()));
        append(canonical, "scope.from", time(scope == null ? null : scope.getReleaseApprovedFrom()));
        append(canonical, "scope.to", time(scope == null ? null : scope.getReleaseApprovedTo()));
        append(canonical, "logic", upper(request.getLogic()));
        List<Condition> conditions = request.getConditions() == null ? List.of() : request.getConditions();
        for (int index = 0; index < conditions.size(); index++) {
            Condition condition = conditions.get(index);
            append(canonical, "condition." + index + ".id", trim(condition.getConditionId()));
            append(canonical, "condition." + index + ".evidenceKey", trim(condition.getEvidenceKey()));
            append(canonical, "condition." + index + ".sourceView", upper(condition.getSourceView()));
            append(canonical, "condition." + index + ".operator", upper(condition.getOperator()));
            append(canonical, "condition." + index + ".value", canonicalValue(condition.getValue()));
            Map<String, String> qualifiers = condition.getQualifiers() == null ? Map.of() :
                    new TreeMap<>(condition.getQualifiers());
            for (Map.Entry<String, String> qualifier : qualifiers.entrySet()) {
                append(canonical, "condition." + index + ".qualifier." + trim(qualifier.getKey()), trim(qualifier.getValue()));
            }
        }
        return hash(canonical.toString());
    }

    private Evaluation evaluateCandidates(List<MesProEdhrBatchExecutionDO> candidates, List<Condition> conditions,
                                          Map<Category, MesProEdhrReverseTraceSourceAdapter> adapters) {
        List<MesProEdhrBatchExecutionDO> matched = new ArrayList<>();
        for (MesProEdhrBatchExecutionDO candidate : candidates) {
            if (!isCompleteChainContext(toChainContext(candidate, candidate.getId()))) {
                return new Evaluation(List.of(), SOURCE_MISSING, "候选批次主链正式关系投影缺失");
            }
            Map<Category, List<Condition>> grouped = groupByCategory(conditions, adapters);
            boolean candidateMatched = true;
            for (Map.Entry<Category, List<Condition>> entry : grouped.entrySet()) {
                MesProEdhrReverseTraceSourceAdapter adapter = adapters.get(entry.getKey());
                MesProEdhrReverseTraceSourceAdapter.CatalogResult catalog = readCatalogSafely(adapter, candidate);
                if (catalog == null || StrUtil.isBlank(catalog.sourceVersion())
                        || StrUtil.isBlank(catalog.sourceIdentity())) {
                    return new Evaluation(List.of(), SOURCE_CONFLICT, "候选来源目录版本或身份缺失");
                }
                for (Condition condition : entry.getValue()) {
                    CatalogItem item = findCatalogItem(catalog.items(), condition);
                    if (item == null) {
                        candidateMatched = false;
                        continue;
                    }
                    MesProEdhrReverseTraceSourceAdapter.ValidationResult validation = adapter.validateCondition(item, condition);
                    if (validation == null || !validation.valid()) {
                        return new Evaluation(List.of(), "CONDITION_INVALID",
                                validation == null ? "候选条件不符合目录契约" : validation.reason());
                    }
                }
                MesProEdhrReverseTraceSourceAdapter.EvaluationResult result =
                        evaluateSafely(adapter, candidate, entry.getValue());
                if (result == null) {
                    return new Evaluation(List.of(), SOURCE_MISSING, "正式来源评估缺失");
                }
                if (!"COMPLETE".equals(result.status())) {
                    return new Evaluation(List.of(), StrUtil.blankToDefault(result.reasonCode(), SOURCE_MISSING),
                            StrUtil.blankToDefault(result.reason(), "正式来源评估未完成"));
                }
                if (!Objects.equals(result.sourceVersion(), catalog.sourceVersion())
                        || StrUtil.isBlank(result.sourceIdentity())
                        || !Objects.equals(result.sourceIdentity(), catalog.sourceIdentity())) {
                    return new Evaluation(List.of(), SOURCE_CONFLICT, "正式来源评估版本或身份不一致");
                }
                Map<String, MesProEdhrReverseTraceSourceAdapter.ConditionMatch> matches =
                        (result.matches() == null ? List.<MesProEdhrReverseTraceSourceAdapter.ConditionMatch>of() : result.matches())
                                .stream().collect(Collectors.toMap(MesProEdhrReverseTraceSourceAdapter.ConditionMatch::conditionId,
                                        Function.identity(), (first, ignored) -> first));
                for (Condition condition : entry.getValue()) {
                    if (!matches.getOrDefault(condition.getConditionId(),
                            new MesProEdhrReverseTraceSourceAdapter.ConditionMatch(condition.getConditionId(), false, null, null)).matched()) {
                        candidateMatched = false;
                    }
                }
            }
            if (candidateMatched) {
                matched.add(candidate);
            }
        }
        return new Evaluation(matched, null, null);
    }

    private Map<Category, List<Condition>> groupByCategory(List<Condition> conditions,
                                                            Map<Category, MesProEdhrReverseTraceSourceAdapter> adapters) {
        Map<Category, List<Condition>> grouped = new EnumMap<>(Category.class);
        for (Condition condition : conditions) {
            MesProEdhrReverseTraceSourceAdapter adapter = adapters.values().stream()
                    .filter(candidate -> candidate.supports(condition.getEvidenceKey()))
                    .findFirst().orElse(null);
            if (adapter == null) {
                throw new IllegalArgumentException("SOURCE_MISSING: evidenceKey=" + condition.getEvidenceKey());
            }
            grouped.computeIfAbsent(adapter.category(), ignored -> new ArrayList<>()).add(condition);
        }
        return grouped;
    }

    private AdapterResolution resolveAdapters(MesProEdhrBatchExecutionDO anchor, List<Condition> conditions) {
        Map<Category, MesProEdhrReverseTraceSourceAdapter> byCategory = adapterMap();
        Map<Category, List<Condition>> byCondition = new EnumMap<>(Category.class);
        for (Condition condition : conditions) {
            MesProEdhrReverseTraceSourceAdapter adapter = byCategory.values().stream()
                    .filter(candidate -> candidate.supports(condition.getEvidenceKey())).findFirst().orElse(null);
            if (adapter == null) {
                return new AdapterResolution(false, SOURCE_MISSING, byCategory, byCondition,
                        "没有正式来源适配器支持 evidenceKey=" + condition.getEvidenceKey());
            }
            MesProEdhrReverseTraceSourceAdapter.CatalogResult catalog = readCatalogSafely(adapter, anchor);
            CatalogItem item = findCatalogItem(catalog == null ? null : catalog.items(), condition);
            if (catalog == null || "BLOCKED".equals(catalog.status())
                    || SOURCE_MISSING.equals(catalog.status()) || SOURCE_CONFLICT.equals(catalog.status())) {
                return new AdapterResolution(false, StrUtil.blankToDefault(catalog == null ? null : catalog.reasonCode(), SOURCE_MISSING),
                        byCategory, byCondition, StrUtil.blankToDefault(catalog == null ? null : catalog.reason(), "正式来源目录阻断"));
            }
            MesProEdhrReverseTraceSourceAdapter.ValidationResult validation = adapter.validateCondition(item, condition);
            if (validation == null || !validation.valid()) {
                return new AdapterResolution(false, "CONDITION_INVALID", byCategory, byCondition,
                        validation == null ? "条件不符合目录契约" : validation.reason());
            }
            byCondition.computeIfAbsent(adapter.category(), ignored -> new ArrayList<>()).add(condition);
        }
        return new AdapterResolution(true, null, byCategory, byCondition, null);
    }

    private boolean matchesCatalogItem(CatalogItem item, Condition condition) {
        if (item == null || condition == null
                || !Objects.equals(item.getEvidenceKey(), condition.getEvidenceKey())
                || !Objects.equals(item.getSourceView(), condition.getSourceView())) {
            return false;
        }
        Map<String, String> qualifiers = condition.getQualifiers();
        if (item.getCategory() == Category.PARAMETER && !hasCompleteParameterSemanticIdentity(qualifiers)) {
            return false;
        }
        if (qualifiers == null || qualifiers.isEmpty()) {
            return true;
        }
        Map<String, String> itemQualifiers = item.getQualifiers();
        return itemQualifiers != null && qualifiers.entrySet().stream()
                .allMatch(entry -> Objects.equals(entry.getValue(), itemQualifiers.get(entry.getKey())));
    }

    private static boolean hasCompleteParameterSemanticIdentity(Map<String, String> qualifiers) {
        return qualifiers != null
                && StrUtil.isNotBlank(qualifiers.get("routeVersionId"))
                && StrUtil.isNotBlank(qualifiers.get("routeProcessId"))
                && StrUtil.isNotBlank(qualifiers.get("processId"))
                && qualifiers.containsKey("unit")
                && PARAMETER_SEMANTIC_IDENTITY_KEYS.stream().allMatch(qualifiers::containsKey);
    }

    private CatalogItem findCatalogItem(List<CatalogItem> items, Condition condition) {
        if (items == null || condition == null) {
            return null;
        }
        List<CatalogItem> matches = items.stream().filter(item -> matchesCatalogItem(item, condition)).toList();
        if (matches.isEmpty()) {
            return null;
        }
        CatalogItem first = matches.get(0);
        // 同一条件可对应多条正式事实；这里只校验条件契约，评估与证据读取仍保留全部事实。
        return matches.stream().allMatch(item -> Objects.equals(first.getSemanticIdentity(), item.getSemanticIdentity())
                && Objects.equals(first.getQualifiers(), item.getQualifiers())
                && Objects.equals(first.getValueType(), item.getValueType())
                && Objects.equals(first.getUnit(), item.getUnit())
                && Objects.equals(first.getAllowedOperators(), item.getAllowedOperators())) ? first : null;
    }

    private CatalogSnapshot calculateCatalogSnapshot(MesProEdhrBatchExecutionDO anchor,
                                                     List<MesProEdhrBatchExecutionDO> candidates) {
        Map<Long, MesProEdhrBatchExecutionDO> versionSources = new LinkedHashMap<>();
        if (anchor != null) versionSources.put(anchor.getId(), anchor);
        candidates.forEach(batch -> versionSources.putIfAbsent(batch.getId(), batch));
        StringBuilder source = new StringBuilder("P2-CATALOG-V1|");
        for (MesProEdhrBatchExecutionDO batch : versionSources.values().stream()
                .sorted(Comparator.comparing(MesProEdhrBatchExecutionDO::getId)).toList()) {
            append(source, "batch.id", id(batch.getId()));
            append(source, "batch.update", time(batch.getUpdateTime()));
            append(source, "batch.aggregate", batch.getAggregateHash());
            for (MesProEdhrReverseTraceSourceAdapter adapter : sourceAdapters.stream()
                    .sorted(Comparator.comparing(item -> item.category().name())).toList()) {
                MesProEdhrReverseTraceSourceAdapter.CatalogResult result = readCatalogSafely(adapter, batch);
                append(source, "source." + adapter.category(), result == null ? null : result.sourceVersion());
                append(source, "source.status." + adapter.category(), result == null ? null : result.status());
            }
        }
        String digest = hash(source.toString());
        return new CatalogSnapshot("catalog-v1:" + digest, digest);
    }

    private MesProEdhrReverseTraceSourceAdapter.CatalogResult readCatalogSafely(
            MesProEdhrReverseTraceSourceAdapter adapter, MesProEdhrBatchExecutionDO batch) {
        try {
            return adapter.readCatalog(batch);
        } catch (IllegalStateException exception) {
            if (!isSourceFailure(exception)) throw exception;
            return new MesProEdhrReverseTraceSourceAdapter.CatalogResult(null, null, BLOCKED,
                    sourceFailureCode(exception), sourceFailureReason(exception), List.of());
        }
    }

    private MesProEdhrReverseTraceSourceAdapter.EvaluationResult evaluateSafely(
            MesProEdhrReverseTraceSourceAdapter adapter, MesProEdhrBatchExecutionDO batch,
            List<Condition> conditions) {
        try {
            return adapter.evaluate(batch, conditions);
        } catch (IllegalStateException exception) {
            if (!isSourceFailure(exception)) throw exception;
            return new MesProEdhrReverseTraceSourceAdapter.EvaluationResult(null, null, BLOCKED,
                    sourceFailureCode(exception), sourceFailureReason(exception), List.of());
        }
    }

    private static boolean isSourceFailure(IllegalStateException exception) {
        String message = exception.getMessage();
        return message != null && (message.startsWith("SOURCE_MISSING:") || message.startsWith("SOURCE_CONFLICT:"));
    }

    private static String sourceFailureCode(IllegalStateException exception) {
        String message = exception.getMessage();
        return message != null && message.startsWith("SOURCE_CONFLICT:") ? SOURCE_CONFLICT : SOURCE_MISSING;
    }

    private static String sourceFailureReason(IllegalStateException exception) {
        String message = exception.getMessage();
        int separator = message == null ? -1 : message.indexOf(':');
        return separator < 0 ? "正式来源读取失败" : message.substring(separator + 1).trim();
    }

    private List<MesProEdhrBatchExecutionDO> loadReleasedCandidates(TargetScope scope) {
        List<MesProEdhrBatchExecutionDO> result = new ArrayList<>();
        int pageNo = 1;
        long total = Long.MAX_VALUE;
        while (result.size() < total) {
            EdhrBatchExecutionPageReqVO request = new EdhrBatchExecutionPageReqVO();
            request.setReleasedOnly(Boolean.TRUE);
            request.setReleaseApprovedTime(releaseTime(scope));
            request.setPageNo(pageNo);
            request.setPageSize(INTERNAL_PAGE_SIZE);
            PageResult<MesProEdhrBatchExecutionDO> page = batchExecutionMapper.selectPage(request);
            if (page == null || page.getList() == null || page.getTotal() == null) {
                throw new IllegalArgumentException("READ_SCOPE_INVALID: candidate page/total is missing");
            }
            if (page.getList().isEmpty() && result.size() < page.getTotal()) {
                throw new IllegalArgumentException("READ_SCOPE_INVALID: candidate page is truncated");
            }
            long previous = result.isEmpty() ? Long.MAX_VALUE : result.get(result.size() - 1).getId();
            for (MesProEdhrBatchExecutionDO batch : page.getList()) {
                if (batch == null || batch.getId() == null || batch.getId() >= previous) {
                    throw new IllegalArgumentException("READ_SCOPE_INVALID: candidate IDs are duplicate or non-monotonic");
                }
                previous = batch.getId();
            }
            result.addAll(page.getList());
            total = page.getTotal();
            pageNo++;
        }
        if (result.size() != total) {
            throw new IllegalArgumentException("READ_SCOPE_INVALID: candidate total does not match unique rows");
        }
        List<MesProEdhrBatchExecutionDO> visible = result.stream().filter(this::sameTenant).filter(this::visible).toList();
        return visible;
    }

    private MesProEdhrBatchExecutionDO loadReleasedBatch(Long batchExecutionId) {
        try {
            MesProEdhrBatchExecutionDO batch = loadBatch(batchExecutionId, Boolean.TRUE);
            return isReleasedHistoryBatch(batch) ? batch : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private MesProEdhrBatchExecutionDO loadBatch(Long batchExecutionId, Boolean releasedOnly) {
        EdhrBatchExecutionPageReqVO request = new EdhrBatchExecutionPageReqVO();
        request.setBatchExecutionIds(List.of(batchExecutionId));
        request.setReleasedOnly(releasedOnly);
        request.setPageNo(1);
        request.setPageSize(1);
        PageResult<MesProEdhrBatchExecutionDO> page = batchExecutionMapper.selectPage(request);
        if (page == null || page.getList() == null || page.getList().size() != 1) {
            throw new IllegalArgumentException(Boolean.TRUE.equals(releasedOnly)
                    ? "RELEASED_HISTORY batchExecutionId not found"
                    : "batchExecutionId not found");
        }
        MesProEdhrBatchExecutionDO batch = page.getList().get(0);
        if (!sameTenant(batch)) {
            throw new IllegalArgumentException("BATCH_SCOPE_INVALID");
        }
        visibilityService.requireVisibleBatch(batch, SecurityFrameworkUtils.getLoginUserId());
        return batch;
    }

    private boolean isReleasedHistoryBatch(MesProEdhrBatchExecutionDO batch) {
        MesProEdhrReleaseTransactionDO transaction = releaseTransactionMapper
                .selectByBatchExecutionId(batch.getId());
        return transaction != null && "RELEASED".equals(transaction.getReleaseStatus());
    }

    private boolean sameTenant(MesProEdhrBatchExecutionDO batch) {
        return Objects.equals(TenantContextHolder.getRequiredTenantId(), batch.getTenantId());
    }

    private boolean visible(MesProEdhrBatchExecutionDO batch) {
        if (!sameTenant(batch)) {
            return false;
        }
        if (!visibilityService.canViewBatch(batch, SecurityFrameworkUtils.getLoginUserId())) {
            return false;
        }
        return true;
    }

    private Map<Category, MesProEdhrReverseTraceSourceAdapter> adapterMap() {
        Map<Category, MesProEdhrReverseTraceSourceAdapter> result = new EnumMap<>(Category.class);
        for (MesProEdhrReverseTraceSourceAdapter adapter : sourceAdapters) {
            result.putIfAbsent(adapter.category(), adapter);
        }
        return result;
    }

    private boolean hasCompleteAdapterCoverage() {
        return adapterMap().keySet().containsAll(CATEGORIES) && adapterMap().size() == CATEGORIES.size();
    }

    private MesProEdhrReverseTraceSourceAdapter adapterForCategory(Category category) {
        return adapterMap().get(category);
    }

    private List<Category> categories(Category category) {
        return category == null ? CATEGORIES : List.of(category);
    }

    private void addBlockedCategories(CatalogResponse response, Category requested) {
        for (Category category : categories(requested)) {
            response.getCategories().add(blockedCategory(category, SOURCE_MISSING, "六类正式来源适配器未完整接入"));
        }
    }

    private CatalogResponse blockedCatalog(String anchorBatchExecutionId, Category requested, String reason) {
        CatalogResponse response = new CatalogResponse()
                .setAnchorBatchExecutionId(anchorBatchExecutionId)
                .setCategories(new ArrayList<>())
                .setItems(new ArrayList<>())
                .setTotal(null);
        for (Category category : categories(requested)) {
            response.getCategories().add(blockedCategory(category, BATCH_SCOPE_INVALID, reason));
        }
        return response;
    }

    private CategoryStatus blockedCategory(Category category, String reasonCode, String reason) {
        return new CategoryStatus().setCategory(category).setStatus(BLOCKED)
                .setReasonCode(StrUtil.blankToDefault(reasonCode, SOURCE_MISSING))
                .setReason(StrUtil.blankToDefault(reason, "正式来源阻断"));
    }

    private QueryResponse blockedQuery(QueryResponse base, String code, String reason) {
        return base.setQueryStatus(BLOCKED).setCoverageStatus(BLOCKED).setReasonCode(code).setReason(reason).setTotal(null)
                .setList(new ArrayList<>());
    }

    private EvidenceResponse blockedEvidence(EvidenceResponse base, String code, String reason) {
        return base.setEvidenceStatus(BLOCKED).setReasonCode(code).setReason(reason).setTotal(null)
                .setItems(new ArrayList<>());
    }

    private MesProEdhrReverseTraceModels.QueryItem toQueryItem(MesProEdhrBatchExecutionDO batch, Long anchorId,
                                                               List<Condition> conditions) {
        return new MesProEdhrReverseTraceModels.QueryItem()
                .setBatchExecutionId(id(batch.getId()))
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setBatchCode(batch.getBatchCode())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setProductName(batch.getProductName())
                .setReleaseStatus("RELEASED")
                .setAnchor(Objects.equals(batch.getId(), anchorId))
                .setMatchCount(conditions.size())
                .setConditionIds(conditions.stream().map(Condition::getConditionId).toList())
                .setMatchSummary("同一batchExecutionId满足全部条件");
    }

    private void validateCommonChains(MesProEdhrBatchExecutionDO anchor, List<MesProEdhrBatchExecutionDO> candidates) {
        Map<Long, MesProEdhrBatchExecutionDO> batches = new LinkedHashMap<>();
        if (anchor != null) batches.put(anchor.getId(), anchor);
        candidates.forEach(batch -> batches.putIfAbsent(batch.getId(), batch));
        for (MesProEdhrBatchExecutionDO batch : batches.values()) {
            if (!isCompleteChainContext(toChainContext(batch, batch.getId()))) {
                throw new IllegalStateException("SOURCE_MISSING:批次主链正式关系投影缺失 batchExecutionId=" + batch.getId());
            }
        }
    }

    private ChainContext toChainContext(MesProEdhrBatchExecutionDO batch, Long anchorId) {
        if (batch == null || batch.getId() == null) {
            return null;
        }
        List<MesProEdhrBatchExecutionOriginDO> origins = originMapper.selectListByBatchExecutionId(batch.getId());
        if (origins == null || origins.isEmpty()) {
            return null;
        }
        Set<Long> activeOrderIds = origins.stream().map(MesProEdhrBatchExecutionOriginDO::getActiveOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> workOrderIds = origins.stream().map(MesProEdhrBatchExecutionOriginDO::getWorkOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> receiptIds = origins.stream().map(MesProEdhrBatchExecutionOriginDO::getCompletionBackfillReceiptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (activeOrderIds.size() != 1 || workOrderIds.size() != 1 || receiptIds.size() != 1
                || !workOrderIds.contains(batch.getWorkOrderId())) {
            return null;
        }
        Long activeOrderId = activeOrderIds.iterator().next();
        Long receiptId = receiptIds.iterator().next();
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        MesProcessPoolActiveOrderCompletionReceiptDO receipt = completionReceiptMapper
                .selectByIdAndTenantId(receiptId, TenantContextHolder.getRequiredTenantId());
        for (MesProEdhrBatchExecutionOriginDO origin : origins) {
            MesProEdhrReverseTraceReceiptValidator.validate(batch, origin, receipt);
        }
        if (activeOrder == null || receipt == null || !Objects.equals(receipt.getActiveOrderId(), activeOrderId)
                || !Objects.equals(activeOrder.getWorkOrderId(), batch.getWorkOrderId())) {
            return null;
        }
        List<MesProcessPoolActiveOrderReleaseApplicationDO> applications =
                releaseApplicationMapper.selectListByBatchExecutionIds(List.of(batch.getId()));
        if (applications == null || applications.size() != 1) {
            return null;
        }
        MesProcessPoolActiveOrderReleaseApplicationDO application = applications.get(0);
        if (!Objects.equals(application.getActiveOrderId(), activeOrderId)
                || !Objects.equals(application.getBatchExecutionId(), batch.getId())
                || application.getPqcReleaseWorkTaskId() == null
                || application.getReleaseTransactionId() == null
                || application.getReleaseApprovalWorkTaskId() == null) {
            return null;
        }
        MesProEdhrReleaseTransactionDO releaseTransaction = releaseTransactionMapper
                .selectByBatchExecutionId(batch.getId());
        if (releaseTransaction == null || !Objects.equals(releaseTransaction.getId(), application.getReleaseTransactionId())
                || !Objects.equals(releaseTransaction.getBatchExecutionId(), batch.getId())
                || !Objects.equals(releaseTransaction.getReleaseStatus(), "RELEASED")) {
            return null;
        }
        List<String> qaVersionRefs = activeOrder.getQaRegulationVersionId() == null ? List.of()
                : List.of(id(activeOrder.getQaRegulationVersionId()));
        if (qaVersionRefs.isEmpty()) {
            return null;
        }
        return new ChainContext().setAnchorBatchExecutionId(id(anchorId)).setTargetBatchExecutionId(id(batch.getId()))
                .setBatchExecutionCode(batch.getBatchExecutionCode()).setActiveOrderId(id(activeOrderId))
                .setWorkOrderId(id(batch.getWorkOrderId())).setRouteVersionId(id(batch.getRouteVersionId()))
                .setQaVersionRefs(qaVersionRefs).setCompletionReceiptId(id(receiptId))
                .setReleaseApplicationId(id(application.getId()))
                .setPqcReleaseWorkTaskId(id(application.getPqcReleaseWorkTaskId()))
                .setReleaseTransactionId(id(application.getReleaseTransactionId()))
                .setReleaseApprovalWorkTaskId(id(application.getReleaseApprovalWorkTaskId()))
                .setSourceStatus(RELEASED_HISTORY);
    }

    private boolean isCompleteChainContext(ChainContext context) {
        return context != null
                && StrUtil.isNotBlank(context.getAnchorBatchExecutionId())
                && StrUtil.isNotBlank(context.getTargetBatchExecutionId())
                && StrUtil.isNotBlank(context.getActiveOrderId())
                && StrUtil.isNotBlank(context.getWorkOrderId())
                && StrUtil.isNotBlank(context.getRouteVersionId())
                && context.getQaVersionRefs() != null && !context.getQaVersionRefs().isEmpty()
                && StrUtil.isNotBlank(context.getCompletionReceiptId())
                && StrUtil.isNotBlank(context.getReleaseApplicationId())
                && StrUtil.isNotBlank(context.getPqcReleaseWorkTaskId())
                && StrUtil.isNotBlank(context.getReleaseTransactionId())
                && StrUtil.isNotBlank(context.getReleaseApprovalWorkTaskId());
    }

    private static QueryRequest toQueryRequest(EvidenceRequest request) {
        return new QueryRequest().setAnchorBatchExecutionId(request.getAnchorBatchExecutionId())
                .setCatalogVersion(request.getCatalogVersion()).setTargetScope(request.getTargetScope())
                .setSourceVersionDigest(request.getSourceVersionDigest())
                .setLogic(request.getLogic()).setConditions(request.getConditions())
                .setPageNo(request.getPageNo()).setPageSize(request.getPageSize());
    }

    private static QueryRequest normalizeQuery(QueryRequest request) {
        QueryRequest result = new QueryRequest().setAnchorBatchExecutionId(trim(request.getAnchorBatchExecutionId()))
                .setCatalogVersion(trim(request.getCatalogVersion())).setSourceVersionDigest(trim(request.getSourceVersionDigest())).setLogic(upper(request.getLogic()))
                .setPageNo(request.getPageNo()).setPageSize(request.getPageSize())
                .setTargetScope(new TargetScope().setKind(upper(request.getTargetScope().getKind()))
                        .setReleaseApprovedFrom(request.getTargetScope().getReleaseApprovedFrom())
                        .setReleaseApprovedTo(request.getTargetScope().getReleaseApprovedTo()));
        result.setConditions(request.getConditions().stream().map(condition ->
                new Condition().setConditionId(trim(condition.getConditionId())).setEvidenceKey(trim(condition.getEvidenceKey()))
                        .setSourceView(upper(condition.getSourceView())).setOperator(upper(condition.getOperator()))
                        .setValue(condition.getValue()).setQualifiers(condition.getQualifiers())).toList());
        return result;
    }

    private static void validateCatalogRequest(CatalogRequest request) {
        if (request == null || StrUtil.isBlank(request.getAnchorBatchExecutionId())) {
            throw new IllegalArgumentException("anchorBatchExecutionId is required");
        }
        validatePage(request.getPageNo(), request.getPageSize());
        validateTargetScope(request.getTargetScope());
    }

    private static void validateTargetScope(TargetScope scope) {
        if (scope == null || !RELEASED_HISTORY.equals(upper(scope.getKind()))) {
            throw new IllegalArgumentException("targetScope.kind must be RELEASED_HISTORY");
        }
        if (scope.getReleaseApprovedFrom() != null && scope.getReleaseApprovedTo() != null
                && scope.getReleaseApprovedFrom().isAfter(scope.getReleaseApprovedTo())) {
            throw new IllegalArgumentException("releaseApprovedFrom must not be after releaseApprovedTo");
        }
    }

    private static void validateQueryRequest(QueryRequest request) {
        if (request == null || StrUtil.isBlank(request.getAnchorBatchExecutionId())
                || request.getTargetScope() == null
                || StrUtil.isBlank(request.getLogic()) || request.getConditions() == null
                || request.getConditions().isEmpty() || request.getConditions().size() > 10) {
            throw new IllegalArgumentException("invalid reverse-trace query request");
        }
        validatePage(request.getPageNo(), request.getPageSize());
        validateTargetScope(request.getTargetScope());
        if (!AND.equals(upper(request.getLogic()))) {
            throw new IllegalArgumentException("logic must be AND");
        }
        Set<String> conditionIds = new HashSet<>();
        for (Condition condition : request.getConditions()) {
            if (condition == null || StrUtil.isBlank(condition.getConditionId())
                    || !condition.getConditionId().equals(condition.getConditionId().trim())
                    || !conditionIds.add(condition.getConditionId())) {
                throw new IllegalArgumentException("conditionId must be unique");
            }
        }
    }

    private static void validateEvidenceRequest(EvidenceRequest request) {
        if (request == null || StrUtil.isBlank(request.getAnchorBatchExecutionId())
                || StrUtil.isBlank(request.getTargetBatchExecutionId()) || request.getTargetScope() == null
                || StrUtil.isBlank(request.getLogic()) || request.getConditions() == null
                || request.getConditions().isEmpty()) {
            throw new IllegalArgumentException("invalid reverse-trace evidence request");
        }
        validateQueryRequest(toQueryRequest(request));
    }

    private static void validatePage(Integer pageNo, Integer pageSize) {
        if (pageNo == null || pageNo < 1 || pageSize == null || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageNo/pageSize out of range");
        }
    }

    private static LocalDateTime[] releaseTime(TargetScope scope) {
        if (scope == null || (scope.getReleaseApprovedFrom() == null && scope.getReleaseApprovedTo() == null)) {
            return null;
        }
        return new LocalDateTime[]{scope.getReleaseApprovedFrom(), scope.getReleaseApprovedTo()};
    }

    private static Long parsePositiveId(String value, String field) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " must be a positive integer string", ex);
        }
    }

    private static String id(Long value) { return value == null ? null : String.valueOf(value); }

    private static String trim(String value) { return value == null ? "" : value.trim(); }

    private static String upper(String value) { return trim(value).toUpperCase(java.util.Locale.ROOT); }

    private static String time(LocalDateTime value) { return value == null ? "" : TIME_FORMAT.format(value); }

    private static void append(StringBuilder builder, String key, String value) {
        String actual = value == null ? "" : value;
        builder.append(key.length()).append(':').append(key).append(actual.length()).append(':').append(actual).append('|');
    }

    private static String canonicalValue(Object value) {
        if (value == null) return token("null", "");
        if (value instanceof Number number) {
            return token("number", new BigDecimal(number.toString()).stripTrailingZeros().toPlainString());
        }
        if (value instanceof Boolean bool) return token("boolean", String.valueOf(bool));
        if (value instanceof String text) return token("string", trim(text));
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream().sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .map(entry -> token("key", trim(String.valueOf(entry.getKey()))) + canonicalValue(entry.getValue()))
                    .collect(Collectors.joining("", token("map", String.valueOf(map.size()) + ":"), ""));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(MesProEdhrReverseTraceServiceImpl::canonicalValue)
                    .collect(Collectors.joining("", token("list", String.valueOf(collection.size()) + ":"), ""));
        }
        if (value.getClass().isArray()) {
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) values.add(Array.get(value, i));
            return canonicalValue(values);
        }
        return token(value.getClass().getName(), String.valueOf(value));
    }

    private static String token(String type, String value) {
        return type.length() + ":" + type + value.length() + ":" + value;
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is required for reverse-trace versioning", ex);
        }
    }

    private record CatalogSnapshot(String version, String sourceDigest) { }

    private record AdapterResolution(boolean complete, String reasonCode,
                                     Map<Category, MesProEdhrReverseTraceSourceAdapter> byCategory,
                                     Map<Category, List<Condition>> conditionsByCategory,
                                     String reason) { }

    private record Evaluation(List<MesProEdhrBatchExecutionDO> matched, String blockedReason, String reason) { }
}
