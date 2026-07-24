package cn.iocoder.yudao.module.srm.dal.dataobject.supplier;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SRM 只读引用 ERP 供应商主数据。
 */
@TenantIgnore
@TableName("erp_supplier")
@KeySequence("erp_supplier_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmErpSupplierDO extends BaseDO {

    @TableId
    private Long id;

    private String name;

    private String contact;

    private String mobile;

    private String email;

    private String remark;

    private Integer status;

    private String taxNo;

    private String bankName;

    private String bankAccount;

    private String bankAddress;

    private Long tenantId;
}
