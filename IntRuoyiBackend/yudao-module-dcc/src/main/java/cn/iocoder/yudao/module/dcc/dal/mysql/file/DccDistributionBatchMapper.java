package cn.iocoder.yudao.module.dcc.dal.mysql.file;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccDistributionBatchDO;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface DccDistributionBatchMapper extends BaseMapperX<DccDistributionBatchDO> {
    @Select("SELECT * FROM dcc_distribution_batch WHERE tenant_id=#{tenant} AND controlled_file_id=#{file} AND approval_process_instance_id=#{round} AND deleted=0")
    DccDistributionBatchDO selectVersion(@Param("tenant") Long tenant,@Param("file") Long file,@Param("round") String round);
    @Select("SELECT * FROM dcc_distribution_batch WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=0 FOR UPDATE")
    DccDistributionBatchDO lock(@Param("tenant") Long tenant,@Param("id") Long id);
    @Select("SELECT * FROM dcc_distribution_batch WHERE tenant_id=#{tenant} AND controlled_file_id=#{file} AND approval_process_instance_id=#{round} AND deleted=0 FOR UPDATE")
    DccDistributionBatchDO lockVersion(@Param("tenant") Long tenant,@Param("file") Long file,@Param("round") String round);
    @Select("SELECT * FROM dcc_distribution_batch WHERE tenant_id=#{tenant} AND status IN ('PENDING','FAILED','DISPATCH_READY') AND deleted=0 ORDER BY id")
    List<DccDistributionBatchDO> recoverable(@Param("tenant") Long tenant);
}
