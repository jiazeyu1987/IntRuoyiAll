package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledContentAdapter;
import cn.iocoder.yudao.module.dcc.service.file.DccExternalFileReviewServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DccExternalFileReviewStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccExternalFileReviewServiceImpl externalFileReviewService;
    @Resource
    private DccControlledContentAdapter platformAdapter;

    @Override
    public String getProcessDefinitionKey() {
        return DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        Long fileId = Long.valueOf(event.getBusinessKey());
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
            DccControlledFileDO file = controlledFileMapper.selectById(fileId);
            if (file == null) {
                return;
            }
            LocalDateTime approvedTime = LocalDateTime.now();
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(fileId)
                    .status(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus())
                    .approvedTime(approvedTime)
                    .build());
            file.setStatus(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus());
            file.setApprovedTime(approvedTime);
            platformAdapter.recordApprovedReadyToPublish(file, event.getActorUserId(), event.getId());
            externalFileReviewService.closeExternalReview(fileId);
            return;
        }
        if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(event.getStatus())) {
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(fileId)
                    .status(DccControlledFileStatusEnum.REJECTED.getStatus())
                    .rejectedTime(LocalDateTime.now())
                    .rejectReason(event.getReason())
                    .build());
            externalFileReviewService.closeExternalReview(fileId);
            return;
        }
        if (BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(event.getStatus())) {
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(fileId)
                    .status(DccControlledFileStatusEnum.WITHDRAWN.getStatus())
                    .rejectReason(event.getReason())
                    .build());
            externalFileReviewService.closeExternalReview(fileId);
        }
    }
}
