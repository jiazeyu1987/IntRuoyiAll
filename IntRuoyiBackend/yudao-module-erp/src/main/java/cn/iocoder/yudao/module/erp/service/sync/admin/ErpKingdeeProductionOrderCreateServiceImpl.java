package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeProductionOrderCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeProductionOrderCreateRespVO;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderCreateRequest;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderCreateResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_ORDER_REQUEST_INVALID;

@Service
@Validated
@RequiredArgsConstructor
public class ErpKingdeeProductionOrderCreateServiceImpl implements ErpKingdeeProductionOrderCreateService {

    private final ErpKingdeeConfigService kingdeeConfigService;
    private final ErpKingdeeProductionOrderClient productionOrderClient;

    @Override
    public ErpKingdeeProductionOrderCreateRespVO createProductionOrder(
            ErpKingdeeProductionOrderCreateReqVO reqVO) {
        validateReqVO(reqVO);
        kingdeeConfigService.assertExternalWriteEnabled();
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        ErpKingdeeProductionOrderCreateResult result = productionOrderClient.createAndSubmitProductionOrder(
                properties, buildClientRequest(properties, reqVO));
        return buildRespVO(result);
    }

    private void validateReqVO(ErpKingdeeProductionOrderCreateReqVO reqVO) {
        if (reqVO.getPlannedFinishDate().isBefore(reqVO.getPlannedStartDate())) {
            throw exception(KINGDEE_PRODUCTION_ORDER_REQUEST_INVALID,
                    "plannedFinishDate must not be before plannedStartDate");
        }
    }

    private ErpKingdeeProductionOrderCreateRequest buildClientRequest(ErpKingdeeProperties properties,
                                                                     ErpKingdeeProductionOrderCreateReqVO reqVO) {
        return ErpKingdeeProductionOrderCreateRequest.builder()
                .billNo(reqVO.getBillNo())
                .templateBillNo(properties.getProductionOrder().getTemplateBillNo())
                .materialNumber(reqVO.getMaterialNumber())
                .unitNumber(reqVO.getUnitNumber())
                .quantity(reqVO.getQuantity())
                .plannedStartDate(reqVO.getPlannedStartDate())
                .plannedFinishDate(reqVO.getPlannedFinishDate())
                .sourceBillNo(reqVO.getSourceBillNo())
                .batchNumber(reqVO.getBatchNumber())
                .build();
    }

    private ErpKingdeeProductionOrderCreateRespVO buildRespVO(ErpKingdeeProductionOrderCreateResult result) {
        ErpKingdeeProductionOrderCreateRespVO respVO = new ErpKingdeeProductionOrderCreateRespVO();
        respVO.setErpFid(result.getErpFid());
        respVO.setErpBillNo(result.getErpBillNo());
        respVO.setSaved(result.getSaved());
        respVO.setSubmitted(result.getSubmitted());
        return respVO;
    }

}
