package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.dal.dataobject.category.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual complete taxonomy service/Controller with explicit directory ports; row blockers never replace technical errors. */
class DccCategoryApprovalSourceProjectionTest {
    private DccFileTypeTaxonomyAdminServiceImpl service(String mode,boolean duplicate) throws Exception {
        var categories=new ArrayList<DccFileCategoryDO>();
        var cat=DccFileCategoryDO.builder().id(8L).active(true).fileTypeTaxonomyId(3L).build();
        var modeField=DccFileCategoryDO.class.getDeclaredField("approvalSourceMode");modeField.setAccessible(true);modeField.set(cat,mode);
        if(!"NO_MAPPING".equals(mode))categories.add(cat);if(duplicate)categories.add(cat);
        categories.add(DccFileCategoryDO.builder().id(9L).active(true).fileTypeTaxonomyId(4L).approvalSourceMode("PUBLIC_MATRIX").build());
        categories.add(DccFileCategoryDO.builder().id(10L).active(true).fileTypeTaxonomyId(5L).approvalSourceMode("APPLICATION_CUSTOM").build());
        var types=List.of(DccFileTypeTaxonomyDO.builder().id(1L).parentId(0L).active(true).levelNo(1).name("设计").sort(1).build(),
                DccFileTypeTaxonomyDO.builder().id(2L).parentId(1L).active(true).levelNo(2).name("输出").sort(2).build(),
                DccFileTypeTaxonomyDO.builder().id(3L).parentId(2L).active(true).levelNo(3).name("坏A").sort(3).build(),
                DccFileTypeTaxonomyDO.builder().id(4L).parentId(2L).active(true).levelNo(3).name("合法B").sort(4).build(),
                DccFileTypeTaxonomyDO.builder().id(5L).parentId(2L).active(true).levelNo(3).name("合法C").sort(5).build(),
                DccFileTypeTaxonomyDO.builder().id(7L).parentId(1L).active(true).levelNo(2).name("不足三级D").sort(7).build());
        var service=new DccFileTypeTaxonomyAdminServiceImpl();
        for(var pair:List.of(new Object[]{"categoryMapper",DccFileCategoryMapper.class,categories},new Object[]{"taxonomyMapper",DccFileTypeTaxonomyMapper.class,types})) {
            var proxy=Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{(Class<?>)pair[1]},(p,m,args)->{
                if(m.getName().equals("selectList"))return pair[2];
                if(m.getName().equals("selectCount"))return 0L;
                throw new UnsupportedOperationException("Projection port: "+m.getName());
            });var field=service.getClass().getDeclaredField((String)pair[0]);field.setAccessible(true);field.set(service,proxy);
        }return service;
    }
    private Object row(List<?> rows,Long id)throws Exception {
        for(Object row:rows)if(row.getClass().getMethod("getId").invoke(row).equals(id))return row;
        throw new AssertionError("Expected retained option "+id);
    }
    private Object field(Object row,String name)throws Exception {
        return row.getClass().getMethod("get"+Character.toUpperCase(name.charAt(0))+name.substring(1)).invoke(row);
    }
    private void unblocked(List<?> rows,Long id,String mode)throws Exception {
        var selected=row(rows,id);assertEquals(mode,field(selected,"approvalSourceMode"));assertNull(field(selected,"uploadBlocker"));
    }
    private void blocked(List<?> rows,Long id,String reason)throws Exception {
        var selected=row(rows,id);assertNull(field(selected,"approvalSourceMode"));assertTrue(((String)field(selected,"uploadBlocker")).contains(reason));
    }
    private List<?> projection(DccFileTypeTaxonomyAdminServiceImpl service)throws Exception {
        try {return (List<?>)service.getClass().getMethod("getUploadTaxonomyOptions").invoke(service);}
        catch(InvocationTargetException failure){if(failure.getCause() instanceof RuntimeException cause)throw cause;throw failure;}
    }
    @Test void formalLeafSourceProjectsWhileParentNullRemainsExplicit()throws Exception {
        var rows=projection(service("APPLICATION_CUSTOM",false));assertEquals(6,rows.size());
        unblocked(rows,1L,null);unblocked(rows,2L,null);unblocked(rows,3L,"APPLICATION_CUSTOM");
        unblocked(rows,4L,"PUBLIC_MATRIX");unblocked(rows,5L,"APPLICATION_CUSTOM");blocked(rows,7L,"至少三级");
    }
    @Test void unknownLeafSourceAndAmbiguousMappingOnlyBlockTheirRowsWhileLegalBCRemain()throws Exception {
        for(String unknown:Arrays.asList(null,"","OTHER"," PUBLIC_MATRIX")) {
            var rows=projection(service(unknown,false));blocked(rows,3L,"审批来源未配置或无效");
            unblocked(rows,4L,"PUBLIC_MATRIX");unblocked(rows,5L,"APPLICATION_CUSTOM");
        }
        var rows=projection(service("PUBLIC_MATRIX",true));blocked(rows,3L,"多个启用文件类别");unblocked(rows,4L,"PUBLIC_MATRIX");
    }
    @Test void badUnmappedAAndLevelDCoexistWithLegalBCInActualProjection()throws Exception {
        var rows=projection(service("NO_MAPPING",false));assertEquals(6,rows.size());
        blocked(rows,3L,"未配置启用文件类别");blocked(rows,7L,"至少三级");
        unblocked(rows,1L,null);unblocked(rows,2L,null);unblocked(rows,4L,"PUBLIC_MATRIX");unblocked(rows,5L,"APPLICATION_CUSTOM");
    }
    @Test void currentUploadControllerDelegatesFormalProjectionAndKeepsItsPermissionAnnotation()throws Exception {
        var controller=new cn.iocoder.yudao.module.dcc.controller.admin.category.DccFileTypeTaxonomyController();
        var field=controller.getClass().getDeclaredField("taxonomyAdminService");field.setAccessible(true);field.set(controller,service("APPLICATION_CUSTOM",false));
        var response=controller.getUploadTaxonomyOptions();assertEquals(6,response.getData().size());
        unblocked(response.getData(),4L,"PUBLIC_MATRIX");unblocked(response.getData(),5L,"APPLICATION_CUSTOM");
        blocked(response.getData(),7L,"至少三级");
        var method=controller.getClass().getMethod("getUploadTaxonomyOptions");
        assertEquals("@ss.hasPermission('dcc:controlled-file:submit')",method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value());
    }
    @Test void technicalCategoryAndTaxonomyDirectoryErrorsPropagateUnchanged()throws Exception {
        for(String port:List.of("categoryMapper","taxonomyMapper")) {
            var service=service("PUBLIC_MATRIX",false);var actualFailure=new IllegalStateException("真实目录连接失败");
            var field=service.getClass().getDeclaredField(port);field.setAccessible(true);
            var type=field.getType();field.set(service,Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{type},(p,m,args)->{throw actualFailure;}));
            assertSame(actualFailure,assertThrows(IllegalStateException.class,()->projection(service)));
        }
    }
    @Test void actualResponseSerializationRetainsAllThreeExplicitStatesWithNonNullGlobalSettings()throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        var rows=projection(service("NO_MAPPING",false));
        var parent=mapper.readTree(mapper.writeValueAsString(row(rows,1L)));
        assertTrue(parent.has("approvalSourceMode"));assertTrue(parent.get("approvalSourceMode").isNull());
        assertTrue(parent.has("uploadBlocker"));assertTrue(parent.get("uploadBlocker").isNull());
        var bad=mapper.readTree(mapper.writeValueAsString(row(rows,3L)));
        assertTrue(bad.get("approvalSourceMode").isNull());assertTrue(bad.get("uploadBlocker").textValue().contains("未配置启用文件类别"));
        var good=mapper.readTree(mapper.writeValueAsString(row(rows,4L)));
        assertEquals("PUBLIC_MATRIX",good.get("approvalSourceMode").textValue());assertTrue(good.get("uploadBlocker").isNull());
    }
}
