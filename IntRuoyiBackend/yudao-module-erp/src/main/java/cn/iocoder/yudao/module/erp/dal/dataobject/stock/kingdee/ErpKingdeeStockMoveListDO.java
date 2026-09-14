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

import java.time.LocalDateTime;

@TableName("erp_kingdee_stock_move")
@KeySequence("erp_kingdee_stock_move_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeStockMoveListDO extends BaseDO {

    @TableId
    private Long id;
    private String sourceFormId;
    private String sourceFid;
    private String sourceBillNo;
    private LocalDateTime billDate;
    private String documentStatus;
    private String transferDirect;
    private String transferBizType;
    private String remark;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private String rawPayload;

}
