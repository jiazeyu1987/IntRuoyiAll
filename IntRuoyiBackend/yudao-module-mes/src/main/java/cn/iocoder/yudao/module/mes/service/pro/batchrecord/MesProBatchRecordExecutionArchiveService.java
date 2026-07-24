package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveDownloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchivePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveRespVO;

public interface MesProBatchRecordExecutionArchiveService {

    MesProBatchRecordExecutionArchiveRespVO generateExecutionArchive(
            MesProBatchRecordExecutionArchiveGenerateReqVO reqVO);

    PageResult<MesProBatchRecordExecutionArchiveRespVO> getExecutionArchivePage(
            MesProBatchRecordExecutionArchivePageReqVO pageReqVO);

    MesProBatchRecordExecutionArchiveRespVO getLatestExecutionArchive(Long executionId, String artifactType);

    MesProBatchRecordExecutionArchiveDownloadRespVO downloadExecutionArchive(Long id);
}
