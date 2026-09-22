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

@TableName("mes_pro_edhr_nonconformance_review")
@KeySequence("mes_pro_edhr_nonconformance_review_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrNonconformanceReviewDO extends BaseDO {

    @TableId
    private Long id;

    private String reviewCode;

    private String sourceType;

    private Long sourceId;

    private Long activeOrderId;

    private Long batchExecutionId;

    private String batchExecutionCode;

    private Long workOrderId;

    private String workOrderCode;

    private String batchCode;

    private Integer previousBatchStatus;

    private Boolean previousWorkOrderTemporaryFrozen;

    private String reviewStatus;

    private String nonconformanceReason;

    private String reviewMaterialUrl;

    private Long reviewMaterialFileId;

    private String reviewMaterialsJson;

    private String reviewOpinion;

    private String qaSignature;

    private Long qaUserId;

    private LocalDateTime frozenAt;

    private LocalDateTime closedAt;

    private LocalDateTime unfrozenAt;

    private LocalDateTime voidedAt;

    private String disposition;

    private String traceSnapshotJson;

    private String remark;

    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    public MesProEdhrNonconformanceReviewDO setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }
}
