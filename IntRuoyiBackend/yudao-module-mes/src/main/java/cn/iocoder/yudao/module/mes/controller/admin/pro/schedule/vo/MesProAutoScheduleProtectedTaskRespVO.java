package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 自动排产受保护任务 Response VO")
@Data
public class MesProAutoScheduleProtectedTaskRespVO {

    private Long taskId;

    private String taskCode;

    private Long workOrderId;

    private String workOrderCode;

    private Long processId;

    private String processName;

    private Long workstationId;

    private String workstationName;

    private String scheduleSource;

    private Boolean locked;

    private Integer status;

    private String protectionReason;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
