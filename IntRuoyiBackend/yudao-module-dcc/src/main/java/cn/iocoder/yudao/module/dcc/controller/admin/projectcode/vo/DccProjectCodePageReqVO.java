package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - DCC 项目代码分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DccProjectCodePageReqVO extends PageParam {

    @Schema(description = "关联产品主数据编号")
    private Long productMasterId;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目代码")
    private String projectCode;

    @Schema(description = "类别")
    private String category;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "工艺路线是否已配置")
    private Boolean routeConfigured;

    @Schema(description = "主批记录是否已配置")
    private Boolean mainBatchRecordConfigured;

    @Schema(description = "QA 规程是否已配置")
    private Boolean qaRegulationConfigured;

    @Schema(description = "是否仅返回绑定合法 DCC 产品编号的项目代码")
    private Boolean requireDccProductCode;

    @Schema(description = "关联文件数排序，asc 或 desc")
    private String fileCountSort;
}
