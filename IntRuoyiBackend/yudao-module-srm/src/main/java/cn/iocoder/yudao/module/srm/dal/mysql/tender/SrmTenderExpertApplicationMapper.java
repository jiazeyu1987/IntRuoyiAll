package cn.iocoder.yudao.module.srm.dal.mysql.tender;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.tender.SrmTenderExpertApplicationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmTenderExpertApplicationMapper extends BaseMapperX<SrmTenderExpertApplicationDO> {

    default SrmTenderExpertApplicationDO selectByProjectId(Long projectId) {
        return selectOne(new LambdaQueryWrapperX<SrmTenderExpertApplicationDO>()
                .eq(SrmTenderExpertApplicationDO::getProjectId, projectId)
                .last("LIMIT 1"));
    }
}
