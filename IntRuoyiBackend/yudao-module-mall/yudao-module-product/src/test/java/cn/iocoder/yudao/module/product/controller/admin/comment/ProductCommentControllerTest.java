package cn.iocoder.yudao.module.product.controller.admin.comment;

import cn.iocoder.yudao.module.product.controller.admin.comment.vo.ProductCommentRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.comment.ProductCommentDO;
import cn.iocoder.yudao.module.product.service.comment.ProductCommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCommentControllerTest {

    @Mock
    private ProductCommentService productCommentService;
    @InjectMocks
    private ProductCommentController controller;

    @Test
    void getComment_returnsDetailAndKeepsQueryContract() throws Exception {
        when(productCommentService.getComment(9L))
                .thenReturn(new ProductCommentDO().setId(9L).setContent("detail"));

        ProductCommentRespVO result = controller.getComment(9L).getData();

        assertEquals(9L, result.getId());
        assertEquals("detail", result.getContent());
        Method method = ProductCommentController.class.getDeclaredMethod("getComment", Long.class);
        assertArrayEquals(new String[]{"/get"}, method.getAnnotation(GetMapping.class).value());
        assertEquals("id", method.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('product:comment:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
