package cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 临时角色授权分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TemporaryRoleGrantPageReqVO extends PageParam {

    @Schema(description = "被授权用户编号")
    private Long userId;

    @Schema(description = "角色编号")
    private Long roleId;

    @Schema(description = "状态：PENDING/ACTIVE/REVOKED/EXPIRED")
    private String status;

}
