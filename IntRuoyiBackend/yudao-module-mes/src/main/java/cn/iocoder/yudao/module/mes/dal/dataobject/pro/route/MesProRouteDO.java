package cn.iocoder.yudao.module.mes.dal.dataobject.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * MES 工艺路线 DO
 *
 * @author 瑛泰源码
 */
@TableName("mes_pro_route")
@KeySequence("mes_pro_route_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProRouteDO extends BaseDO {

    /** 树形归属仅影响配置管理，不改变已有流程配置与历史执行身份。 */
    private Long parentId;

    /** FLOW 为实际流程，GROUP 为只供继承与统一发布的分组模板。 */
    private String nodeType;

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 工艺路线编码
     */
    private String code;
    /**
     * 工艺路线名称
     */
    private String name;
    /**
     * 工艺路线说明
     */
    private String description;
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
