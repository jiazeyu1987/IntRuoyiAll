# 测试服 DCC 项目代码阶段/文件类型同步

## Task Goal

在测试服务器 `172.30.30.58` 上，使用既有正式后端批量文件分类能力，将“文控权限/类别列表”中启用文件类别的阶段-文件类型关系，应用到全部 DCC 项目代码详情的关联文件分组。

## Scope

- 环境：仅测试服 `172.30.30.58`。
- 范围：全部 DCC 项目代码关联文件。
- 数据处理边界：只处理未分类、缺分类、或当前阶段/文件类型无法对应到启用类别规则的文件。
- 禁止事项：不直接写 SQL，不改代码，不引入 fallback、降级、mock 成功或绕过链路。

## Milestones

- [x] 建立任务记录、BDD/TDD 证据、授权范围和阻塞策略。
- [x] 只读预检测试服健康、登录权限、角色权限、页面入口和启用类别规则数量。
- [x] 只读导出候选影响面，并记录 RED 证据。
- [x] 若候选数量大于 0，调用官方批量分类接口并轮询终态。
- [x] 执行 API 复核并记录 blocker。
- [x] 完成验证报告和任务阻塞记录。

## Expected Verification

- RED：执行前使用真实测试服页面/API 证明至少存在候选不一致或未分类文件，并记录候选总数；若候选为 0，则记录“无需变更”并不启动写入任务。
- GREEN：批量任务完成后候选数为 0；项目代码详情阶段/文件类型计数更新；任务 `failedCount/conflictCount/ambiguousCount` 为 0。
- E2E：使用 Playwright 打开测试服 DCC 项目代码详情，抽样核对“设计和开发策划阶段/输入阶段/输出阶段/验证/确认/转换阶段”等左侧阶段与右侧文件类型分组。

## Current Status

blocked

## Blocked Reason

测试服官方批量分类任务已按授权使用 `芋道源码/zhaohaichen` 执行完成，但 GREEN 条件未满足，按无 fallback/no SQL 策略保持阻塞：

- 批量任务 `35` 终态为 `COMPLETED`，`totalCount=14990`、`processedCount=14990`、`successCount=6292`、`failedCount=0`、`conflictCount=0`、`ambiguousCount=1207`、`unclassifiedCount=7491`、`remainingCount=0`。
- 批量任务成功分类了 6292 个文件，但仍有 1207 个歧义、7491 个未识别；这些文件未满足“按启用类别规则聚合，不再留在未分类文件类型”的验收标准。
- 任务后复扫 DCC 项目代码关联文件，仍有 `candidateTotal=8736`、`candidateProjectCount=93`，样本仍显示 `未分类 / 未分类文件类型` 或阶段有值但文件类型为空。
- 已导出明细：`task-35-ambiguous-recognition-records.xlsx`、`task-35-unclassified-recognition-records.xlsx`，并保存终态核验证据 `task-35-final-verification.json`。
- 未改用 SQL、未修改类别规则、未循环调用单文件 API、未把 `UNCLASSIFIED/AMBIGUOUS` 当作成功。

## Authorization Scope

用户已明确要求在测试服 `172.30.30.58` 实施该计划；不操作正式服、备用服或非本任务数据环境。

## Blocker Policy

若出现测试服不可达、登录凭据不可用、缺少 `doc_control` 或 DCC 查询/更新权限、活动任务冲突、缺少 `dcc_project_code_id`、分类歧义、官方接口失败、导出失败明细不可用，立即停止并记录 blocker；不改用 SQL、mock、默认值或其它绕过方式。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：否，官方批量分类链路已执行但仍存在 1207 个歧义和 7491 个未识别，需要补充正式类别匹配规则或人工处理歧义后再重跑。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

任务目录创建前，上一执行者已只读检查 `docs/experience-index.md` 并发现相关测试服、DCC lifecycle stage、PowerShell、Git、E2E 与数据规则条目；本任务已按这些门禁执行到只读预检并因正式前置条件缺失停止。

### DCC 批量分类同租户权限/规则门禁

- Trigger: 在测试服执行 DCC 项目代码关联文件全局批量分类、类别规则同步、阶段/文件类型映射。
- Preflight check: 同一租户内必须同时确认：可登录账号具备 `doc_control`；DCC 查询/更新权限通过；`dcc_file_category` 对应接口返回启用且绑定 `fileTypeTaxonomyId` 的类别规则；候选影响面为真实项目代码关联文件。
- Blocker: 规则存在于 `芋道源码` 但可登录账号缺 `doc_control`，或账号存在于 `测试租户` 但类别规则缺 `fileTypeTaxonomyId`，都必须停止。
- Verification: 记录租户、账号标签、`doc_control` 布尔值、关键权限布尔值、启用绑定类别数量、项目代码数量、候选文件数量和官方批量任务权限检查结果。
- Forbidden action: 禁止直接 SQL 修数、跨租户搬运规则、修改角色、per-file API 循环绕过、把 `super_admin` 当作 `doc_control`、或在目标阶段/文件类型为空时启动写入任务。
- Evidence: 本任务 `execution-log.md` 与 `verification-report.md`。
