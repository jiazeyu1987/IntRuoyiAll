# 工序池 F5/F6 验收文档任务

## Task Goal

启动 2 个子 agent 分别为 F5 审核副本上下限修正、F6 原始记录修改日志与重新电子签名编写 BDD/TDD 验收文档草案；主线程按既有 21 条需求门禁 review，放行后合并进 `docs/acceptance/production-line-process-pool/`。

## Milestones

- [x] 读取任务、编码、BDD/TDD 和经验门禁。
- [x] 创建任务文档目录。
- [x] 启动 2 个子 agent 分别起草 F5、F6。
- [x] 主线程整合并 review 文档。
- [x] 运行文档结构和 UTF-8 校验。
- [x] 提交、推送并完成 closeout。

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi`。
- `python -X utf8` 读取新增/修改 Markdown 和 JSON 文档成功。
- `git diff --check` 对本任务修改范围通过。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过后提交和推送。

## Applicable Gates

- 本任务只写文档，不修改生产代码、不启动服务、不运行真实 E2E。
- 子 agent 只产出草案，不直接提交、不改代码。
- F5/F6 文档必须继续满足 R01-R21，尤其 R18/R19。
- 不新增 fallback、默认成功、默认审核副本、默认签名、默认上下限、静默降级或 mock 成功。
- 批记录表单、表单槽位 `formBindings`、工序开始配置三条链路不得混用。
- 适用经验：`docs/worktree-memory.md#子-agent-主工作区溢出基线门禁`、`docs/worktree-memory.md#跨分支运行时契约复验门禁`。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，文档阶段明确 F5/F6 的正式模型、测试优先路径和阻塞条件。
- `是否存在临时补丁或绕过`：否。
