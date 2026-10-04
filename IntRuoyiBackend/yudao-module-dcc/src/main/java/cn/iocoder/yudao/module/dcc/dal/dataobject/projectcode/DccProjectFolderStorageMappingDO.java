package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
@TableName("dcc_project_folder_storage_mapping")
@Data
@EqualsAndHashCode(callSuper=true)
public class DccProjectFolderStorageMappingDO extends TenantBaseDO {
 @TableId private Long id;
 private Long projectCodeId;
 private Long projectFolderId;
 private Long categoryId;
 private Long baseDirectoryId;
 private Long storageDirectoryId;
}
