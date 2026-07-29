package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 一线设备账号可切换员工 Response VO")
@Data
public class MesFrontlineEmployeeCandidateRespVO {

    @Schema(description = "用户编号")
    private Long userId;
    @Schema(description = "用户账号")
    private String username;
    @Schema(description = "用户昵称")
    private String nickname;

}
