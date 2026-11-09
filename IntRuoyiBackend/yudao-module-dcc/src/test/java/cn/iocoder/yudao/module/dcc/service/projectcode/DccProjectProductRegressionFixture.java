package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductAuditService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;
import static org.mockito.Mockito.when;

/** Explicit role facts for new requests and immutable legacy fixtures for two-stage historical regression. */
final class DccProjectProductRegressionFixture {
    static void login(long actor) {
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(actor).setTenantId(1L).setUserType(2),new MockHttpServletRequest());
    }
    static void roles(DataSource source,PermissionApi permissions) {
        var jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,code VARCHAR(64),name VARCHAR(64),status INT,deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_user_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,user_id BIGINT,role_id BIGINT,deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(64),nickname VARCHAR(64),status INT,deleted INT DEFAULT 0)");
        cleanup(source);
        jdbc.update("INSERT INTO system_role VALUES(10,1,'dcc_project_reviewer','项目审核员',0,0)");
        jdbc.update("INSERT INTO system_user_role VALUES(10,1,1,10,0)");
        jdbc.update("INSERT INTO system_users(id,tenant_id,username,nickname,status) VALUES(1,1,'admin','显式配置测试审核人',0),(9,1,'actual-applicant-9','正式账号9',0)");
        for(long actor:new long[]{1,9})when(permissions.hasAnyPermissions(actor,"dcc:project-code:query")).thenReturn(true);
        when(permissions.hasAnyPermissions(1L,"dcc:project-code:review")).thenReturn(true);
        login(9L);
    }
    static void cleanup(DataSource source) {
        var jdbc=new JdbcTemplate(source);
        jdbc.update("DELETE FROM system_user_role");jdbc.update("DELETE FROM system_role");jdbc.update("DELETE FROM system_users");
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    static Long historical(DataSource source,DccFolderTemplateService templates,DccProjectProductCreateRequestMapper requests,
            DccProjectProductIdentityClaimMapper claims,DccProjectProductAuditService audit,DccProjectProductCreateReqVO input) {
        return new TransactionTemplate(new DataSourceTransactionManager(source)).execute(status->{
            var before=audit.beforeCreate(input.getFolderTemplateId());
            var row=DccProjectProductCreateRequestDO.builder().projectName(input.getProjectName()).projectCode(input.getProjectCode())
                    .projectLeader("正式负责人").projectLeaderUserId(input.getProjectLeaderUserId()).folderTemplateId(input.getFolderTemplateId())
                    .folderTemplateSnapshotJson(templates.captureForRequest(input.getFolderTemplateId()))
                    .defaultAttributesJson(JsonUtils.toJsonString(input.getDefaultAttributes())).productCode(input.getProductCode())
                    .productName(input.getProductName()).classification(input.getClassification()).creationReason(input.getCreationReason())
                    .status("PENDING_REVIEW").applicantUserId(9L).configuredReviewerUserId(1L).configuredReviewerUsername("admin")
                    .configuredReviewerNickname("显式配置测试审核人").submittedTime(LocalDateTime.now()).build();
            row.setTenantId(1L);if(requests.insert(row)!=1)throw new IllegalStateException("historical fixture request insert failed");
            for(var entry:Map.of("PROJECT_CODE",row.getProjectCode(),"PRODUCT_CODE",row.getProductCode(),"PRODUCT_NAME",row.getProductName()).entrySet()) {
                var claim=DccProjectProductIdentityClaimDO.builder().requestId(row.getId()).identityType(entry.getKey()).identityValue(entry.getValue()).build();claim.setTenantId(1L);
                if(claims.insert(claim)!=1)throw new IllegalStateException("historical fixture claim insert failed");
            }
            audit.append("dcc.project-product.create",row.getId(),"1",input.getCreationReason(),before,null);
            return row.getId();
        });
    }
}
