package cn.iocoder.yudao.module.mdm.dal.mysql.enterprise;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterprisePageReqVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MdmEnterpriseMapper extends BaseMapperX<MdmEnterpriseDO> {

    default PageResult<MdmEnterpriseDO> selectPage(MdmEnterprisePageReqVO reqVO) {
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        LambdaQueryWrapperX<MdmEnterpriseDO> wrapper = new LambdaQueryWrapperX<MdmEnterpriseDO>()
                .likeIfPresent(MdmEnterpriseDO::getEnterpriseCode, reqVO.getEnterpriseCode())
                .likeIfPresent(MdmEnterpriseDO::getName, reqVO.getName())
                .eqIfPresent(MdmEnterpriseDO::getType, reqVO.getType())
                .eqIfPresent(MdmEnterpriseDO::getStatus, reqVO.getStatus())
                .orderByDesc(MdmEnterpriseDO::getId);
        if (keyword != null) {
            wrapper.and(item -> item.like(MdmEnterpriseDO::getEnterpriseCode, keyword)
                    .or().like(MdmEnterpriseDO::getName, keyword));
        }
        return selectPage(reqVO, wrapper);
    }

    default List<MdmEnterpriseDO> selectSimpleList(String type, String status, String keyword) {
        LambdaQueryWrapperX<MdmEnterpriseDO> wrapper = new LambdaQueryWrapperX<MdmEnterpriseDO>()
                .eqIfPresent(MdmEnterpriseDO::getType, type)
                .eqIfPresent(MdmEnterpriseDO::getStatus, status);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(item -> item.like(MdmEnterpriseDO::getEnterpriseCode, keyword)
                    .or().like(MdmEnterpriseDO::getName, keyword));
        }
        wrapper.orderByAsc(MdmEnterpriseDO::getName).orderByAsc(MdmEnterpriseDO::getId);
        return selectList(wrapper);
    }

    @Select("""
            <script>
            SELECT id,
                   tenant_id,
                   deleted,
                   enterprise_code,
                   name,
                   type,
                   status,
                   revision
            FROM mdm_enterprise
            WHERE id IN
            <foreach collection="enterpriseIds" item="enterpriseId" open="(" separator="," close=")">
                #{enterpriseId}
            </foreach>
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<MdmEnterpriseDO> selectClassificationByIds(@Param("enterpriseIds") Collection<Long> enterpriseIds);

    @Select("""
            <script>
            SELECT id,
                   tenant_id,
                   deleted,
                   enterprise_code,
                   name,
                   type,
                   status,
                   revision
            FROM mdm_enterprise
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND status = 'ENABLE'
              AND type IN
              <foreach collection="types" item="type" open="(" separator="," close=")">
                  #{type}
              </foreach>
              <if test="keyword != null and keyword != ''">
                AND (enterprise_code LIKE CONCAT('%', #{keyword}, '%')
                  OR name LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY name ASC, id ASC
            LIMIT #{limit}
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<MdmEnterpriseDO> selectEnabledByTypes(@Param("tenantId") Long tenantId,
                                               @Param("types") Collection<String> types,
                                               @Param("keyword") String keyword,
                                               @Param("limit") Integer limit);

}
