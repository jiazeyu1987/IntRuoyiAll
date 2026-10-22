package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UserDirectSupervisorRespVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING) private Long tenantId;
    @JsonFormat(shape = JsonFormat.Shape.STRING) private Long userId;
    @JsonFormat(shape = JsonFormat.Shape.STRING) private Long supervisorUserId;
    @JsonFormat(shape = JsonFormat.Shape.STRING) private Long relationVersion;
    private String username;
    private String nickname;
    private String supervisorUsername;
    private String supervisorNickname;
    private Integer supervisorStatus;
}
