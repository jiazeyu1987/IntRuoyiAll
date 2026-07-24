package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrPrintPolicyCreateReqVO {

    @NotBlank(message = "打印策略编码不能为空")
    private String policyCode;

    @NotBlank(message = "打印策略名称不能为空")
    private String policyName;

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @NotNull(message = "首次打印次数上限不能为空")
    @Min(value = 0, message = "首次打印次数上限不能小于0")
    private Integer firstPrintLimit;

    @NotNull(message = "补打次数上限不能为空")
    @Min(value = 0, message = "补打次数上限不能小于0")
    private Integer reprintLimit;

    @NotBlank(message = "补打原因字典不能为空")
    private String reasonDictJson;

    @NotBlank(message = "水印模板不能为空")
    private String watermarkTemplate;

    @NotBlank(message = "作废历史副本水印不能为空")
    private String voidCopyWatermark;

    private String remark;
}
