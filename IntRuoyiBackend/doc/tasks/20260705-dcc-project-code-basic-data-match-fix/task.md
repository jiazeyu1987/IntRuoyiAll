# DCC 批量识别项目代码基础数据匹配失败修复

## 任务目标
- 修复 DCC 批量识别进度中出现 `DCC project-code recognition returned no DCC basic-data match` 的问题。
- 识别结果必须能按真实 DCC 项目代码基础数据稳定匹配，不引入 fallback、不吞异常、不用默认成功掩盖失败。

## 上一任务检查
- 根仓最近任务 `20260705-lightcure-tail-microgrid-branch-review` 仍为 in_progress，但属于独立 worktree 候选评审，不在本次 DCC 后端修复范围内。
- 后端最近任务包含正式服展厅发布相关未收尾记录；本次只修改本机后端 DCC 源码与测试，不操作正式服/测试服。

## 经验门禁
- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写使用显式 UTF-8，不使用 `&&`。
- 缺陷回归闭环：已读取 `bug-regression-fix-loop` 与 `bug-contract.md`；需要 RED/GREEN 证据。
- 项目经验索引：已读取 `docs/experience-index.md`；本次命中 PowerShell 与缺陷修复，不执行真实 E2E、高风险写入、服务器操作或数据库迁移。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；修复项目代码识别结果与基础数据匹配规则的真实边界条件。
- 是否存在临时补丁或绕过：否。

## BDD 场景
- BDD: DCC 项目代码识别结果规范化后匹配基础数据 -> Given Codex/内容识别返回的项目代码文本与基础数据编码只存在大小写、空格、下划线、连字符或全角符号差异 / When 批量识别验证候选结果 / Then 应匹配唯一启用的 DCC 项目代码并写入识别账本。
- BDD: 仍然拒绝非唯一或不存在候选 -> Given 识别结果规范化后没有唯一启用项目代码或项目名称 / When 执行识别 / Then 必须 fail fast 并记录失败账本，不得写入错误关联。

## 里程碑
1. 建立任务文档与经验门禁 - 已完成
2. 复现基础数据匹配失败 - 已完成
3. 增加回归测试并最小修复 - 已完成
4. 运行验证、更新证据并提交 - 已完成

## 预期验证
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_codexProjectCodeMatchIgnoresCommonSeparators" "-Dsurefire.failIfNoSpecifiedTests=false" test` 先 RED 后 GREEN。
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。

## 当前状态
- 状态：in_progress
- 当前阶段：定位项目代码匹配规范化边界。
## 最终验证
- RED：`DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_codexProjectCodeMatchIgnoresCommonSeparators` 先失败，证明精确字符串比较导致识别文本无法匹配基础数据编码。
- GREEN：同名单测通过，1 test。
- GREEN：`DccControlledFileProjectCodeRecognitionServiceTest` 全类通过，27 tests。

## 完成状态
- 当前状态：Completed
- 完成时间：2026-07-05 17:48 +08:00