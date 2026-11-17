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

    /** 项目代码基础资料维护：项目代码管理员或正式 EDIT/OWNER，不授予项目文件编辑权限。 */
    void assertProjectMetadataEditable(Long userId, Long projectCodeId);

    record EditorScope(boolean administrator, java.util.Set<Long> projectIds, boolean qms, boolean general) { }
    /** Current permission configuration only; task rows remain selected/countable in SQL. */
    EditorScope editorScope(Long userId);

    /** 校验项目查看权限；同租户超级管理员、租户管理员拥有全部项目 OWNER，其余账号读取正式规则。 */
    void assertProjectViewerOrAbove(Long userId, Long projectCodeId);

    /** 同租户超级管理员、租户管理员可读全部启用项目，其余账号取正式规则与分配范围的交集。 */
    List<Long> listReadableProjectIds(Long userId);

}
