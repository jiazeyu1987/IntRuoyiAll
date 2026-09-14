package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Schema(description = "管理后台 - MES PQC组长当前活跃任务 Response VO")
@Data
@Accessors(chain = true)
public class MesPqcLeaderActiveTaskRespVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long pqcTaskId;
    private String taskStatus;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long activeOrderId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workOrderId;
    private String workOrderCode;
    private String workOrderName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long qaRegulationId;
    private String qaRegulationCode;
    private String qaRegulationName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long qaVersionId;
    private String qaVersionNo;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long qaProcessId;
    private String qaProcessCode;
    private String qaProcessName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long routeId;
    private String routeCode;
    private String routeName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long routeVersionId;
    private String routeVersionNo;
    private String inspectionRuleKey;
    private String inspectionType;
    private LocalDate businessDate;
    private String shiftCode;
    private Integer roundNo;
    private Integer plannedInspectionQuantity;
    private Integer actualInspectionQuantity;
}
