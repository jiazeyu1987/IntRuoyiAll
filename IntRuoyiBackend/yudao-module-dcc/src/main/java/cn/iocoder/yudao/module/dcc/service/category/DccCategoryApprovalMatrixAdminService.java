package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixEffectivePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixUserLookupRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;

import java.util.List;

public interface DccCategoryApprovalMatrixAdminService {

    DccCategoryApprovalMatrixRespVO getApprovalMatrix(Long categoryId);

    DccCategoryApprovalRouteDO saveApprovalMatrix(Long categoryId, DccCategoryApprovalMatrixSaveReqVO reqVO);

    DccCategoryApprovalRouteDO importApprovalMatrix(Long categoryId, DccCategoryApprovalMatrixSaveReqVO reqVO);

    List<DccCategoryReviewMatrixRowRespVO> getReviewMatrixRows(String code, String name, Boolean active,
                                                               Boolean configured);

    DccCategoryReviewMatrixEffectivePreviewRespVO previewApprovalMatrix(Long categoryId,
                                                                        DccCategoryApprovalMatrixSaveReqVO reqVO);

    List<DccCategoryReviewMatrixUserLookupRespVO> getUserReviewMatrixAccess(Long userId);

    void deleteApprovalMatrix(Long categoryId);
}
