package cn.iocoder.yudao.module.bpm.approval.service.provider;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;

import java.util.List;
import java.util.Set;

/**
 * New approval modules must implement this provider.
 * Do not create a private approval center.
 * The unified platform keeps detailRoute as the module formal page routing boundary.
 */
public interface ApprovalTaskProvider {

    ApprovalModuleCode getModuleCode();

    String getModuleName();

    String getProviderCode();

    String getProviderVersion();

    Set<ApprovalTaskViewType> getSupportedViewTypes();

    Set<ApprovalTaskCapability> getCapabilities();

    default boolean isVisibleTo(Long loginUserId) {
        return true;
    }

    PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context);

    List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context);

    default void review(ApprovalTaskReviewContext context) {
        throw new UnsupportedOperationException("APPROVAL_REVIEW_UNSUPPORTED: " + getModuleCode());
    }
}
