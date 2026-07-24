package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DccControlledFilePublishReqVO {

    @NotBlank(message = "reason is required")
    private String reason;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    private Map<String, List<Long>> startUserSelectAssignees;
}
