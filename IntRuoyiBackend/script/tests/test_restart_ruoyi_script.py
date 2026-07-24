from pathlib import Path
import re


def test_restart_script_uses_runtime_copy_for_backend_jar() -> None:
    script_path = Path(__file__).resolve().parents[3] / "script" / "deploy" / "restart-ruoyi-local-component.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "runtimeBackendJar" in text, "restart script should define a dedicated runtime backend jar path"
    assert "Copy-Item -LiteralPath $backendJar -Destination $runtimeBackendJar -Force" in text, (
        "restart script should copy the built backend jar into a runtime-specific location before launch"
    )
    assert 'Start-Process -FilePath \'java\'' in text
    assert "'-jar', $runtimeBackendJar" in text, (
        "restart script must launch Java with the copied runtime jar instead of the mutable target jar"
    )


def test_restart_script_stops_target_backend_before_packaging() -> None:
    script_path = Path(__file__).resolve().parents[3] / "script" / "deploy" / "restart-ruoyi-local-component.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "Stopping existing backend process for this workspace before packaging." in text
    assert text.index("Stopping existing backend process for this workspace before packaging.") < text.index(
        "Building latest backend jar."
    )


def test_restart_script_passes_backend_runtime_base_config_to_local_backend() -> None:
    script_path = Path(__file__).resolve().parents[3] / "script" / "deploy" / "restart-ruoyi-local-component.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "Import-OptionalEnvironmentVariables $BackendRuntimeBaseEnv" in text
    assert "'INTRUOYI_BACKEND_RUNTIME_BASE_MODE'" in text
    assert "New-OptionalSpringArgumentFromEnv 'yudao.runtime-control.release-package.backend-runtime-base-mode' 'INTRUOYI_BACKEND_RUNTIME_BASE_MODE'" in text
    assert "New-OptionalSpringArgumentFromEnv 'yudao.runtime-control.release-package.backend-runtime-base-tar-path' 'INTRUOYI_BACKEND_RUNTIME_BASE_TAR'" in text
    assert "New-OptionalSpringArgumentFromEnv 'yudao.runtime-control.release-package.backend-runtime-base-tar-sha256' 'INTRUOYI_BACKEND_RUNTIME_BASE_TAR_SHA256'" in text
    assert "New-OptionalSpringArgumentFromEnv 'yudao.runtime-control.release-package.backend-runtime-base-image' 'INTRUOYI_BACKEND_RUNTIME_BASE_IMAGE'" in text
    assert "New-OptionalSpringArgumentFromEnv 'yudao.runtime-control.release-package.backend-runtime-base-digest' 'INTRUOYI_BACKEND_RUNTIME_BASE_DIGEST'" in text
    assert "New-OptionalSpringArgumentFromEnv 'yudao.runtime-control.release-package.backend-runtime-base-version' 'INTRUOYI_BACKEND_RUNTIME_BASE_VERSION'" in text


def test_restart_script_does_not_hardcode_showroom_public_release_readback_origin() -> None:
    script_path = Path(__file__).resolve().parents[3] / "script" / "deploy" / "restart-ruoyi-local-component.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "showroom.release.public-website-origin" not in text
    assert "showroomPublicReleaseOrigin" not in text


def test_restart_script_uses_local_profile_quartz_defaults_without_mes_smoke_switch() -> None:
    script_path = Path(__file__).resolve().parents[3] / "script" / "deploy" / "restart-ruoyi-local-component.ps1"
    text = script_path.read_text(encoding="utf-8")

    assert "function Test-MesSmokeRuntime" not in text
    assert "MES smoke runtime detected; enabling Quartz for local smoke flows." not in text
    assert "--spring.autoconfigure.exclude=org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration,org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration" not in text


def test_local_profile_enables_quartz_and_disables_local_auto_jobs_by_default() -> None:
    local_config_path = Path(__file__).resolve().parents[2] / "yudao-server" / "src" / "main" / "resources" / "application-local.yaml"
    text = local_config_path.read_text(encoding="utf-8")

    assert "org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration" not in text
    assert "quartz-auto-run-handler-whitelist:" in text
    assert "- kingdeeProductionOrderSyncJob" in text
    assert "- kingdeeBomSyncJob" in text
    assert "dcc-batch-recognition-enabled: false" in text
    assert "dcc-nas-transfer-enabled: false" in text
    assert "dcc-nas-permission-restore-enabled: false" in text
    assert "showroom-release-auto-publish-enabled: false" in text
    assert "showroom-product-cover-batch-resume-enabled: false" in text
    assert "showroom-product-batch-narration-audio-auto-check-enabled: false" in text
    assert "showroom-product-batch-narration-script-auto-check-enabled: false" in text


def test_local_profile_sets_showroom_public_release_readback_origin() -> None:
    local_config_path = Path(__file__).resolve().parents[2] / "yudao-server" / "src" / "main" / "resources" / "application-local.yaml"
    text = local_config_path.read_text(encoding="utf-8")

    assert "showroom:" in text
    assert 'public-website-origin: "${SHOWROOM_RELEASE_PUBLIC_WEBSITE_ORIGIN:http://127.0.0.1:${server.port}}"' in text


def test_active_docs_do_not_recommend_running_target_backend_jar_directly() -> None:
    docs_root = Path(__file__).resolve().parents[2] / "docs"
    offending = []
    needle = r"yudao-server\target\yudao-server.jar"
    direct_launch_patterns = [
        re.compile(r"-jar',\s*\$jar"),
        re.compile(r"-jar',\s*\$builtJar"),
        re.compile(r"-jar\s+.*yudao-server\\\\target\\\\yudao-server\.jar"),
    ]
    for path in docs_root.rglob("*.md"):
        text = path.read_text(encoding="utf-8")
        if needle in text and any(pattern.search(text) for pattern in direct_launch_patterns):
            offending.append(str(path))

    assert not offending, (
        "active docs must not recommend running the mutable target backend jar directly: "
        + ", ".join(offending)
    )
