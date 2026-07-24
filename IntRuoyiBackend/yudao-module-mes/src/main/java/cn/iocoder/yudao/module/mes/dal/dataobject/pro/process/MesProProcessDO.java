package cn.iocoder.yudao.module.mes.dal.dataobject.pro.process;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * MES 生产工序 DO
 *
 * @author 瑛泰源码
 */
@TableName("mes_pro_process")
@KeySequence("mes_pro_process_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProProcessDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 产品名称
     */
    private String productName;
    /**
     * 工序编码
     */
    private String code;
    /**
     * 工序名称
     */
    private String name;
    /**
     * 工艺要求
     */
    private String attention;
    /**
     * 状态
     *
     * 枚举 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 人工班次产能
     */
    private BigDecimal manualShiftCapacity;
    /**
     * 备注
     */
    private String remark;

}
