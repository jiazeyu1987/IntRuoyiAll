package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordbookEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_RECORD_BOOK_EVENT_MISSING;

@Service
@Validated
public class MesProFrontlineRecordbookEntryServiceImpl implements MesProFrontlineRecordbookEntryService {

    private static final String EVENT_ENTRY_CREATE = "ENTRY_CREATE";

    @Resource
    private MesProEdhrRecordbookService recordbookService;
    @Resource
    private MesProEdhrRecordbookEventMapper recordbookEventMapper;

    @Override
    public MesProFrontlineRecordbookEntryResult createOriginalEntry(MesProFrontlineRecordbookEntryPayload payload) {
        MesProEdhrRecordbookEntryCreateReqVO reqVO = new MesProEdhrRecordbookEntryCreateReqVO()
                .setRecordbookId(payload.getRecordbookId())
                .setEntryTitle(payload.getEntryTitle())
                .setEntryContent(payload.getEntryContent())
                .setTagCodes(payload.getTagCodes())
                .setIdempotencyKey(payload.getIdempotencyKey())
                .setRemark(payload.getRemark());
        MesProEdhrRecordbookEntryRespVO entry = recordbookService.createEntry(reqVO);
        MesProEdhrRecordbookEventDO event =
                recordbookEventMapper.selectLatestByEntryIdAndEventType(entry.getId(), EVENT_ENTRY_CREATE);
        if (event == null) {
            throw exception(PRO_FRONTLINE_RECORD_BOOK_EVENT_MISSING, entry.getId());
        }
        return new MesProFrontlineRecordbookEntryResult(entry.getId(), event.getId());
    }

}
