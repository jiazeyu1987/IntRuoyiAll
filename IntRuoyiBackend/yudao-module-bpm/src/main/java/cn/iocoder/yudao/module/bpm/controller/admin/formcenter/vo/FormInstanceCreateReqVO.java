package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class FormInstanceCreateReqVO {

    @Valid
    @NotNull(message = "业务动作上下文不能为空")
    private BusinessActionContextReqVO context;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;

    private Map<String, Object> formData;

}
