package cn.iocoder.yudao.module.system.service.tablecolumn;

import cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo.UserTableColumnConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo.UserTableColumnConfigSaveReqVO;

public interface UserTableColumnConfigService {

    UserTableColumnConfigRespVO getConfig(String tableKey);

    void saveConfig(UserTableColumnConfigSaveReqVO reqVO);

    void resetConfig(String tableKey);

}
