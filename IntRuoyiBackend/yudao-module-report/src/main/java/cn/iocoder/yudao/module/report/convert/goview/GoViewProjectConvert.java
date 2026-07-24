package cn.iocoder.yudao.module.report.convert.goview;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.report.controller.admin.goview.vo.project.GoViewProjectCreateReqVO;
import cn.iocoder.yudao.module.report.controller.admin.goview.vo.project.GoViewProjectRespVO;
import cn.iocoder.yudao.module.report.controller.admin.goview.vo.project.GoViewProjectUpdateReqVO;
import cn.iocoder.yudao.module.report.dal.dataobject.goview.GoViewProjectDO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface GoViewProjectConvert {

    GoViewProjectConvert INSTANCE = Mappers.getMapper(GoViewProjectConvert.class);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "name", target = "name")
    GoViewProjectDO convert(GoViewProjectCreateReqVO bean);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "content", target = "content")
    @Mapping(source = "picUrl", target = "picUrl")
    @Mapping(source = "remark", target = "remark")
    GoViewProjectDO convert(GoViewProjectUpdateReqVO bean);

    GoViewProjectRespVO convert(GoViewProjectDO bean);

    PageResult<GoViewProjectRespVO> convertPage(PageResult<GoViewProjectDO> page);

}
