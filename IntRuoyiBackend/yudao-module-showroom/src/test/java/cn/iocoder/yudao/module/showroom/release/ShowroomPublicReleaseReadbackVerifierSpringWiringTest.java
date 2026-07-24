package cn.iocoder.yudao.module.showroom.release;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShowroomPublicReleaseReadbackVerifierSpringWiringTest {

    @Test
    void shouldUseExplicitSpringConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-showroom-release",
                    Map.of("showroom.release.public-website-origin", "http://127.0.0.1:8083")));
            context.register(ShowroomPublicReleaseReadbackVerifier.class);

            context.refresh();

            assertNotNull(context.getBean(ShowroomPublicReleaseReadbackVerifier.class));
        }
    }
}
