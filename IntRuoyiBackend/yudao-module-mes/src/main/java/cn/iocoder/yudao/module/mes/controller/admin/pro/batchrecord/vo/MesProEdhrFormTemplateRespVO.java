package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES eDHR 独立表单模板 Response VO")
@Data
public class MesProEdhrFormTemplateRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板版本")
    private String templateVersion;

    @Schema(description = "字段定义 JSON")
    private String fieldSchemaJson;

    @Schema(description = "模板状态")
    private String status;

    @Schema(description = "启用人")
    private Long activeBy;

    @Schema(description = "启用时间")
    private LocalDateTime activeAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
