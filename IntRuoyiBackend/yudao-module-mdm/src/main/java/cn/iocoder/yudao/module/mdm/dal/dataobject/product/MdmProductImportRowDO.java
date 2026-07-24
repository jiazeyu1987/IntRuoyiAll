package cn.iocoder.yudao.module.mdm.dal.dataobject.product;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mdm_product_import_row")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductImportRowDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long batchId;
    private Integer rowNo;
    private String productCode;
    private String dccProductCode;
    private String nameCn;
    private String nameEn;
    private String modelSpecification;
    private String category;
    private String currentStatus;
    private String importAction;
    private String failureReason;

}
