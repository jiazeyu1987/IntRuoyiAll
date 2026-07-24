package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.module.infra.service.file.NasAclReadResult;

public interface DccNasPermissionSnapshotCaptureService {

    void captureDirectorySnapshot(Long transferTaskId,
                                  Long transferTaskItemId,
                                  String nasPath,
                                  Long dccDirectoryId,
                                  NasAclReadResult acl);

    void completeSnapshotForTask(Long transferTaskId);
}
