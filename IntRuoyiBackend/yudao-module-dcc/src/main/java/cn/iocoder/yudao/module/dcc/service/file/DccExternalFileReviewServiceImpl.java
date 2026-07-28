package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCreateSignTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileReturnTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTransferTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccExternalFileReviewSubmitReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccExternalFileReviewDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccExternalFileReviewMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketMarkBoundCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketResolveCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.EXTERNAL_FILE_REVIEW_OUTPUT_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.EXTERNAL_FILE_REVIEW_REQUIRED_METADATA_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;

@Service
@Validated
public class DccExternalFileReviewServiceImpl implements DccExternalFileReviewService {

    public static final String BPM_PROCESS_DEFINITION_KEY = "dcc-external-file-review";

    @Resource
    private DccControlledFileWorkflowServiceImpl workflowService;
    @Resource
    private DccExternalFileReviewMapper externalReviewMapper;
    @Resource
    private BpmProcessDefinitionService processDefinitionService;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccUploadTicketService uploadTicketService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitExternalReview(Long userId, DccExternalFileReviewSubmitReqVO reqVO) {
        validateExternalSubmit(reqVO);
        validateActiveProcessDefinition();
        DccControlledFileSubmitReqVO submitReqVO = toControlledFileSubmitReqVO(reqVO);
        Long controlledFileId = workflowService.submitControlledFileWithProcessDefinitionKey(
                userId, submitReqVO, BPM_PROCESS_DEFINITION_KEY);
        externalReviewMapper.insert(DccExternalFileReviewDO.builder()
                .controlledFileId(controlledFileId)
                .externalSource(StrUtil.trim(reqVO.getExternalSource()))
                .externalOwner(StrUtil.trim(reqVO.getExternalOwner()))
                .reviewReason(StrUtil.trim(reqVO.getReviewReason()))
                .participantUserIds(joinIds(reqVO.getParticipantUserIds()))
                .build());
        return controlledFileId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(Long userId, Long id, DccExternalFileReviewApproveTaskReqVO reqVO) {
        DccControlledFileDO controlledFile = controlledFileMapper.selectById(id);
        boolean finalDocControlApproval = controlledFile != null
                && DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus()
                .equals(controlledFile.getStatus());
        if (reqVO.getOutputFileId() != null) {
            throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        if (finalDocControlApproval
                && (StrUtil.isBlank(reqVO.getReviewConclusion()) || StrUtil.isBlank(reqVO.getOutputUploadTicket()))) {
            throw exception(EXTERNAL_FILE_REVIEW_OUTPUT_FILE_REQUIRED);
        }
        if (StrUtil.isNotBlank(reqVO.getReviewConclusion()) || StrUtil.isNotBlank(reqVO.getOutputUploadTicket())
                || StrUtil.isNotBlank(reqVO.getConclusionComment())) {
            if (StrUtil.isBlank(reqVO.getOutputUploadTicket())) {
                throw exception(EXTERNAL_FILE_REVIEW_OUTPUT_FILE_REQUIRED);
            }
            DccUploadTicketBoundFile outputFile = uploadTicketService.resolveForBinding(
                    new DccUploadTicketResolveCommand(reqVO.getOutputUploadTicket(), userId, reqVO.getSessionId(),
                            DccControlledFileUploadTypePolicy.PURPOSE_EXTERNAL_REVIEW_OUTPUT));
            if (outputFile == null || outputFile.storageFileId() == null) {
                throw exception(CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
            }
            externalReviewMapper.updateByControlledFileId(id, DccExternalFileReviewDO.builder()
                    .reviewConclusion(StrUtil.trim(reqVO.getReviewConclusion()))
                    .conclusionComment(StrUtil.trim(reqVO.getConclusionComment()))
                    .outputFileId(outputFile.storageFileId())
                    .build());
            uploadTicketService.markBound(new DccUploadTicketMarkBoundCommand(reqVO.getOutputUploadTicket(), userId,
                    reqVO.getSessionId(), DccControlledFileUploadTypePolicy.PURPOSE_EXTERNAL_REVIEW_OUTPUT, id));
        }
        workflowService.approveTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY, false);
    }

    @Override
    public void rejectTask(Long userId, Long id, DccControlledFileRejectTaskReqVO reqVO) {
        workflowService.rejectTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY);
    }

    @Override
    public void returnTask(Long userId, Long id, DccControlledFileReturnTaskReqVO reqVO) {
        workflowService.returnTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY);
    }

    @Override
    public void transferTask(Long userId, Long id, DccControlledFileTransferTaskReqVO reqVO) {
        workflowService.transferTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY);
    }

    @Override
    public void createSignTask(Long userId, Long id, DccControlledFileCreateSignTaskReqVO reqVO) {
        workflowService.createSignTaskWithProcessDefinitionKey(userId, id, reqVO, BPM_PROCESS_DEFINITION_KEY);
    }

    public void closeExternalReview(Long controlledFileId) {
        externalReviewMapper.updateByControlledFileId(controlledFileId, DccExternalFileReviewDO.builder()
                .closedTime(LocalDateTime.now())
                .build());
    }

    private void validateActiveProcessDefinition() {
        if (processDefinitionService.getActiveProcessDefinition(BPM_PROCESS_DEFINITION_KEY) == null) {
            throw exception(EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_MISSING);
        }
    }

    private void validateExternalSubmit(DccExternalFileReviewSubmitReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getExternalSource()) || StrUtil.isBlank(reqVO.getExternalOwner())
                || StrUtil.isBlank(reqVO.getReviewReason()) || CollUtil.isEmpty(reqVO.getParticipantUserIds())) {
            throw exception(EXTERNAL_FILE_REVIEW_REQUIRED_METADATA_MISSING);
        }
    }

    private DccControlledFileSubmitReqVO toControlledFileSubmitReqVO(DccExternalFileReviewSubmitReqVO reqVO) {
        DccControlledFileSubmitReqVO submitReqVO = new DccControlledFileSubmitReqVO();
        submitReqVO.setCategoryId(reqVO.getCategoryId());
        submitReqVO.setSessionId(reqVO.getSessionId());
        submitReqVO.setOriginalUploadTicket(reqVO.getOriginalUploadTicket());
        submitReqVO.setSourceUploadTicket(reqVO.getSourceUploadTicket());
        submitReqVO.setSourceFileName(reqVO.getSourceFileName());
        submitReqVO.setDrawingPdfUploadTicket(reqVO.getDrawingPdfUploadTicket());
        submitReqVO.setProductMasterId(null);
        submitReqVO.setProductCode(reqVO.getProductCode());
        submitReqVO.setDccProjectCodeId(reqVO.getDccProjectCodeId());
        submitReqVO.setNeedTraining(Boolean.FALSE);
        submitReqVO.setProcessType(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode());
        submitReqVO.setChangeType(DccControlledFileChangeTypeEnum.NEW.getCode());
        submitReqVO.setSelectedSignoffUserIds(reqVO.getParticipantUserIds());
        submitReqVO.setFileName(reqVO.getFileName());
        submitReqVO.setFileNumber(reqVO.getFileNumber());
        submitReqVO.setDirectoryId(reqVO.getDirectoryId());
        submitReqVO.setVersionNo(reqVO.getVersionNo());
        submitReqVO.setEffectiveDate(reqVO.getEffectiveDate());
        submitReqVO.setRemark(reqVO.getRemark());
        return submitReqVO;
    }

    private String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
