package cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
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

/**
 * ERP 生产用料清单同步明细 DO
 *
 * @author Codex
 */
@TableName("mes_kingdee_production_material_list")
@KeySequence("mes_kingdee_production_material_list_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesKingdeeProductionMaterialListDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * ERP 来源表单标识
     */
    private String sourceFormId;
    /**
     * ERP 生产用料清单单据编号
     */
    private String sourceBillNo;
    /**
     * ERP 来源分录 ID
     */
    private String sourceEntryId;
    /**
     * 产品编码
     */
    private String productCode;
    /**
     * 生产订单编号
     */
    private String productionOrderNo;
    /**
     * 生产订单行号
     */
    private Integer productionOrderLineNo;
    /**
     * 生产订单状态
     */
    private String productionOrderStatus;
    /**
     * 子项物料编码
     */
    private String childMaterialCode;
    /**
     * 子项物料名称
     */
    private String childMaterialName;
    /**
     * 规格型号
     */
    private String childMaterialSpecification;
    /**
     * 子项类型
     */
    private String childMaterialType;
    /**
     * 分子
     */
    private BigDecimal numerator;
    /**
     * 分母
     */
    private BigDecimal denominator;
    /**
     * 子项单位
     */
    private String childUnitName;
    /**
     * 应发数量
     */
    private BigDecimal requiredQuantity;
    /**
     * 发料方式
     */
    private String issueMethod;
    /**
     * 需求日期
     */
    private LocalDateTime demandTime;
    /**
     * 本地生产工单 ID
     *
     * 关联 {@link MesProWorkOrderDO#getId()}
     */
    private Long workOrderId;
    /**
     * 本地生产工单编号
     */
    private String workOrderCode;
    /**
     * 本地生产工单 BOM 明细 ID
     *
     * 关联 {@link MesProWorkOrderBomDO#getId()}
     */
    private Long workOrderBomId;
    /**
     * 本地产品物料 ID
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long productId;
    /**
     * 本地子项物料 ID
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long childMaterialId;
    /**
     * ERP 来源修改时间
     */
    private LocalDateTime sourceModifyTime;
    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;
    /**
     * ERP 原始载荷快照
     */
    private String rawPayload;

}

