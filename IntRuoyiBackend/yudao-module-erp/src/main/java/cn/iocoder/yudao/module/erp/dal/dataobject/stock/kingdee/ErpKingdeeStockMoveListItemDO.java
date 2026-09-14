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

@TableName("erp_kingdee_stock_move_item")
@KeySequence("erp_kingdee_stock_move_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeStockMoveListItemDO extends BaseDO {

    @TableId
    private Long id;
    private Long stockMoveId;
    private String sourceFormId;
    private String sourceFid;
    private String sourceEntryId;
    private String sourceLineKey;
    private String sourceBillNo;
    private String materialNumber;
    private String materialName;
    private String materialSpecification;
    private String unitName;
    private BigDecimal quantity;
    private String fromWarehouseNumber;
    private String fromWarehouseName;
    private String toWarehouseNumber;
    private String toWarehouseName;
    private String fromStockLocation;
    private String toStockLocation;
    private String lotNumber;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private String rawPayload;

}
