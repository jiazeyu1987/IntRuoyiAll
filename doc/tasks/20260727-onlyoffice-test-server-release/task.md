# 本地 OnlyOffice 修复测试服发布

## Task Goal

将已进入 `origin/int_main` 的 DCC OnlyOffice 本地下载基址修复发布到测试服务器 `172.30.30.58`，仅执行测试服发布与运行态验证，不执行正式服、备用服、mark-tested、promote-prod 或 promote-backup。

## Milestones

- [ ] 创建发布任务记录，读取测试服发布、服务器、worktree、PowerShell 与 CI/CD 门禁。
- [ ] 锁定发布基线、releaseTag、目标环境和禁止动作。
- [ ] 创建或识别干净发布来源，避免从脏主工作区构建。
- [ ] 执行构建、发布到测试服，并记录 release operation。
- [ ] 验证测试服后端 health、前端 HTTP、镜像 tag、release-info 和 DCC preview 相关契约。
- [ ] 记录证据、阻塞项和收尾状态。

## Expected Verification

- 目标服务器：`172.30.30.58`。
- 发布范围：`test` only，组件范围 `intruoyi`。
- 发布基线：`origin/int_main` commit `9562dca4982007f36c302aaa99847a59d6a4c28e`。
- 测试服后端 `http://172.30.30.58:48081/actuator/health` 返回 `UP`。
- 测试服前端 `http://172.30.30.58:8081/` 返回 HTTP 200。
- 运行态 release tag、后端/前端镜像、manifest sourceRepos 与本轮发布一致。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本任务仅发布既有修复，不修改运行逻辑。
- `是否从根因和长期维护角度解决`：是；发布前确认测试服发布基线已包含 `public-file-base-url` 修复。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 测试服发布门禁：必须冻结发布基线、验证 manifest/sourceRepos、publish-test 目标字段、远端 `.env IMAGE_TAG`、实际镜像、backend health、frontend HTTP 和 release-info；不得拼接不同 releaseTag。
- 服务器访问门禁：仅允许访问测试服务器 `172.30.30.58`，不得操作正式服 `172.30.30.57` 或备用服 `172.30.30.59`。
- Worktree 门禁：不得从脏主工作区直接构建；如需 worktree，只能在 `D:\IntRuoyiWorktree\` 下创建。
- PowerShell 门禁：不使用 `&&`；中文、JSON、SSH/stdin、日志证据使用 UTF-8；不得记录密码、token、私钥或连接串密钥。
- Git 门禁：当前主工作区 ahead/dirty 不作为本轮发布输入；发布任务自有记录单独保留，避免混入并发任务。
