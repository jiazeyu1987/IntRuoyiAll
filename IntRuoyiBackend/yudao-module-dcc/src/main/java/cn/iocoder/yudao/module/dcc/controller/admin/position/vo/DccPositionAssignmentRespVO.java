package cn.iocoder.yudao.module.dcc.controller.admin.position.vo;

import lombok.Data;

@Data
public class DccPositionAssignmentRespVO {
    private Long id;
    private Long positionId;
    private String assignmentType;
    private Long systemPostId;
    private Long userId;
    private Boolean active;
    private String changeReason;
}
