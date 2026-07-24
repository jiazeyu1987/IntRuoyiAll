package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleRespVO;

public interface MesProEdhrLocalStateSampleService {

    EdhrLocalStateSampleRespVO createLocalStateSample(EdhrLocalStateSampleReqVO reqVO);
}
