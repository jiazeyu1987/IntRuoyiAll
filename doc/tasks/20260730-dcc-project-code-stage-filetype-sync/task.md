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
- [ ] 若候选数量大于 0，调用官方批量分类接口并轮询终态。
- [ ] 执行 API 与 Playwright 真实路径复核，记录 GREEN 或 blocker。
- [ ] 完成验证报告、经验沉淀和任务收尾。

## Expected Verification

- RED：执行前使用真实测试服页面/API 证明至少存在候选不一致或未分类文件，并记录候选总数；若候选为 0，则记录“无需变更”并不启动写入任务。
- GREEN：批量任务完成后候选数为 0；项目代码详情阶段/文件类型计数更新；任务 `failedCount/conflictCount/ambiguousCount` 为 0。
- E2E：使用 Playwright 打开测试服 DCC 项目代码详情，抽样核对“设计和开发策划阶段/输入阶段/输出阶段/验证/确认/转换阶段”等左侧阶段与右侧文件类型分组。

## Current Status

in_progress

## Blocked Reason

测试服官方批量分类前置条件未同时满足，已按无 fallback/no SQL 策略停止，未调用写入型批量任务：

- `测试租户/aoteman`：登录、`doc_control` 角色和 DCC 查询/更新权限均通过；项目代码 124 个，候选文件 1 个；但启用且绑定 `fileTypeTaxonomyId` 的类别规则数量为 0，无法按类别规则映射目标阶段/文件类型。
- `芋道源码/admin`：类别规则完整，启用且绑定 `fileTypeTaxonomyId` 的类别 60 条，阶段分布为 `INPUT=7, PLAN=3, OUTPUT=25, VERIFICATION=5, VALIDATION=12, TRANSFER=8`；项目代码 117 个，候选项目 93 个，候选文件 15028 个；但该登录用户不具备 `doc_control` 角色，`/dcc/controlled-files/batch-recognition/tasks/latest?recognitionType=FILE_CATEGORY` 返回 403。
- `芋道源码` 租户已只读确认存在 `doc_control` 角色，分配用户为 `wangsiyu`、`zhaohaichen`；本任务没有这些账号凭据，且不允许修改角色或绕过 `@ss.hasRole('doc_control')`。
- 用户已补充 `芋道源码/zhaohaichen` 凭据；本任务重新进入执行态，仅通过正式 API 复核并继续，不记录密码。

## Authorization Scope

用户已明确要求在测试服 `172.30.30.58` 实施该计划；不操作正式服、备用服或非本任务数据环境。

## Blocker Policy

若出现测试服不可达、登录凭据不可用、缺少 `doc_control` 或 DCC 查询/更新权限、活动任务冲突、缺少 `dcc_project_code_id`、分类歧义、官方接口失败、导出失败明细不可用，立即停止并记录 blocker；不改用 SQL、mock、默认值或其它绕过方式。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：否，正式批量链路前置条件未满足；需要同一租户内同时具备启用且绑定 `fileTypeTaxonomyId` 的类别规则，以及有 `doc_control` 角色的可登录账号。
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
