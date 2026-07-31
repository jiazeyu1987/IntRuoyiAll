# 同步生产用料清单到测试服务器

## Task Goal

将本地芋道源码中与“生产用料清单”相关的当前代码同步到测试服务器，目标环境为测试服务器 `172.30.30.58`，默认按源码发布处理，不做数据库、MinIO 或业务数据同步，除非正式发布脚本的必需迁移门禁要求执行。

## Milestones

- [ ] 明确“生产用料清单”对应源码范围与测试服务器发布方式。
- [ ] 完成发布前门禁检查：本地 Git 状态、发布脚本、目标环境、回滚路径与必要经验门禁。
- [ ] 执行测试服务器同步/发布。
- [ ] 验证测试服务器后端健康、前端可访问，并记录 releaseTag 或同步证据。
- [ ] 完成任务文档、验证报告与收尾状态。

## Expected Verification

- 测试服务器目标主机确认为 `172.30.30.58`。
- 发布或同步命令退出码为 0。
- 后端健康检查 `http://172.30.30.58:48081/actuator/health` 返回健康状态。
- 前端 `http://172.30.30.58:8081/` 返回 HTTP 200。
- 若有 releaseTag、manifest、镜像 tag 或发布脚本摘要，必须记录。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按项目发布脚本和测试服发布门禁同步，不手工拼接远端状态。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 测试服发布范围：仅允许 `build-release` 与 `deploy-release` 到测试服务器 `172.30.30.58`；禁止正式服、备份服、`mark-tested`、`promote-prod`、`promote-backup`。
- code-only 范围：使用 `-Component intruoyi -SkipDatabaseSync -SkipMinioSync`；不同步本地数据库、MinIO 或业务数据，但仍必须执行 schema/menu/permission/seed 等 required SQL 门禁。
- 发布来源：不得从脏主工作区直接构建；本轮 release worktree 为 `D:\IntRuoyiWorktree\pml-test-r260731`，冻结提交为 `363a887f03200bf58c6e8c649b8805c0fe66b06b`。
- Manifest 门禁：构建后必须校验 `manifest.json` 中 `releaseTag`、`component=intruoyi`、`publishScope=code-only`、backend/frontend commit、`dirty=false` 和 artifact 哈希。
- 测试服运行态：发布后必须核对远端 `.env IMAGE_TAG`、backend/frontend 实际镜像、容器 running、后端 health、前端 HTTP 200、release lock 和 migration 状态。
- Worktree 清理：发布验证完成后，release worktree 只能按当前任务范围清理；不得删除并行任务 worktree、进程或端口登记项。
