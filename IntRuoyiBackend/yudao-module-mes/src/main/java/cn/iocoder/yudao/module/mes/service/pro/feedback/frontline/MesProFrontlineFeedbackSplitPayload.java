package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventCreateReqBO;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProFrontlineFeedbackSplitPayload {

    private MesProFeedbackSaveReqVO feedbackPayload;
    private MesProFrontlineRecordbookEntryPayload recordbookEntryPayload;
    private MesProcessPoolSubmitEventCreateReqBO processPoolEventPayload;

}
