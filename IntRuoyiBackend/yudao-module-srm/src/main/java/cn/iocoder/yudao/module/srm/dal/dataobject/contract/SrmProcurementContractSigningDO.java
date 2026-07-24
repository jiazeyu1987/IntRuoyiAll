package cn.iocoder.yudao.module.srm.dal.dataobject.contract;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

@TableName("srm_procurement_contract_signing")
@KeySequence("srm_procurement_contract_signing_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmProcurementContractSigningDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long contractId;

    private String signingParty;

    private String signerName;

    private LocalDate signingDate;

    private String signingRemark;
}
