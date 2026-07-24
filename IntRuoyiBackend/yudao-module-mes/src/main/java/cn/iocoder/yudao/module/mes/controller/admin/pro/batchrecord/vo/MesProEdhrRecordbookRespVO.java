package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES eDHR 记录本 Response VO")
@Data
public class MesProEdhrRecordbookRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "记录本编码")
    private String recordbookCode;

    @Schema(description = "记录本名称")
    private String recordbookName;

    @Schema(description = "模板 ID")
    private Long templateId;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板版本")
    private String templateVersion;

    @Schema(description = "记录本类型")
    private String recordbookType;

    @Schema(description = "记录本状态")
    private String status;

    @Schema(description = "责任人")
    private Long ownerUserId;

    @Schema(description = "责任部门")
    private Long ownerDeptId;

    @Schema(description = "业务对象编码")
    private String businessObjectCode;

    @Schema(description = "开本时间")
    private LocalDateTime openedAt;

    @Schema(description = "条目数量")
    private Integer entryCount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
