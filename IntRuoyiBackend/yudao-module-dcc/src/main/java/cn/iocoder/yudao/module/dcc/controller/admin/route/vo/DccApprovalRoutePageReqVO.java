package cn.iocoder.yudao.module.dcc.controller.admin.route.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - DCC 审批路线分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DccApprovalRoutePageReqVO extends PageParam {

    @Schema(description = "文件类别编号")
    private Long categoryId;
}
