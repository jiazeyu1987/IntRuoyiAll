package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - DCC controlled file product option response")
@Data
public class DccControlledFileProductOptionRespVO {

    @Schema(description = "Product master id")
    private Long id;

    @Schema(description = "Product code")
    private String productCode;

    @Schema(description = "DCC product code")
    private String dccProductCode;

    @Schema(description = "Chinese product name")
    private String nameCn;

    @Schema(description = "English product name")
    private String nameEn;

    @Schema(description = "Model specification")
    private String modelSpecification;

    @Schema(description = "Product category")
    private String category;

    @Schema(description = "Product status")
    private String status;

    public static DccControlledFileProductOptionRespVO from(MdmProductRespDTO product) {
        DccControlledFileProductOptionRespVO respVO = new DccControlledFileProductOptionRespVO();
        respVO.setId(product.getId());
        respVO.setProductCode(product.getProductCode());
        respVO.setDccProductCode(product.getDccProductCode());
        respVO.setNameCn(product.getNameCn());
        respVO.setNameEn(product.getNameEn());
        respVO.setModelSpecification(product.getModelSpecification());
        respVO.setCategory(product.getCategory());
        respVO.setStatus(product.getStatus());
        return respVO;
    }
}
