package cn.iocoder.yudao.module.srm.dal.dataobject.tender;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("srm_tender_candidate")
@KeySequence("srm_tender_candidate_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmTenderCandidateDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long projectId;

    private Long submissionId;

    private Long supplierId;

    private String supplierName;

    private BigDecimal bidAmount;

    private Integer rankNo;

    private String candidateStatus;
}
