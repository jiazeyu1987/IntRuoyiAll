package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationBatchOptionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationBatchOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationCreateRequestDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationCreateRequestMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpWriteOperation;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import cn.hutool.core.util.StrUtil;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_BATCH_MARKET_RELEASED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_BATCH_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_IDEMPOTENCY_KEY_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_INITIATOR_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_MARKET_RELEASE_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_LEVEL_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_CATEGORY_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_REQUIRED_CONTENT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_CREATE_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_SIGNATURE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_FORMAL_ACTIVE_SOURCE_REQUIRED;

@Service
public class MesProEdhrDeviationServiceImpl implements MesProEdhrDeviationService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String LEVEL_NORMAL = "NORMAL";
    public static final String LEVEL_CRITICAL = "CRITICAL";
    private static final Set<String> CATEGORY_CODES = Set.of(
            "PRODUCTION_PROCESS", "PRODUCTION_UTILITY", "INSPECTION", "VALIDATION_CONFIRMATION",
            "MATERIAL_MANAGEMENT", "DOCUMENT_RECORD", "PRODUCT_RELEASE", "OTHER_SYSTEM");

    private final MesProEdhrDeviationMapper deviationMapper;
    private final MesProEdhrDeviationCreateRequestMapper createRequestMapper;
    private final MesProEdhrBatchExecutionMapper batchExecutionMapper;
    private final MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private final MesProEdhrDeviationNumberGenerator numberGenerator;
    private final MesProBatchRecordExecutionSignatureService executionSignatureService;
    private final GxpAuditService gxpAuditService;
    @Resource
    private MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper;
    @Resource
    private MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;

    @Override
    public List<MesProEdhrDeviationBatchOptionRespVO> getBatchOptionsByActiveOrder(Long activeOrderId) {
        if (activeOrderId == null || activeOrderId <= 0) {
            throw exception(PRO_EDHR_DEVIATION_BATCH_NOT_EXISTS, activeOrderId);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return batchExecutionOriginMapper.selectListByTraceFilter(activeOrderId, null, null, null).stream()
                .map(origin -> batchExecutionMapper.selectByTenantIdAndId(tenantId, origin.getBatchExecutionId()))
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(MesProEdhrBatchExecutionDO::getId, batch -> batch,
                        (left, right) -> left, java.util.LinkedHashMap::new))
                .values().stream()
                .map(batch -> new MesProEdhrDeviationBatchOptionRespVO()
                        .setBatchExecutionId(batch.getId()).setBatchExecutionCode(batch.getBatchExecutionCode())
                        .setWorkOrderId(batch.getWorkOrderId()).setWorkOrderCode(batch.getWorkOrderCode())
                        .setBatchCode(batch.getBatchCode()).setProductCode(batch.getProductCode())
                        .setProductName(batch.getProductName()).setRouteName(batch.getRouteName())
                        .setBatchStatus(batch.getStatus()))
                .toList();
    }

    public MesProEdhrDeviationServiceImpl(MesProEdhrDeviationMapper deviationMapper,
                                         MesProEdhrDeviationCreateRequestMapper createRequestMapper,
                                         MesProEdhrBatchExecutionMapper batchExecutionMapper,
                                         MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
                                         MesProEdhrDeviationNumberGenerator numberGenerator,
                                         MesProBatchRecordExecutionSignatureService executionSignatureService,
                                         GxpAuditService gxpAuditService) {
        this.deviationMapper = deviationMapper;
        this.createRequestMapper = createRequestMapper;
        this.batchExecutionMapper = batchExecutionMapper;
        this.releaseTransactionMapper = releaseTransactionMapper;
        this.numberGenerator = numberGenerator;
        this.executionSignatureService = executionSignatureService;
        this.gxpAuditService = gxpAuditService;
    }

    @Override
    public PageResult<MesProEdhrDeviationBatchOptionRespVO> getBatchOptions(
            MesProEdhrDeviationBatchOptionPageReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        PageResult<MesProEdhrBatchExecutionDO> page =
                batchExecutionMapper.selectDeviationOptionsPage(reqVO, tenantId, reqVO.getSearch());
        List<MesProEdhrDeviationBatchOptionRespVO> rows = page.getList().stream()
                .map(batch -> new MesProEdhrDeviationBatchOptionRespVO()
                        .setBatchExecutionId(batch.getId())
                        .setBatchExecutionCode(batch.getBatchExecutionCode())
                        .setWorkOrderId(batch.getWorkOrderId())
                        .setWorkOrderCode(batch.getWorkOrderCode())
                        .setBatchCode(batch.getBatchCode())
                        .setProductCode(batch.getProductCode())
                        .setProductName(batch.getProductName())
                        .setRouteName(batch.getRouteName())
                        .setBatchStatus(batch.getStatus()))
                .toList();
        return new PageResult<>(rows, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GxpWriteOperation(operationId = "edhr.deviation.create")
    public MesProEdhrDeviationRespVO create(Long actorUserId, MesProEdhrDeviationCreateReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (actorUserId == null || actorUserId <= 0) {
            throw exception(PRO_EDHR_DEVIATION_INITIATOR_REQUIRED);
        }
        String idempotencyKey = reqVO.getIdempotencyKey();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw exception(PRO_EDHR_DEVIATION_IDEMPOTENCY_KEY_REQUIRED);
        }
        validateCreateRequest(reqVO);
        String payloadHash = buildCreatePayloadHash(reqVO);

        gxpAuditService.acquireLedgerLock();
        if (createRequestMapper.reserveOrLock(tenantId, idempotencyKey, payloadHash) < 0) {
            throw new IllegalStateException("EDHR_DEVIATION_IDEMPOTENCY_RESERVATION_FAILED");
        }
        MesProEdhrDeviationCreateRequestDO request = createRequestMapper.selectForUpdate(tenantId, idempotencyKey);
        if (request == null || !Objects.equals(request.getPayloadHash(), payloadHash)) {
            throw exception(PRO_EDHR_DEVIATION_IDEMPOTENCY_CONFLICT, idempotencyKey);
        }
        if (request.getDeviationId() != null) {
            MesProEdhrDeviationDO existing = deviationMapper.selectByTenantAndId(tenantId, request.getDeviationId());
            if (existing == null) {
                throw new IllegalStateException("EDHR_DEVIATION_IDEMPOTENCY_RESULT_MISSING");
            }
            return toResp(existing);
        }
        if (StrUtil.isBlank(reqVO.getSignaturePassword())) {
            throw exception(PRO_EDHR_DEVIATION_SIGNATURE_REQUIRED);
        }

        // The batch row is the shared serialization boundary used by deviation creation and market release.
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectByTenantIdAndIdForUpdate(
                tenantId, reqVO.getBatchExecutionId());
        if (batch == null || !Objects.equals(tenantId, batch.getTenantId())
                || !Objects.equals(reqVO.getBatchExecutionId(), batch.getId())
                || Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionMapper.BATCH_STATUS_VOIDED)) {
            throw exception(PRO_EDHR_DEVIATION_BATCH_NOT_EXISTS, reqVO.getBatchExecutionId());
        }
        boolean formallyLinked = batchExecutionOriginMapper.selectListByBatchExecutionId(batch.getId()).stream()
                .anyMatch(origin -> origin.getActiveOrderId() != null
                        && MesProEdhrBatchTraceFormalSourceResolver.isActiveOrderEntryType(origin.getEntryType()));
        if (!formallyLinked) {
            throw exception(PRO_EDHR_BATCH_FORMAL_ACTIVE_SOURCE_REQUIRED);
        }
        List<MesProEdhrReleaseTransactionDO> transactions =
                releaseTransactionMapper.selectListByTenantAndBatchExecutionIdForUpdate(
                        tenantId, batch.getId());
        if (transactions.stream().anyMatch(tx -> "RELEASED".equals(tx.getReleaseStatus()))) {
            throw exception(PRO_EDHR_DEVIATION_BATCH_MARKET_RELEASED, batch.getId());
        }

        String deviationCode = numberGenerator.nextCode(tenantId);
        LocalDateTime initiatedAt = LocalDateTime.now(MesProEdhrDeviationNumberGenerator.BUSINESS_ZONE);
        String initiatorName = trimToNull(SecurityFrameworkUtils.getLoginUserNickname());
        MesProEdhrDeviationDO deviation = new MesProEdhrDeviationDO()
                .setTenantId(tenantId)
                .setDeviationCode(deviationCode)
                .setBatchExecutionId(batch.getId())
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setLevel(reqVO.getLevel())
                .setDiscoveryDepartmentId(reqVO.getDiscoveryDepartmentId())
                .setDiscoveryDepartmentName(trimToNull(reqVO.getDiscoveryDepartmentName()))
                .setDiscovererId(reqVO.getDiscovererId())
                .setDiscovererName(trimToNull(reqVO.getDiscovererName()))
                .setDiscoveredAt(reqVO.getDiscoveredAt())
                .setDiscoveryLocation(trimToNull(reqVO.getDiscoveryLocation()))
                .setProductName(trimToNull(reqVO.getProductName()))
                .setProductSpecification(trimToNull(reqVO.getProductSpecification()))
                .setEquipmentOrSystem(trimToNull(reqVO.getEquipmentOrSystem()))
                .setReportedAt(reqVO.getReportedAt())
                .setReceiverId(reqVO.getReceiverId())
                .setReceiverName(trimToNull(reqVO.getReceiverName()))
                .setCategoryCodesJson(JSON.toJSONString(normalizedCategoryCodes(reqVO)))
                .setDescription(reqVO.getDescription().trim())
                .setEmergencyAction(trimToNull(reqVO.getEmergencyAction()))
                .setLevelBasis(reqVO.getLevelBasis().trim())
                .setInitiatorUserId(actorUserId)
                .setInitiatorName(initiatorName)
                .setInitiatedAt(initiatedAt)
                .setStatus(STATUS_OPEN)
                .setCreateIdempotencyKey(idempotencyKey)
                .setCreatePayloadHash(payloadHash)
                .setVersion(1);
        if (deviationMapper.insert(deviation) != 1 || deviation.getId() == null) {
            throw exception(PRO_EDHR_DEVIATION_CREATE_FAILED);
        }
        String canonicalContent = canonicalInitiationContent(deviation);
        String contentHash = DigestUtil.sha256Hex(canonicalContent);
        Long signatureId = executionSignatureService.recordDeviationInitiationSignature(
                actorUserId,
                batch.getId(),
                deviation.getId(),
                deviationCode,
                batch.getBatchExecutionCode(),
                reqVO.getSignaturePassword(),
                contentHash);
        if (signatureId == null || signatureId <= 0) {
            throw exception(PRO_EDHR_DEVIATION_CREATE_FAILED);
        }
        if (deviationMapper.attachInitiatorSignature(
                tenantId, deviation.getId(), signatureId, contentHash, initiatorName) != 1) {
            throw exception(PRO_EDHR_DEVIATION_CREATE_FAILED);
        }
        deviation.setInitiatorSignatureId(signatureId)
                .setInitiatorContentHash(contentHash)
                .setInitiatorName(initiatorName);
        if (createRequestMapper.linkDeviation(tenantId, idempotencyKey, payloadHash, deviation.getId()) != 1) {
            throw exception(PRO_EDHR_DEVIATION_CREATE_FAILED);
        }
        appendInitiationAudit(tenantId, deviation, canonicalContent, contentHash);
        return toResp(deviation);
    }

    @Override
    public PageResult<MesProEdhrDeviationRespVO> getPage(MesProEdhrDeviationPageReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Page<MesProEdhrDeviationDO> page = deviationMapper.selectPageByTenant(
                new Page<>(reqVO.getPageNo(), reqVO.getPageSize()), tenantId, reqVO.getStatus(),
                reqVO.getLevel(), reqVO.getBatchExecutionId(), reqVO.getSearch(), reqVO.getSortField(),
                reqVO.getSortOrder(), reqVO.getInitiatedAtStart(), reqVO.getInitiatedAtEnd());
        return new PageResult<>(page.getRecords().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public MesProEdhrDeviationRespVO get(Long id) {
        if (id == null || id <= 0) {
            throw exception(PRO_EDHR_DEVIATION_BATCH_NOT_EXISTS, id);
        }
        MesProEdhrDeviationDO deviation = deviationMapper.selectByTenantAndId(
                TenantContextHolder.getRequiredTenantId(), id);
        if (deviation == null) {
            throw exception(PRO_EDHR_DEVIATION_BATCH_NOT_EXISTS, id);
        }
        return toResp(deviation);
    }

    @Override
    public void ensureNoOpenDeviationForMarketRelease(Long tenantId, Long batchExecutionId) {
        if (tenantId == null || !Objects.equals(tenantId, TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalStateException("EDHR_DEVIATION_TENANT_CONTEXT_MISMATCH");
        }
        if (deviationMapper.selectOpenIdByTenantAndBatchExecutionIdForUpdate(tenantId, batchExecutionId) != null) {
            throw exception(PRO_EDHR_DEVIATION_MARKET_RELEASE_BLOCKED, batchExecutionId);
        }
    }

    private void validateCreateRequest(MesProEdhrDeviationCreateReqVO reqVO) {
        if (!LEVEL_NORMAL.equals(reqVO.getLevel()) && !LEVEL_CRITICAL.equals(reqVO.getLevel())) {
            throw exception(PRO_EDHR_DEVIATION_LEVEL_INVALID);
        }
        if (reqVO.getCategoryCodes() == null || reqVO.getCategoryCodes().isEmpty()
                || reqVO.getCategoryCodes().stream().anyMatch(code -> !CATEGORY_CODES.contains(code))) {
            throw exception(PRO_EDHR_DEVIATION_CATEGORY_INVALID);
        }
        if (reqVO.getDescription() == null || reqVO.getDescription().isBlank()
                || reqVO.getLevelBasis() == null || reqVO.getLevelBasis().isBlank()) {
            throw exception(PRO_EDHR_DEVIATION_REQUIRED_CONTENT_MISSING);
        }
    }

    String buildCreatePayloadHash(MesProEdhrDeviationCreateReqVO reqVO) {
        Map<String, Object> payload = new TreeMap<>();
        payload.put("batchExecutionId", reqVO.getBatchExecutionId());
        payload.put("level", reqVO.getLevel());
        payload.put("discoveryDepartmentId", reqVO.getDiscoveryDepartmentId());
        payload.put("discoveryDepartmentName", trimToNull(reqVO.getDiscoveryDepartmentName()));
        payload.put("discovererId", reqVO.getDiscovererId());
        payload.put("discovererName", trimToNull(reqVO.getDiscovererName()));
        payload.put("discoveredAt", reqVO.getDiscoveredAt());
        payload.put("discoveryLocation", trimToNull(reqVO.getDiscoveryLocation()));
        payload.put("productName", trimToNull(reqVO.getProductName()));
        payload.put("productSpecification", trimToNull(reqVO.getProductSpecification()));
        payload.put("equipmentOrSystem", trimToNull(reqVO.getEquipmentOrSystem()));
        payload.put("reportedAt", reqVO.getReportedAt());
        payload.put("receiverId", reqVO.getReceiverId());
        payload.put("receiverName", trimToNull(reqVO.getReceiverName()));
        payload.put("categoryCodes", normalizedCategoryCodes(reqVO));
        payload.put("description", reqVO.getDescription().trim());
        payload.put("emergencyAction", trimToNull(reqVO.getEmergencyAction()));
        payload.put("levelBasis", reqVO.getLevelBasis().trim());
        payload.put("idempotencyKey", reqVO.getIdempotencyKey());
        // signaturePassword is intentionally excluded from persisted hashes and canonical payloads.
        return DigestUtil.sha256Hex(JSON.toJSONString(payload));
    }

    private List<String> normalizedCategoryCodes(MesProEdhrDeviationCreateReqVO reqVO) {
        return reqVO.getCategoryCodes().stream().distinct().sorted().toList();
    }

    private String canonicalInitiationContent(MesProEdhrDeviationDO deviation) {
        Map<String, Object> content = new TreeMap<>();
        content.put("deviationId", deviation.getId());
        content.put("deviationCode", deviation.getDeviationCode());
        content.put("tenantId", deviation.getTenantId());
        content.put("batchExecutionId", deviation.getBatchExecutionId());
        content.put("batchExecutionCode", deviation.getBatchExecutionCode());
        content.put("workOrderId", deviation.getWorkOrderId());
        content.put("workOrderCode", deviation.getWorkOrderCode());
        content.put("batchCode", deviation.getBatchCode());
        content.put("level", deviation.getLevel());
        content.put("discoveryDepartmentId", deviation.getDiscoveryDepartmentId());
        content.put("discoveryDepartmentName", deviation.getDiscoveryDepartmentName());
        content.put("discovererId", deviation.getDiscovererId());
        content.put("discovererName", deviation.getDiscovererName());
        content.put("discoveredAt", deviation.getDiscoveredAt());
        content.put("discoveryLocation", deviation.getDiscoveryLocation());
        content.put("productName", deviation.getProductName());
        content.put("productSpecification", deviation.getProductSpecification());
        content.put("equipmentOrSystem", deviation.getEquipmentOrSystem());
        content.put("reportedAt", deviation.getReportedAt());
        content.put("receiverId", deviation.getReceiverId());
        content.put("receiverName", deviation.getReceiverName());
        content.put("categoryCodes", deviation.getCategoryCodesJson());
        content.put("description", deviation.getDescription());
        content.put("emergencyAction", deviation.getEmergencyAction());
        content.put("levelBasis", deviation.getLevelBasis());
        content.put("initiatorUserId", deviation.getInitiatorUserId());
        content.put("initiatorName", deviation.getInitiatorName());
        content.put("initiatedAt", deviation.getInitiatedAt());
        return JSON.toJSONString(content);
    }

    private void appendInitiationAudit(Long tenantId, MesProEdhrDeviationDO deviation,
                                       String canonicalContent, String contentHash) {
        Map<String, Object> before = Map.of(
                "exists", false,
                "batchExecutionId", deviation.getBatchExecutionId());
        Map<String, Object> after = new TreeMap<>();
        after.put("exists", true);
        after.put("deviationId", deviation.getId());
        after.put("deviationCode", deviation.getDeviationCode());
        after.put("batchExecutionId", deviation.getBatchExecutionId());
        after.put("level", deviation.getLevel());
        after.put("contentHash", contentHash);
        after.put("signatureId", deviation.getInitiatorSignatureId());
        after.put("canonicalContent", canonicalContent);
        var appended = gxpAuditService.append(GxpAuditCommand.builder()
                .operationId("edhr.deviation.create")
                .subjectId("MES_EDHR_DEVIATION:" + deviation.getId())
                .subjectVersion(String.valueOf(deviation.getVersion()))
                .reason("DEVIATION_INITIATION["
                        + String.join(",", JSON.parseArray(deviation.getCategoryCodesJson(), String.class))
                        + "]: " + deviation.getDescription())
                .beforeState(GxpAuditStateEnvelope.builder()
                        .state("ABSENT").objectVersion("0").canonicalJson(JSON.toJSONString(before)).build())
                .afterState(GxpAuditStateEnvelope.builder()
                        .state("PRESENT").objectVersion(String.valueOf(deviation.getVersion()))
                        .canonicalJson(JSON.toJSONString(after)).build())
                .idempotencyKey("EDHR_DEVIATION_CREATE:" + DigestUtil.sha256Hex(
                        tenantId + "|" + deviation.getCreateIdempotencyKey()))
                .requestId("EDHR_DEVIATION_CREATE:" + deviation.getCreateIdempotencyKey())
                .source("MesProEdhrDeviationServiceImpl.create")
                .signatureRecordId(String.valueOf(deviation.getInitiatorSignatureId()))
                .signatureContentHash(contentHash)
                .build());
        if (appended == null || appended.eventId() == null || StrUtil.isBlank(appended.eventHash())) {
            throw exception(PRO_EDHR_DEVIATION_CREATE_FAILED);
        }
    }

    private MesProEdhrDeviationRespVO toResp(MesProEdhrDeviationDO deviation) {
        MesProEdhrDeviationRespVO response = new MesProEdhrDeviationRespVO()
                .setId(deviation.getId())
                .setDeviationCode(deviation.getDeviationCode())
                .setBatchExecutionId(deviation.getBatchExecutionId())
                .setBatchExecutionCode(deviation.getBatchExecutionCode())
                .setBatchCode(deviation.getBatchCode())
                .setLevel(deviation.getLevel())
                .setStatus(deviation.getStatus())
                .setDescription(deviation.getDescription())
                .setDiscoveryDepartmentName(deviation.getDiscoveryDepartmentName())
                .setDiscovererName(deviation.getDiscovererName())
                .setDiscoveredAt(deviation.getDiscoveredAt())
                .setDiscoveryLocation(deviation.getDiscoveryLocation())
                .setProductName(deviation.getProductName())
                .setProductSpecification(deviation.getProductSpecification())
                .setEquipmentOrSystem(deviation.getEquipmentOrSystem())
                .setReportedAt(deviation.getReportedAt())
                .setReceiverName(deviation.getReceiverName())
                .setCategoryCodesJson(deviation.getCategoryCodesJson())
                .setEmergencyAction(deviation.getEmergencyAction())
                .setLevelBasis(deviation.getLevelBasis())
                .setCloseReason(deviation.getCloseReason())
                .setClosedAt(deviation.getClosedAt())
                .setNonconformanceReviewId(deviation.getNonconformanceReviewId())
                .setInitiatedAt(deviation.getInitiatedAt())
                .setInitiatorSignatureId(deviation.getInitiatorSignatureId())
                .setInitiatorContentHash(deviation.getInitiatorContentHash());
        if (nonconformanceReviewMapper != null && deviation.getNonconformanceReviewId() != null) {
            var review = nonconformanceReviewMapper.selectById(deviation.getNonconformanceReviewId());
            if (review != null) {
                response.setNonconformanceReviewCode(review.getReviewCode())
                        .setNonconformanceReviewStatus(review.getReviewStatus())
                        .setNonconformanceDisposition(review.getDisposition())
                        .setNonconformanceClosedAt(review.getClosedAt());
            }
        }
        return response;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
