package cn.iocoder.yudao.module.product.service.comment;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.product.dal.dataobject.comment.ProductCommentDO;
import cn.iocoder.yudao.module.product.dal.mysql.comment.ProductCommentMapper;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

class ProductCommentQueryServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ProductCommentServiceImpl productCommentService;
    @Mock
    private ProductCommentMapper productCommentMapper;
    @Mock
    private ProductSpuService productSpuService;
    @Mock
    private ProductSkuService productSkuService;
    @Mock
    private MemberUserApi memberUserApi;

    @Test
    void getComment_returnsMapperResult() {
        ProductCommentDO comment = new ProductCommentDO().setId(9L);
        when(productCommentMapper.selectById(9L)).thenReturn(comment);

        assertSame(comment, productCommentService.getComment(9L));
    }
}
