package cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 临时角色授权审计 Response VO")
@Data
public class TemporaryRoleGrantAuditRespVO {

    private Long id;
    private Long grantId;
    private String eventType;
    private Long userId;
    private Long roleId;
    private String permissionCode;
    private Long operatorUserId;
    private String operatorUsername;
    private String message;
    private LocalDateTime createTime;

}
