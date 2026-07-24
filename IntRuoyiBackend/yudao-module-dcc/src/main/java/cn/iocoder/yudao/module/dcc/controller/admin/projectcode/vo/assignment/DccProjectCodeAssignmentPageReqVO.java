package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccProjectCodeAssignmentPageReqVO extends PageParam {

    private Long assigneeUserId;
    private String status;
    private String keyword;
    private LocalDateTime[] createdTime;

}
