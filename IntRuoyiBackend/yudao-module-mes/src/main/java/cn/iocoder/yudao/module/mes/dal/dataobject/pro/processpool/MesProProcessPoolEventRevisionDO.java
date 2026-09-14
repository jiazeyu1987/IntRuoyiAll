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

@TableName("mes_pro_process_pool_event_revision")
@KeySequence("mes_pro_process_pool_event_revision_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProProcessPoolEventRevisionDO extends TenantBaseDO {

    public static final String STATUS_EFFECTIVE = "EFFECTIVE";

    @TableId
    private Long id;

    private Long eventId;
    private Long poolId;
    private Long workOrderId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
    private String beforePayload;
    private String afterPayload;
    private String changeReason;
    private Long revisionSignatureId;
    private Long revisionSignatureUserId;
    private String revisionSignatureSnapshot;
    private Long modifiedByUserId;
    private LocalDateTime serverRevisionTime;
    private String revisionStatus;
}
