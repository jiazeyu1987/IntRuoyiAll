package cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * MES 工作站 DO
 *
 * @author 瑛泰源码
 */
@TableName("mes_md_workstation")
@KeySequence("mes_md_workstation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesMdWorkstationDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 工作站编码
     */
    private String code;
    /**
     * 工作站名称
     */
    private String name;
    /**
     * 工作站地点
     */
    private String address;
    /**
     * 所在车间编号
     *
     * 关联 {@link MesMdWorkshopDO#getId()}
     */
    private Long workshopId;
    /**
     * 工序编号
     *
     * 关联 {@link MesProProcessDO#getId()}
     */
    private Long processId;
    /**
     * 产线编号
     *
     * 关联 {@link MesMdProductionLineDO#getId()}
     */
    private Long productionLineId;
    /**
     * 线边库编号
     *
     * 关联 {@link MesWmWarehouseDO#getId()}
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long warehouseId;
    /**
     * 库区编号
     *
     * 关联 {@link MesWmWarehouseLocationDO#getId()}
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long locationId;
    /**
     * 库位编号
     *
     * 关联 {@link MesWmWarehouseAreaDO#getId()}
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long areaId;
    /**
     * 单人标准小时产能
     */
    private BigDecimal singleStandardHourlyCapacity;
    /**
     * 班次小时数
     */
    private BigDecimal shiftHours;
    /**
     * 状态
     *
     * 枚举 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
