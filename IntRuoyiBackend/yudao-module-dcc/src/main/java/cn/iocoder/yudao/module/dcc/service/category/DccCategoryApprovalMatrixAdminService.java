package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixEffectivePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixRowRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryReviewMatrixUserLookupRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;

import java.util.List;
import java.util.Map;

public interface DccCategoryApprovalMatrixAdminService {

    DccCategoryApprovalMatrixRespVO getApprovalMatrix(Long categoryId);

    DccCategoryApprovalRouteDO saveApprovalMatrix(Long categoryId, DccCategoryApprovalMatrixSaveReqVO reqVO);

    DccCategoryApprovalRouteDO importApprovalMatrix(Long categoryId, DccCategoryApprovalMatrixSaveReqVO reqVO);

    List<DccCategoryReviewMatrixRowRespVO> getReviewMatrixRows(String code, String name, Boolean active,
                                                               Boolean configured);

    DccCategoryReviewMatrixEffectivePreviewRespVO previewApprovalMatrix(Long categoryId,
                                                                        DccCategoryApprovalMatrixSaveReqVO reqVO);

    List<DccCategoryReviewMatrixUserLookupRespVO> getUserReviewMatrixAccess(Long userId);

    Map<Long, MatrixPositionIds> getActiveMatrixPositionIdsByCategoryIds(List<Long> categoryIds);

    void deleteApprovalMatrix(Long categoryId);

    record MatrixPositionIds(List<Long> signoffPositionIds, List<Long> approvalPositionIds) {
    }
}
