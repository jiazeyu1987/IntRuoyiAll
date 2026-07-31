package cn.iocoder.yudao.module.infra.service.file;

import java.util.Collection;

public interface NasRecursiveScanService {

    void scan(NasConnectionConfig config, Collection<String> rootPaths, NasRecursiveScanHandler handler);
}
