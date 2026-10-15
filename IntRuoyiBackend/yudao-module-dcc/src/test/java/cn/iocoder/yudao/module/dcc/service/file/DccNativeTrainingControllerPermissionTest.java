package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileWorkflowController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DccNativeTrainingControllerPermissionTest {
 public static class Permissions {private final Set<String> allowed;Permissions(String...p){allowed=Set.of(p);}public boolean hasPermission(String p){return allowed.contains(p);}public boolean hasAnyPermissions(String...p){return Arrays.stream(p).anyMatch(allowed::contains);}}
 boolean allowed(String...permissions)throws Exception{String expression=DccControlledFileWorkflowController.class.getMethod("uploadTrainingRecord",Long.class,DccControlledFileTrainingRecordReqVO.class).getAnnotation(PreAuthorize.class).value();var context=new StandardEvaluationContext();context.setBeanResolver((c,n)->new Permissions(permissions));return Boolean.TRUE.equals(new SpelExpressionParser().parseExpression(expression).getValue(context,Boolean.class));}
 @Test void originalUploaderFunctionalEntryAcceptedWithoutApprove()throws Exception{assertTrue(allowed("dcc:controlled-file:query","dcc:controlled-file:submit"));}
 @Test void oldApprovalFunctionalEntryRemainsAvailable()throws Exception{assertTrue(allowed("dcc:controlled-file:approve"));}
 @Test void queryOnlyAndUnrelatedPermissionsDoNotAcquireTrainingWriteEntry()throws Exception{assertFalse(allowed("dcc:controlled-file:query"));assertFalse(allowed("dcc:controlled-file:review"));}
}
