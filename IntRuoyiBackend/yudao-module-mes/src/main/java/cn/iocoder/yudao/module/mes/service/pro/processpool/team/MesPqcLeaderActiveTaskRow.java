package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class MesPqcLeaderActiveTaskRow {

    private Long pqcTaskId;
    private String taskStatus;
    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private String workOrderName;
    private Long qaRegulationId;
    private String qaRegulationCode;
    private String qaRegulationName;
    private Long qaVersionId;
    private String qaVersionNo;
    private Long qaProcessId;
    private String qaProcessCode;
    private String qaProcessName;
    private Long routeId;
    private String routeCode;
    private String routeName;
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
