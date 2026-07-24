package cn.iocoder.yudao.module.infra.convert.config;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ConfigConvert {

    ConfigConvert INSTANCE = Mappers.getMapper(ConfigConvert.class);

    PageResult<ConfigRespVO> convertPage(PageResult<ConfigDO> page);

    List<ConfigRespVO> convertList(List<ConfigDO> list);

    @Mapping(source = "configKey", target = "key")
    ConfigRespVO convert(ConfigDO bean);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "key", target = "configKey")
    @Mapping(source = "id", target = "id")
    @Mapping(source = "category", target = "category")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "value", target = "value")
    @Mapping(source = "visible", target = "visible")
    @Mapping(source = "remark", target = "remark")
    ConfigDO convert(ConfigSaveReqVO bean);

}
