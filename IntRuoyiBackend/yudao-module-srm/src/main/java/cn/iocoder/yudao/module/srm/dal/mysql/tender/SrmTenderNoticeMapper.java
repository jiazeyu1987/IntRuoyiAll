package cn.iocoder.yudao.module.srm.dal.mysql.tender;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.tender.SrmTenderNoticeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmTenderNoticeMapper extends BaseMapperX<SrmTenderNoticeDO> {

    default SrmTenderNoticeDO selectByProjectId(Long projectId) {
        return selectOne(new LambdaQueryWrapperX<SrmTenderNoticeDO>()
                .eq(SrmTenderNoticeDO::getProjectId, projectId)
                .last("LIMIT 1"));
    }
}
