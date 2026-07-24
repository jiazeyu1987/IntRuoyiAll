package cn.iocoder.yudao.module.bpm.convert.definition;

import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.process.BpmProcessDefinitionRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BpmProcessDefinitionConvertTest {

    @Test
    public void testCopyTo_preservesDefinitionFieldsAndMapsMetaInfo() {
        BpmProcessDefinitionInfoDO from = BpmProcessDefinitionInfoDO.builder()
                .id(10L)
                .modelType(20)
                .icon("icon")
                .description("desc")
                .formType(30)
                .formId(40L)
                .formConf("conf")
                .formFields(List.of("field"))
                .visible(true)
                .sort(50L)
                .build();
        BpmProcessDefinitionRespVO to = new BpmProcessDefinitionRespVO();
        to.setId("process-id");
        to.setVersion(2);
        to.setName("name");
        to.setKey("key");
        to.setCategoryName("category");
        to.setFormName("form");
        to.setSuspensionState(1);
        to.setDeploymentTime(LocalDateTime.of(2024, 1, 1, 0, 0));
        to.setBpmnXml("<xml />");

        BpmProcessDefinitionConvert.INSTANCE.copyTo(from, to);

        assertEquals("process-id", to.getId());
        assertEquals(20, to.getType());
        assertEquals(20, to.getModelType());
        assertEquals("icon", to.getIcon());
        assertEquals("desc", to.getDescription());
        assertEquals(30, to.getFormType());
        assertEquals(40L, to.getFormId());
        assertEquals("conf", to.getFormConf());
        assertEquals(List.of("field"), to.getFormFields());
        assertTrue(to.getVisible());
        assertEquals(50L, to.getSort());
        assertEquals(2, to.getVersion());
        assertEquals("name", to.getName());
        assertEquals("key", to.getKey());
        assertEquals("category", to.getCategoryName());
        assertEquals("form", to.getFormName());
        assertEquals(1, to.getSuspensionState());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), to.getDeploymentTime());
        assertEquals("<xml />", to.getBpmnXml());
    }

}
