package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@TableName("dcc_folder_template_history")
@Data @EqualsAndHashCode(callSuper = true)
public class DccFolderTemplateHistoryDO extends TenantBaseDO {
    @TableId private Long id;
    private Long templateId;
    private Long operatorUserId;
    private String operation;
    private String beforeJson;
    private String afterJson;
}
