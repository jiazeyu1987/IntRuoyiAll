package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionApprovalEventDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MesProBatchRecordVersionApprovalEventMapper
        extends BaseMapperX<MesProBatchRecordVersionApprovalEventDO> {

    default MesProBatchRecordVersionApprovalEventDO selectByApprovalEvent(String approvalInstanceId,
                                                                          String approvalEventId) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordVersionApprovalEventDO>()
                .eq(MesProBatchRecordVersionApprovalEventDO::getApprovalInstanceId, approvalInstanceId)
                .eq(MesProBatchRecordVersionApprovalEventDO::getApprovalEventId, approvalEventId));
    }

    @Delete("DELETE FROM mes_pro_batch_record_version_approval_event WHERE definition_id = #{definitionId}")
    int deleteHardByDefinitionId(@Param("definitionId") Long definitionId);
}
