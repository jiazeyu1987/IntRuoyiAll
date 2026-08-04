package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool;

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

import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_pqc_record")
@KeySequence("mes_pro_process_pool_pqc_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProProcessPoolPqcRecordDO extends TenantBaseDO {

    public static final String INSPECTION_RESULT_SUCCESS = "SUCCESS";
    public static final String INSPECTION_RESULT_FAILURE = "FAILURE";

    @TableId
    private Long id;

    private Long poolId;
    private Long eventId;
    private Long productionSubmitEventId;
    private Long workOrderId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
    private Long actualEmployeeId;
    private Long signatureId;
    private Long signatureUserId;
    private String inspectionResult;
    private LocalDateTime serverSubmitTime;
    private String rawPayload;
}
