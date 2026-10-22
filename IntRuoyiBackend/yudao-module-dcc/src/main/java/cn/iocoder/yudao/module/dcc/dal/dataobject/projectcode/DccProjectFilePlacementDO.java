package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
/** 项目目录位置与文件原存储目录事实分别保存；不把folderId塞进File.directoryId。 */
@TableName("dcc_project_file_placement")
@Data @EqualsAndHashCode(callSuper = true)
public class DccProjectFilePlacementDO extends TenantBaseDO {
    @TableId @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long id;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long projectCodeId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long projectFolderId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long controlledFileId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long storageDirectoryId;
}
