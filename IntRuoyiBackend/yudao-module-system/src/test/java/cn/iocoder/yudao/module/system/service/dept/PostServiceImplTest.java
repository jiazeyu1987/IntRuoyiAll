package cn.iocoder.yudao.module.system.service.dept;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.PostMapper;
import cn.iocoder.yudao.module.system.service.permission.GlobalApprovalAuthorityCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

/**
 * {@link PostServiceImpl} 的单元测试类
 *
 * @author niudehua
 */
@Import(PostServiceImpl.class)
public class PostServiceImplTest extends BaseDbUnitTest {

    @Resource
    private PostServiceImpl postService;

    @Resource
    private PostMapper postMapper;

    @MockitoBean
    private GlobalApprovalAuthorityCoordinator approvalAuthority;

    @Test
    public void testCreatePost_success() {
        // 准备参数
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class,
                o -> o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()))
                .setId(null); // 防止 id 被设置
        // 调用
        Long postId = postService.createPost(reqVO);

        // 断言
        assertNotNull(postId);
        // 校验记录的属性是否正确
        PostDO post = postMapper.selectById(postId);
        assertPojoEquals(reqVO, post, "id");
        assertEquals("GW-001", post.getCode());
    }

    @Test
    public void testCreatePost_generatesConsecutiveCodesWithoutClientCode() {
        // 历史业务编码不占用 GW 流水号。
        postMapper.insert(randomPostDO(o -> o.setCode("WORKSHOP_DIRECTOR")));
        Long first = postService.createPost(newPostRequest("生产组长"));
        Long second = postService.createPost(newPostRequest("生产操作员"));
        assertEquals("GW-001", postMapper.selectById(first).getCode());
        assertEquals("GW-002", postMapper.selectById(second).getCode());
    }

    @Test
    public void testCreatePost_usesHighestNumberIncludingDisabledAndDeletedPosts() {
        postMapper.insert(randomPostDO(o -> o.setCode("GW-002")));
        postMapper.insert(randomPostDO(o -> {
            o.setCode("GW-009");
            o.setStatus(CommonStatusEnum.DISABLE.getStatus());
        }));
        PostDO deleted = randomPostDO(o -> o.setCode("GW-012"));
        postMapper.insert(deleted);
        postMapper.deleteById(deleted.getId());
        assertNull(postMapper.selectById(deleted.getId()));

        Long id = postService.createPost(newPostRequest("质量负责人"));
        assertEquals("GW-013", postMapper.selectById(id).getCode());
    }

    @Test
    public void testCreatePost_keepsGrowingPastThreeDigitsAndReadsCaseInsensitiveHistory() {
        postMapper.insert(randomPostDO(o -> o.setCode("gw-999")));
        Long id = postService.createPost(newPostRequest("文控专员"));
        assertEquals("GW-1000", postMapper.selectById(id).getCode());
    }

    @Test
    public void testCreatePost_rollsBackWhenAuthorityRevisionCannotBeWritten() {
        doThrow(new IllegalStateException("authority write failed"))
                .when(approvalAuthority).incrementLockedTenants(anyList());
        assertThrows(IllegalStateException.class,
                () -> postService.createPost(newPostRequest("生产组长")));
        assertNull(postMapper.selectByName("生产组长"));
        assertTrue(postMapper.selectGeneratedCodesForUpdate().isEmpty());
    }

    @Test
    public void testUpdatePost_success() {
        // mock 数据
        PostDO postDO = randomPostDO();
        postMapper.insert(postDO);// @Sql: 先插入出一条存在的数据
        // 准备参数
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class, o -> {
            // 设置更新的 ID
            o.setId(postDO.getId());
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus());
        });

        // 调用
        postService.updatePost(reqVO);
        // 校验是否更新正确
        PostDO post = postMapper.selectById(reqVO.getId());
        assertPojoEquals(reqVO, post);
        assertEquals(postDO.getCode(), post.getCode(), "编辑岗位不得重编或覆盖编码");
    }

    @Test
    public void testDeletePost_success() {
        // mock 数据
        PostDO postDO = randomPostDO();
        postMapper.insert(postDO);
        // 准备参数
        Long id = postDO.getId();

        // 调用
        postService.deletePost(id);
        assertNull(postMapper.selectById(id));
    }

    @Test
    public void testValidatePost_notFoundForDelete() {
        // 准备参数
        Long id = randomLongId();

        // 调用, 并断言异常
        assertServiceException(() -> postService.deletePost(id), POST_NOT_FOUND);
    }

    @Test
    public void testValidatePost_nameDuplicateForCreate() {
        // mock 数据
        PostDO postDO = randomPostDO();
        postMapper.insert(postDO);// @Sql: 先插入出一条存在的数据
        // 准备参数
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class,
            // 模拟 name 重复
            o -> o.setName(postDO.getName()));
        assertServiceException(() -> postService.createPost(reqVO), POST_NAME_DUPLICATE);
    }

    @Test
    public void testUpdatePost_preservesLegacyBusinessCode() {
        PostDO postDO = randomPostDO(o -> o.setCode("WORKSHOP_DIRECTOR"));
        postMapper.insert(postDO);
        PostSaveReqVO reqVO = newPostRequest("车间主任").setId(postDO.getId());
        postService.updatePost(reqVO);
        PostDO updated = postMapper.selectById(postDO.getId());
        assertEquals("车间主任", updated.getName());
        assertEquals("WORKSHOP_DIRECTOR", updated.getCode());
    }

    @Test
    public void testGetPostPage() {
        // mock 数据
        PostDO postDO = randomPojo(PostDO.class, o -> {
            o.setName("码仔");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        postMapper.insert(postDO);
        // 测试 name 不匹配
        postMapper.insert(cloneIgnoreId(postDO, o -> o.setName("程序员")));
        // 测试 status 不匹配
        postMapper.insert(cloneIgnoreId(postDO, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // 准备参数
        PostPageReqVO reqVO = new PostPageReqVO();
        reqVO.setName("码");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        // 调用
        PageResult<PostDO> pageResult = postService.getPostPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(postDO, pageResult.getList().get(0));
    }

    @Test
    public void testGetPostList() {
        // mock 数据
        PostDO postDO01 = randomPojo(PostDO.class);
        postMapper.insert(postDO01);
        // 测试 id 不匹配
        PostDO postDO02 = randomPojo(PostDO.class);
        postMapper.insert(postDO02);
        // 准备参数
        List<Long> ids = singletonList(postDO01.getId());

        // 调用
        List<PostDO> list = postService.getPostList(ids);
        // 断言
        assertEquals(1, list.size());
        assertPojoEquals(postDO01, list.get(0));
    }

    @Test
    public void testGetPostList_idsAndStatus() {
        // mock 数据
        PostDO postDO01 = randomPojo(PostDO.class, o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        postMapper.insert(postDO01);
        // 测试 status 不匹配
        PostDO postDO02 = randomPojo(PostDO.class, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        postMapper.insert(postDO02);
        // 准备参数
        List<Long> ids = Arrays.asList(postDO01.getId(), postDO02.getId());

        // 调用
        List<PostDO> list = postService.getPostList(ids, singletonList(CommonStatusEnum.ENABLE.getStatus()));
        // 断言
        assertEquals(1, list.size());
        assertPojoEquals(postDO01, list.get(0));
    }

    @Test
    public void testGetPost() {
        // mock 数据
        PostDO dbPostDO = randomPostDO();
        postMapper.insert(dbPostDO);
        // 准备参数
        Long id = dbPostDO.getId();
        // 调用
        PostDO post = postService.getPost(id);
        // 断言
        assertNotNull(post);
        assertPojoEquals(dbPostDO, post);
    }

    @Test
    public void testValidatePostList_success() {
        // mock 数据
        PostDO postDO = randomPostDO().setStatus(CommonStatusEnum.ENABLE.getStatus());
        postMapper.insert(postDO);
        // 准备参数
        List<Long> ids = singletonList(postDO.getId());

        // 调用，无需断言
        postService.validatePostList(ids);
    }

    @Test
    public void testValidatePostList_notFound() {
        // 准备参数
        List<Long> ids = singletonList(randomLongId());

        // 调用, 并断言异常
        assertServiceException(() -> postService.validatePostList(ids), POST_NOT_FOUND);
    }

    @Test
    public void testValidatePostList_notEnable() {
        // mock 数据
        PostDO postDO = randomPostDO().setStatus(CommonStatusEnum.DISABLE.getStatus());
        postMapper.insert(postDO);
        // 准备参数
        List<Long> ids = singletonList(postDO.getId());

        // 调用, 并断言异常
        assertServiceException(() -> postService.validatePostList(ids), POST_NOT_ENABLE,
                postDO.getName());
    }

    @SafeVarargs
    private static PostDO randomPostDO(Consumer<PostDO>... consumers) {
        Consumer<PostDO> consumer = (o) -> {
            o.setStatus(randomCommonStatus()); // 保证 status 的范围
        };
        return randomPojo(PostDO.class, ArrayUtils.append(consumer, consumers));
    }

    private static PostSaveReqVO newPostRequest(String name) {
        return new PostSaveReqVO().setName(name).setSort(0)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setRemark("");
    }

}
