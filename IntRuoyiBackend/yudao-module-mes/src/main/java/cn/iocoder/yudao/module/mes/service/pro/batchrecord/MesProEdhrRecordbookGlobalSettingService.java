package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordbookGlobalSettingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordbookGlobalSettingUpdateReqVO;
import jakarta.validation.Valid;

public interface MesProEdhrRecordbookGlobalSettingService {

    String CONFIG_KEY = "mes.edhr.recordbook.global.enabled";

    EdhrRecordbookGlobalSettingRespVO getGlobalSetting();

    EdhrRecordbookGlobalSettingRespVO updateGlobalSetting(@Valid EdhrRecordbookGlobalSettingUpdateReqVO reqVO);

    boolean isGlobalRecordbookEnabled();

    Boolean resolveEffectiveRecordbookEnabled(Boolean recordbookEnabled, String recordCategory);

    void requireRecordbookWriteAllowed(Boolean recordbookEnabled, String recordCategory);
}
