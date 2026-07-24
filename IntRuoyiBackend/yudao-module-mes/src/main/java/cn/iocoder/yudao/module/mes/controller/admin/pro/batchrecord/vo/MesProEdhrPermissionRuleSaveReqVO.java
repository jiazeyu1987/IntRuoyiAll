package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrPermissionRuleSaveReqVO {

    @NotBlank(message = "权限主体类型不能为空")
    private String subjectType;

    @NotNull(message = "权限主体编号不能为空")
    private Long subjectId;

    @NotBlank(message = "权限能力不能为空")
    private String ability;

    @NotBlank(message = "权限决策不能为空")
    private String decision;

    private Integer priority;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private String status;
}
