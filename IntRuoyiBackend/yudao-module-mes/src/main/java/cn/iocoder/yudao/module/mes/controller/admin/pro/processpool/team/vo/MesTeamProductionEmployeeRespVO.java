package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 生产人员档案 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamProductionEmployeeRespVO {

    private Long id;
    private Long systemUserId;
    private String employeeCode;
    private String employeeName;
    private String displayName;
    private String employeeType;
    private Boolean enabled;
    private LocalDateTime disabledAt;
    private String signaturePasswordManagedBy;
}
