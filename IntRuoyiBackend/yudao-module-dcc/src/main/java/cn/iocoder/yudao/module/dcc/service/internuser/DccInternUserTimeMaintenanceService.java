package cn.iocoder.yudao.module.dcc.service.internuser;

import cn.iocoder.yudao.module.dcc.controller.admin.internuser.vo.DccInternUserTimeUpdateReqVO;

public interface DccInternUserTimeMaintenanceService {

    void updatePublishedTime(DccInternUserTimeUpdateReqVO reqVO);

    void updateObsoletedTime(DccInternUserTimeUpdateReqVO reqVO);

}
