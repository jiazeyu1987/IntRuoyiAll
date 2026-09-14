package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组员工档案保存 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamEmployeeProfileSaveReqVO {

    @Schema(description = "关联系统用户编号；临时工可为空", example = "2001")
    private Long systemUserId;

    @Schema(description = "员工编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TMP-001")
    @NotBlank
    private String employeeCode;

    @Schema(description = "员工姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "临时工甲")
    @NotBlank
    private String employeeName;

    @Schema(description = "员工类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEMPORARY")
    @NotBlank
    private String employeeType;
}
