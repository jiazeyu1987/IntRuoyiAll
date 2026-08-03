package cn.iocoder.yudao.module.mdm.api.product;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductShowroomWorkbookRowDTO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportExcelVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductSaveReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductDO;
import cn.iocoder.yudao.module.mdm.service.product.MdmProductService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Validated
public class MdmProductApiImpl implements MdmProductApi {

    @Resource
    private MdmProductService productService;

    @Override
    public MdmProductRespDTO getProduct(Long id) {
        return toRespDTO(productService.getProduct(id));
    }

    @Override
    public MdmProductRespDTO getEnabledDccProduct(Long id) {
        return toRespDTO(productService.getEnabledDccProduct(id));
    }

    @Override
    public MdmProductRespDTO getEnabledDccProductByDccProductCode(String dccProductCode) {
        return toRespDTO(productService.getEnabledDccProductByDccProductCode(dccProductCode));
    }

    @Override
    public Long createProduct(MdmProductSaveReqVO reqVO) {
        return productService.createProduct(reqVO);
    }

    @Override
    public List<MdmProductRespDTO> listSimpleProducts(String status, Boolean requireDccProductCode, String keyword) {
        return BeanUtils.toBean(productService.listSimpleProducts(status, requireDccProductCode, keyword),
                MdmProductRespDTO.class);
    }

    @Override
    public List<MdmProductShowroomWorkbookRowDTO> exportForShowroomWorkbook(Collection<String> productCodes) {
        return BeanUtils.toBean(productService.exportForShowroomWorkbook(productCodes),
                MdmProductShowroomWorkbookRowDTO.class);
    }

    @Override
    public Map<String, Long> importFromShowroomWorkbook(List<MdmProductShowroomWorkbookRowDTO> rows) {
        return productService.importFromShowroomWorkbook(BeanUtils.toBean(rows, MdmProductImportExcelVO.class));
    }

    private MdmProductRespDTO toRespDTO(MdmProductDO product) {
        return product == null ? null : BeanUtils.toBean(product, MdmProductRespDTO.class);
    }

}
