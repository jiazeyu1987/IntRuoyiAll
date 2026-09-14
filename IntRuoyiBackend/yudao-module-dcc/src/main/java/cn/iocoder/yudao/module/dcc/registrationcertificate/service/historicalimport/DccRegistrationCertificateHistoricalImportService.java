package cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportPageReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportRespVO;

public interface DccRegistrationCertificateHistoricalImportService {

    PageResult<DccRegistrationCertificateHistoricalImportRespVO> getHistoricalImportPage(
            DccRegistrationCertificateHistoricalImportPageReqVO reqVO);
}
