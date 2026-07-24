package cn.iocoder.yudao.module.dcc.controller.admin.training.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DccTrainingViewSessionHeartbeatReqVO {

    @NotBlank(message = "clientSessionId 不能为空")
    private String clientSessionId;
}
