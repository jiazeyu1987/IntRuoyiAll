package cn.iocoder.yudao.module.mes.service.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportAttributeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportBatchSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportConfirmBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MesProFeedbackImportRecordService {

    PageResult<MesProFeedbackImportRecordRespVO> getImportRecordPage(MesProFeedbackImportRecordPageReqVO reqVO);

    MesProFeedbackImportBatchSummaryRespVO getImportRecordBatchSummary(Collection<Long> importRecordIds);

    List<MesProFeedbackImportCandidateRespVO> getAttributionCandidates(Long importRecordId);

    Long attributeImportRecord(MesProFeedbackImportAttributeReqVO reqVO);

    Long reattributeImportRecord(MesProFeedbackImportAttributeReqVO reqVO);

    void confirmImportRecordBatch(MesProFeedbackImportConfirmBatchReqVO reqVO);

    Map<Long, MesProFeedbackImportRecordDO> getImportRecordMapByFeedbackIds(Collection<Long> feedbackIds);

    Map<Long, MesProFeedbackImportRecordDO> getImportRecordMapByFeedbacks(Collection<MesProFeedbackDO> feedbacks);
}
