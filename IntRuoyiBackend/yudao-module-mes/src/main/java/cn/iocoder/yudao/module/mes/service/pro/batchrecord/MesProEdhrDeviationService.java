package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationBatchOptionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationBatchOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationPageReqVO;

import java.util.List;

public interface MesProEdhrDeviationService {

    PageResult<cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationOrderOptionRespVO> getOrderOptions(
            MesProEdhrDeviationBatchOptionPageReqVO reqVO);

    MesProEdhrDeviationRespVO createByWorkOrder(Long actorUserId, MesProEdhrDeviationCreateReqVO reqVO);

    List<MesProEdhrDeviationBatchOptionRespVO> getCreationBatchOptionsByActiveOrder(Long actorUserId, Long activeOrderId);

    PageResult<MesProEdhrDeviationBatchOptionRespVO> getBatchOptions(
            MesProEdhrDeviationBatchOptionPageReqVO reqVO);

    List<MesProEdhrDeviationBatchOptionRespVO> getBatchOptionsByActiveOrder(Long activeOrderId);

    MesProEdhrDeviationRespVO create(Long actorUserId, MesProEdhrDeviationCreateReqVO reqVO);

    PageResult<MesProEdhrDeviationRespVO> getPage(MesProEdhrDeviationPageReqVO reqVO);

    MesProEdhrDeviationRespVO get(Long id);

    void ensureNoOpenDeviationForMarketRelease(Long tenantId, Long batchExecutionId);
}
