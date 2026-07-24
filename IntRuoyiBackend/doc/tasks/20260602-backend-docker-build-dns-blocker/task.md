# 20260602 后端 Docker 构建 DNS 修复

## Task Goal

定位并修复 `publish-int-ruoyi.ps1` 构建测试服发布包时，后端镜像在 `Dockerfile.backend` 的 apt 安装阶段失败的问题，确保 Docker BuildKit 能解析 Ubuntu apt 源并完成 `python3 docker.io docker-compose-v2` 安装。

## Milestones

- [x] M1: 读取发布日志、Dockerfile、发布脚本与相关脚本测试，确认失败点和依赖意图。
- [x] M2: 使用最小 Docker 探针验证容器运行时 DNS、BuildKit DNS、`--network host` 与 Docker Desktop 代理路径。
- [x] M3: 使用宿主机当前可用 DNS 修复 Docker Desktop daemon DNS 配置并重启 Docker Desktop。
- [x] M4: 重新验证 Docker run、BuildKit DNS、apt 安装模拟和真实后端 Dockerfile 构建。
- [x] M5: 记录恢复证据，执行 task-closeout-cleanup 预览并清理临时验证镜像。

## Expected Verification

- 后端 Maven 构建、管理端构建、Website 构建成功的事实被保留。
- Docker 构建环境能解析 `archive.ubuntu.com`、`security.ubuntu.com`，并能安装 `python3 docker.io docker-compose-v2`。
- 真实 `Dockerfile.backend` 构建到临时 tag `intruoyi-backend:codex-dns-verify` 成功，证明原失败步骤已恢复。
- `JSONArgsRecommended` 仅为 Dockerfile CMD 格式警告，不影响本次镜像构建成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修复 Docker Desktop daemon DNS 与宿主机有效 DNS 不一致的根因，不改 Dockerfile、不硬编码 apt 源、不删除运行时工具依赖。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Closeout

- bug-regression evidence 校验通过。
- CI/CD evidence 校验通过。
- task-closeout-cleanup 预览结果：delete `<none>`、blocked `<none>`、warnings `<none>`。
- 临时验证镜像 `intruoyi-backend:codex-dns-verify` 已删除。

## Blocker

无。

## Root Cause

本机宿主机 DNS 为 `223.5.5.5`、`114.114.114.114`，但 Docker Desktop daemon 配置为 `192.168.101.1`、`1.1.1.1`。后两者在容器内解析 `archive.ubuntu.com` 与 `security.ubuntu.com` 均失败，导致 BuildKit 中 `apt-get update` 退出码 100。

## Fix Applied

- 更新 `C:\Users\BJB110\.docker\daemon.json` 的 `dns` 为 `223.5.5.5`、`114.114.114.114`。
- 执行 `docker desktop restart` 使 Docker Desktop daemon DNS 生效。
- 未修改 `Dockerfile.backend`、发布脚本、服务器、数据库、MinIO 或 NAS。

## Recovery Verification

已运行：

```powershell
docker run --rm eclipse-temurin:21-jre sh -lc "getent hosts archive.ubuntu.com && getent hosts security.ubuntu.com"
```

已运行 BuildKit 最小 DNS 探针、apt 安装模拟探针，并运行真实后端 Dockerfile 构建：

```powershell
docker build --progress=plain --no-cache -t intruoyi-backend:codex-dns-verify -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\int-ruoyi-test\Dockerfile.backend D:\ProjectPackage\Int\IntRuoyi
```

结果：`RUN apt-get update && apt-get install -y --no-install-recommends python3 docker.io docker-compose-v2 && rm -rf /var/lib/apt/lists/*` 成功完成，镜像 `intruoyi-backend:codex-dns-verify` 构建成功。
收尾时该临时镜像已删除。

## Cleanup Keep

- doc/tasks/20260602-backend-docker-build-dns-blocker/ci-cd-evidence.md
- doc/tasks/20260602-backend-docker-build-dns-blocker/bug-regression-evidence.md
