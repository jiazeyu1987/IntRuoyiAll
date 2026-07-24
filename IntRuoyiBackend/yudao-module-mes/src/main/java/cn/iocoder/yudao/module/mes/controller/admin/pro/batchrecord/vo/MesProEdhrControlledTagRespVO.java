package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES eDHR 受控标签 Response VO")
@Data
public class MesProEdhrControlledTagRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "标签编码")
    private String tagCode;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签类型")
    private String tagType;

    @Schema(description = "标签状态")
    private String tagStatus;

    @Schema(description = "启用时间")
    private LocalDateTime activeAt;

    @Schema(description = "停用时间")
    private LocalDateTime disabledAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
