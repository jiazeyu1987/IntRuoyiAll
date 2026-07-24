# 任务：DCC 短编码文件名识别硬化

## 任务目标

修复 DCC 文件名直连识别把短编码当作普通长 token 子串误命中的问题，避免错误项目编码/项目名称被直接回写。

## 当前状态

COMPLETED：已在独立 clean worktree 内为短编码误命中补齐 RED 回归测试，并以最小改动把文件名直连收紧为“高置信度短路”规则；当前短编码和嵌入长 ASCII token 的误命中已不再跳过内容识别，长编码快捷路径回归通过。

## Current Status

COMPLETED

## 上一任务检查

- 上一个 backend 任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-int-main-workspace-normalization\task.md`
- 状态：已在本轮只读核对中按完成态处理
- 处理：本任务只处理 DCC 识别规则硬化，不碰当前主工作区里的其他本地现场。

## 经验门禁

- 已读取：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- 本任务适用强制门禁：
  - 先补失败回归测试，再改生产代码；不得直接修改行为绕过 RED。
  - 当前主 backend 工作区存在无关脏改动，必须在独立 clean worktree 内重放本任务补丁和验证。
  - 不得引入 fallback、静默降级或“猜测即回写”分支。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: 短编码作为普通长 token 子串时不得直连 -> Given 启用项目编码中存在 IN/OC/EC 等短编码 / When 源文件名只是 INT、OCP、ECR 等普通长 token 子串 / Then 后端不得把该短编码当作唯一直连命中结果。`
- `BDD: 低置信度短编码不能仅凭文件名直接回写 -> Given 文件名仅命中低置信度短编码 / When 文控执行基础信息识别 / Then 后端必须继续正式识别链路，而不是直接回写项目编码和项目名称。`
- `BDD: 边界清晰的长编码仍可保留快捷路径 -> Given 源文件名包含边界明确且唯一的长项目编码 / When 文控执行基础信息识别 / Then 后端仍可在读取文件内容前直接锁定该编码。`

## 里程碑

1. 建立 clean worktree 任务台账并补齐短编码误命中的 RED 回归测试。`DONE`
2. 以最小改动实现边界感知命中与低置信度短编码拦截。`DONE`
3. 跑通 DCC 定向回归并补齐 bug 证据。`DONE`

## 预期验证

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileProjectCodeRecognitionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccProjectCodeCodexCliClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\doc\tasks\20260623-dcc-short-code-recognition-hardening\bug-regression-evidence.md`

## 完成结果

- 文件名直连不再接受低置信度短编码。
- 文件名中嵌入更长 ASCII token 的编码子串不再被误当作完整编码。
- 现有长编码快捷路径仍可保留。
- 最终与真实内容识别链路的联动验证留给测试服内容识别任务继续完成。
