package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrTemplateVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrDhrTemplateVersionMapper extends BaseMapperX<MesProEdhrDhrTemplateVersionDO> {

    default List<MesProEdhrDhrTemplateVersionDO> selectListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrDhrTemplateVersionDO>()
                .eq(MesProEdhrDhrTemplateVersionDO::getTemplateId, templateId)
                .orderByDesc(MesProEdhrDhrTemplateVersionDO::getId));
    }
}
