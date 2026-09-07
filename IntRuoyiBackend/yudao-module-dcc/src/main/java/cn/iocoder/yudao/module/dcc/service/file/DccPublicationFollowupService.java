package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;

public interface DccPublicationFollowupService {

    void recordPublishedRevision(DccControlledFileDO publishedFile, DccControlledFileDO previousActiveFile);
}
