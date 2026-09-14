package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DccControlledFileSourceGovernanceBatchMapper
        extends BaseMapperX<DccControlledFileSourceGovernanceBatchDO> {

    @TenantIgnore
    @Select("""
            SELECT *
            FROM dcc_controlled_file_source_governance_batch
            WHERE task_key = #{taskKey}
              AND deleted = 0
            LIMIT 1
            """)
    DccControlledFileSourceGovernanceBatchDO selectByTaskKey(@Param("taskKey") String taskKey);
}
