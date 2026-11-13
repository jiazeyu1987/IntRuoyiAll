package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;

/** Explicit routing for platform-managed, immutable retention storage (reserved negative ids). */
public interface ProtectedFileClientProvider {
    Long getFileConfigId();
    FileClient getFileClient();
}
