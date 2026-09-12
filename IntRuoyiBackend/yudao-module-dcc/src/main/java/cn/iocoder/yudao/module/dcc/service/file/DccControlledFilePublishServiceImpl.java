package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormActionResolutionRespVO;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePublishReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpWriteOperation;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

@Service
@Validated
public class DccControlledFilePublishServiceImpl implements DccControlledFilePublishService {

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileFinalizationService finalizationService;
    @Resource
    private FormCenterRuntimeService formCenterRuntimeService;
    @Resource
    private DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver;
    @Resource
    private GxpAuditService gxpAuditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GxpWriteOperation(operationId = "dcc.controlled-file.publish")
    public FormInstanceRespVO publishControlledFile(Long userId, Long id, DccControlledFilePublishReqVO reqVO) {
        DccControlledFileDO file = requirePublishRequest(userId, id, reqVO);
        Map<String, Object> formData = buildPublishFormData(file, reqVO);
        BusinessActionContextReqVO publishContext = buildPublishContext(file, reqVO);
        FormActionResolutionRespVO resolution = formCenterRuntimeService.resolveAction(publishContext);
        if (resolution == null || StrUtil.isBlank(resolution.getApprovalMode())) {
            throw new IllegalStateException("DCC publish action policy could not be resolved");
        }
        boolean requiresBpm = Boolean.TRUE.equals(resolution.getRequiresBpm());
        FormInstanceCreateReqVO createReqVO = new FormInstanceCreateReqVO();
        createReqVO.setContext(publishContext);
        createReqVO.setIdempotencyKey(reqVO.getIdempotencyKey());
        createReqVO.setFormData(formData);
        FormInstanceRespVO draft = formCenterRuntimeService.createInstance(createReqVO, userId);

        FormInstanceSubmitReqVO submitReqVO = new FormInstanceSubmitReqVO();
        submitReqVO.setFormData(formData);
        Map<String, List<Long>> startUserSelectAssignees = Map.of();
        if (requiresBpm) {
            startUserSelectAssignees = reqVO.getStartUserSelectAssignees();
            if (startUserSelectAssignees == null || startUserSelectAssignees.isEmpty()) {
                startUserSelectAssignees = approvalRouteAssigneeResolver.resolveStartUserSelectAssignees(file, userId);
            }
        }
        submitReqVO.setStartUserSelectAssignees(startUserSelectAssignees);
        FormInstanceRespVO submitted = formCenterRuntimeService.submitInstance(draft.getId(), submitReqVO, userId);
        DccControlledFileDO afterFile = controlledFileMapper.selectById(id);
        if (afterFile == null) {
            throw new IllegalStateException("Controlled file is missing after publish action submission");
        }
        gxpAuditService.append(buildGxpAuditCommand(file, afterFile, reqVO, submitted, requiresBpm));
        return submitted;
    }

    private DccControlledFileDO requirePublishRequest(Long userId, Long id, DccControlledFilePublishReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getReason())) {
            throw new IllegalArgumentException("DCC publish reason is required");
        }
        if (StrUtil.isBlank(reqVO.getIdempotencyKey())) {
            throw new IllegalArgumentException("DCC publish idempotencyKey is required");
        }
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        finalizationService.precheckPublishControlledFile(userId, id);
        return file;
    }

    private BusinessActionContextReqVO buildPublishContext(DccControlledFileDO file,
            DccControlledFilePublishReqVO reqVO) {
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setDataDomain("DCC");
        context.setSystemCode("DCC");
        context.setObjectType("CONTROLLED_FILE");
        context.setObjectId(String.valueOf(file.getId()));
        context.setObjectVersion(file.getVersionNo());
        context.setActionCode("PUBLISH");
        context.setObjectState(file.getStatus());
        context.setProductCode(file.getProductCode());
        context.setCategoryCode(file.getCategoryId() == null ? null : String.valueOf(file.getCategoryId()));
        context.setReason(reqVO.getReason());
        return context;
    }

    private Map<String, Object> buildPublishFormData(DccControlledFileDO file, DccControlledFilePublishReqVO reqVO) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("controlledFileId", file.getId());
        formData.put("reason", reqVO.getReason());
        return formData;
    }

    private GxpAuditCommand buildGxpAuditCommand(DccControlledFileDO file,
                                                 DccControlledFileDO afterFile,
                                                 DccControlledFilePublishReqVO reqVO,
                                                 FormInstanceRespVO submitted,
                                                 boolean requiresBpm) {
        String objectVersion = StrUtil.blankToDefault(file.getVersionNo(), String.valueOf(file.getId()));
        return GxpAuditCommand.builder()
                .operationId("dcc.controlled-file.publish")
                .subjectId("CONTROLLED_FILE:" + file.getId())
                .subjectVersion(objectVersion)
                .reason(reqVO.getReason())
                .beforeState(GxpAuditStateEnvelope.builder()
                        .state(file.getStatus())
                        .objectVersion(objectVersion)
                        .canonicalJson(fileStateJson(file))
                        .build())
                .afterState(GxpAuditStateEnvelope.builder()
                        .state(requiresBpm ? "PUBLISH_APPROVAL_STARTED" : afterFile.getStatus())
                        .objectVersion(objectVersion)
                        .canonicalJson(publishApprovalStateJson(file, afterFile, submitted, requiresBpm))
                        .build())
                .idempotencyKey(reqVO.getIdempotencyKey())
                .requestId(StrUtil.blankToDefault(submitted.getBpmProcessInstanceId(), String.valueOf(submitted.getId())))
                .source("DccControlledFilePublishServiceImpl.publishControlledFile")
                .build();
    }

    private String fileStateJson(DccControlledFileDO file) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("controlledFileId", file.getId());
        payload.put("masterId", file.getMasterId());
        payload.put("fileNumber", file.getFileNumber());
        payload.put("fileName", file.getFileName());
        payload.put("versionNo", file.getVersionNo());
        payload.put("status", file.getStatus());
        payload.put("productCode", file.getProductCode());
        payload.put("categoryId", file.getCategoryId());
        return cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(payload);
    }

    private String publishApprovalStateJson(DccControlledFileDO file, DccControlledFileDO afterFile,
                                              FormInstanceRespVO submitted, boolean requiresBpm) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("controlledFileId", file.getId());
        payload.put("versionNo", file.getVersionNo());
        payload.put("fromStatus", file.getStatus());
        payload.put("approvalState", requiresBpm ? "PUBLISH_APPROVAL_STARTED" : afterFile.getStatus());
        payload.put("formInstanceId", submitted.getId());
        payload.put("formInstanceStatus", submitted.getStatus());
        payload.put("bpmProcessInstanceId", submitted.getBpmProcessInstanceId());
        return cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(payload);
    }
}
