package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("mes_pqc_process_inspection_aggregate_detail")
@KeySequence("mes_pqc_process_inspection_aggregate_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesPqcProcessInspectionAggregateDetailDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long sourcePqcRecordId;
    private Long sourcePieceDetailId;
    private Long eventId;
    private Long reviewId;
    private Long productionSubmitEventId;
    private Long pqcTaskId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long routeId;
    private Long routeVersionId;
    private Long routeProcessId;
    private Long processId;
    private Long regulationVersionId;
    private String inspectionType;
    private LocalDate businessDate;
    private String shiftCode;
    private Integer roundNo;
    private Integer actualInspectionQuantity;
    private Integer sampleNo;
    private String itemCode;
    private String itemName;
    private String inspectionMethod;
    private String standardText;
    private Long selectedEquipmentId;
    private String selectedEquipmentCode;
    private String selectedEquipmentName;
    private String selectedEquipmentNumber;
    private BigDecimal standardLowerLimit;
    private BigDecimal standardUpperLimit;
    private String standardUnit;
    private Integer standardPrecision;
    private String resultType;
    private String itemResult;
    private String measuredValue;
    private String judgement;
    private Boolean simulated;
    private String simulationStage;
    private String simulationRunId;
    private LocalDateTime aggregatedAt;
}
