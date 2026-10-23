package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@TableName("dcc_folder_template")
@Data @EqualsAndHashCode(callSuper = true)
public class DccFolderTemplateDO extends TenantBaseDO {
    @TableId private Long id;
    private String name;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    private Boolean active;
    private String structureJson;
    private Long editedByUserId;
    private Boolean everUsed;
}
