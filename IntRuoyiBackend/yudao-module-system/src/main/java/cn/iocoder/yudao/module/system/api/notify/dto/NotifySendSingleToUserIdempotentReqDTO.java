package cn.iocoder.yudao.module.system.api.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * Idempotent station-message request for an Admin user.
 */
@Data
public class NotifySendSingleToUserIdempotentReqDTO {

    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @NotEmpty(message = "站内信模板编号不能为空")
    private String templateCode;

    @NotNull(message = "站内信模板参数不能为空")
    private Map<String, Object> templateParams;

    @NotBlank(message = "站内信业务键不能为空")
    @Size(max = 255, message = "站内信业务键长度不能超过 255")
    private String businessKey;
}
