package cn.iocoder.yudao.module.srm.dal.dataobject.tender;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("srm_tender_expert")
@KeySequence("srm_tender_expert_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmTenderExpertDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String expertName;

    private String specialtyType;

    private String expertStatus;

    private Long auditBy;

    private String auditName;

    private LocalDateTime auditTime;

    private String auditRemark;
}
