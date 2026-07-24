package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("erp_kingdee_customer_sync_record")
@KeySequence("erp_kingdee_customer_sync_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeCustomerSyncRecordDO extends BaseDO {

    @TableId
    private Long id;

    private String sourceCustomerNumber;

    private String sourceCustomerName;

    private Long customerId;

}
