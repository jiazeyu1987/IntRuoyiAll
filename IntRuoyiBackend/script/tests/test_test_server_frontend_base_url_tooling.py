from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
INT_RUOYI_ROOT = REPO_ROOT.parent
FRONTEND_ROOT = INT_RUOYI_ROOT / "yudao-ui-admin-vue3"


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_test_and_prod_frontend_env_do_not_default_api_to_localhost():
    env_test = read_text(FRONTEND_ROOT / ".env.test")
    env_prod = read_text(FRONTEND_ROOT / ".env.prod")
    publish_script = read_text(REPO_ROOT / "script" / "deploy" / "publish-int-ruoyi.ps1")

    assert "VITE_BASE_URL='http://localhost:48080'" not in env_test
    assert "VITE_BASE_URL='http://localhost:48080'" not in env_prod
    assert "VITE_BASE_URL=''" in env_test
    assert "VITE_BASE_URL=''" in env_prod
    assert "$env:VITE_BASE_URL = ''" in publish_script
