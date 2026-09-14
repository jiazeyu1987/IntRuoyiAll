package cn.iocoder.yudao.module.dcc.service.projectcode.access;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.access.DccProjectAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;

import java.util.List;

public interface DccProjectAccessService {

    List<DccProjectAccessRuleDO> getProjectAccessRules(Long projectCodeId);

    List<DccProjectAccessRuleDO> replaceProjectAccessRules(Long projectCodeId,
                                                           List<DccProjectAccessRuleSaveReqVO> rules);

    boolean hasProjectOwner(Long userId, Long projectCodeId);

    boolean hasProjectEditorOrOwner(Long userId, Long projectCodeId);

    void assertProjectOwner(Long userId, Long projectCodeId);

    void assertProjectEditorOrOwner(Long userId, Long projectCodeId);

}
