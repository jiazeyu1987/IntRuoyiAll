package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixEffectivePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryViewMatrixUserLookupRespVO;

import java.util.List;

public interface DccCategoryViewMatrixAdminService {

    List<DccCategoryViewMatrixRowRespVO> getViewMatrixRows(String code, String name, Boolean active,
                                                           Boolean configured);

    DccCategoryViewMatrixEffectivePreviewRespVO previewViewMatrix(Long categoryId,
                                                                  DccCategoryViewMatrixSaveReqVO reqVO);

    List<DccCategoryViewMatrixRowRespVO.Rule> saveViewMatrix(Long categoryId,
                                                             DccCategoryViewMatrixSaveReqVO reqVO);

    List<DccCategoryViewMatrixRowRespVO.Rule> importViewMatrix(Long categoryId,
                                                               DccCategoryViewMatrixSaveReqVO reqVO);

    List<DccCategoryViewMatrixUserLookupRespVO> getUserViewMatrixAccess(Long userId);
}
