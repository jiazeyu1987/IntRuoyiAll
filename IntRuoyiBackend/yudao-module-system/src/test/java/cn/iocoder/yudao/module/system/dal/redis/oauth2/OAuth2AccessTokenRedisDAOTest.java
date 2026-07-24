package cn.iocoder.yudao.module.system.dal.redis.oauth2;

import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AccessTokenRedisDAOTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @Test
    void get_whenRedisValueReadThrowsSerializerTypeException_returnsNull() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new InvalidDataAccessApiUsageException("broken redis payload",
                new ClassCastException("class java.lang.String cannot be cast to class [B")));

        OAuth2AccessTokenDO result = assertDoesNotThrow(() -> oauth2AccessTokenRedisDAO.get("broken-token"));
        assertNull(result);
    }

}
