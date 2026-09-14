package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo;

import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 注册证上传公司候选响应数据")
@Data
public class DccRegistrationCertificateUploadCompanyRespVO {

    @Schema(description = "公司编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "公司编码")
    private String enterpriseCode;

    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    public static DccRegistrationCertificateUploadCompanyRespVO from(MdmEnterpriseRespDTO item) {
        DccRegistrationCertificateUploadCompanyRespVO result =
                new DccRegistrationCertificateUploadCompanyRespVO();
        result.setId(item.getId());
        result.setEnterpriseCode(item.getEnterpriseCode());
        result.setName(item.getName());
        return result;
    }
}
