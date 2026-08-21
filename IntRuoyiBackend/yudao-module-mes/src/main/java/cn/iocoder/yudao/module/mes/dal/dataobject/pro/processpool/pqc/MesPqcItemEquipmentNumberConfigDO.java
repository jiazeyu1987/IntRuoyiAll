package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc;

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

@TableName("mes_pqc_item_equipment_number_config")
@KeySequence("mes_pqc_item_equipment_number_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesPqcItemEquipmentNumberConfigDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long configId;
    private String itemCode;
    private Long equipmentId;
    private String equipmentNumber;
    private Boolean enabled;
    private Integer sort;
}
