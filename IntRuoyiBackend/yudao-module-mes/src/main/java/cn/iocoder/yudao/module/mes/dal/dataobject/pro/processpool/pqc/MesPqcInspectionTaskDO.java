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

import java.time.LocalDate;

@TableName("mes_pqc_inspection_task")
@KeySequence("mes_pqc_inspection_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesPqcInspectionTaskDO extends TenantBaseDO {

    public static final String TASK_STATUS_PENDING = "PENDING";
    public static final String TASK_STATUS_SUBMITTED = "SUBMITTED";
    public static final String TASK_STATUS_CONFIRMED = "CONFIRMED";

    @TableId
    private Long id;

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
    private Integer plannedInspectionQuantity;
    private Integer actualInspectionQuantity;
    private String taskStatus;
}
