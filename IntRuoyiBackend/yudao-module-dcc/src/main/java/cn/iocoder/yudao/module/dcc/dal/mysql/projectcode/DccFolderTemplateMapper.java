package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccFolderTemplateDO;
import org.apache.ibatis.annotations.*;
@Mapper
public interface DccFolderTemplateMapper extends BaseMapperX<DccFolderTemplateDO> {
    @Select("SELECT * FROM dcc_folder_template WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    DccFolderTemplateDO selectByIdForUpdate(@Param("id") Long id);
}
