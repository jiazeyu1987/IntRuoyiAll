package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceVerifyReqVO;

public interface MesProBatchRecordDomainTraceService {

    MesProBatchRecordDomainTraceDetailRespVO getTraceDetail(Long executionId);

    PageResult<MesProBatchRecordDomainTracePageRespVO> getTracePage(MesProBatchRecordDomainTracePageReqVO pageReqVO);

    MesProBatchRecordDomainTraceDetailRespVO verify(MesProBatchRecordDomainTraceVerifyReqVO reqVO);

    MesProBatchRecordDomainTraceDetailRespVO verifyForSubmit(Long executionId);

    MesProBatchRecordDomainTraceDetailRespVO verifyForApproval(Long executionId, String expectedDomainTraceHash);

    MesProBatchRecordDomainTraceDetailRespVO verifyForArchive(Long executionId, String expectedDomainTraceHash);
}
