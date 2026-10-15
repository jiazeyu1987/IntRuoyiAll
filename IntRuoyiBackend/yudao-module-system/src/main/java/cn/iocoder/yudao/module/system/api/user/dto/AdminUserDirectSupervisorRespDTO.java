package cn.iocoder.yudao.module.system.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Persisted user-management relation; a null supervisor is explicitly unconfigured. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AdminUserDirectSupervisorRespDTO {
    private Long tenantId;
    private Long userId;
    private Long supervisorUserId;
    private Long relationVersion;
}
