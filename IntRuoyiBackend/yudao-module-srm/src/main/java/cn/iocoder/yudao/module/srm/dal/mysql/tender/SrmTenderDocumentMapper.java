package cn.iocoder.yudao.module.srm.dal.mysql.tender;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.tender.SrmTenderDocumentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmTenderDocumentMapper extends BaseMapperX<SrmTenderDocumentDO> {

    default SrmTenderDocumentDO selectByProjectId(Long projectId) {
        return selectOne(new LambdaQueryWrapperX<SrmTenderDocumentDO>()
                .eq(SrmTenderDocumentDO::getProjectId, projectId)
                .last("LIMIT 1"));
    }
}
