package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** The formal Root mapping is authoritative; this mapper never allocates a round or guesses one. */
@Mapper
public interface DccControlledFileApplicationEvidenceMapper {
    @Select("""
        SELECT application_id AS controlled_file_id, application_type, bpm_round, attribute_round
        FROM dcc_application_round_link
        WHERE tenant_id=#{tenant} AND project_id=#{project} AND application_id=#{file} AND bpm_round IS NOT NULL
        ORDER BY application_type, attribute_round DESC
        """)
    List<cn.iocoder.yudao.module.dcc.service.file.DccApplicationRoundSummary> selectApplicationRounds(
            @Param("tenant") Long tenant, @Param("project") Long project, @Param("file") Long file);

    @Select("""
        SELECT attribute_round FROM dcc_application_round_link
        WHERE tenant_id=#{tenant} AND project_id=#{project} AND application_type=#{type}
              AND application_id=#{file} AND bpm_round=#{bpm}
        """)
    List<Integer> selectBoundRounds(@Param("tenant") Long tenant, @Param("project") Long project,
            @Param("type") String type, @Param("file") Long file, @Param("bpm") String bpm);
    @Select("""
        SELECT * FROM dcc_project_application_attributes
        WHERE tenant_id=#{tenant} AND project_code_id=#{project} AND application_type=#{type}
              AND application_id=#{file} AND application_round=#{round} AND deleted=0
        """)
    DccProjectApplicationAttributesDO selectSnapshot(@Param("tenant") Long tenant, @Param("project") Long project,
            @Param("type") String type, @Param("file") Long file, @Param("round") Integer round);
}
