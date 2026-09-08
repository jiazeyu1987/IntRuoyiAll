package cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 临时角色授权 Response VO")
@Data
public class TemporaryRoleGrantRespVO {

    private Long id;
    private Long userId;
    private Long roleId;
    private String reason;
    private String status;
    private LocalDateTime applyTime;
    private Long applicantUserId;
    private String applicantUsername;
    private LocalDateTime approveTime;
    private Long approverUserId;
    private String approverUsername;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private LocalDateTime revokeTime;
    private Long revokerUserId;
    private String revokerUsername;
    private String revokeReason;
    private LocalDateTime createTime;

}
