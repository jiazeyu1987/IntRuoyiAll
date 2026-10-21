package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesDossierUploadIntentDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface MesDossierUploadIntentMapper extends BaseMapperX<MesDossierUploadIntentDO> {
    @Select("SELECT * FROM mes_dossier_upload_intent WHERE id=#{id} AND deleted=0 FOR UPDATE")
    MesDossierUploadIntentDO lock(@Param("id") Long id);
    @InterceptorIgnore(tenantLine="true")
    @Select("SELECT * FROM mes_dossier_upload_intent WHERE deleted=0 AND state IN ('UPLOADING','READY','CLEANING') AND expires_at < CURRENT_TIMESTAMP ORDER BY expires_at,id LIMIT 20")
    List<MesDossierUploadIntentDO> expired();
}
