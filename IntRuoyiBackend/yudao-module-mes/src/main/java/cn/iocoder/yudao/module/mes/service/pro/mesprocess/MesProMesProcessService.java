package cn.iocoder.yudao.module.mes.service.pro.mesprocess;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessRespVO;

import jakarta.validation.Valid;

public interface MesProMesProcessService {

    PageResult<MesProMesProcessRespVO> getMesProcessPage(@Valid MesProMesProcessPageReqVO pageReqVO);
}
