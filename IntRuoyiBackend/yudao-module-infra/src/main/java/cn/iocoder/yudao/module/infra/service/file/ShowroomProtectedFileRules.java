package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;

import java.util.Objects;

final class ShowroomProtectedFileRules {

    static final Long FILE_CONFIG_ID = 28L;
    static final String FILE_PATH_PREFIX = "showroom/";

    private ShowroomProtectedFileRules() {
    }

    static boolean isProtectedFileConfig(Long id) {
        return Objects.equals(FILE_CONFIG_ID, id);
    }

    static boolean isProtectedShowroomFile(FileDO file) {
        return file != null && isProtectedFileConfig(file.getConfigId())
                && file.getPath() != null && file.getPath().startsWith(FILE_PATH_PREFIX);
    }

}
