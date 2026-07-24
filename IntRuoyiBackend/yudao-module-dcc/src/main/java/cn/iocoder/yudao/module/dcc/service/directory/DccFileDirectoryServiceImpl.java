package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_NOT_EXISTS;

/**
 * DCC file directory service implementation.
 */
@Service
@Validated
public class DccFileDirectoryServiceImpl implements DccFileDirectoryService {

    @Resource
    private DccFileDirectoryMapper fileDirectoryMapper;

    @Override
    public List<DccFileDirectoryDO> listEnabledChildDirectories(Long parentId) {
        if (parentId != null && fileDirectoryMapper.selectById(parentId) == null) {
            throw exception(FILE_DIRECTORY_NOT_EXISTS);
        }
        return fileDirectoryMapper.selectEnabledListByParentId(parentId);
    }

}
