package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFormalProductionPickListSourceException;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFormalProductionPickListSourceResolver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_MATERIAL_BATCH_SOURCE_INVALID;

@Service
public class MesProFeedbackMaterialBatchQueryServiceImpl implements MesProFeedbackMaterialBatchQueryService {

    private final MesFormalProductionPickListSourceResolver sourceResolver;

    public MesProFeedbackMaterialBatchQueryServiceImpl(
            MesFormalProductionPickListSourceResolver sourceResolver) {
        this.sourceResolver = sourceResolver;
    }

    @Override
    public List<String> listBatchCodes(Long workOrderId, String materialCode) {
        return resolveEvidence(workOrderId, materialCode).batchCodes();
    }

    @Override
    public MesProFeedbackMaterialBatchEvidence resolveEvidence(Long workOrderId, String materialCode) {
        if (workOrderId == null || workOrderId <= 0) {
            throw invalid("生产工单编号不能为空");
        }
        String normalizedMaterialCode = normalize(materialCode);
        if (normalizedMaterialCode == null) {
            throw invalid("物料编码不能为空");
        }
        final MesFormalProductionPickListSourceResolver.Resolution resolution;
        try {
            resolution = sourceResolver.resolve(workOrderId);
        } catch (MesFormalProductionPickListSourceException sourceException) {
            throw invalid("生产工单或正式领料单来源无效：" + sourceException.getMessage());
        }
        List<ErpKingdeeProductionPickListItemDO> matchedItems = resolution.sources().stream()
                .flatMap(source -> source.items().stream())
                .filter(row -> Objects.equals(normalizedMaterialCode, normalize(row.getMaterialNumber())))
                .toList();
        List<String> batchCodes = matchedItems.stream()
                .map(ErpKingdeeProductionPickListItemDO::getLotNumber)
                .map(MesProFeedbackMaterialBatchQueryServiceImpl::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (batchCodes.isEmpty()) {
            throw invalid("正式领料单未包含物料或批号：" + normalizedMaterialCode);
        }
        return new MesProFeedbackMaterialBatchEvidence(normalizedMaterialCode, batchCodes,
                sum(matchedItems, ErpKingdeeProductionPickListItemDO::getRequestedQuantity),
                sum(matchedItems, ErpKingdeeProductionPickListItemDO::getActualQuantity),
                sum(matchedItems, ErpKingdeeProductionPickListItemDO::getBaseActualQuantity),
                resolution.sources().stream().map(source -> source.header().getId()).toList(),
                matchedItems.stream().map(ErpKingdeeProductionPickListItemDO::getId).toList(),
                resolution.hash());
    }

    private static java.math.BigDecimal sum(List<ErpKingdeeProductionPickListItemDO> items,
                                            java.util.function.Function<ErpKingdeeProductionPickListItemDO,
                                                    java.math.BigDecimal> getter) {
        return items.stream().map(getter).filter(Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
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
