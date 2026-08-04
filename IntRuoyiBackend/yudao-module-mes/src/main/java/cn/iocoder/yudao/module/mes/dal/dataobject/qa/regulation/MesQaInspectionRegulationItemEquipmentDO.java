package cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation;

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
import lombok.experimental.Accessors;

@TableName("mes_qa_inspection_regulation_item_equipment")
@KeySequence("mes_qa_inspection_regulation_item_equipment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesQaInspectionRegulationItemEquipmentDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long regulationVersionId;
    private String inspectionType;
    private String itemCode;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String equipmentNumber;
    private Boolean defaultFlag;
    private Integer sort;
}
