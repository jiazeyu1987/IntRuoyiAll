package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo;

import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 注册证上传受托企业候选响应数据")
@Data
public class DccRegistrationCertificateUploadEntrustedEnterpriseRespVO {

    @Schema(description = "受托企业编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "受托企业编码")
    private String enterpriseCode;

    @Schema(description = "受托企业名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    public static DccRegistrationCertificateUploadEntrustedEnterpriseRespVO from(MdmEnterpriseRespDTO item) {
        DccRegistrationCertificateUploadEntrustedEnterpriseRespVO result =
                new DccRegistrationCertificateUploadEntrustedEnterpriseRespVO();
        result.setId(item.getId());
        result.setEnterpriseCode(item.getEnterpriseCode());
        result.setName(item.getName());
        return result;
    }
}
