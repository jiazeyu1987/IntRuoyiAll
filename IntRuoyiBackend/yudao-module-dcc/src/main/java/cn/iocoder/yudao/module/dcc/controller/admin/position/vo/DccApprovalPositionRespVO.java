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
    @io.swagger.v3.oas.annotations.media.Schema(description = "当前后端支持执行的分配类型，用于配置入口能力校验")
    private List<String> supportedAssignmentTypes = List.of("USER", "POST", "DIRECT_SUPERVISOR", "DEPT_LEADER");
}
