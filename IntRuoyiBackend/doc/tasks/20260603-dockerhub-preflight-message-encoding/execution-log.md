# 执行记录：修复 Docker Hub 前置检查提示乱码

BDD: Docker Hub 前置检查失败提示可读 -> Given Docker Desktop 无法访问 Docker Hub registry / When 发布脚本执行基础镜像 metadata 前置检查 / Then 失败信息必须包含 ASCII 错误码和可读的 Docker Desktop proxy/DNS/network 指引，不得出现乱码。

BDD: Docker Hub 网络失败仍然 fail-fast -> Given Docker Hub 不可达 / When 前置检查失败 / Then 发布脚本仍必须失败，不得切换镜像源、使用缓存镜像或静默继续构建。

## Bug

用户反馈发布脚本输出 `[FAIL] Docker Hub 网络/代理前置条件未通过...` 时，部分中文字符显示为 `�`，例如 `基础镜像元数�?'eclipse-temurin:21-jre-noble'`。

## Expected

Docker Hub 网络失败时，发布脚本仍然 fail-fast，但错误提示在 Windows PowerShell / Docker Desktop 日志中必须保持可读，至少包含稳定的 ASCII 错误码、失败镜像、Docker Desktop proxy/DNS/network 指引和原始 Docker 错误。

## Reproduction

Reproduction path: 运行发布构建，在 Docker Hub 不可达时触发 `Assert-DockerBaseImageMetadataAvailable`，用户日志显示中文提示乱码。

## Root Cause

发布脚本新增的 Docker Hub 前置检查直接输出中文错误消息。该消息在当前 Windows PowerShell / 控制台输出链路中可能被非 UTF-8 解码，导致用户看到乱码；同时脚本没有显式初始化 UTF-8 控制台输出。

## RED

RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "dockerhub or utf8"` -> FAIL，预期原因：发布脚本缺少 UTF-8 控制台输出初始化，且 Docker Hub 前置检查失败提示仍使用易乱码的中文长句。

## GREEN

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "dockerhub or utf8"` -> PASS，2 passed。

## REGRESSION

REGRESSION: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py` -> PASS，54 passed。

## Verification

发布脚本已在 `function Fail` 前设置 `[Console]::InputEncoding`、`[Console]::OutputEncoding` 与 `$OutputEncoding` 为 UTF-8 no BOM；Docker Hub 前置检查失败提示已改为 `DOCKERHUB_PREFLIGHT_FAILED` ASCII 错误码，并保留 `auth.docker.io / registry-1.docker.io`、Docker Desktop proxy/DNS/network 指引与原始 Docker 错误。

## Blockers

真实 Docker Hub 连接失败仍是外部网络前置条件，本任务只修复提示乱码和日志可读性；未引入镜像源 fallback、缓存镜像绕过或静默继续构建。

## Closeout

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260603-dockerhub-preflight-message-encoding\execution-log.md` -> PASS。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dockerhub-preflight-message-encoding --mode preview` -> READY，keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。

GREEN: `python -X utf8 tool\verify_tdd_compliance.py --task-dir doc\tasks\20260603-dockerhub-preflight-message-encoding --all-changed` -> PASS。
