package cn.iocoder.yudao.module.srm.dal.mysql.tender;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.tender.SrmTenderWinningResultDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmTenderWinningResultMapper extends BaseMapperX<SrmTenderWinningResultDO> {

    default SrmTenderWinningResultDO selectByProjectId(Long projectId) {
        return selectOne(new LambdaQueryWrapperX<SrmTenderWinningResultDO>()
                .eq(SrmTenderWinningResultDO::getProjectId, projectId)
                .last("LIMIT 1"));
    }
}
