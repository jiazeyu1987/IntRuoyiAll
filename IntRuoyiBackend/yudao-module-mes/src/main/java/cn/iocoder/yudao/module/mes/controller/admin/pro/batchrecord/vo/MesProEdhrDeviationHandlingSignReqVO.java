package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class MesProEdhrDeviationHandlingSignReqVO {
    @NotNull
    private Long deviationId;
    @NotBlank
    private String node;
    @NotBlank
    @ToString.Exclude
    private String password;
    @Size(max = 1000)
    private String comment;
}
