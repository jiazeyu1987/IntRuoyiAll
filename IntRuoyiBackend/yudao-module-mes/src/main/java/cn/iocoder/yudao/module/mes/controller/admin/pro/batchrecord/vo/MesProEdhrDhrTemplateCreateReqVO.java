package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrDhrTemplateCreateReqVO {

    @NotNull(message = "DHR目录ID不能为空")
    private Long catalogId;

    @NotBlank(message = "DHR模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "DHR模板名称不能为空")
    private String templateName;

    @NotBlank(message = "DHR模板版本不能为空")
    private String currentVersion;

    @NotBlank(message = "DHR模板快照不能为空")
    private String templateSnapshotJson;

    private String productCode;

    private String routeCode;

    private String processCode;

    private String batchType;

    private String remark;
}
