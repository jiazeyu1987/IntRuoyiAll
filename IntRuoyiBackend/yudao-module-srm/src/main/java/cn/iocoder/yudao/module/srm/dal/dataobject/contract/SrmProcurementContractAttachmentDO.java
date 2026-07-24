package cn.iocoder.yudao.module.srm.dal.dataobject.contract;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("srm_procurement_contract_attachment")
@KeySequence("srm_procurement_contract_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmProcurementContractAttachmentDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long contractId;

    private String attachmentName;

    private String attachmentUrl;

    private String attachmentType;
}
