package cn.iocoder.yudao.module.dcc.controller.admin.log.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 文控日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccControlledFileLogPageReqVO extends PageParam {

    @Schema(description = "日志类型")
    private String logType;

    @Schema(description = "关键字")
    private String keyword;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "结果")
    private String result;

    @Schema(description = "受控文件编号")
    private Long controlledFileId;

    @Schema(description = "项目代码编号")
    private Long projectCodeId;

    @Schema(description = "修正任务编号")
    private Long assignmentId;

    @Schema(description = "操作人编号")
    private Long operatorUserId;

    @Schema(description = "字段名")
    private String fieldName;

    @Schema(description = "发生时间")
    private LocalDateTime[] occurredAt;

}
