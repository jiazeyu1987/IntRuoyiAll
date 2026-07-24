package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

import java.util.List;

public interface ApprovalCenterService {

    List<ApprovalProviderDescriptor> listProviders(Long loginUserId);

    PageResult<ApprovalTaskSummary> getTaskPage(Long loginUserId, ApprovalTaskQuery query);

    List<ApprovalTaskTimelineEntry> listTaskTimeline(Long loginUserId, ApprovalTaskTimelineQuery query);

    void reviewTask(Long loginUserId, ApprovalTaskReviewCommand command);
}
