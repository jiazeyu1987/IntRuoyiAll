package cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@TableName("mes_pro_craft_change")
@Data @EqualsAndHashCode(callSuper = true)
public class MesProCraftChangeDO extends BaseDO {
    @TableId private Long id;
    private Long craftId;
    private String changeType;
    private String status;
    private String beforeJson;
    private String afterJson;
    private String candidateIdsJson;
    private String candidateHashesJson;
    private String reason;
}
