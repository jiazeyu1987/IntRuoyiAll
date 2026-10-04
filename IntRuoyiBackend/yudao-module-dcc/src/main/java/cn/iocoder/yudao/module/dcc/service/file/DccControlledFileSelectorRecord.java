package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper=true)
public class DccControlledFileSelectorRecord extends DccControlledFileDO {
    private String projectName;
    private String sourceDirectoryName;
    private Long latestControlledFileId;
    private Long projectFolderId;
    private String projectFolderName;
}
