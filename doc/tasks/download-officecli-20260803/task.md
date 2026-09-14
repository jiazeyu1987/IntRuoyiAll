# 下载 OfficeCLI

## Task Goal

从 GitHub 官方仓库下载 OfficeCLI Windows x64 二进制到本机用户下载目录，不执行安装脚本、不修改 PATH、不引入项目代码变更。

## Milestones

- [x] 创建任务目录并读取任务/编码规则。
- [x] 读取 GitHub 下载相关经验门禁。
- [x] 下载官方 Windows x64 Release 二进制和 SHA256SUMS。
- [x] 校验 SHA256 并验证二进制可执行。
- [x] 记录验证结果并准备收尾。

## Expected Verification

- GitHub Release 元数据可读取，目标资产为 `officecli-win-x64.exe`。
- 下载文件的 SHA256 与官方 `SHA256SUMS` 中 `officecli-win-x64.exe` 条目一致。
- 使用 `OFFICECLI_SKIP_UPDATE=1` 运行本地二进制版本命令成功。

## Applicable Experience Gates

### GitHub HTTPS 443 本地代理门禁

- Trigger: GitHub HTTPS 下载、`git fetch`、`git ls-remote` 或访问 GitHub 失败。
- Preflight check: 优先确认 GitHub HTTPS 可访问；如遇本地代理错误，检查 Git 配置代理、代理端口监听和 `github.com:443` 连通性。
- Blocker: GitHub HTTPS 直连不可用、代理端口未监听或代理配置与实际监听不一致时停止，不静默切换 SSH 或其它来源。
- Verification: 官方 GitHub HTTPS Release 资产下载成功并完成 SHA256 校验。
- Forbidden action: 禁止改用镜像源、SourceForge 镜像、SSH 或安装脚本作为未授权 fallback。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；仅下载官方 Release 二进制并校验完整性。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

下载与校验已完成，文件位于 `C:\Users\BJB110\Downloads\OfficeCLI\v1.0.143\officecli.exe`。Cleanup preview/apply 已通过且无删除项。仓库存在非本任务脏改动、Git 扫描警告和 ahead 状态，最终提交/推送门禁阻塞；按上级工作区安全规则不得混入或处理无关改动。
