package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP Kingdee K3Cloud 采购订单同步记录 DO
 */
@TableName("erp_kingdee_purchase_order_sync_record")
@KeySequence("erp_kingdee_purchase_order_sync_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeePurchaseOrderSyncRecordDO extends BaseDO {

    public static final int SYNC_STATUS_SUCCESS = 10;

    @TableId
    private Long id;

    /**
     * Kingdee FormId，例如 PUR_PurchaseOrder
     */
    private String sourceFormId;
    /**
     * Kingdee FID
     */
    private String sourceFid;
    /**
     * Kingdee 单据编号
     */
    private String sourceBillNo;
    /**
     * IntRuoyi ERP 采购订单编号
     */
    private Long purchaseOrderId;
    /**
     * 同步状态
     */
    private Integer syncStatus;
    /**
     * 失败信息，当前手工同步失败时直接抛错，不落失败记录
     */
    private String failureMessage;
    /**
     * Kingdee 原始载荷快照
     */
    private String rawPayload;

}
