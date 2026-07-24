package cn.iocoder.yudao.module.srm.service.naslocator;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorBlacklistRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorBlacklistSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorFileRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorStatusRespVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

public interface SrmNasLocatorService {

    SrmNasLocatorStatusRespVO getStatus();

    SrmNasLocatorBlacklistRespVO getBlacklist();

    void saveBlacklist(@Valid SrmNasLocatorBlacklistSaveReqVO reqVO);

    PageResult<SrmNasLocatorFileRespVO> getFilePage(@Valid SrmNasLocatorPageReqVO pageReqVO);

    void triggerRefresh();

    void download(Long id, HttpServletResponse response) throws Exception;
}
