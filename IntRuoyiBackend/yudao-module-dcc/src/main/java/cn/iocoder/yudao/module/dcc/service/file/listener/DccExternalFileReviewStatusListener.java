package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
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

    @Override
    public String getProcessDefinitionKey() {
        return DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        Long fileId = Long.valueOf(event.getBusinessKey());
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(fileId)
                    .status(DccControlledFileStatusEnum.APPROVED.getStatus())
                    .approvedTime(LocalDateTime.now())
                    .build());
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
