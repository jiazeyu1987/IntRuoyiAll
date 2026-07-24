package cn.iocoder.yudao.module.showroom.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@TableName("showroom_hall_item")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomHallItemDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long hallId;

    private String itemType;

    private Long itemId;

    private Integer displayOrder;

    private BigDecimal layoutX;

    private BigDecimal layoutY;

    private BigDecimal layoutWidth;

    private BigDecimal layoutHeight;
}
