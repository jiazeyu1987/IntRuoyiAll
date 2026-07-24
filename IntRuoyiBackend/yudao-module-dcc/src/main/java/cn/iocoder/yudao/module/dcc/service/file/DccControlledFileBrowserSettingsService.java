package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccBrowserExtensionBlacklistRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccBrowserExtensionBlacklistSaveReqVO;

import java.util.List;

public interface DccControlledFileBrowserSettingsService {

    DccBrowserExtensionBlacklistRespVO getExtensionBlacklist();

    List<String> getBlacklistedExtensionPatterns();

    void saveExtensionBlacklist(DccBrowserExtensionBlacklistSaveReqVO reqVO);

}
