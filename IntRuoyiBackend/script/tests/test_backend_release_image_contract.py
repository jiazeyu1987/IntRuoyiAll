from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
BACKEND_DOCKERFILE = REPO_ROOT / "deploy" / "int-ruoyi-test" / "Dockerfile.backend"


def test_backend_release_image_consumes_one_shot_extra_args_without_overriding_compose_args() -> None:
    dockerfile = BACKEND_DOCKERFILE.read_text(encoding="utf-8")

    assert 'ENV ARGS=""' in dockerfile
    assert 'ENV INTRUOYI_EXTRA_ARGS=""' in dockerfile
    assert "${ARGS} ${INTRUOYI_EXTRA_ARGS}" in dockerfile
