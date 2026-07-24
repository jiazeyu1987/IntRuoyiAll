package cn.iocoder.yudao.module.srm.dal.dataobject.procurement;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("srm_sourcing_project")
@KeySequence("srm_sourcing_project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSourcingProjectDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String projectNo;

    private String projectTitle;

    private String projectType;

    private String projectStatus;

    private Long sourcePlanId;

    private String sourcePlanNo;

    private BigDecimal expectedAmount;

    private String quoteMode;

    private LocalDateTime quoteStartTime;

    private LocalDateTime quoteEndTime;

    private String publishAttachmentUrl;

    private LocalDateTime publishedTime;

    private Long dealQuoteId;

    private Long dealSupplierId;

    private String dealSupplierName;

    private BigDecimal dealAmount;

    private String dealRemark;

    private LocalDateTime dealTime;

    private Long contractId;
}
