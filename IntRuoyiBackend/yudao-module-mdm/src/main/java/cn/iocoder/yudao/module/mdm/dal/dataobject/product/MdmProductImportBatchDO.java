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

import java.time.LocalDateTime;

@TableName("mdm_product_import_batch")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductImportBatchDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String status;
    private Integer totalCount;
    private Integer createCount;
    private Integer updateCount;
    private Integer disableCount;
    private Integer unchangedCount;
    private Integer failureCount;
    private LocalDateTime confirmedAt;

}
