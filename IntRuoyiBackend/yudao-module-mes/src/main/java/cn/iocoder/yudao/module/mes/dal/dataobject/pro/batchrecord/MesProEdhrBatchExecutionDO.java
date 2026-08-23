package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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

@TableName("mes_pro_edhr_batch_execution")
@KeySequence("mes_pro_edhr_batch_execution_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrBatchExecutionDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 租户边界快照。批次执行基础表已经持久化该列，Tx-C 读取时必须显式校验。
     */
    private Long tenantId;

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private String activeContextKey;

    private Integer attemptNo;

    private Long sourceRejectedBatchExecutionId;

    private Long supersededByBatchExecutionId;

    private Long reexecutedByChangeEventId;

    private Long productId;

    private String productCode;

    private String productName;

    private Long routeId;

    private Long routeVersionId;

    private String routeVersionNo;

    private String routeSnapshotJson;

    private String routeCode;

    private String routeName;

    private Integer status;

    private Integer taskTotal;

    private Integer taskApprovedCount;

    private Integer blockedCount;

    private String aggregateHash;

    private Long closeSignatureId;

    private Long closedBy;

    private LocalDateTime closedAt;

    private Long rejectSignatureId;

    private Long rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectReason;

    private String remark;
}
