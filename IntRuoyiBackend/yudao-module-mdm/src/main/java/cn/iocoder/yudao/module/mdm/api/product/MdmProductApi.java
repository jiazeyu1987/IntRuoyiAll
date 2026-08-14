package cn.iocoder.yudao.module.mdm.api.product;

import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductShowroomWorkbookRowDTO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductSaveReqVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MdmProductApi {

    MdmProductRespDTO getProduct(Long id);

    MdmProductRespDTO getEnabledDccProduct(Long id);

    MdmProductRespDTO getEnabledDccProductByDccProductCode(String dccProductCode);

    Long createProduct(MdmProductSaveReqVO reqVO);

    List<MdmProductRespDTO> listSimpleProducts(String status, Boolean requireDccProductCode, String keyword);

    List<MdmProductShowroomWorkbookRowDTO> exportForShowroomWorkbook(Collection<String> productCodes);

    Map<String, Long> importFromShowroomWorkbook(List<MdmProductShowroomWorkbookRowDTO> rows);

}
