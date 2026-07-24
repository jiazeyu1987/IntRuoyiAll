package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccProjectCodeAssignmentAuditPageReqVO extends PageParam {

    private Long projectCodeId;
    private Long assignmentId;
    private Long controlledFileId;
    private Long operatorUserId;
    private String fieldName;
    private String source;
    private LocalDateTime[] changedTime;

}
