package cn.iocoder.yudao.module.srm.controller.admin.framework.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SrmFrameworkPlanAuditReqVO {

    @NotNull(message = "框架计划编号不能为空")
    private Long id;

    private String auditRemark;
}
