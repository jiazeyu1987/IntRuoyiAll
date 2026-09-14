package cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
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

@TableName("erp_kingdee_production_replenishment_list_item")
@KeySequence("erp_kingdee_production_replenishment_list_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeProductionReplenishmentListItemDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long productionReplenishmentListId;
    private String sourceFormId;
    private String sourceFid;
    private String sourceEntryId;
    private String sourceLineKey;
    private String sourceBillNo;
    private String materialNumber;
    private String materialName;
    private String materialSpecification;
    private String unitName;
    private BigDecimal requestedQuantity;
    private BigDecimal actualQuantity;
    private BigDecimal baseActualQuantity;
    private String warehouseNumber;
    private String warehouseName;
    private String stockLocationNumber;
    private String stockLocationName;
    private String lotNumber;
    private String productionOrderNo;
    private Integer productionOrderLineNo;
    private String productionMaterialListNo;
    private Integer productionMaterialListLineNo;
    private String workshopNumber;
    private String workshopName;
    private String stockStatusNumber;
    private String stockStatusName;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private String rawPayload;

}
