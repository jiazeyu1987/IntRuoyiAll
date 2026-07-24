package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;

import java.util.List;

public interface DccDirectoryAdminService {

    Long createDirectory(cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO reqVO);

    void updateDirectory(cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO reqVO);

    DccDirectoryImportResult importDirectoriesFromIntAuth();

    List<DccFileDirectoryDO> getDirectoryTree(Long userId);

    List<DccVisibleDirectoryNode> listVisibleChildDirectories(Long userId, Long parentId);

    List<DccVisibleDirectoryNode> searchVisibleDirectories(Long userId, String keyword, Integer limit);

    DccFileDirectoryDO getDirectory(Long userId, Long id);

    DccDirectoryDeleteSubtreeResult deleteDirectorySubtree(Long id, String confirmText);

    List<DccDirectoryAccessRuleDirectorySummary> listAccessRuleDirectories();

    List<DccDirectoryAccessRuleDO> getAccessRules(Long directoryId);

    void deleteAccessRules(Long directoryId);

    void replaceAccessRules(Long directoryId, List<DccDirectoryAccessRuleSaveReqVO> rules);
}
