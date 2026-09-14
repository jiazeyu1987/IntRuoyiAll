package cn.iocoder.yudao.module.system.service.profileworkbench;

import cn.iocoder.yudao.module.system.controller.admin.profileworkbench.vo.ProfileWorkbenchTaskVisibilitySaveReqVO;

import java.util.List;

public interface ProfileWorkbenchTaskVisibilityService {

    List<String> getHiddenTaskKeys();

    void hideTask(ProfileWorkbenchTaskVisibilitySaveReqVO reqVO);

    void restoreTask(String taskKey);
}
