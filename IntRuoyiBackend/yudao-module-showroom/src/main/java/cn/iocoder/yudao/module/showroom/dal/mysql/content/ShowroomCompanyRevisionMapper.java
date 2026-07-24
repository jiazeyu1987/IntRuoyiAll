package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyRevisionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomCompanyRevisionMapper extends BaseMapperX<ShowroomCompanyRevisionDO> {

    default ShowroomCompanyRevisionDO selectLatestByCompanyId(Long companyId) {
        return selectOne(new LambdaQueryWrapperX<ShowroomCompanyRevisionDO>()
                .eq(ShowroomCompanyRevisionDO::getCompanyId, companyId)
                .orderByDesc(ShowroomCompanyRevisionDO::getRevisionNo)
                .orderByDesc(ShowroomCompanyRevisionDO::getId)
                .last("LIMIT 1"));
    }

    default List<ShowroomCompanyRevisionDO> selectPublishedByCompanyId(Long companyId) {
        return selectList(new LambdaQueryWrapperX<ShowroomCompanyRevisionDO>()
                .eq(ShowroomCompanyRevisionDO::getCompanyId, companyId)
                .eq(ShowroomCompanyRevisionDO::getStatus, "PUBLISHED")
                .orderByDesc(ShowroomCompanyRevisionDO::getRevisionNo)
                .orderByDesc(ShowroomCompanyRevisionDO::getId));
    }

}
