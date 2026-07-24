# rewrite-access-docs-current-system

## Task Goal

将 `docs/login-access.md` 与 `docs/server-access.md` 改写为符合当前 `E:\IntRuoyi` 系统、目录结构、环境门禁与访问约定的文档。

## Milestones

- [x] 建立任务记录与执行日志。
- [x] 核对当前系统目录、脚本、端口、环境访问约定与现有文档差异。
- [x] 改写登录访问与服务器访问文档。
- [x] 验证文档内容不包含凭据、旧路径或无依据 fallback。
- [x] 收尾并记录最终验证结果。

## Expected Verification

- 结构验证：确认两个目标 Markdown 文件可读取且标题、章节、链接/路径符合当前系统。
- 内容验证：确认文档未记录密码、私钥、令牌等敏感信息。
- 路径验证：确认不再引用已不适用的旧项目路径。

## Current Status

completed

## Verification Evidence

- `docs/experience-index.md`：缺失；本次为低风险访问说明文档改写，已记录该缺口，未新建经验门禁。
- `rg -n "D:\\ProjectPackage|ruoyi-vue-pro" docs/login-access.md docs/server-access.md`：PASS，无旧项目路径匹配。
- `rg -n "(admin123|111111|password\s*=|PASSWORD=|BEGIN .*PRIVATE KEY|token=|secret=|密钥=|密码=)" docs/login-access.md docs/server-access.md`：PASS，无明文凭据模式匹配。
- 当前系统锚点验证：PASS，目标文档包含 `E:\IntRuoyi\IntRuoyiBackend`、`E:\IntRuoyi\IntRuoyiFronted`、`172.30.30.57/58/59`、`/opt/intruoyi/runtime` 和备用服务器 `/mnt/intruoyi-data` 参数。
- `task-closeout-cleanup preview/apply`：PASS，无删除项、无阻塞、无警告，当前为主工作区非 linked worktree。
- `project-experience-consolidation`：PASS，登录、租户、账号与环境访问经验已合并到现有 `docs/login-access.md` / `docs/server-access.md`，未新建长期经验文档。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是按当前系统目录、环境门禁和访问脚本重写文档。
- `是否存在临时补丁或绕过`：否。
