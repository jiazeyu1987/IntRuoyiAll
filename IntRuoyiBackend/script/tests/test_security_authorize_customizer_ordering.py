from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CUSTOMIZER = ROOT / "yudao-framework" / "yudao-spring-boot-starter-security" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "framework" / "security" / "config" / "AuthorizeRequestsCustomizer.java"


def test_authorize_requests_customizer_uses_annotation_ordering():
    source = CUSTOMIZER.read_text(encoding="utf-8")

    assert "import org.springframework.core.Ordered;" not in source
    assert "implements Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>, Ordered" not in source
    assert "import org.springframework.core.annotation.Order;" in source
    assert "@Order(0)" in source
