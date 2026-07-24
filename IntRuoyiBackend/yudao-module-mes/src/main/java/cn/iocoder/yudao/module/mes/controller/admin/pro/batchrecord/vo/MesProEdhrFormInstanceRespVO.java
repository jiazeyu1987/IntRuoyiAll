package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - MES eDHR 独立表单实例 Response VO")
@Data
public class MesProEdhrFormInstanceRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "实例编码")
    private String instanceCode;

    @Schema(description = "模板 ID")
    private Long templateId;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板版本")
    private String templateVersion;

    @Schema(description = "字段定义 JSON")
    private String fieldSchemaJson;

    @Schema(description = "实例状态")
    private String status;

    @Schema(description = "版本")
    private Integer version;

    @Schema(description = "业务范围")
    private String businessScope;

    @Schema(description = "业务对象类型")
    private String businessObjectType;

    @Schema(description = "业务对象 ID")
    private Long businessObjectId;

    @Schema(description = "业务对象编码")
    private String businessObjectCode;

    @Schema(description = "提交人")
    private Long submittedBy;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "字段值")
    private Map<String, Object> values;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
