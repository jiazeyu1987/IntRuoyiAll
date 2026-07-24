package cn.iocoder.yudao.module.srm.dal.mysql.tender;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.tender.SrmTenderExpertDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmTenderExpertMapper extends BaseMapperX<SrmTenderExpertDO> {

    default SrmTenderExpertDO selectByExpertName(String expertName) {
        return selectOne(new LambdaQueryWrapperX<SrmTenderExpertDO>()
                .eq(SrmTenderExpertDO::getExpertName, expertName)
                .last("LIMIT 1"));
    }
}
