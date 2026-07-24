package cn.iocoder.yudao.module.srm.controller.admin.tender.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SrmTenderExpertAuditReqVO {

    @NotNull(message = "专家编号不能为空")
    private Long id;

    private String auditRemark;
}
