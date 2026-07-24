package cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_kingdee_inventory_list")
@KeySequence("erp_kingdee_inventory_list_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeInventoryListDO extends BaseDO {

    @TableId
    private Long id;
    private String sourceFormId;
    private String sourceLineKey;
    private String materialNumber;
    private String materialName;
    private String materialSpecification;
    private String warehouseNumber;
    private String warehouseName;
    private String lotNumber;
    private String unitName;
    private BigDecimal quantity;
    private String stockOrgNumber;
    private String stockOrgName;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private String rawPayload;

}
