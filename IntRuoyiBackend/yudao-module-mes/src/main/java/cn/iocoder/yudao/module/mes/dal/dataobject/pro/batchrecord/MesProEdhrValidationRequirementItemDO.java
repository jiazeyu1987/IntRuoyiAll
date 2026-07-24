package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

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
import lombok.experimental.Accessors;

@TableName("mes_pro_edhr_validation_requirement_item")
@KeySequence("mes_pro_edhr_validation_requirement_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrValidationRequirementItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long packageId;

    private String itemCode;

    private String itemName;

    private String itemType;

    private String itemVersion;

    private String itemStatus;

    private String ownerName;

    private String signoffRole;

    private String sourceDocument;

    private String businessProcess;

    private String acceptanceCriteria;

    private Integer sort;

    private String remark;
}
