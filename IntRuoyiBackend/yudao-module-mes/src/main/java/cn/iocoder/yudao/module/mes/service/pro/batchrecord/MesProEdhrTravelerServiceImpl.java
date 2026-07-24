package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplateRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrTravelerEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrTravelerInstanceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrTravelerTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.sn.MesWmSnDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTravelerEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTravelerInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrTravelerTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.sn.MesWmSnMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_ACTIVE_TEMPLATE_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_ALREADY_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_BATCH_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_PROCESS_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_ROUTE_PROCESS_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_ROUTE_PROCESS_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_SN_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_SN_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_TEMPLATE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_TEMPLATE_SCOPE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerErrorCodeConstants.PRO_EDHR_TRAVELER_TEMPLATE_STATUS_INVALID;

@Service
@Validated
public class MesProEdhrTravelerServiceImpl implements MesProEdhrTravelerService {

    private static final String TEMPLATE_STATUS_DRAFT = "DRAFT";
    private static final String TEMPLATE_STATUS_ACTIVE = "ACTIVE";
    private static final String TRAVELER_STATUS_GENERATED = "GENERATED";
    private static final String PRINT_STATUS_NOT_PRINTED = "NOT_PRINTED";
    private static final String SCOPE_TYPE_BATCH_LEVEL = "BATCH_LEVEL";
    private static final String SCOPE_TYPE_SN_LEVEL = "SN_LEVEL";
    private static final String EVENT_TYPE_GENERATE = "GENERATE";
    private static final String EVENT_TYPE_GENERATE_DUPLICATE = "GENERATE_DUPLICATE";
    private static final String EVENT_RESULT_SUCCESS = "SUCCESS";
    private static final String EVENT_RESULT_BLOCKED = "BLOCKED";

    @Resource
    private MesProEdhrTravelerTemplateMapper templateMapper;
    @Resource
    private MesProEdhrTravelerInstanceMapper instanceMapper;
    @Resource
    private MesProEdhrTravelerEventMapper eventMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesWmSnMapper snMapper;

    @Override
    public PageResult<MesProEdhrTravelerTemplateRespVO> getTemplatePage(MesProEdhrTravelerTemplatePageReqVO reqVO) {
        return BeanUtils.toBean(templateMapper.selectPage(reqVO), MesProEdhrTravelerTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrTravelerTemplateRespVO createTemplate(MesProEdhrTravelerTemplateCreateReqVO reqVO) {
        if (templateMapper.selectByTemplateCode(reqVO.getTemplateCode()) != null) {
            throw exception(PRO_EDHR_TRAVELER_TEMPLATE_CODE_DUPLICATE);
        }
        MesProEdhrTravelerTemplateDO template = new MesProEdhrTravelerTemplateDO()
                .setTemplateCode(StrUtil.trim(reqVO.getTemplateCode()))
                .setTemplateName(StrUtil.trim(reqVO.getTemplateName()))
                .setTemplateVersion(StrUtil.trim(reqVO.getTemplateVersion()))
                .setStatus(TEMPLATE_STATUS_DRAFT)
                .setApplicableProductCode(StrUtil.emptyToNull(StrUtil.trim(reqVO.getApplicableProductCode())))
                .setApplicableRouteId(reqVO.getApplicableRouteId())
                .setApplicableRouteCode(StrUtil.emptyToNull(StrUtil.trim(reqVO.getApplicableRouteCode())))
                .setApplicableProcessId(normalizeApplicableProcessId(
                        reqVO.getApplicableRouteId(), reqVO.getApplicableProcessId()))
                .setApplicableProcessCode(StrUtil.emptyToNull(StrUtil.trim(reqVO.getApplicableProcessCode())))
                .setApplicableProcessName(StrUtil.emptyToNull(StrUtil.trim(reqVO.getApplicableProcessName())))
                .setRemark(reqVO.getRemark());
        templateMapper.insert(template);
        return BeanUtils.toBean(template, MesProEdhrTravelerTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrTravelerTemplateRespVO activateTemplate(MesProEdhrTravelerActivateReqVO reqVO) {
        MesProEdhrTravelerTemplateDO template = requireTemplate(reqVO.getId());
        if (!TEMPLATE_STATUS_DRAFT.equals(template.getStatus()) && !TEMPLATE_STATUS_ACTIVE.equals(template.getStatus())) {
            throw exception(PRO_EDHR_TRAVELER_TEMPLATE_STATUS_INVALID);
        }
        template.setApplicableProcessId(normalizeApplicableProcessId(
                template.getApplicableRouteId(), template.getApplicableProcessId()));
        MesProEdhrTravelerTemplateDO active = templateMapper.selectActiveTemplatesByProductAndRoute(
                        template.getApplicableProductCode(), template.getApplicableRouteId()).stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), template.getId()))
                .filter(candidate -> isSameApplicableProcessScope(
                        candidate.getApplicableProcessId(), template.getApplicableProcessId()))
                .findFirst()
                .orElse(null);
        if (active != null && !Objects.equals(active.getId(), template.getId())) {
            throw exception(PRO_EDHR_TRAVELER_ACTIVE_TEMPLATE_EXISTS);
        }
        template.setStatus(TEMPLATE_STATUS_ACTIVE).setActiveAt(now());
        templateMapper.updateById(template);
        return BeanUtils.toBean(template, MesProEdhrTravelerTemplateRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrTravelerRespVO> getPage(MesProEdhrTravelerPageReqVO reqVO) {
        return BeanUtils.toBean(instanceMapper.selectPage(reqVO), MesProEdhrTravelerRespVO.class);
    }

    @Override
    public MesProEdhrTravelerRespVO get(Long id) {
        MesProEdhrTravelerInstanceDO traveler = id == null ? null : instanceMapper.selectById(id);
        if (traveler == null) {
            throw exception(PRO_EDHR_TRAVELER_NOT_EXISTS);
        }
        return BeanUtils.toBean(traveler, MesProEdhrTravelerRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrTravelerRespVO generate(MesProEdhrTravelerGenerateReqVO reqVO) {
        MesProEdhrTravelerTemplateDO template = requireActiveTemplate(reqVO.getTemplateId());
        MesProEdhrBatchExecutionDO batch = requireBatchExecution(reqVO.getBatchExecutionId());
        MesProRouteProcessDO routeProcess = requireRouteProcess(reqVO.getRouteProcessId(), batch.getRouteId());
        MesProProcessDO process = requireProcess(routeProcess.getProcessId());
        String serialNo = StrUtil.emptyToNull(StrUtil.trim(reqVO.getSerialNo()));
        validateSerialNo(batch, serialNo);
        validateTemplateScope(template, batch, process);

        String businessKeyHash = businessKeyHash(template.getId(), batch.getId(), routeProcess.getId(), serialNo);
        MesProEdhrTravelerInstanceDO existing = instanceMapper.selectByBusinessKeyHash(businessKeyHash);
        if (existing != null) {
            recordEvent(existing, EVENT_TYPE_GENERATE_DUPLICATE, EVENT_RESULT_BLOCKED,
                    "同一业务对象已存在有效流转单", reqVO.getRequestId());
            throw exception(PRO_EDHR_TRAVELER_ALREADY_EXISTS, existing.getTravelerCode());
        }

        MesProEdhrTravelerInstanceDO traveler = buildTraveler(template, batch, routeProcess, process, serialNo,
                businessKeyHash, reqVO.getRemark());
        instanceMapper.insert(traveler);
        recordEvent(traveler, EVENT_TYPE_GENERATE, EVENT_RESULT_SUCCESS, null, reqVO.getRequestId());
        return BeanUtils.toBean(traveler, MesProEdhrTravelerRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrTravelerEventRespVO> getEventPage(MesProEdhrTravelerEventPageReqVO reqVO) {
        return BeanUtils.toBean(eventMapper.selectPage(reqVO), MesProEdhrTravelerEventRespVO.class);
    }

    private MesProEdhrTravelerTemplateDO requireTemplate(Long id) {
        MesProEdhrTravelerTemplateDO template = id == null ? null : templateMapper.selectById(id);
        if (template == null) {
            throw exception(PRO_EDHR_TRAVELER_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private MesProEdhrTravelerTemplateDO requireActiveTemplate(Long id) {
        MesProEdhrTravelerTemplateDO template = requireTemplate(id);
        if (!TEMPLATE_STATUS_ACTIVE.equals(template.getStatus())) {
            throw exception(PRO_EDHR_TRAVELER_TEMPLATE_STATUS_INVALID);
        }
        return template;
    }

    private MesProEdhrBatchExecutionDO requireBatchExecution(Long id) {
        MesProEdhrBatchExecutionDO batch = id == null ? null : batchExecutionMapper.selectById(id);
        if (batch == null) {
            throw exception(PRO_EDHR_TRAVELER_BATCH_EXECUTION_NOT_EXISTS);
        }
        return batch;
    }

    private MesProRouteProcessDO requireRouteProcess(Long routeProcessId, Long batchRouteId) {
        if (routeProcessId == null) {
            throw exception(PRO_EDHR_TRAVELER_ROUTE_PROCESS_NOT_EXISTS);
        }
        MesProRouteProcessDO routeProcess =
                routeProcessService.resolveFrozenRouteProcess(routeProcessId, batchRouteId, null);
        if (routeProcess == null) {
            throw exception(PRO_EDHR_TRAVELER_ROUTE_PROCESS_NOT_EXISTS);
        }
        if (!Objects.equals(routeProcess.getRouteId(), batchRouteId)) {
            throw exception(PRO_EDHR_TRAVELER_ROUTE_PROCESS_MISMATCH);
        }
        return routeProcess;
    }

    private MesProProcessDO requireProcess(Long processId) {
        MesProProcessDO process = processId == null ? null : processMapper.selectById(processId);
        if (process == null) {
            throw exception(PRO_EDHR_TRAVELER_PROCESS_NOT_EXISTS);
        }
        return process;
    }

    private void validateSerialNo(MesProEdhrBatchExecutionDO batch, String serialNo) {
        if (StrUtil.isBlank(serialNo)) {
            return;
        }
        MesWmSnDO sn = snMapper.selectOne(new LambdaQueryWrapperX<MesWmSnDO>()
                .eq(MesWmSnDO::getCode, serialNo));
        if (sn == null) {
            throw exception(PRO_EDHR_TRAVELER_SN_NOT_EXISTS);
        }
        if (!Objects.equals(sn.getWorkOrderId(), batch.getWorkOrderId())
                || !StrUtil.equals(sn.getBatchCode(), batch.getBatchCode())) {
            throw exception(PRO_EDHR_TRAVELER_SN_MISMATCH);
        }
    }

    private void validateTemplateScope(MesProEdhrTravelerTemplateDO template, MesProEdhrBatchExecutionDO batch,
                                       MesProProcessDO process) {
        if (StrUtil.isNotBlank(template.getApplicableProductCode())
                && !StrUtil.equals(template.getApplicableProductCode(), batch.getProductCode())) {
            throw exception(PRO_EDHR_TRAVELER_TEMPLATE_SCOPE_MISMATCH, "产品编码");
        }
        if (template.getApplicableRouteId() != null && !Objects.equals(template.getApplicableRouteId(), batch.getRouteId())) {
            throw exception(PRO_EDHR_TRAVELER_TEMPLATE_SCOPE_MISMATCH, "工艺路线");
        }
        if (template.getApplicableProcessId() != null
                && !isSameProcessIdentity(template.getApplicableProcessId(), process.getId())) {
            throw exception(PRO_EDHR_TRAVELER_TEMPLATE_SCOPE_MISMATCH, "工序");
        }
    }

    private Long normalizeApplicableProcessId(Long applicableRouteId, Long applicableProcessId) {
        if (applicableProcessId == null || applicableRouteId == null) {
            return applicableProcessId;
        }
        return routeProcessService.resolveCurrentRouteProcess(
                null, applicableRouteId, applicableProcessId).getProcessId();
    }

    private boolean isSameApplicableProcessScope(Long leftProcessId, Long rightProcessId) {
        if (leftProcessId == null || rightProcessId == null) {
            return leftProcessId == null && rightProcessId == null;
        }
        return isSameProcessIdentity(leftProcessId, rightProcessId);
    }

    private boolean isSameProcessIdentity(Long sourceProcessId, Long targetProcessId) {
        if (Objects.equals(sourceProcessId, targetProcessId)) {
            return true;
        }
        if (sourceProcessId == null || targetProcessId == null) {
            return false;
        }
        Map<Long, Long> processIdentityMap = routeProcessService.getProcessIdentityMap(List.of(targetProcessId));
        Long normalizedSourceProcessId = processIdentityMap.getOrDefault(sourceProcessId, sourceProcessId);
        Long normalizedTargetProcessId = processIdentityMap.getOrDefault(targetProcessId, targetProcessId);
        return Objects.equals(normalizedSourceProcessId, normalizedTargetProcessId);
    }

    private MesProEdhrTravelerInstanceDO buildTraveler(MesProEdhrTravelerTemplateDO template,
                                                       MesProEdhrBatchExecutionDO batch,
                                                       MesProRouteProcessDO routeProcess,
                                                       MesProProcessDO process,
                                                       String serialNo,
                                                       String businessKeyHash,
                                                       String remark) {
        LocalDateTime generatedAt = now();
        String travelerCode = "EDHR-TL-" + batch.getId() + "-" + routeProcess.getId()
                + "-" + businessKeyHash.substring(0, 8).toUpperCase();
        return new MesProEdhrTravelerInstanceDO()
                .setTravelerCode(travelerCode)
                .setTemplateId(template.getId())
                .setTemplateCode(template.getTemplateCode())
                .setTemplateVersion(template.getTemplateVersion())
                .setBatchExecutionId(batch.getId())
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setWorkOrderId(batch.getWorkOrderId())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setProductId(batch.getProductId())
                .setProductCode(batch.getProductCode())
                .setProductName(batch.getProductName())
                .setSerialNo(serialNo)
                .setScopeType(StrUtil.isBlank(serialNo) ? SCOPE_TYPE_BATCH_LEVEL : SCOPE_TYPE_SN_LEVEL)
                .setRouteId(batch.getRouteId())
                .setRouteCode(batch.getRouteCode())
                .setRouteName(batch.getRouteName())
                .setRouteProcessId(routeProcess.getId())
                .setRouteProcessSort(routeProcess.getSort())
                .setProcessId(process.getId())
                .setProcessCode(process.getCode())
                .setProcessName(process.getName())
                .setStatus(TRAVELER_STATUS_GENERATED)
                .setPrintStatus(PRINT_STATUS_NOT_PRINTED)
                .setBusinessKeyHash(businessKeyHash)
                .setGeneratedBy(SecurityFrameworkUtils.getLoginUserId())
                .setGeneratedAt(generatedAt)
                .setRemark(remark);
    }

    private String businessKeyHash(Long templateId, Long batchExecutionId, Long routeProcessId, String serialNo) {
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(String.join("|",
                "EDHR_TRAVELER_V1",
                value(templateId),
                value(batchExecutionId),
                value(routeProcessId),
                value(serialNo)));
    }

    private void recordEvent(MesProEdhrTravelerInstanceDO traveler, String eventType, String resultStatus,
                             String failureReason, String requestId) {
        eventMapper.insert(new MesProEdhrTravelerEventDO()
                .setTravelerId(traveler.getId())
                .setTravelerCode(traveler.getTravelerCode())
                .setEventType(eventType)
                .setResultStatus(resultStatus)
                .setFailureReason(failureReason)
                .setOperatorUserId(SecurityFrameworkUtils.getLoginUserId())
                .setOperatorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setOccurredAt(now())
                .setMetadataJson(StrUtil.isBlank(requestId) ? null : JsonUtils.toJsonString(Map.of("requestId", requestId))));
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
