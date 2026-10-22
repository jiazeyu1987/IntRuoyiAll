package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.access.DccProjectAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;

import java.util.List;

public interface DccProjectAccessService {

    /** Called only inside the approved creation transaction; never grants a pre-existing project. */
    void initializeApprovedProjectLeaderOwner(Long projectCodeId, Long selectedLeaderUserId, String reason);

    List<DccProjectAccessRuleDO> getProjectAccessRules(Long projectCodeId);

    List<DccProjectAccessRuleDO> replaceProjectAccessRules(Long projectCodeId,
                                                           List<DccProjectAccessRuleSaveReqVO> rules);

    boolean hasProjectOwner(Long userId, Long projectCodeId);

    boolean hasProjectEditorOrOwner(Long userId, Long projectCodeId);

    void assertProjectOwner(Long userId, Long projectCodeId);

    void assertProjectEditorOrOwner(Long userId, Long projectCodeId);

    /** 只读项目目录范围；不授予配置写入或文件正文权限。 */
    void assertProjectViewerOrAbove(Long userId, Long projectCodeId);

    /** 当前正式项目规则与既有分配硬范围的只读交集，不由文件存在与否推断。 */
    List<Long> listReadableProjectIds(Long userId);

}
