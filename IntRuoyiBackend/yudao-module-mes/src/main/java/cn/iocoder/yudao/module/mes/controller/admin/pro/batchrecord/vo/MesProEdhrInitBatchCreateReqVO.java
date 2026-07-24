package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MesProEdhrInitBatchCreateReqVO {

    @NotBlank(message = "项目编码不能为空")
    private String projectCode;

    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    @NotBlank(message = "目标环境不能为空")
    private String targetEnvironment;

    @NotNull(message = "目标租户不能为空")
    private Long targetTenantId;

    @NotBlank(message = "数据版本不能为空")
    private String dataVersion;

    @NotNull(message = "交付负责人不能为空")
    private Long ownerUserId;

    @NotNull(message = "审批负责人不能为空")
    private Long approvalOwnerUserId;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    @NotBlank(message = "初始化范围不能为空")
    private String initScopeJson;

    private String remark;
}
