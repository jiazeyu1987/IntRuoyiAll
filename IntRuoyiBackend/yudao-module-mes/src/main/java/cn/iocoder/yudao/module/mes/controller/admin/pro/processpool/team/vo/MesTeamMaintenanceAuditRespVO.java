package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 班组维护审计 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamMaintenanceAuditRespVO {

    private Long id;
    private Long operatorUserId;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String resultStatus;
    private String changeSummary;
    private LocalDateTime auditTime;
}
