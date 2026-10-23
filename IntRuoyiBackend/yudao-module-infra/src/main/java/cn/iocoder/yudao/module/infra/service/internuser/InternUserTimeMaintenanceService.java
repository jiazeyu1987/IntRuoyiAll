package cn.iocoder.yudao.module.infra.service.internuser;

import cn.iocoder.yudao.module.infra.controller.admin.internuser.vo.InternUserFileUploadTimeUpdateReqVO;

public interface InternUserTimeMaintenanceService {

    /**
     * 修改文件记录的上传时间。
     *
     * @param reqVO 修改请求
     */
    void updateFileUploadTime(InternUserFileUploadTimeUpdateReqVO reqVO);

}
