package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageRespVO;

public interface MesProEdhrFormFillLogService {

    PageResult<MesProEdhrFormFillLogPageRespVO> getPage(MesProEdhrFormFillLogPageReqVO reqVO);

    MesProEdhrFormFillLogDetailRespVO getDetail(Long auditBatchId);
}
