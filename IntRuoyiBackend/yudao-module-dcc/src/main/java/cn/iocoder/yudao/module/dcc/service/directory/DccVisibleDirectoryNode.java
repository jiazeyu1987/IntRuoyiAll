package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;

public record DccVisibleDirectoryNode(DccFileDirectoryDO directory, Boolean hasChildren, String directoryPath) {
}
