package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@TableName("dcc_project_folder")
@Data @EqualsAndHashCode(callSuper = true)
public class DccProjectFolderDO extends TenantBaseDO {
    @TableId private Long id;
    private Long projectCodeId;
    private Long parentId;
    private String name;
    private Integer sortOrder;
    private Boolean active;
    private Long sourceTemplateId;
    private String sourceNodeKey;
}
