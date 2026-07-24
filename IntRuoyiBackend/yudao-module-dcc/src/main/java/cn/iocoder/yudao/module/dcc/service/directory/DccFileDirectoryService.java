package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;

import java.util.List;

/**
 * DCC file directory service.
 */
public interface DccFileDirectoryService {

    /**
     * Returns enabled child directories under the given parent.
     *
     * @param parentId parent directory id, or null for root
     * @return enabled child directories
     */
    List<DccFileDirectoryDO> listEnabledChildDirectories(Long parentId);

}
