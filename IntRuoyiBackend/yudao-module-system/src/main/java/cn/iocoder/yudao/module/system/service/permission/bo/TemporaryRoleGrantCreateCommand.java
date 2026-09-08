package cn.iocoder.yudao.module.system.service.permission.bo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TemporaryRoleGrantCreateCommand {

    private Long userId;

    private Long roleId;

    private String reason;

    private LocalDateTime expireTime;

    private Long applicantUserId;

    private String applicantUsername;

}
