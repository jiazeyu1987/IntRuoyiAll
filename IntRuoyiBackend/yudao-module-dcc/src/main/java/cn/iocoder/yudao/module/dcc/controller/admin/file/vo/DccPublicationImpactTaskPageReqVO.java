package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.Pattern;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccPublicationImpactTaskPageReqVO extends PageParam {
    @Pattern(regexp = "^$|PENDING|UNASSIGNED|IN_REVIEW|COMPLETED$", message = "taskStatus is invalid")
    private String taskStatus;
    @Pattern(regexp = "^$|NOT_APPLICABLE|NOT_STARTED|REVISION_LINKED|RESOLVED$",
            message = "revisionTrackingStatus is invalid")
    private String revisionTrackingStatus;
}
