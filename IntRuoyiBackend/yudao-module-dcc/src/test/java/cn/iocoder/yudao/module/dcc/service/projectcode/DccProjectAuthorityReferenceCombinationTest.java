package cn.iocoder.yudao.module.dcc.service.projectcode;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;
@Import({DccProjectReferenceAuthorityImpl.class,DccProjectReferenceService.class,DccLatestControlledFileResolverImpl.class,DccRelationStore.class})
class DccProjectAuthorityReferenceCombinationTest extends DccProjectFormalCombinationTest {
    @Resource DccProjectReferenceService references;
    @Test void i05RealLeaderDirectoryResolverReferenceAndAuditPreserveSourceAfterExactCancellation() {
        policy("dcc.project-reference.create");policy("dcc.project-reference.cancel");
        var sourceProject=project("SOURCE",9L);var target=project("TARGET",7L);
        var first=folder(target.getId(),"F1");var second=folder(target.getId(),"F2");
        var source=file(sourceProject.getId(),101L,"source.pdf");
        String before=JsonUtils.toJsonString(files.selectById(source.getId()));
        var ref1=references.create(7L,target.getId(),first.getId(),source.getId(),"真实引用");
        var ref2=references.create(7L,target.getId(),second.getId(),source.getId(),"第二目录引用");
        assertEquals(1,ref1.referenceProjectCount());assertEquals(1,ref2.referenceProjectCount());
        for(long actor:new long[]{1,8,9}) {
            assertThrows(RuntimeException.class,()->references.create(actor,target.getId(),first.getId(),source.getId(),"无权引用"));
            assertThrows(RuntimeException.class,()->references.cancel(actor,target.getId(),first.getId(),source.getMasterId(),ref1.reference().id(),true,"无权取消"));
        }
        assertEquals(1,references.cancel(7L,target.getId(),first.getId(),source.getMasterId(),ref1.reference().id(),true,"确认取消"));
        assertEquals(0,references.cancel(7L,target.getId(),second.getId(),source.getMasterId(),ref2.reference().id(),true,"取消最后引用"));
        assertEquals(before,JsonUtils.toJsonString(files.selectById(source.getId())));
        assertEquals(4,events.selectList().size());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
    }
}
