package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelInstancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelInstanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskMarkFailedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintExportAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintHistoryCopyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintHistoryCopyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintHistoryExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReprintApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReprintRequestRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrLabelTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintExportAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintHistoryCopyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintPolicyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReprintRequestDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrLabelInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrLabelTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPrintExportAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPrintEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPrintHistoryCopyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPrintPolicyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPrintTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReprintRequestMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_LABEL_ACTIVE_TEMPLATE_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_LABEL_PREVIEW_FIELD_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_LABEL_TEMPLATE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_LABEL_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_LABEL_TEMPLATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_CONFIRMATION_EVIDENCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_EXPORT_FILTER_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_EXPORT_IDEMPOTENCY_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_FAILURE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_ORIGINAL_TASK_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_POLICY_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_POLICY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_POLICY_SCOPE_ACTIVE_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_POLICY_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_REPRINT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_TASK_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_VOID_COPY_SOURCE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_PRINT_VOID_COPY_WATERMARK_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_REPRINT_LIMIT_EXCEEDED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintErrorCodeConstants.PRO_EDHR_REPRINT_REASON_INVALID;

@Service
@Validated
public class MesProEdhrLabelPrintServiceImpl implements MesProEdhrLabelPrintService {

    private static final String TEMPLATE_STATUS_DRAFT = "DRAFT";
    private static final String TEMPLATE_STATUS_ACTIVE = "ACTIVE";
    private static final String POLICY_STATUS_DRAFT = "DRAFT";
    private static final String POLICY_STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String STATUS_SUCCESS_CONFIRMED = "SUCCESS_CONFIRMED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_VOID_RESTRICTED = "VOID_RESTRICTED";
    private static final String CONFIRM_STATUS_NOT_CONFIRMED = "NOT_CONFIRMED";
    private static final String CONFIRM_STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String CONFIRM_STATUS_SUCCESS_CONFIRMED = "SUCCESS_CONFIRMED";
    private static final String CONFIRM_STATUS_FAILED = "FAILED";
    private static final String EVENT_TYPE_PRINT_REQUESTED = "PRINT_REQUESTED";
    private static final String EVENT_TYPE_PRINT_REPRINT_REQUESTED = "PRINT_REPRINT_REQUESTED";
    private static final String EVENT_TYPE_PRINT_MARK_FAILED = "PRINT_MARK_FAILED";
    private static final String EVENT_TYPE_PRINT_CONFIRM_SUCCESS = "PRINT_CONFIRM_SUCCESS";
    private static final String EVENT_TYPE_PRINT_POLICY_CREATED = "PRINT_POLICY_CREATED";
    private static final String EVENT_TYPE_PRINT_POLICY_ACTIVATED = "PRINT_POLICY_ACTIVATED";
    private static final String EVENT_TYPE_PRINT_REPRINT_POLICY_ACCEPTED = "PRINT_REPRINT_POLICY_ACCEPTED";
    private static final String EVENT_TYPE_PRINT_VOID_HISTORY_COPY_CREATED = "PRINT_VOID_HISTORY_COPY_CREATED";
    private static final String EVENT_TYPE_PRINT_HISTORY_EXPORTED = "PRINT_HISTORY_EXPORTED";
    private static final String EVENT_RESULT_SUCCESS = "SUCCESS";
    private static final String REPRINT_STATUS_REQUESTED = "REQUESTED";
    private static final String HISTORY_COPY_STATUS_VOID_HISTORY_COPY = "VOID_HISTORY_COPY";
    private static final String EXPORT_STATUS_RECORDED = "EXPORT_RECORDED";

    @Resource
    private MesProEdhrLabelTemplateMapper labelTemplateMapper;
    @Resource
    private MesProEdhrLabelInstanceMapper labelInstanceMapper;
    @Resource
    private MesProEdhrPrintTaskMapper printTaskMapper;
    @Resource
    private MesProEdhrPrintEventMapper printEventMapper;
    @Resource
    private MesProEdhrPrintPolicyMapper printPolicyMapper;
    @Resource
    private MesProEdhrReprintRequestMapper reprintRequestMapper;
    @Resource
    private MesProEdhrPrintHistoryCopyMapper printHistoryCopyMapper;
    @Resource
    private MesProEdhrPrintExportAuditMapper printExportAuditMapper;

    @Override
    public PageResult<MesProEdhrLabelTemplateRespVO> getLabelTemplatePage(MesProEdhrLabelTemplatePageReqVO reqVO) {
        return BeanUtils.toBean(labelTemplateMapper.selectPage(reqVO), MesProEdhrLabelTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrLabelTemplateRespVO createLabelTemplate(MesProEdhrLabelTemplateCreateReqVO reqVO) {
        if (labelTemplateMapper.selectByTemplateCode(reqVO.getTemplateCode()) != null) {
            throw exception(PRO_EDHR_LABEL_TEMPLATE_CODE_DUPLICATE);
        }
        MesProEdhrLabelTemplateDO template = new MesProEdhrLabelTemplateDO()
                .setTemplateCode(StrUtil.trim(reqVO.getTemplateCode()))
                .setTemplateName(StrUtil.trim(reqVO.getTemplateName()))
                .setTemplateVersion(StrUtil.trim(reqVO.getTemplateVersion()))
                .setBusinessObjectType(StrUtil.trim(reqVO.getBusinessObjectType()))
                .setFieldModelJson(StrUtil.trim(reqVO.getFieldModelJson()))
                .setLayoutJson(StrUtil.trim(reqVO.getLayoutJson()))
                .setParserVersion(StrUtil.trim(reqVO.getParserVersion()))
                .setWatermarkTemplate(StrUtil.emptyToNull(StrUtil.trim(reqVO.getWatermarkTemplate())))
                .setStatus(TEMPLATE_STATUS_DRAFT)
                .setRemark(reqVO.getRemark());
        labelTemplateMapper.insert(template);
        return BeanUtils.toBean(template, MesProEdhrLabelTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrLabelTemplateRespVO activateLabelTemplate(MesProEdhrLabelTemplateActivateReqVO reqVO) {
        MesProEdhrLabelTemplateDO template = requireLabelTemplate(reqVO.getId());
        if (!TEMPLATE_STATUS_DRAFT.equals(template.getStatus()) && !TEMPLATE_STATUS_ACTIVE.equals(template.getStatus())) {
            throw exception(PRO_EDHR_LABEL_TEMPLATE_STATUS_INVALID);
        }
        MesProEdhrLabelTemplateDO active = labelTemplateMapper.selectActiveTemplate(template.getBusinessObjectType());
        if (active != null && !Objects.equals(active.getId(), template.getId())) {
            throw exception(PRO_EDHR_LABEL_ACTIVE_TEMPLATE_EXISTS);
        }
        template.setStatus(TEMPLATE_STATUS_ACTIVE).setActiveAt(now());
        labelTemplateMapper.updateById(template);
        return BeanUtils.toBean(template, MesProEdhrLabelTemplateRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrLabelInstanceRespVO> getLabelPage(MesProEdhrLabelInstancePageReqVO reqVO) {
        return BeanUtils.toBean(labelInstanceMapper.selectPage(reqVO), MesProEdhrLabelInstanceRespVO.class);
    }

    @Override
    public MesProEdhrLabelPreviewRespVO previewLabel(MesProEdhrLabelPreviewReqVO reqVO) {
        MesProEdhrLabelTemplateDO template = requireActiveLabelTemplate(reqVO.getTemplateId());
        if (StrUtil.isBlank(reqVO.getBusinessObjectPayloadJson())) {
            throw exception(PRO_EDHR_LABEL_PREVIEW_FIELD_MISSING);
        }
        String snapshot = JsonUtils.toJsonString(Map.of(
                "templateCode", template.getTemplateCode(),
                "templateVersion", template.getTemplateVersion(),
                "parserVersion", template.getParserVersion(),
                "businessObjectCode", reqVO.getBusinessObjectCode(),
                "businessObjectPayloadJson", reqVO.getBusinessObjectPayloadJson()));
        return new MesProEdhrLabelPreviewRespVO()
                .setTemplateId(template.getId())
                .setTemplateCode(template.getTemplateCode())
                .setTemplateVersion(template.getTemplateVersion())
                .setBusinessType(reqVO.getBusinessType())
                .setBusinessObjectId(reqVO.getBusinessObjectId())
                .setBusinessObjectCode(reqVO.getBusinessObjectCode())
                .setParserVersion(template.getParserVersion())
                .setRenderSnapshotJson(snapshot);
    }

    @Override
    public PageResult<MesProEdhrPrintTaskRespVO> getPrintTaskPage(MesProEdhrPrintTaskPageReqVO reqVO) {
        return BeanUtils.toBean(printTaskMapper.selectPage(reqVO), MesProEdhrPrintTaskRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrPrintTaskRespVO createPrintTask(MesProEdhrPrintTaskCreateReqVO reqVO) {
        requireReprintReason(reqVO.getIsReprint(), reqVO.getReprintReason());
        requireOriginalPrintTask(reqVO.getIsReprint(), reqVO.getOriginalPrintTaskId());
        MesProEdhrPrintTaskDO existing = printTaskMapper.selectByIdempotencyKey(reqVO.getIdempotencyKey());
        if (existing != null) {
            return BeanUtils.toBean(existing, MesProEdhrPrintTaskRespVO.class);
        }
        LocalDateTime requestedAt = now();
        boolean printCountDeducted = false;
        MesProEdhrPrintTaskDO printTask = new MesProEdhrPrintTaskDO()
                .setTaskCode(buildTaskCode(reqVO.getSourceType(), reqVO.getSourceObjectId(), reqVO.getIdempotencyKey()))
                .setSourceType(StrUtil.trim(reqVO.getSourceType()))
                .setSourceObjectId(reqVO.getSourceObjectId())
                .setSourceObjectCode(StrUtil.trim(reqVO.getSourceObjectCode()))
                .setTemplateType(StrUtil.trim(reqVO.getTemplateType()))
                .setTemplateId(reqVO.getTemplateId())
                .setTemplateCode(StrUtil.trim(reqVO.getTemplateCode()))
                .setLabelInstanceId(reqVO.getLabelInstanceId())
                .setTravelerId(reqVO.getTravelerId())
                .setStatus(STATUS_WAITING)
                .setPrintConfirmStatus(CONFIRM_STATUS_NOT_CONFIRMED)
                .setIsReprint(Boolean.TRUE.equals(reqVO.getIsReprint()))
                .setOriginalPrintTaskId(reqVO.getOriginalPrintTaskId())
                .setReprintReason(StrUtil.emptyToNull(StrUtil.trim(reqVO.getReprintReason())))
                .setWatermarkText(StrUtil.emptyToNull(StrUtil.trim(reqVO.getWatermarkText())))
                .setIdempotencyKey(StrUtil.trim(reqVO.getIdempotencyKey()))
                .setPrintCountDeducted(printCountDeducted)
                .setRequestedBy(SecurityFrameworkUtils.getLoginUserId())
                .setRequestedAt(requestedAt);
        printTaskMapper.insert(printTask);
        recordPrintEvent(printTask,
                Boolean.TRUE.equals(printTask.getIsReprint()) ? EVENT_TYPE_PRINT_REPRINT_REQUESTED : EVENT_TYPE_PRINT_REQUESTED,
                EVENT_RESULT_SUCCESS, null, null);
        return BeanUtils.toBean(printTask, MesProEdhrPrintTaskRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrPrintTaskRespVO markPrintTaskFailed(MesProEdhrPrintTaskMarkFailedReqVO reqVO) {
        requireFailureReason(reqVO.getFailureReason());
        MesProEdhrPrintTaskDO printTask = requirePrintTaskForUpdate(reqVO.getId());
        printTask.setStatus(STATUS_FAILED)
                .setPrintConfirmStatus(CONFIRM_STATUS_FAILED)
                .setFailureReason(StrUtil.trim(reqVO.getFailureReason()));
        printTaskMapper.updateById(printTask);
        recordPrintEvent(printTask, EVENT_TYPE_PRINT_MARK_FAILED, EVENT_RESULT_SUCCESS,
                printTask.getFailureReason(), null);
        return BeanUtils.toBean(printTask, MesProEdhrPrintTaskRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrPrintTaskRespVO confirmPrintTask(MesProEdhrPrintTaskConfirmReqVO reqVO) {
        requireConfirmationEvidence(reqVO.getConfirmationEvidenceHash());
        MesProEdhrPrintTaskDO printTask = requirePrintTaskForUpdate(reqVO.getId());
        if (STATUS_FAILED.equals(printTask.getStatus())) {
            throw exception(PRO_EDHR_PRINT_TASK_STATUS_INVALID);
        }
        printTask.setStatus(STATUS_SUCCESS_CONFIRMED)
                .setPrintConfirmStatus(CONFIRM_STATUS_SUCCESS_CONFIRMED)
                .setPrintCountDeducted(true)
                .setConfirmedBy(SecurityFrameworkUtils.getLoginUserId())
                .setConfirmedAt(now())
                .setConfirmationEvidenceHash(StrUtil.trim(reqVO.getConfirmationEvidenceHash()));
        printTaskMapper.updateById(printTask);
        recordPrintEvent(printTask, EVENT_TYPE_PRINT_CONFIRM_SUCCESS, EVENT_RESULT_SUCCESS,
                null, printTask.getConfirmationEvidenceHash());
        return BeanUtils.toBean(printTask, MesProEdhrPrintTaskRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrPrintPolicyRespVO> getPrintPolicyPage(MesProEdhrPrintPolicyPageReqVO reqVO) {
        return BeanUtils.toBean(printPolicyMapper.selectPage(reqVO), MesProEdhrPrintPolicyRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrPrintPolicyRespVO createPrintPolicy(MesProEdhrPrintPolicyCreateReqVO reqVO) {
        if (printPolicyMapper.selectByPolicyCode(reqVO.getPolicyCode()) != null) {
            throw exception(PRO_EDHR_PRINT_POLICY_CODE_DUPLICATE);
        }
        MesProEdhrPrintPolicyDO policy = new MesProEdhrPrintPolicyDO()
                .setPolicyCode(StrUtil.trim(reqVO.getPolicyCode()))
                .setPolicyName(StrUtil.trim(reqVO.getPolicyName()))
                .setBusinessType(StrUtil.trim(reqVO.getBusinessType()))
                .setTemplateType(StrUtil.trim(reqVO.getTemplateType()))
                .setFirstPrintLimit(reqVO.getFirstPrintLimit())
                .setReprintLimit(reqVO.getReprintLimit())
                .setReasonDictJson(StrUtil.trim(reqVO.getReasonDictJson()))
                .setWatermarkTemplate(StrUtil.trim(reqVO.getWatermarkTemplate()))
                .setVoidCopyWatermark(StrUtil.trim(reqVO.getVoidCopyWatermark()))
                .setStatus(POLICY_STATUS_DRAFT)
                .setRemark(reqVO.getRemark());
        requireVoidWatermark(policy);
        printPolicyMapper.insert(policy);
        String eventType = EVENT_TYPE_PRINT_POLICY_CREATED;
        policy.setRemark(StrUtil.emptyToDefault(policy.getRemark(), eventType));
        return BeanUtils.toBean(policy, MesProEdhrPrintPolicyRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrPrintPolicyRespVO activatePrintPolicy(MesProEdhrPrintPolicyActivateReqVO reqVO) {
        MesProEdhrPrintPolicyDO policy = requirePrintPolicy(reqVO.getId());
        if (!POLICY_STATUS_DRAFT.equals(policy.getStatus()) && !POLICY_STATUS_ACTIVE.equals(policy.getStatus())) {
            throw exception(PRO_EDHR_PRINT_POLICY_STATUS_INVALID);
        }
        MesProEdhrPrintPolicyDO active = printPolicyMapper.selectActivePolicy(policy.getBusinessType(), policy.getTemplateType());
        if (active != null && !Objects.equals(active.getId(), policy.getId())) {
            throw exception(PRO_EDHR_PRINT_POLICY_SCOPE_ACTIVE_EXISTS);
        }
        String eventType = EVENT_TYPE_PRINT_POLICY_ACTIVATED;
        policy.setStatus(POLICY_STATUS_ACTIVE)
                .setActiveAt(now())
                .setRemark(StrUtil.emptyToDefault(policy.getRemark(), eventType));
        printPolicyMapper.updateById(policy);
        return BeanUtils.toBean(policy, MesProEdhrPrintPolicyRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrReprintRequestRespVO applyReprint(MesProEdhrReprintApplyReqVO reqVO) {
        MesProEdhrReprintRequestDO existing = reprintRequestMapper.selectByIdempotencyKey(reqVO.getIdempotencyKey());
        if (existing != null) {
            return BeanUtils.toBean(existing, MesProEdhrReprintRequestRespVO.class);
        }
        MesProEdhrPrintTaskDO originalPrintTask = requirePrintTaskForUpdate(reqVO.getOriginalPrintTaskId());
        MesProEdhrPrintPolicyDO policy = requireActivePrintPolicy(
                originalPrintTask.getSourceType(), originalPrintTask.getTemplateType());
        requireReasonInPolicy(policy, reqVO.getReprintReasonCode());
        int usedReprintCount = requireReprintLimit(originalPrintTask.getId(), policy);
        String watermarkText = buildWatermark(policy.getWatermarkTemplate(), reqVO.getReprintReasonCode(), originalPrintTask);
        String reprintTaskIdempotency = "REPRINT:" + StrUtil.trim(reqVO.getIdempotencyKey());
        MesProEdhrPrintTaskDO printTask = new MesProEdhrPrintTaskDO()
                .setTaskCode(buildTaskCode(originalPrintTask.getSourceType(), originalPrintTask.getSourceObjectId(), reprintTaskIdempotency))
                .setSourceType(originalPrintTask.getSourceType())
                .setSourceObjectId(originalPrintTask.getSourceObjectId())
                .setSourceObjectCode(originalPrintTask.getSourceObjectCode())
                .setTemplateType(originalPrintTask.getTemplateType())
                .setTemplateId(originalPrintTask.getTemplateId())
                .setTemplateCode(originalPrintTask.getTemplateCode())
                .setLabelInstanceId(originalPrintTask.getLabelInstanceId())
                .setTravelerId(originalPrintTask.getTravelerId())
                .setStatus(STATUS_WAITING)
                .setPrintConfirmStatus(CONFIRM_STATUS_NOT_CONFIRMED)
                .setIsReprint(true)
                .setOriginalPrintTaskId(originalPrintTask.getId())
                .setReprintReason(StrUtil.trim(reqVO.getReprintReason()))
                .setWatermarkText(watermarkText)
                .setIdempotencyKey(reprintTaskIdempotency)
                .setPrintCountDeducted(false)
                .setRequestedBy(SecurityFrameworkUtils.getLoginUserId())
                .setRequestedAt(now());
        printTaskMapper.insert(printTask);
        MesProEdhrReprintRequestDO reprintRequest = new MesProEdhrReprintRequestDO()
                .setRequestCode(buildRequestCode("EDHR-RPT", originalPrintTask.getId(), reqVO.getIdempotencyKey()))
                .setPrintTaskId(printTask.getId())
                .setOriginalPrintTaskId(originalPrintTask.getId())
                .setReprintReasonCode(StrUtil.trim(reqVO.getReprintReasonCode()))
                .setReprintReason(StrUtil.trim(reqVO.getReprintReason()))
                .setUsedReprintCount(usedReprintCount + 1)
                .setReprintLimit(policy.getReprintLimit())
                .setWatermarkText(watermarkText)
                .setStatus(REPRINT_STATUS_REQUESTED)
                .setIdempotencyKey(StrUtil.trim(reqVO.getIdempotencyKey()));
        reprintRequestMapper.insert(reprintRequest);
        recordPrintEvent(printTask, EVENT_TYPE_PRINT_REPRINT_POLICY_ACCEPTED, EVENT_RESULT_SUCCESS,
                null, buildEvidenceHash(EVENT_TYPE_PRINT_REPRINT_POLICY_ACCEPTED, reprintRequest.getRequestCode()));
        return BeanUtils.toBean(reprintRequest, MesProEdhrReprintRequestRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrPrintHistoryCopyRespVO createVoidHistoryCopy(MesProEdhrPrintHistoryCopyReqVO reqVO) {
        MesProEdhrPrintHistoryCopyDO existing = printHistoryCopyMapper.selectByIdempotencyKey(reqVO.getIdempotencyKey());
        if (existing != null) {
            return BeanUtils.toBean(existing, MesProEdhrPrintHistoryCopyRespVO.class);
        }
        MesProEdhrPrintTaskDO sourceTask = requirePrintTaskForUpdate(reqVO.getSourcePrintTaskId());
        requireVoidRestrictedSource(sourceTask);
        MesProEdhrPrintPolicyDO policy = requireActivePrintPolicy(sourceTask.getSourceType(), sourceTask.getTemplateType());
        requireVoidWatermark(policy);
        String evidenceHash = buildEvidenceHash(EVENT_TYPE_PRINT_VOID_HISTORY_COPY_CREATED,
                sourceTask.getTaskCode(), reqVO.getSourceObjectType(), reqVO.getSourceObjectCode(), reqVO.getCopyReason());
        MesProEdhrPrintHistoryCopyDO historyCopy = new MesProEdhrPrintHistoryCopyDO()
                .setCopyCode(buildRequestCode("EDHR-HCP", sourceTask.getId(), reqVO.getIdempotencyKey()))
                .setSourcePrintTaskId(sourceTask.getId())
                .setSourceObjectType(StrUtil.trim(reqVO.getSourceObjectType()))
                .setSourceObjectCode(StrUtil.trim(reqVO.getSourceObjectCode()))
                .setCopyReason(StrUtil.trim(reqVO.getCopyReason()))
                .setWatermarkText(policy.getVoidCopyWatermark())
                .setEvidenceHash(evidenceHash)
                .setIdempotencyKey(StrUtil.trim(reqVO.getIdempotencyKey()));
        printHistoryCopyMapper.insert(historyCopy);
        String status = HISTORY_COPY_STATUS_VOID_HISTORY_COPY;
        recordPrintEvent(sourceTask, EVENT_TYPE_PRINT_VOID_HISTORY_COPY_CREATED, status,
                null, evidenceHash);
        return BeanUtils.toBean(historyCopy, MesProEdhrPrintHistoryCopyRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrPrintExportAuditRespVO exportPrintHistory(MesProEdhrPrintHistoryExportReqVO reqVO) {
        requireExportIdempotency(reqVO.getIdempotencyKey());
        if (StrUtil.isBlank(reqVO.getFilterSnapshotJson())) {
            throw exception(PRO_EDHR_PRINT_EXPORT_FILTER_REQUIRED);
        }
        MesProEdhrPrintExportAuditDO existing = printExportAuditMapper.selectByIdempotencyKey(reqVO.getIdempotencyKey());
        if (existing != null) {
            return BeanUtils.toBean(existing, MesProEdhrPrintExportAuditRespVO.class);
        }
        String evidenceHash = buildEvidenceHash(EVENT_TYPE_PRINT_HISTORY_EXPORTED, reqVO.getFilterSnapshotJson());
        MesProEdhrPrintExportAuditDO exportAudit = new MesProEdhrPrintExportAuditDO()
                .setExportCode(buildRequestCode("EDHR-EXP", SecurityFrameworkUtils.getLoginUserId(), reqVO.getIdempotencyKey()))
                .setFilterSnapshotJson(StrUtil.trim(reqVO.getFilterSnapshotJson()))
                .setResultStatus(EXPORT_STATUS_RECORDED)
                .setEvidenceHash(evidenceHash)
                .setIdempotencyKey(StrUtil.trim(reqVO.getIdempotencyKey()))
                .setExportedBy(SecurityFrameworkUtils.getLoginUserId())
                .setExportedAt(now());
        printExportAuditMapper.insert(exportAudit);
        return BeanUtils.toBean(exportAudit, MesProEdhrPrintExportAuditRespVO.class);
    }

    private MesProEdhrPrintPolicyDO requirePrintPolicy(Long id) {
        MesProEdhrPrintPolicyDO policy = id == null ? null : printPolicyMapper.selectById(id);
        if (policy == null) {
            throw exception(PRO_EDHR_PRINT_POLICY_NOT_EXISTS);
        }
        return policy;
    }

    private MesProEdhrPrintPolicyDO requireActivePrintPolicy(String businessType, String templateType) {
        MesProEdhrPrintPolicyDO policy = printPolicyMapper.selectActivePolicy(businessType, templateType);
        if (policy == null || !POLICY_STATUS_ACTIVE.equals(policy.getStatus())) {
            throw exception(PRO_EDHR_PRINT_POLICY_NOT_EXISTS);
        }
        return policy;
    }

    private void requireReasonInPolicy(MesProEdhrPrintPolicyDO policy, String reasonCode) {
        String reasonDictJson = StrUtil.trim(policy.getReasonDictJson());
        String trimmedReasonCode = StrUtil.trim(reasonCode);
        boolean jsonLike = StrUtil.startWith(reasonDictJson, "{") || StrUtil.startWith(reasonDictJson, "[");
        if (!jsonLike || StrUtil.isBlank(trimmedReasonCode)
                || !StrUtil.contains(reasonDictJson, "\"" + trimmedReasonCode + "\"")) {
            throw exception(PRO_EDHR_REPRINT_REASON_INVALID);
        }
    }

    private int requireReprintLimit(Long originalPrintTaskId, MesProEdhrPrintPolicyDO policy) {
        int usedReprintCount = Math.toIntExact(reprintRequestMapper.countByOriginalPrintTaskId(originalPrintTaskId));
        if (usedReprintCount >= policy.getReprintLimit()) {
            throw exception(PRO_EDHR_REPRINT_LIMIT_EXCEEDED);
        }
        return usedReprintCount;
    }

    private void requireVoidWatermark(MesProEdhrPrintPolicyDO policy) {
        if (policy == null || StrUtil.isBlank(policy.getVoidCopyWatermark())) {
            throw exception(PRO_EDHR_PRINT_VOID_COPY_WATERMARK_REQUIRED);
        }
    }

    private void requireVoidRestrictedSource(MesProEdhrPrintTaskDO printTask) {
        if (printTask == null || !STATUS_VOID_RESTRICTED.equals(printTask.getStatus())) {
            throw exception(PRO_EDHR_PRINT_VOID_COPY_SOURCE_INVALID);
        }
    }

    private void requireExportIdempotency(String idempotencyKey) {
        if (StrUtil.isBlank(idempotencyKey)) {
            throw exception(PRO_EDHR_PRINT_EXPORT_IDEMPOTENCY_REQUIRED);
        }
    }

    private MesProEdhrLabelTemplateDO requireLabelTemplate(Long id) {
        MesProEdhrLabelTemplateDO template = id == null ? null : labelTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(PRO_EDHR_LABEL_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private MesProEdhrLabelTemplateDO requireActiveLabelTemplate(Long id) {
        MesProEdhrLabelTemplateDO template = requireLabelTemplate(id);
        if (!TEMPLATE_STATUS_ACTIVE.equals(template.getStatus())) {
            throw exception(PRO_EDHR_LABEL_TEMPLATE_STATUS_INVALID);
        }
        return template;
    }

    private MesProEdhrPrintTaskDO requirePrintTaskForUpdate(Long id) {
        MesProEdhrPrintTaskDO printTask = id == null ? null : printTaskMapper.selectByIdForUpdate(id);
        if (printTask == null) {
            throw exception(PRO_EDHR_PRINT_TASK_NOT_EXISTS);
        }
        return printTask;
    }

    private void requireReprintReason(Boolean isReprint, String reprintReason) {
        if (Boolean.TRUE.equals(isReprint) && StrUtil.isBlank(reprintReason)) {
            throw exception(PRO_EDHR_PRINT_REPRINT_REASON_REQUIRED);
        }
    }

    private void requireOriginalPrintTask(Boolean isReprint, Long originalPrintTaskId) {
        if (!Boolean.TRUE.equals(isReprint)) {
            return;
        }
        if (originalPrintTaskId == null) {
            throw exception(PRO_EDHR_PRINT_ORIGINAL_TASK_REQUIRED);
        }
        if (printTaskMapper.selectById(originalPrintTaskId) == null) {
            throw exception(PRO_EDHR_PRINT_TASK_NOT_EXISTS);
        }
    }

    private void requireFailureReason(String failureReason) {
        if (StrUtil.isBlank(failureReason)) {
            throw exception(PRO_EDHR_PRINT_FAILURE_REASON_REQUIRED);
        }
    }

    private void requireConfirmationEvidence(String confirmationEvidenceHash) {
        if (StrUtil.isBlank(confirmationEvidenceHash)) {
            throw exception(PRO_EDHR_PRINT_CONFIRMATION_EVIDENCE_REQUIRED);
        }
    }

    private void recordPrintEvent(MesProEdhrPrintTaskDO printTask, String eventType, String resultStatus,
                                  String failureReason, String evidenceHash) {
        printEventMapper.insert(new MesProEdhrPrintEventDO()
                .setPrintTaskId(printTask.getId())
                .setTaskCode(printTask.getTaskCode())
                .setEventType(eventType)
                .setResultStatus(resultStatus)
                .setFailureReason(failureReason)
                .setOperatorUserId(SecurityFrameworkUtils.getLoginUserId())
                .setOperatorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setOccurredAt(now())
                .setEvidenceHash(evidenceHash)
                .setMetadataJson(JsonUtils.toJsonString(Map.of(
                        "status", value(printTask.getStatus()),
                        "printConfirmStatus", value(printTask.getPrintConfirmStatus())))));
    }

    private String buildTaskCode(String sourceType, Long sourceObjectId, String idempotencyKey) {
        String hash = MesProBatchRecordExecutionFieldAuditHasher.sha256(String.join("|",
                "EDHR_PRINT_TASK_V1",
                value(sourceType),
                value(sourceObjectId),
                value(idempotencyKey)));
        return "EDHR-PRT-" + value(sourceObjectId) + "-" + hash.substring(0, 8).toUpperCase();
    }

    private String buildRequestCode(String prefix, Object sourceId, String idempotencyKey) {
        String hash = MesProBatchRecordExecutionFieldAuditHasher.sha256(String.join("|",
                prefix,
                value(sourceId),
                value(idempotencyKey)));
        return prefix + "-" + value(sourceId) + "-" + hash.substring(0, 8).toUpperCase();
    }

    private String buildWatermark(String template, String reasonCode, MesProEdhrPrintTaskDO printTask) {
        return StrUtil.trim(template)
                .replace("{reasonCode}", value(reasonCode))
                .replace("{taskCode}", value(printTask.getTaskCode()))
                .replace("{sourceObjectCode}", value(printTask.getSourceObjectCode()));
    }

    private String buildEvidenceHash(String eventType, Object... values) {
        StringBuilder builder = new StringBuilder(eventType);
        for (Object item : values) {
            builder.append('|').append(value(item));
        }
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(builder.toString());
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
