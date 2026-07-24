package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEffectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeImpactPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeImpactRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeRecalculateImpactReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeSubmitReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrUnifiedChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrUnifiedChangeRequestDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrUnifiedChangeEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrUnifiedChangeImpactMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrUnifiedChangeRequestMapper;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_DIFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_IDEMPOTENCY_KEY_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_IMPACT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_OBJECT_TYPE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_OVERWRITE_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_SIGNOFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID;

@Service
public class MesProEdhrUnifiedChangeServiceImpl implements MesProEdhrUnifiedChangeService {

    public static final String OBJECT_TYPE_FORM_TEMPLATE = "FORM_TEMPLATE";
    public static final String OBJECT_TYPE_DHR_TEMPLATE = "DHR_TEMPLATE";
    public static final String OBJECT_TYPE_RECORDBOOK_TEMPLATE = "RECORDBOOK_TEMPLATE";
    public static final String OBJECT_TYPE_BATCH_RECORD_VERSION = "BATCH_RECORD_VERSION";

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_EFFECT_BLOCKED = "EFFECT_BLOCKED";

    public static final String EVENT_TYPE_CREATE = "CREATE";
    public static final String EVENT_TYPE_SUBMIT = "SUBMIT";
    public static final String EVENT_TYPE_IMPACT_RECALCULATE = "IMPACT_RECALCULATE";
    public static final String EVENT_TYPE_APPROVE = "APPROVE";
    public static final String EVENT_TYPE_EFFECT_REQUEST_BLOCKED = "EFFECT_REQUEST_BLOCKED";

    private static final Set<String> SUPPORTED_OBJECT_TYPES = Set.of(
            OBJECT_TYPE_FORM_TEMPLATE, OBJECT_TYPE_DHR_TEMPLATE, OBJECT_TYPE_RECORDBOOK_TEMPLATE,
            OBJECT_TYPE_BATCH_RECORD_VERSION);
    private static final Set<String> OVERWRITE_MARKERS = Set.of("CURRENT", "OVERWRITE_CURRENT", "HISTORY_CURRENT");

    @Resource
    private MesProEdhrUnifiedChangeRequestMapper unifiedChangeRequestMapper;
    @Resource
    private MesProEdhrUnifiedChangeImpactMapper unifiedChangeImpactMapper;
    @Resource
    private MesProEdhrUnifiedChangeEventMapper unifiedChangeEventMapper;

    @Override
    public PageResult<MesProEdhrUnifiedChangeRespVO> getPage(MesProEdhrUnifiedChangePageReqVO reqVO) {
        return BeanUtils.toBean(unifiedChangeRequestMapper.selectPage(reqVO), MesProEdhrUnifiedChangeRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrUnifiedChangeImpactRespVO> getImpactPage(MesProEdhrUnifiedChangeImpactPageReqVO reqVO) {
        return BeanUtils.toBean(unifiedChangeImpactMapper.selectPage(reqVO), MesProEdhrUnifiedChangeImpactRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrUnifiedChangeEventRespVO> getEventPage(MesProEdhrUnifiedChangeEventPageReqVO reqVO) {
        return BeanUtils.toBean(unifiedChangeEventMapper.selectPage(reqVO), MesProEdhrUnifiedChangeEventRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrUnifiedChangeRespVO create(MesProEdhrUnifiedChangeCreateReqVO reqVO) {
        String controlledObjectType = requireObjectType(reqVO.getControlledObjectType());
        String controlledObjectId = requireText(reqVO.getControlledObjectId(), PRO_EDHR_UNIFIED_CHANGE_OBJECT_TYPE_INVALID);
        String controlledObjectCode = requireText(reqVO.getControlledObjectCode(), PRO_EDHR_UNIFIED_CHANGE_OBJECT_TYPE_INVALID);
        String currentVersion = requireText(reqVO.getCurrentVersion(), PRO_EDHR_UNIFIED_CHANGE_OVERWRITE_FORBIDDEN);
        String targetVersion = requireText(reqVO.getTargetVersion(), PRO_EDHR_UNIFIED_CHANGE_OVERWRITE_FORBIDDEN);
        rejectOverwriteCurrentVersion(currentVersion, targetVersion);
        String changeType = requireText(reqVO.getChangeType(), PRO_EDHR_UNIFIED_CHANGE_OBJECT_TYPE_INVALID);
        String riskLevel = requireText(reqVO.getRiskLevel(), PRO_EDHR_UNIFIED_CHANGE_IMPACT_REQUIRED);
        String reason = requireReason(reqVO.getReason());
        String diffSnapshotJson = requireDiffSnapshot(reqVO.getDiffSnapshotJson());
        String impactSummaryJson = requireImpactScope(reqVO.getImpactSummaryJson());
        String idempotencyKey = requireIdempotencyKey(reqVO.getIdempotencyKey());

        MesProEdhrUnifiedChangeRequestDO existing = unifiedChangeRequestMapper
                .selectByControlledObjectTypeAndControlledObjectIdAndChangeTypeAndIdempotencyKey(
                        controlledObjectType, controlledObjectId, changeType, idempotencyKey);
        if (existing != null) {
            return toResp(existing);
        }

        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        String impactRecalculationHash = recalculateImpactScope(controlledObjectType, controlledObjectId,
                currentVersion, targetVersion, diffSnapshotJson, impactSummaryJson, occurredAt);
        String evidenceHash = buildChangeEvidenceHash(controlledObjectType, controlledObjectId, currentVersion,
                targetVersion, changeType, STATUS_DRAFT, reason, diffSnapshotJson, impactSummaryJson,
                impactRecalculationHash, idempotencyKey);

        MesProEdhrUnifiedChangeRequestDO request = MesProEdhrUnifiedChangeRequestDO.builder()
                .changeCode("EDHR-CHANGE-" + occurredAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                .controlledObjectType(controlledObjectType)
                .controlledObjectId(controlledObjectId)
                .controlledObjectCode(controlledObjectCode)
                .currentVersion(currentVersion)
                .targetVersion(targetVersion)
                .changeType(changeType)
                .changeStatus(STATUS_DRAFT)
                .riskLevel(riskLevel)
                .reasonCategory(StrUtil.trim(reqVO.getReasonCategory()))
                .reason(reason)
                .diffSnapshotJson(diffSnapshotJson)
                .impactSummaryJson(impactSummaryJson)
                .impactRecalculatedAt(occurredAt)
                .impactRecalculationHash(impactRecalculationHash)
                .requestedBy(actorUserId)
                .requestedAt(occurredAt)
                .idempotencyKey(idempotencyKey)
                .evidenceHash(evidenceHash)
                .build();
        unifiedChangeRequestMapper.insert(request);
        recordUnifiedChangeEvent(request, EVENT_TYPE_CREATE, null, STATUS_DRAFT, actorUserId, reason, null,
                idempotencyKey, occurredAt);
        return toResp(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrUnifiedChangeRespVO submit(MesProEdhrUnifiedChangeSubmitReqVO reqVO) {
        MesProEdhrUnifiedChangeRequestDO request = requireChangeRequest(reqVO.getChangeRequestId());
        requireStatus(request, STATUS_DRAFT);
        String reason = requireReason(reqVO.getReason());
        String signoffEvidenceHash = requireSignoffEvidence(reqVO.getSignoffEvidenceHash());
        String idempotencyKey = requireIdempotencyKey(reqVO.getIdempotencyKey());
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();

        unifiedChangeRequestMapper.updateById(new MesProEdhrUnifiedChangeRequestDO()
                .setId(request.getId())
                .setChangeStatus(STATUS_SUBMITTED)
                .setSubmittedBy(actorUserId)
                .setSubmittedAt(occurredAt)
                .setEvidenceHash(buildChangeEvidenceHash(request, STATUS_SUBMITTED, idempotencyKey)));
        MesProEdhrUnifiedChangeRequestDO updated = requireChangeRequest(request.getId());
        recordUnifiedChangeEvent(updated, EVENT_TYPE_SUBMIT, STATUS_DRAFT, STATUS_SUBMITTED, actorUserId, reason,
                signoffEvidenceHash, idempotencyKey, occurredAt);
        return toResp(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrUnifiedChangeRespVO recalculateImpact(MesProEdhrUnifiedChangeRecalculateImpactReqVO reqVO) {
        MesProEdhrUnifiedChangeRequestDO request = requireChangeRequest(reqVO.getChangeRequestId());
        String impactSummaryJson = requireImpactScope(reqVO.getImpactSummaryJson());
        String idempotencyKey = requireIdempotencyKey(reqVO.getIdempotencyKey());
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        String impactRecalculationHash = recalculateImpactScope(request.getControlledObjectType(),
                request.getControlledObjectId(), request.getCurrentVersion(), request.getTargetVersion(),
                request.getDiffSnapshotJson(), impactSummaryJson, occurredAt);

        unifiedChangeRequestMapper.updateById(new MesProEdhrUnifiedChangeRequestDO()
                .setId(request.getId())
                .setImpactSummaryJson(impactSummaryJson)
                .setImpactRecalculatedAt(occurredAt)
                .setImpactRecalculationHash(impactRecalculationHash)
                .setEvidenceHash(buildChangeEvidenceHash(request.getControlledObjectType(), request.getControlledObjectId(),
                        request.getCurrentVersion(), request.getTargetVersion(), request.getChangeType(),
                        request.getChangeStatus(), request.getReason(), request.getDiffSnapshotJson(),
                        impactSummaryJson, impactRecalculationHash, idempotencyKey)));
        MesProEdhrUnifiedChangeRequestDO updated = requireChangeRequest(request.getId());
        recordUnifiedChangeEvent(updated, EVENT_TYPE_IMPACT_RECALCULATE, request.getChangeStatus(),
                request.getChangeStatus(), actorUserId, "影响范围复算", null, idempotencyKey, occurredAt);
        return toResp(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrUnifiedChangeRespVO approve(MesProEdhrUnifiedChangeApproveReqVO reqVO) {
        MesProEdhrUnifiedChangeRequestDO request = requireChangeRequest(reqVO.getChangeRequestId());
        requireStatus(request, STATUS_SUBMITTED);
        String approvalOpinion = requireReason(reqVO.getApprovalOpinion());
        String signoffEvidenceHash = requireSignoffEvidence(reqVO.getSignoffEvidenceHash());
        String idempotencyKey = requireIdempotencyKey(reqVO.getIdempotencyKey());
        String impactSummaryJson = requireImpactScope(request.getImpactSummaryJson());
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        String impactRecalculationHash = recalculateImpactScope(request.getControlledObjectType(),
                request.getControlledObjectId(), request.getCurrentVersion(), request.getTargetVersion(),
                request.getDiffSnapshotJson(), impactSummaryJson, occurredAt);

        unifiedChangeRequestMapper.updateById(new MesProEdhrUnifiedChangeRequestDO()
                .setId(request.getId())
                .setChangeStatus(STATUS_APPROVED)
                .setImpactRecalculatedAt(occurredAt)
                .setImpactRecalculationHash(impactRecalculationHash)
                .setApprovedBy(actorUserId)
                .setApprovedAt(occurredAt)
                .setApprovalOpinion(approvalOpinion)
                .setApprovalSignoffEvidenceHash(signoffEvidenceHash)
                .setEvidenceHash(buildChangeEvidenceHash(request.getControlledObjectType(), request.getControlledObjectId(),
                        request.getCurrentVersion(), request.getTargetVersion(), request.getChangeType(),
                        STATUS_APPROVED, request.getReason(), request.getDiffSnapshotJson(), impactSummaryJson,
                        impactRecalculationHash, idempotencyKey)));
        MesProEdhrUnifiedChangeRequestDO updated = requireChangeRequest(request.getId());
        recordUnifiedChangeEvent(updated, EVENT_TYPE_APPROVE, STATUS_SUBMITTED, STATUS_APPROVED, actorUserId,
                approvalOpinion, signoffEvidenceHash, idempotencyKey, occurredAt);
        return toResp(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrUnifiedChangeRespVO requestEffect(MesProEdhrUnifiedChangeEffectReqVO reqVO) {
        MesProEdhrUnifiedChangeRequestDO request = requireChangeRequest(reqVO.getChangeRequestId());
        requireStatus(request, STATUS_APPROVED);
        String signoffEvidenceHash = requireSignoffEvidence(reqVO.getSignoffEvidenceHash());
        String idempotencyKey = requireIdempotencyKey(reqVO.getIdempotencyKey());
        LocalDateTime occurredAt = now();
        Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
        String reason = StrUtil.blankToDefault(StrUtil.trim(reqVO.getReason()), "缺少模板版本生效适配器，已阻断生效申请");

        unifiedChangeRequestMapper.updateById(new MesProEdhrUnifiedChangeRequestDO()
                .setId(request.getId())
                .setChangeStatus(STATUS_EFFECT_BLOCKED)
                .setEffectRequestedBy(actorUserId)
                .setEffectRequestedAt(occurredAt)
                .setEffectSignoffEvidenceHash(signoffEvidenceHash)
                .setEvidenceHash(buildChangeEvidenceHash(request, STATUS_EFFECT_BLOCKED, idempotencyKey)));
        MesProEdhrUnifiedChangeRequestDO updated = requireChangeRequest(request.getId());
        recordUnifiedChangeEvent(updated, EVENT_TYPE_EFFECT_REQUEST_BLOCKED, STATUS_APPROVED, STATUS_EFFECT_BLOCKED,
                actorUserId, reason, signoffEvidenceHash, idempotencyKey, occurredAt);
        return toResp(updated);
    }

    private MesProEdhrUnifiedChangeRequestDO requireChangeRequest(Long id) {
        MesProEdhrUnifiedChangeRequestDO request = id == null ? null : unifiedChangeRequestMapper.selectById(id);
        if (request == null) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID);
        }
        return request;
    }

    private void requireStatus(MesProEdhrUnifiedChangeRequestDO request, String expectedStatus) {
        if (!Objects.equals(request.getChangeStatus(), expectedStatus)) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID);
        }
    }

    private String requireObjectType(String rawObjectType) {
        String objectType = StrUtil.trim(rawObjectType);
        if (!SUPPORTED_OBJECT_TYPES.contains(objectType)) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_OBJECT_TYPE_INVALID);
        }
        return objectType;
    }

    private String requireText(String rawText, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        String text = StrUtil.trim(rawText);
        if (StrUtil.isBlank(text)) {
            throw exception(errorCode);
        }
        return text;
    }

    private String requireReason(String rawReason) {
        String reason = StrUtil.trim(rawReason);
        if (StrUtil.isBlank(reason)) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_REASON_REQUIRED);
        }
        return reason;
    }

    private String requireDiffSnapshot(String rawDiffSnapshotJson) {
        String diffSnapshotJson = StrUtil.trim(rawDiffSnapshotJson);
        if (StrUtil.isBlank(diffSnapshotJson) || isEmptyJson(diffSnapshotJson)) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_DIFF_REQUIRED);
        }
        return diffSnapshotJson;
    }

    private String requireImpactScope(String rawImpactSummaryJson) {
        String impactSummaryJson = StrUtil.trim(rawImpactSummaryJson);
        if (StrUtil.isBlank(impactSummaryJson) || isEmptyJson(impactSummaryJson)) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_IMPACT_REQUIRED);
        }
        return impactSummaryJson;
    }

    private boolean isEmptyJson(String json) {
        Object parsed = JSON.parse(json);
        if (parsed instanceof Map<?, ?> parsedMap) {
            return parsedMap.isEmpty();
        }
        if (parsed instanceof Collection<?> parsedCollection) {
            return parsedCollection.isEmpty();
        }
        return false;
    }

    private String requireSignoffEvidence(String rawSignoffEvidenceHash) {
        String signoffEvidenceHash = StrUtil.trim(rawSignoffEvidenceHash);
        if (StrUtil.isBlank(signoffEvidenceHash)) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_SIGNOFF_REQUIRED);
        }
        return signoffEvidenceHash;
    }

    private String requireIdempotencyKey(String rawIdempotencyKey) {
        String idempotencyKey = StrUtil.trim(rawIdempotencyKey);
        if (StrUtil.isBlank(idempotencyKey)) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_IDEMPOTENCY_KEY_REQUIRED);
        }
        return idempotencyKey;
    }

    private void rejectOverwriteCurrentVersion(String currentVersion, String targetVersion) {
        if (Objects.equals(currentVersion, targetVersion) || OVERWRITE_MARKERS.contains(targetVersion)) {
            throw exception(PRO_EDHR_UNIFIED_CHANGE_OVERWRITE_FORBIDDEN);
        }
    }

    private String recalculateImpactScope(String controlledObjectType, String controlledObjectId, String currentVersion,
                                          String targetVersion, String diffSnapshotJson, String impactSummaryJson,
                                          LocalDateTime occurredAt) {
        requireDiffSnapshot(diffSnapshotJson);
        requireImpactScope(impactSummaryJson);
        return DigestUtil.sha256Hex(String.join("|",
                StrUtil.nullToEmpty(controlledObjectType),
                StrUtil.nullToEmpty(controlledObjectId),
                StrUtil.nullToEmpty(currentVersion),
                StrUtil.nullToEmpty(targetVersion),
                diffSnapshotJson,
                impactSummaryJson,
                String.valueOf(occurredAt)));
    }

    private void recordUnifiedChangeEvent(MesProEdhrUnifiedChangeRequestDO request,
                                          String eventType,
                                          String fromStatus,
                                          String toStatus,
                                          Long actorUserId,
                                          String reason,
                                          String signoffEvidenceHash,
                                          String idempotencyKey,
                                          LocalDateTime occurredAt) {
        MesProEdhrUnifiedChangeEventDO existing = unifiedChangeEventMapper
                .selectByChangeRequestIdAndEventTypeAndIdempotencyKey(request.getId(), eventType, idempotencyKey);
        if (existing != null) {
            return;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("changeRequestId", request.getId());
        snapshot.put("changeCode", request.getChangeCode());
        snapshot.put("controlledObjectType", request.getControlledObjectType());
        snapshot.put("controlledObjectId", request.getControlledObjectId());
        snapshot.put("controlledObjectCode", request.getControlledObjectCode());
        snapshot.put("currentVersion", request.getCurrentVersion());
        snapshot.put("targetVersion", request.getTargetVersion());
        snapshot.put("eventType", eventType);
        snapshot.put("fromStatus", fromStatus);
        snapshot.put("toStatus", toStatus);
        snapshot.put("actorUserId", actorUserId);
        snapshot.put("reason", reason);
        snapshot.put("signoffEvidenceHash", signoffEvidenceHash);
        snapshot.put("impactRecalculationHash", request.getImpactRecalculationHash());
        snapshot.put("occurredAt", occurredAt);
        String eventSnapshotJson = JSON.toJSONString(snapshot);
        String evidenceHash = DigestUtil.sha256Hex(String.join("|",
                String.valueOf(request.getId()),
                eventType,
                StrUtil.nullToEmpty(fromStatus),
                toStatus,
                String.valueOf(actorUserId),
                StrUtil.nullToEmpty(signoffEvidenceHash),
                idempotencyKey,
                eventSnapshotJson));
        unifiedChangeEventMapper.insert(MesProEdhrUnifiedChangeEventDO.builder()
                .changeRequestId(request.getId())
                .eventType(eventType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actorUserId(actorUserId)
                .reason(StrUtil.trim(reason))
                .signoffEvidenceHash(signoffEvidenceHash)
                .eventSnapshotJson(eventSnapshotJson)
                .evidenceHash(evidenceHash)
                .occurredAt(occurredAt)
                .idempotencyKey(idempotencyKey)
                .build());
    }

    private String buildChangeEvidenceHash(MesProEdhrUnifiedChangeRequestDO request, String status, String idempotencyKey) {
        return buildChangeEvidenceHash(request.getControlledObjectType(), request.getControlledObjectId(),
                request.getCurrentVersion(), request.getTargetVersion(), request.getChangeType(), status,
                request.getReason(), request.getDiffSnapshotJson(), request.getImpactSummaryJson(),
                request.getImpactRecalculationHash(), idempotencyKey);
    }

    private String buildChangeEvidenceHash(String controlledObjectType, String controlledObjectId, String currentVersion,
                                           String targetVersion, String changeType, String changeStatus, String reason,
                                           String diffSnapshotJson, String impactSummaryJson,
                                           String impactRecalculationHash, String idempotencyKey) {
        return DigestUtil.sha256Hex(String.join("|",
                StrUtil.nullToEmpty(controlledObjectType),
                StrUtil.nullToEmpty(controlledObjectId),
                StrUtil.nullToEmpty(currentVersion),
                StrUtil.nullToEmpty(targetVersion),
                StrUtil.nullToEmpty(changeType),
                StrUtil.nullToEmpty(changeStatus),
                StrUtil.nullToEmpty(reason),
                StrUtil.nullToEmpty(diffSnapshotJson),
                StrUtil.nullToEmpty(impactSummaryJson),
                StrUtil.nullToEmpty(impactRecalculationHash),
                StrUtil.nullToEmpty(idempotencyKey)));
    }

    private MesProEdhrUnifiedChangeRespVO toResp(MesProEdhrUnifiedChangeRequestDO request) {
        return BeanUtils.toBean(request, MesProEdhrUnifiedChangeRespVO.class);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
