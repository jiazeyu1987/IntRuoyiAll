package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - eDHR OQ/PQ 执行分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrOqPqRunPageReqVO extends PageParam {

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "用例ID")
    private Long caseId;

    @Schema(description = "用例类型")
    private String caseType;

    @Schema(description = "执行状态")
    private String runStatus;

    @Schema(description = "执行编号")
    private String runCode;
}
