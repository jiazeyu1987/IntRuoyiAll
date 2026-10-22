package cn.iocoder.yudao.module.dcc.dal.mysql.file;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccDistributionDispatchAttemptDO;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface DccDistributionDispatchAttemptMapper extends BaseMapperX<DccDistributionDispatchAttemptDO> {
    @Select("SELECT * FROM dcc_distribution_dispatch_attempt WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=0 FOR UPDATE")
    DccDistributionDispatchAttemptDO lock(@Param("tenant") Long tenant,@Param("id") Long id);
    @Select("SELECT * FROM dcc_distribution_dispatch_attempt WHERE tenant_id=#{tenant} AND message_job_id=#{job} AND deleted=0 ORDER BY attempt_no FOR UPDATE")
    List<DccDistributionDispatchAttemptDO> lockJob(@Param("tenant") Long tenant,@Param("job") Long job);
}
