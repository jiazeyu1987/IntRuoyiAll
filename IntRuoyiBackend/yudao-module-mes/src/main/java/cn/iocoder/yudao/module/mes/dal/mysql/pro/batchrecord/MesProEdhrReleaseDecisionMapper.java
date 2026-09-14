package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseDecisionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrReleaseDecisionMapper extends BaseMapperX<MesProEdhrReleaseDecisionDO> {

    default MesProEdhrReleaseDecisionDO selectByTransactionIdAndStatusAndIdempotencyKey(
            Long releaseTransactionId, String decisionStatus, String idempotencyKey) {
        if (releaseTransactionId == null || decisionStatus == null || idempotencyKey == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseDecisionDO>()
                .eq(MesProEdhrReleaseDecisionDO::getReleaseTransactionId, releaseTransactionId)
                .eq(MesProEdhrReleaseDecisionDO::getDecisionStatus, decisionStatus)
                .eq(MesProEdhrReleaseDecisionDO::getIdempotencyKey, idempotencyKey));
    }

    default MesProEdhrReleaseDecisionDO selectReleasedByTransactionIdForUpdate(Long releaseTransactionId) {
        if (releaseTransactionId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseDecisionDO>()
                .eq(MesProEdhrReleaseDecisionDO::getReleaseTransactionId, releaseTransactionId)
                .eq(MesProEdhrReleaseDecisionDO::getDecisionStatus, "RELEASED")
                .last("FOR UPDATE"));
    }

    default MesProEdhrReleaseDecisionDO selectByTransactionIdForUpdate(Long releaseTransactionId) {
        if (releaseTransactionId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseDecisionDO>()
                .eq(MesProEdhrReleaseDecisionDO::getReleaseTransactionId, releaseTransactionId)
                .last("FOR UPDATE"));
    }
}
