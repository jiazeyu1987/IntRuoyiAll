package cn.iocoder.yudao.module.mes.dal.dataobject.md.item;

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

@TableName("mes_kingdee_bom_list")
@KeySequence("mes_kingdee_bom_list_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesKingdeeBomListDO extends BaseDO {

    @TableId
    private Long id;
    private String sourceFormId;
    private String sourceFid;
    private String sourceLineKey;
    private String bomNumber;
    private String bomType;
    private String documentStatus;
    private String parentMaterialCode;
    private String parentMaterialName;
    private String parentMaterialSpecification;
    private BigDecimal parentQuantity;
    private Integer lineNo;
    private String childMaterialCode;
    private String childMaterialName;
    private String childMaterialSpecification;
    private String childUnitName;
    private BigDecimal numerator;
    private BigDecimal denominator;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private String rawPayload;

}
