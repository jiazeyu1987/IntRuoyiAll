# Task: 适配当前项目 AGENTS.md

## Task Goal

将用户提供的旧项目 `AGENTS.md` 规则整理并改写为适合当前 `E:\IntRuoyi` 工作区的项目级 `AGENTS.md`，保留严格无 fallback、任务文档、BDD/TDD、真实 E2E、编码安全和收尾规则，并修正项目路径、仓库结构与本项目约束。

## Milestones

- [x] 创建任务目录并记录初始 RED 证据
- [x] 识别当前项目结构、关键文档和前后端仓库名称
- [x] 编写当前项目根目录 `AGENTS.md`
- [x] 验证文件编码、路径引用和关键规则完整性
- [x] 收尾并记录最终验证结果

## Expected Verification

- `AGENTS.md` 存在于当前项目根目录。
- `AGENTS.md` 使用 UTF-8 文本，中文内容可正常读取。
- 文档中的项目路径指向当前 `E:\IntRuoyi` 工作区及其真实子目录。
- 文档保留无 fallback、任务文档、BDD/TDD、E2E、Git、编码安全、收尾和 PowerShell 规则。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接建立当前项目级 Agent 规则源。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/experience-index.md` 当前不存在；本任务只进行根目录规则文档适配，不执行真实 E2E、服务器、数据库、发布、备份、恢复、worktree 合并/清理等高风险动作。
- 新 `AGENTS.md` 已规定：后续高风险动作若缺少经验索引或必要运行文档，必须 fail fast 并记录影响，除非用户明确授权补齐对应门禁。

## Current Status

completed

## Final Verification Result

PASS。`AGENTS.md` 已按当前 `E:\IntRuoyi` 工作区适配完成；UTF-8 解码、关键规则覆盖、当前路径引用、旧项目绝对路径清理和 task-closeout-cleanup 均验证通过。当前工作区不是 Git 仓库，因此未执行提交。
