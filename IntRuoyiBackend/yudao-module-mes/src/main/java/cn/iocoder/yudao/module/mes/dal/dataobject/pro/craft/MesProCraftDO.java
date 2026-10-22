package cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@TableName("mes_pro_craft")
@Data @EqualsAndHashCode(callSuper = true)
public class MesProCraftDO extends BaseDO {
    @TableId private Long id;
    private String code;
    private String name;
    private Long processId;
    private String parametersJson;
    private String fingerprint;
}
