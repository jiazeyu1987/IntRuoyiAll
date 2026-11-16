package cn.iocoder.yudao.module.mes.dal.mysql.dv.subject;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.subject.vo.MesDvSubjectPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.subject.MesDvSubjectDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesDvSubjectMapper extends BaseMapperX<MesDvSubjectDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("code", ListFilterQueryUtils.text("code")),
            java.util.Map.entry("name", ListFilterQueryUtils.text("name")),
            java.util.Map.entry("type", ListFilterQueryUtils.number("type")),
            java.util.Map.entry("content", ListFilterQueryUtils.text("content")),
            java.util.Map.entry("standard", ListFilterQueryUtils.text("standard")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator"))
    );



    default PageResult<MesDvSubjectDO> selectPage(MesDvSubjectPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesDvSubjectDO>()
                .likeIfPresent(MesDvSubjectDO::getCode, reqVO.getCode())
                .likeIfPresent(MesDvSubjectDO::getName, reqVO.getName())
                .eqIfPresent(MesDvSubjectDO::getType, reqVO.getType())
                .eqIfPresent(MesDvSubjectDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesDvSubjectDO::getId), LIST_FILTER_FIELDS));
    }

    default MesDvSubjectDO selectByCode(String code) {
        return selectOne(MesDvSubjectDO::getCode, code);
    }

}
