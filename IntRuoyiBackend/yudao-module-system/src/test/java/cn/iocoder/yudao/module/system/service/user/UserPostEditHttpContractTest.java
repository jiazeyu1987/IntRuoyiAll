package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.controller.admin.user.UserController;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Actual production ObjectMapper + @Valid + controller + real service proxy / H2.
 * Standalone MockMvc does not install production auth filters or method security. */
public class UserPostEditHttpContractTest extends UserPostRetentionTestSupport {
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void prepareHttp() {
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "deptService", deptService);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)).build();
    }

    @AfterEach
    void releaseHttp() { validator.close(); }

    private String updateBody(AdminUserDO user, String postField) {
        return "{\"id\":" + user.getId() + ",\"username\":\"" + user.getUsername()
                + "\",\"nickname\":\"http-change\"" + postField + "}";
    }

    private void update(AdminUserDO user, String postField) throws Exception {
        mvc.perform(put("/system/user/update").contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(user, postField))).andExpect(status().isOk());
    }

    @Test
    void missingAndExplicitNullCompleteSelectionRejectAtRealServiceWithoutWrites() {
        AdminUserDO user = boundUser(Set.of(10L, 20L));
        List<Map<String, Object>> before = snapshot(user);
        for (String field : List.of("", ",\"postIds\":null")) {
            Exception failure = assertThrows(Exception.class, () -> update(user, field));
            Throwable root = failure;
            while (root.getCause() != null) root = root.getCause();
            assertInstanceOf(ServiceException.class, root);
            assertEquals(USER_POST_IDS_REQUIRED.getCode(), ((ServiceException) root).getCode());
            assertEquals(before, snapshot(user));
        }
    }

    @Test
    void malformedPostIdentityFormsFailHttpBindingWithoutWrites() throws Exception {
        AdminUserDO user = boundUser(Set.of(10L));
        List<Map<String, Object>> before = snapshot(user);
        for (String value : List.of("1", "\"10\"", "{}", "true", "[null]", "[true]", "[{}]", "[[]]",
                "[1.5]", "[1.0]", "[1e1]", "[-1]", "[0]", "[9223372036854775808]",
                "[\"9223372036854775808\"]", "[\"01\"]", "[\"+1\"]", "[\" 1\"]", "[\"1 \"]", "[\"\"]")) {
            mvc.perform(put("/system/user/update").contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody(user, ",\"postIds\":" + value))).andExpect(status().isBadRequest());
            assertEquals(before, snapshot(user));
        }
    }

    @Test
    void integerAndExactStringDuplicatesUseRequestSetSemantics() throws Exception {
        AdminUserDO user = boundUser(Set.of(10L, 20L));
        update(user, ",\"postIds\":[10,\"10\",\"20\",20]");
        assertStorage(user, Set.of(10L, 20L));
        assertEquals("http-change", userMapper.selectById(user.getId()).getNickname());
    }

    @Test
    void explicitEmptyArrayRemovesAllThroughHttp() throws Exception {
        AdminUserDO user = boundUser(Set.of(10L, 20L));
        update(user, ",\"postIds\":[]");
        assertStorage(user, Set.of());
    }

    @Test
    void exactLargeLongStringRoundTripsWithoutPrecisionLoss() throws Exception {
        long identity = 9007199254740993L;
        jdbc.update("INSERT INTO system_post(id,code,name,sort,status,tenant_id) VALUES(?,'long-id','精确岗位',0,0,1)", identity);
        AdminUserDO user = boundUser(Set.of(10L));
        update(user, ",\"postIds\":[10,\"" + identity + "\"]");
        assertStorage(user, Set.of(10L, identity));
        String body = mvc.perform(get("/system/user/get-for-update").param("id", user.getId().toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(body).get("data");
        assertTrue(data.get("postIds").toString().contains("\"" + identity + "\""));
        JsonNode post = data.get("assignedPosts").get(1);
        assertTrue(post.get("id").isTextual());
        assertEquals(Long.toString(identity), post.get("id").textValue());
        assertEquals(Set.of("id", "name", "status"), objectMapper.convertValue(post, Map.class).keySet());
    }

    @Test
    void createMissingNullAndEmptyPostFieldRetainOptionalHttpContract() throws Exception {
        int index = 0;
        for (String field : List.of("", ",\"postIds\":null", ",\"postIds\":[]")) {
            String body = "{\"username\":\"httpcreate" + ++index + "\",\"nickname\":\"创建\",\"password\":\"Synthetic@2026\"" + field + "}";
            String response = mvc.perform(post("/system/user/create").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            long id = objectMapper.readTree(response).get("data").asLong();
            assertTrue(userService.getUserForUpdate(id).getPostIds().isEmpty());
        }
    }

    @Test
    void editResponseContainsDisabledAssignedPostAndUnboundResponseIsExplicitEmpty() throws Exception {
        AdminUserDO user = boundUser(Set.of(20L));
        String response = mvc.perform(get("/system/user/get-for-update").param("id", user.getId().toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).get("data");
        assertEquals(20L, data.get("postIds").get(0).asLong());
        assertEquals(1, data.get("assignedPosts").get(0).get("status").asInt());
        assertEquals("停用岗位", data.get("assignedPosts").get(0).get("name").asText());
        AdminUserDO empty = seedUser(null);
        response = mvc.perform(get("/system/user/get-for-update").param("id", empty.getId().toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        data = objectMapper.readTree(response).get("data");
        assertTrue(data.get("postIds").isArray()); assertEquals(0, data.get("postIds").size());
        assertTrue(data.get("assignedPosts").isArray()); assertEquals(0, data.get("assignedPosts").size());
    }

    @Test
    void ordinaryGetStillReturnsOriginalProjectionForInconsistentBinding() throws Exception {
        AdminUserDO user = seedUser(Set.of(999L));
        String response = mvc.perform(get("/system/user/get").param("id", user.getId().toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).get("data");
        assertEquals(999L, data.get("postIds").get(0).asLong());
        assertFalse(data.has("assignedPosts"));
        assertCode(USER_POST_BINDING_INCONSISTENT, () -> userService.getUserForUpdate(user.getId()));
    }

    @Test
    void editQueryKeepsTheExistingDeclaredQueryPermission() throws Exception {
        assertEquals(UserController.class.getMethod("getUser", Long.class).getAnnotation(PreAuthorize.class).value(),
                UserController.class.getMethod("getUserForUpdate", Long.class).getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('system:user:query')", UserController.class.getMethod("getUserForUpdate", Long.class).getAnnotation(PreAuthorize.class).value());
    }
}
