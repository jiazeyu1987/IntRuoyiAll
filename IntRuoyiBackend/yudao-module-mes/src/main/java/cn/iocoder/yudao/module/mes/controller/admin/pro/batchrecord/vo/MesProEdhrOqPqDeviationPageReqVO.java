package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - eDHR OQ/PQ 偏差分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrOqPqDeviationPageReqVO extends PageParam {

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "执行记录ID")
    private Long runId;

    @Schema(description = "偏差编号")
    private String deviationCode;

    @Schema(description = "偏差状态")
    private String deviationStatus;
}
