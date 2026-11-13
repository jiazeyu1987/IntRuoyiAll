package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCasePageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCaseDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CodexTestCaseMapper extends BaseMapperX<CodexTestCaseDO> {

    default PageResult<CodexTestCaseDO> selectPage(CodexTestCasePageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<CodexTestCaseDO>()
                .likeIfPresent(CodexTestCaseDO::getName, reqVO.getName())
                .eqIfPresent(CodexTestCaseDO::getProject, reqVO.getProject())
                .eqIfPresent(CodexTestCaseDO::getNodeChainName, reqVO.getNodeChainName())
                .eqIfPresent(CodexTestCaseDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CodexTestCaseDO::getDefaultExecutionMode, reqVO.getExecutionMode())
                .orderByAsc(reqVO.getNodeChainName() != null, CodexTestCaseDO::getNodeChainSort)
                .orderByAsc(CodexTestCaseDO::getSort)
                .orderByDesc(BaseDO::getCreateTime), java.util.Map.ofEntries(
                java.util.Map.entry("id", number("id")),
                java.util.Map.entry("name", text("name")),
                java.util.Map.entry("project", text("CASE WHEN project IN ('智能排产','批记录','文控','工艺路线') THEN project WHEN LOWER(CONCAT_WS(' ',name,method_text,test_data_text,(SELECT CAST(JSON_ARRAYAGG(CONCAT_WS(' ',cp.name,cp.expected_text)) AS CHAR) FROM system_codex_test_checkpoint cp WHERE cp.case_id=system_codex_test_case.id AND cp.tenant_id=system_codex_test_case.tenant_id AND cp.deleted=0))) REGEXP '批记录|记录本|edhr|batch-record|recordbook' THEN '批记录' WHEN LOWER(CONCAT_WS(' ',name,method_text,test_data_text,(SELECT CAST(JSON_ARRAYAGG(CONCAT_WS(' ',cp.name,cp.expected_text)) AS CHAR) FROM system_codex_test_checkpoint cp WHERE cp.case_id=system_codex_test_case.id AND cp.tenant_id=system_codex_test_case.tenant_id AND cp.deleted=0))) REGEXP '文控|/dcc/|controlled-file|dcc-' THEN '文控' WHEN LOWER(CONCAT_WS(' ',name,method_text,test_data_text,(SELECT CAST(JSON_ARRAYAGG(CONCAT_WS(' ',cp.name,cp.expected_text)) AS CHAR) FROM system_codex_test_checkpoint cp WHERE cp.case_id=system_codex_test_case.id AND cp.tenant_id=system_codex_test_case.tenant_id AND cp.deleted=0))) REGEXP '工艺路线|/mes/pro/route|pro-route' THEN '工艺路线' ELSE '智能排产' END")),
                java.util.Map.entry("methodText", text("method_text")),
                java.util.Map.entry("nodeChain", text("CONCAT_WS(' ',node_chain_name,node_chain_sort)")),
                java.util.Map.entry("nodeChainName", text("CONCAT_WS(' ',node_chain_name,node_chain_sort)")),
                java.util.Map.entry("checkpointCount", number("(SELECT COUNT(*) FROM system_codex_test_checkpoint cp WHERE cp.case_id=system_codex_test_case.id AND cp.tenant_id=system_codex_test_case.tenant_id AND cp.deleted=0)")),
                java.util.Map.entry("targetItems", text("COALESCE((SELECT JSON_UNQUOTE(REPLACE(SUBSTRING(CAST(JSON_ARRAYAGG(REGEXP_REPLACE(REPLACE(TRIM(COALESCE(NULLIF(TRIM(cp.expected_text),''),NULLIF(TRIM(cp.name),''),'-')),CHAR(13),''),CONCAT('[[:space:]]*',CHAR(10),'[[:space:]]*'),' ')) OVER (ORDER BY cp.sort,cp.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS CHAR),2,CHAR_LENGTH(CAST(JSON_ARRAYAGG(REGEXP_REPLACE(REPLACE(TRIM(COALESCE(NULLIF(TRIM(cp.expected_text),''),NULLIF(TRIM(cp.name),''),'-')),CHAR(13),''),CONCAT('[[:space:]]*',CHAR(10),'[[:space:]]*'),' ')) OVER (ORDER BY cp.sort,cp.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS CHAR))-2),'\", \"',' ')) FROM system_codex_test_checkpoint cp WHERE cp.case_id=system_codex_test_case.id AND cp.tenant_id=system_codex_test_case.tenant_id AND cp.deleted=0 LIMIT 1),'-')")),
                java.util.Map.entry("parallelSafe", text("CASE parallel_safe WHEN 1 THEN '是' ELSE '否' END")),
                java.util.Map.entry("status", text("CASE status WHEN 'ENABLE' THEN '启用' ELSE '禁用' END")),
                java.util.Map.entry("defaultExecutionMode", text("default_execution_mode"))
        )));
    }

    default List<CodexTestCaseDO> selectListByIds(Collection<Long> ids) {
        return selectList(CodexTestCaseDO::getId, ids);
    }

    default List<CodexTestCaseDO> selectListByProjectAndName(String project, String name) {
        return selectList(new LambdaQueryWrapperX<CodexTestCaseDO>()
                .eq(CodexTestCaseDO::getProject, project)
                .eq(CodexTestCaseDO::getName, name)
                .orderByAsc(CodexTestCaseDO::getId));
    }

    default Long selectCountByNodeChainNameAndSort(String nodeChainName, Integer nodeChainSort, Long excludeId) {
        return selectCount(new LambdaQueryWrapperX<CodexTestCaseDO>()
                .eq(CodexTestCaseDO::getNodeChainName, nodeChainName)
                .eq(CodexTestCaseDO::getNodeChainSort, nodeChainSort)
                .neIfPresent(CodexTestCaseDO::getId, excludeId));
    }

    default List<CodexTestCaseDO> selectListByNodeChainName(String nodeChainName) {
        return selectList(new LambdaQueryWrapperX<CodexTestCaseDO>()
                .eq(CodexTestCaseDO::getNodeChainName, nodeChainName)
                .orderByAsc(CodexTestCaseDO::getNodeChainSort));
    }

    default List<CodexTestCaseDO> selectNodeChainCases() {
        return selectList(new LambdaQueryWrapperX<CodexTestCaseDO>()
                .isNotNull(CodexTestCaseDO::getNodeChainName)
                .ne(CodexTestCaseDO::getNodeChainName, "")
                .orderByAsc(CodexTestCaseDO::getProject)
                .orderByAsc(CodexTestCaseDO::getNodeChainName)
                .orderByAsc(CodexTestCaseDO::getNodeChainSort));
    }

    @Delete("DELETE FROM system_codex_test_case WHERE id = #{id}")
    int deletePhysicalById(Long id);

}
