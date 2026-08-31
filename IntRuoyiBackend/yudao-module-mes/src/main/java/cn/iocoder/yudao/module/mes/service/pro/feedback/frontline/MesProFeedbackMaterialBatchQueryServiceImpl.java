package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_MATERIAL_BATCH_SOURCE_INVALID;

@Service
public class MesProFeedbackMaterialBatchQueryServiceImpl implements MesProFeedbackMaterialBatchQueryService {

    private final MesProWorkOrderMapper workOrderMapper;
    private final ErpKingdeeProductionPickListItemMapper pickListItemMapper;

    public MesProFeedbackMaterialBatchQueryServiceImpl(
            MesProWorkOrderMapper workOrderMapper,
            ErpKingdeeProductionPickListItemMapper pickListItemMapper) {
        this.workOrderMapper = workOrderMapper;
        this.pickListItemMapper = pickListItemMapper;
    }

    @Override
    public List<String> listBatchCodes(Long workOrderId, String materialCode) {
        if (workOrderId == null || workOrderId <= 0) {
            throw invalid("生产工单编号不能为空");
        }
        String normalizedMaterialCode = normalize(materialCode);
        if (normalizedMaterialCode == null) {
            throw invalid("物料编码不能为空");
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        String productionOrderNo = workOrder == null ? null : normalize(workOrder.getCode());
        if (productionOrderNo == null) {
            throw invalid("生产工单不存在或缺少正式订单编号：" + workOrderId);
        }
        List<ErpKingdeeProductionPickListItemDO> rows =
                pickListItemMapper.selectListByProductionOrderNo(productionOrderNo);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> Objects.equals(productionOrderNo, normalize(row.getProductionOrderNo())))
                .filter(row -> Objects.equals(normalizedMaterialCode, normalize(row.getMaterialNumber())))
                .map(ErpKingdeeProductionPickListItemDO::getLotNumber)
                .map(MesProFeedbackMaterialBatchQueryServiceImpl::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static cn.iocoder.yudao.framework.common.exception.ServiceException invalid(String detail) {
        return exception(PRO_FRONTLINE_FEEDBACK_MATERIAL_BATCH_SOURCE_INVALID, detail);
    }
}
