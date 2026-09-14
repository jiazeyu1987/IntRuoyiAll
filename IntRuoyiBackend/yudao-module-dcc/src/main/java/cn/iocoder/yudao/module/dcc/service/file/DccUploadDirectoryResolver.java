package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS;

final class DccUploadDirectoryResolver {

    static final String UNCLASSIFIED_UPLOAD_DIRECTORY_CODE = "UNCLASSIFIED";

    private DccUploadDirectoryResolver() {
    }

    static DccFileDirectoryDO resolveUnclassifiedUploadDirectory(List<DccFileDirectoryDO> directories) {
        List<DccFileDirectoryDO> matches = directories.stream()
                .filter(directory -> UNCLASSIFIED_UPLOAD_DIRECTORY_CODE.equals(directory.getCode()))
                .toList();
        if (matches.size() != 1) {
            throw exception(FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS);
        }
        return matches.get(0);
    }
}
