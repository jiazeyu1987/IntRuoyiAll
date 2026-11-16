package cn.iocoder.yudao.module.showroom.approval;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.context.annotation.Import(cn.iocoder.yudao.module.showroom.support.ShowroomMysqlTextTestConfiguration.class)
class ShowroomApprovalQueryMapperTest extends BaseDbUnitTest {
    @Resource private ShowroomApprovalQueryMapper query;
    @Resource private ShowroomChangeRequestMapper requests;
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void displayedNodeResultAndCapabilityFilterAuthorizedSourceBeforeCountAndWindow() {
        TenantContextHolder.setTenantId(122L);
        Long first=insert(122L,100L,"PENDING_SUPERVISOR_REVIEW");
        Long second=insert(122L,100L,"PENDING_SUPERVISOR_REVIEW");
        insert(122L,101L,"PENDING_SUPERVISOR_REVIEW");
        insert(122L,100L,"PUBLISHED");
        insert(123L,100L,"PENDING_SUPERVISOR_REVIEW");
        var context=ApprovalTaskQueryContext.of(100L,ApprovalTaskViewType.TODO,ApprovalModuleCode.SHOWROOM,null,1,1)
                .setListFilters("[{\"key\":\"node\",\"operator\":\"contains\",\"value\":\"主管审核\"},{\"key\":\"approvalResult\",\"operator\":\"contains\",\"value\":\"决定未知\"},{\"key\":\"capabilities\",\"operator\":\"eq\",\"value\":\"SIGNATURE_AUTHORIZATION\"}]");
        assertEquals(2L,query.count(query.query(context)));
        var firstWindow=query.query(context);firstWindow.last("ORDER BY c.id ASC LIMIT 1");
        assertEquals(List.of(first),query.window(firstWindow));
        var next=query.query(context);next.last("ORDER BY c.id ASC LIMIT 1 OFFSET 1");
        assertEquals(List.of(second),query.window(next));
        context.setListFilters("[{\"key\":\"node\",\"operator\":\"eq\",\"value\":\"已驳回 未配置中文状态\"}]");
        assertEquals(0L,query.count(query.query(context)));
    }
    private Long insert(Long tenant,Long supervisor,String status) {
        var row=ShowroomChangeRequestDO.builder().targetType("PRODUCT").targetId(1L).targetRevisionId(1L)
                .moduleCode("SHOWROOM").requestType("CONTENT_UPDATE").submissionSource("MANUAL")
                .status(status).submittedBy(200L).submittedAt(LocalDateTime.of(2026,1,1,10,0))
                .supervisorUserId(supervisor).gaoxinUserId(300L).build();
        row.setTenantId(tenant);requests.insert(row);return row.getId();
    }
}
