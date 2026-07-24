package cn.iocoder.yudao.module.mdm.service.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportExcelVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductImportPreviewRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductPageReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductReferenceRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductSaveReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MdmProductService {

    Long createProduct(MdmProductSaveReqVO reqVO);

    void updateProduct(MdmProductSaveReqVO reqVO);

    void updateProductStatus(Long id, String status);

    MdmProductDO getProduct(Long id);

    MdmProductDO getEnabledDccProduct(Long id);

    MdmProductDO getEnabledDccProductByDccProductCode(String dccProductCode);

    PageResult<MdmProductDO> getProductPage(MdmProductPageReqVO reqVO);

    List<MdmProductDO> listSimpleProducts(String status, Boolean requireDccProductCode, String keyword);

    MdmProductImportPreviewRespVO previewImport(List<MdmProductImportExcelVO> rows);

    MdmProductImportPreviewRespVO confirmImport(Long batchId);

    List<MdmProductImportExcelVO> exportForShowroomWorkbook(Collection<String> productCodes);

    Map<String, Long> importFromShowroomWorkbook(List<MdmProductImportExcelVO> rows);

    MdmProductReferenceRespVO getReferences(Long productId);

}
