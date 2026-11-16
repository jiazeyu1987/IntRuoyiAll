package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchIntegrityChunkReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchIntegrityChunkRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchSearchMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
public class MesProEdhrBatchIntegrityService {
    private static final int CHUNK_SIZE = 128;
    @Resource private MesProEdhrBatchExecutionMapper batchMapper;
    @Resource private MesProEdhrBatchSearchMapper searchMapper;
    @Resource private MesProEdhrBatchVisibilityQueryService visibilityService;
    @Resource private MesProEdhrBatchLookupService lookupService;

    private LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> historyQuery(EdhrBatchExecutionPageReqVO scope) {
        var query = batchMapper.buildPageQuery(scope);
        query.getExpression().getOrderBy().clear();
        return query;
    }

    public EdhrBatchIntegrityChunkRespVO check(EdhrBatchIntegrityChunkReqVO request, Long userId) {
        var scope = new EdhrBatchExecutionPageReqVO();
        scope.setCompletedTraceOnly(true);
        scope.setExcludeStatuses(List.of(50));
        var visibility = visibilityService.prepare(scope, userId);
        Long upperId = request.getUpperId();
        if (upperId == null) {
            upperId = searchMapper.selectIntegrityUpperId(visibility.tenantId(), visibility.apply(historyQuery(scope)));
            if (upperId == null) upperId = 0L;
        }
        long afterId = request.getAfterId() == null ? 0L : request.getAfterId();
        var batches = searchMapper.selectIntegrityChunk(visibility.tenantId(), visibility.apply(historyQuery(scope)),
                afterId, upperId, CHUNK_SIZE);
        var response = new EdhrBatchIntegrityChunkRespVO();
        response.setUpperId(upperId);
        response.setNextId(afterId);
        for (var batch : batches) {
            try {
                lookupService.validateAllFacts(List.of(batch));
            } catch (ServiceException exception) {
                // Only explicit malformed-evidence errors are batch results; permission and service failures abort.
                if (!Objects.equals(exception.getCode(), GlobalErrorCodeConstants.BAD_REQUEST.getCode())) throw exception;
                response.getErrors().add(new EdhrBatchIntegrityChunkRespVO.BatchError(
                        batch.getId(), batch.getBatchExecutionCode(), exception.getMessage()));
            }
            response.setChecked(response.getChecked() + 1);
            response.setNextId(batch.getId());
        }
        response.setStatus(batches.size() < CHUNK_SIZE || response.getNextId() >= upperId ? "COMPLETED" : "RUNNING");
        return response;
    }
}
