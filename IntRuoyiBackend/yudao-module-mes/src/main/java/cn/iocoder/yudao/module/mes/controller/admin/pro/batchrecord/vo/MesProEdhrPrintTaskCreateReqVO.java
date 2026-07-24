package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrPrintTaskCreateReqVO {

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @NotNull(message = "来源对象ID不能为空")
    private Long sourceObjectId;

    @NotBlank(message = "来源对象编码不能为空")
    private String sourceObjectCode;

    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    private Long labelInstanceId;

    private Long travelerId;

    private Boolean isReprint;

    private Long originalPrintTaskId;

    private String reprintReason;

    private String watermarkText;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
