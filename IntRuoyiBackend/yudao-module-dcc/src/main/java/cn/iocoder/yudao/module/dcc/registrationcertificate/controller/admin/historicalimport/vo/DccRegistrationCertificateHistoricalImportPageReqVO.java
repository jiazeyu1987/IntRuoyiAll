package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DccRegistrationCertificateHistoricalImportPageReqVO extends PageParam {

    private String sourceHash;
}
