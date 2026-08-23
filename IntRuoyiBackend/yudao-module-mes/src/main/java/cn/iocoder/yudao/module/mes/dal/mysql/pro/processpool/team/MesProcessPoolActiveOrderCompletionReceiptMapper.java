package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProcessPoolActiveOrderCompletionReceiptMapper
        extends BaseMapperX<MesProcessPoolActiveOrderCompletionReceiptDO> {

    default MesProcessPoolActiveOrderCompletionReceiptDO selectByActiveOrderIdForUpdate(Long activeOrderId) {
        if (activeOrderId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderCompletionReceiptDO>()
                .eq(MesProcessPoolActiveOrderCompletionReceiptDO::getActiveOrderId, activeOrderId)
                .last("FOR UPDATE"));
    }

    default MesProcessPoolActiveOrderCompletionReceiptDO selectByIdempotencyKeyForUpdate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderCompletionReceiptDO>()
                .eq(MesProcessPoolActiveOrderCompletionReceiptDO::getRequestIdempotencyKey, idempotencyKey)
                .last("FOR UPDATE"));
    }

    default MesProcessPoolActiveOrderCompletionReceiptDO selectByIdAndTenantId(Long receiptId, Long tenantId) {
        if (receiptId == null || tenantId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderCompletionReceiptDO>()
                .eq(MesProcessPoolActiveOrderCompletionReceiptDO::getId, receiptId)
                .eq(MesProcessPoolActiveOrderCompletionReceiptDO::getTenantId, tenantId));
    }
}
