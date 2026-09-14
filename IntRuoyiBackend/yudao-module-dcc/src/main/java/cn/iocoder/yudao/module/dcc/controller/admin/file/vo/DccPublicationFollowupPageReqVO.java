package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccPublicationFollowupPageReqVO extends PageParam {
    private String fileNumber;
    private String versionNo;
    @Pattern(regexp = "^$|PENDING|PROCESSING|READY|PARTIAL_FAILED|COMPLETED$", message = "batchStatus is invalid")
    private String batchStatus;
    @Pattern(regexp = "^$|PENDING|UNASSIGNED|IN_REVIEW|COMPLETED$", message = "taskStatus is invalid")
    private String taskStatus;
    @Pattern(regexp = "^$|PENDING|SENT|FAILED$", message = "notificationStatus is invalid")
    private String notificationStatus;
    @Pattern(regexp = "^$|[1-9]\\d{0,18}$", message = "assigneeUserId is invalid")
    private String assigneeUserId;

    @AssertTrue(message = "assigneeUserId exceeds positive Long range")
    @JsonIgnore
    public boolean isAssigneeUserIdWithinLongRange() {
        if (assigneeUserId == null || assigneeUserId.isBlank()) return true;
        try {
            return Long.parseLong(assigneeUserId) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
