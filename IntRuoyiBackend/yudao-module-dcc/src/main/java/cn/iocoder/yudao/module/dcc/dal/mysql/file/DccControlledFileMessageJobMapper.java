package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DccControlledFileMessageJobMapper extends BaseMapperX<DccControlledFileMessageJobDO> {
    @Select("SELECT * FROM dcc_controlled_file_message_job WHERE tenant_id=#{tenantId} AND distribution_batch_id=#{batchId} AND deleted=0 ORDER BY id FOR UPDATE")
    java.util.List<DccControlledFileMessageJobDO> selectBatchJobsForUpdate(@Param("tenantId") Long tenantId,@Param("batchId") Long batchId);
    @Select("SELECT * FROM dcc_controlled_file_message_job WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
    DccControlledFileMessageJobDO selectByIdAndTenantForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
