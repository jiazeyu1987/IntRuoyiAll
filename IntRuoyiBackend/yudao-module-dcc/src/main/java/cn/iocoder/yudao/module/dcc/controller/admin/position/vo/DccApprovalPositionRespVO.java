package cn.iocoder.yudao.module.dcc.controller.admin.position.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccApprovalPositionRespVO {
    private Long id;
    private String code;
    private String name;
    private Boolean active;
    private String source;
    private String remark;
    private LocalDateTime createTime;
    private List<DccPositionAssignmentRespVO> assignments;
}
