package cn.iocoder.yudao.module.srm.dal.dataobject.procurement;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("srm_procurement_approval_record")
@KeySequence("srm_procurement_approval_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmProcurementApprovalRecordDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String bizType;

    private Long bizId;

    private String action;

    private String actionLabel;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operationTime;

    private String remark;
}
