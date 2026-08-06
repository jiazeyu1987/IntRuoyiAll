# 生产组长工序配置统一表 PRD

- Task ID: `20260805-production-leader-process-config-unification`
- Created: `2026-08-05`
- Workspace: `D:\IntRuoyiWorktree\20260805-process-config-unification`
- User Request: `生产组长损耗管理、设备映射和设备参数设置合并为以路线工序为主线的统一配置表；参数维护目标值、上下限，实际平均值从生产提交统计并显示周期与样本数；完成后端、前端和真实路径验证。`

## Goal

在生产组长工作台建立一张以 `routeProcessId` 为唯一业务主线的工序配置表。每行同时展示该路线工序的损耗原因、当前组长可维护的映射设备及设备参数标准；参数标准维护参数编码、名称、单位、值类型、下限、目标值和上限，实际平均值只读展示。

系统必须使用正式路线工序授权、正式设备映射、正式参数规则和正式生产提交事件，不允许以前端拼接、裸 `processId`、空 `routeProcessId`、默认值或其它配置链路推断业务事实。

## Scope

- 数据库迁移：收紧 `mes_pro_process_pool_device_parameter_rule.route_process_id` 和目标值存储列 `default_value`，调整路线工序维度唯一约束。
- 后端统一读模型：按当前生产组长获授权的路线工序返回损耗原因、设备和参数统计。
- 后端统一写模型：设备绑定和参数保存都以必填 `routeProcessId` 为行上下文，由服务端解析正式 `processId`，不接受调用方自行拼接上下文。
- 参数标准：维护 `parameterCode`、`parameterName`、`unit`、`valueType`、`lowerLimit`、`targetValue`、`upperLimit` 和启用状态。
- 参数统计：只读取近 30 天正式 `PRODUCTION_SUBMIT` 事件的 `raw_payload.equipmentParameters` 数值，并按路线工序、设备和参数编码过滤。
- 前端交互：把独立损耗表、裸 ID 设备映射表单和裸 ID 参数表单收敛为一张统一表及行内操作弹窗。
- 验证：数据库合同、后端单元/控制器测试、前端静态合同、类型检查、相邻回归、真实 E2E 脚本语法检查和用户手动验收交接。2026-08-06 用户明确取消真实 Playwright 写入路径作为合并前完成门禁。

## Non-Goals

- 不合并设备档案、生产人员、活跃订单、PQC 人员、工序异常原因或其它班组配置；统一表只聚合当前生产组长的工序设备映射和参数标准。
- 不把设备实际平均值改为人工维护字段，也不把平均值写入参数规则表。
- 不改变一线正式报工 payload 的 `equipmentParameters` 契约。
- 不把统计周期做成可配置项；本任务固定为滚动近 30 天。
- 不访问、修改或验证远端测试服、生产服或备用服务器。
- 不猜测历史空 `route_process_id` 应属于哪条路线工序，不做自动回填。
- 不增加兼容旧空上下文的 fallback、接口别名、默认成功或异常吞并。

## Preconditions

- 当前分支为 `codex/20260805-process-config-unification`，工作目录固定为本 PRD 记录的 worktree。
- `int_main` worktree slot `4` 已登记，前端端口 `8085`，后端端口 `48085`。
- 本机 Java 17、Maven、Node 和 pnpm 可用；Playwright 脚本保留为可选人工/后续验证资产，合并前只要求语法检查。
- 本机 MySQL、Redis、前后端运行依赖可用，目标数据库结构与当前迁移链一致。
- 用户手动验收时需使用具有生产组长菜单权限和“工序开始”路线工序授权的测试身份。
- 写入型真实页面验证改由用户在主代码手动执行；自动化合并门禁不得用 API-only、mock 或旧截图冒充真实页面已通过。
- 数据库迁移前必须检查全部历史参数规则行的 `route_process_id` 和目标值存储列 `default_value`，不区分 `deleted` 状态；任一存在空值时立即停止。
- 合并前自动化门禁缺失时必须停止并由主 Agent 写入 `task-state.json.blocking_prereqs`，不得切换环境、账号、端口、数据源或测试方式。已被用户明确移出合并门禁的真实 E2E 前置缺失不再阻塞合并，但必须记录为手动验收前置。

## Impacted Areas

- `IntRuoyiBackend/sql/mysql/` 中的参数规则迁移与迁移合同测试。
- `MesProcessPoolDeviceParameterRuleDO`、Mapper、Schema 测试和前线运行态读取逻辑。
- `MesProcessPoolTeamLeaderController` 及生产组长工序配置的 VO、BO、Service。
- `MesTeamLeaderLossReasonServiceImpl`、`MesTeamLeaderRuntimeConfigServiceImpl`、重复参数保存服务及相邻测试。
- `MesProProcessPoolEventDO`、事件 Mapper 和近 30 天统计查询。
- `IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`。
- `TeamLeaderWorkbenchPage.vue` 及生产组长静态合同、真实 E2E。

## Domain Contract

### Unified Row

统一表一行对应一个 `routeProcessId`，至少返回：

- 路线：`routeId`、`routeCode`、`routeName`。
- 工序：`routeProcessId`、`processId`、`processCode`、`processName`、`sort`。
- 损耗：`lossReasons[]`，保留编码、名称、启用状态和正式记录 ID。
- 设备：`devices[]`，保留设备 ID、编码、名称、状态和映射状态。
- 参数：每台设备下返回 `parameters[]`。

参数项至少返回：

- `ruleId`
- `parameterCode`
- `parameterName`
- `unit`
- `valueType`
- `lowerLimit`
- `targetValue`
- `upperLimit`
- `enabled`
- `actualAverage`
- `sampleCount`
- `statisticsStartTime`
- `statisticsEndTime`
- `statisticsWindowDays=30`

### Parameter Identity And Persistence

- 新读写合同统一使用 `targetValue` 业务名称；`defaultValue` 对外必须称为“目标值”。
- 数据库存储复用现有 `default_value`，但新 API、UI、错误消息和验收证据不得继续称为“默认值”。
- 参数唯一业务上下文为当前租户下的 `routeProcessId + deviceId + parameterCode + deleted`。
- 相同上下文再次保存必须更新原规则并返回原规则 ID，不得插入第二条有效规则。
- `routeProcessId`、设备、参数编码、下限、目标值、上限和值类型均为必填。
- 必须满足 `lowerLimit <= targetValue <= upperLimit`。
- 参数设备必须已映射到该路线工序对应的正式工序，且属于当前生产组长可维护设备范围。

### Actual Average

- 统计窗口为服务端当前时间向前滚动 30 天，使用 `server_submit_time`，范围为 `[statisticsStartTime, statisticsEndTime]`。
- 仅统计 `event_type='PRODUCTION_SUBMIT'`。
- 在当前租户内，样本必须按 `routeProcessId + deviceId + parameterCode` 精确过滤，不得以裸 `processId`、空路线工序或其它设备/参数数据补齐。
- 只读取 `raw_payload.equipmentParameters[parameterCode]` 的 JSON 数值。
- 缺字段、`null`、字符串、布尔值、对象和数组不计入样本。
- 合法 payload 中的非数值不算样本；无法解析的 `raw_payload` 属于数据完整性错误，必须显式失败，不得静默跳过。
- `actualAverage` 为数值样本的算术平均值，`sampleCount` 为参与计算的数值数量。
- 无样本时必须返回 `actualAverage=null`、`sampleCount=0`，不得返回目标值、下限、上限、0 或历史默认值作为平均值。

## Phase Plan

### P1: 数据库约束与迁移合同

- Objective: 使参数规则正式归属于路线工序，并在迁移层阻止历史空上下文进入新模型。
- Owned paths: `IntRuoyiBackend/sql/mysql/`、MES schema 测试、迁移策略测试。
- Dependencies: 当前参数规则表结构和本地数据库只读核对可用。
- Deliverables: fail-fast 迁移、`route_process_id NOT NULL`、`default_value NOT NULL`、路线工序唯一索引、迁移与 schema 测试。

### P2: 后端统一读写与统计

- Objective: 建立一个正式工序配置服务和 Controller 合同，聚合损耗、设备、参数与实际平均值。
- Owned paths: MES Controller/VO、Service/BO、Mapper/DO、后端测试。
- Dependencies: P1 数据合同已批准。
- Deliverables: 统一列表 API、路线工序设备绑定 API、参数 upsert API、30 天统计、授权和业务校验、重复参数保存路径收敛。

### P3: 前端统一表与维护交互

- Objective: 用一张路线工序表替代分散的损耗、设备映射和参数设置入口。
- Owned paths: `teamLeader.ts`、`TeamLeaderWorkbenchPage.vue`、任务专用前端静态合同。
- Dependencies: P2 API 合同稳定。
- Deliverables: “工序配置”入口、统一表、设备映射弹窗、参数弹窗、损耗 CRUD、只读统计展示和可见错误状态。

### P4: 回归与人工验收交接

- Objective: 完成保留的目标自动化回归，并把真实页面验证移交给用户手动验收。
- Owned paths: 后端目标测试、前端静态合同、真实 Playwright 脚本语法检查、任务证据目录和变更记录。
- Dependencies: P1-P3 已完成窄范围 GREEN。
- Deliverables: 目标测试命令可运行、真实 E2E 脚本语法有效、合并前保留门禁通过、真实 E2E 取消原因和用户手动验收责任记录完整。

## Phase Acceptance Criteria

### P1

- P1-AC1: 迁移在任何历史参数规则行存在 `route_process_id IS NULL` 或目标值存储列 `default_value IS NULL` 时通过明确 `SIGNAL SQLSTATE '45000'` 失败；检查不得按 `deleted` 状态排除历史行，并说明必须先完成正式数据治理。
- P1-AC2: 无空历史数据时，`route_process_id` 和 `default_value` 均为 `NOT NULL`，唯一索引包含 `tenant_id + route_process_id + device_id + parameter_code + deleted`，旧的不含路线工序唯一索引被移除。
- P1-AC3: 迁移不包含对路线工序或目标值的猜测回填，不使用默认路线、首条路线、按 `process_id` 自动选择路线工序或任意默认目标值，且通过项目 migration policy gate 和 schema 合同。
- Evidence expectation: 迁移 SQL、迁移策略输出、schema 测试和 RED/GREEN 命令记录。

### P2

- P2-AC1: 统一列表只返回当前登录生产组长经“工序开始”正式授权的路线工序，并按路线与工序排序；每行聚合损耗原因、映射设备和参数。
- P2-AC2: 设备绑定请求以 `routeProcessId` 和 `deviceId` 为输入，由服务端解析 `processId`；未授权路线、非当前组长设备、禁用/报修设备或不一致上下文必须拒绝且不写入。
- P2-AC3: 参数保存必须提供非空 `routeProcessId`，设备必须已绑定；`lowerLimit <= targetValue <= upperLimit` 不成立时返回业务错误且不写入。
- P2-AC4: 相同 `routeProcessId + deviceId + parameterCode` 再次保存更新现有规则，规则总数不增加，正式审计记录保留变更前后快照。
- P2-AC5: 实际平均值只统计近 30 天正式 `PRODUCTION_SUBMIT` 的 `raw_payload.equipmentParameters` 数值，并按 `routeProcessId + deviceId + parameterCode` 精确过滤；边界外事件、其它事件类型、其它路线工序、其它设备、其它参数和非数值均不计入。
- P2-AC6: 无样本返回 `actualAverage=null`、`sampleCount=0` 和明确统计起止时间；不得用 `targetValue/defaultValue` 回填平均值。
- P2-AC7: 前线运行态不再接受空 `routeProcessId` 参数规则；重复参数 Service/Controller 写路径收敛为一个正式实现，不保留静默兼容或双写。
- Evidence expectation: Service、Controller、Mapper 和相邻前线运行态测试，覆盖成功与失败路径。

### P3

- P3-AC1: 生产组长页面只保留一个“工序配置”统一入口；原独立损耗表、裸 `processId/deviceId` 设备映射表单和裸 ID 参数表单不再作为可操作入口。
- P3-AC2: 统一表按路线工序展示损耗原因、映射设备、参数标准完成情况；设备可展开查看参数的下限、目标值、上限、单位、值类型、实际平均值、样本数和统计周期。
- P3-AC3: 损耗、设备映射和参数维护都从当前表格行进入，弹窗冻结 `routeProcessId`；设备使用可选列表而非手输 ID，参数弹窗不得编辑平均值、样本数或统计周期。
- P3-AC4: 前端提交前校验必填项和 `lower <= target <= upper`，后端错误必须以可见消息呈现；保存成功后重新读取正式行数据，不用本地假回显。
- P3-AC5: `actualAverage=null` 显示为“暂无样本”或 `--`，同时显示样本数 `0`，且不得展示目标值冒充平均值。
- P3-AC6: 页面在桌面和移动宽度下可用，关键控件有稳定 `data-*` 选择器，页面无重复入口、不可见写按钮或因表格展开导致的布局溢出。
- Evidence expectation: 任务专用静态合同、`pnpm ts:check`、相邻工作台静态回归和源码审查。

### P4

- P4-AC1: 数据库、后端和前端目标测试均取得可追溯 RED/GREEN，相关 Maven、迁移策略、静态合同、类型检查和 `git diff --check` 全部通过。
- P4-AC2: 真实 E2E 脚本保留为可选验证资产且语法检查通过；合并前不运行真实 Playwright 写入路径，不生成截图或 trace，不把 API-only、mock、静态合同或旧截图记录为真实页面通过。
- P4-AC3: 用户手动验收责任已记录，手动验收需覆盖统一表设备映射、合法参数新增、相同编码更新、非法区间拒绝、保存后回显、损耗维护、一线正式提交平均值和无样本 null/0 语义。
- P4-AC4: 验收范围变更、取消原因、保留验证命令、未运行真实 E2E 的边界、非任务历史回归失败和合并风险均写入 `test-report.md`、`verification-report.md`、`execution-log.md` 和 `docs/changes/...`。
- Evidence expectation: `test-report.md`、`verification-report.md`、`execution-log.md`、`docs/changes/20260806-production-leader-process-config-manual-verification.md`、目标测试输出摘要和 `git diff --check` 记录。

## Done Definition

- P1-P4 全部完成，所有 acceptance ID 在 `execution-log.md` 或 `test-report.md` 中有证据。
- 每个后端和前端行为变更均有对应测试，BDD、RED、GREEN 和回归命令已记录。
- 统一表使用正式 `routeProcessId`，不存在空路线工序参数规则或前端裸 ID 拼接。
- 参数规则满足区间约束且同上下文 upsert，不产生重复有效规则。
- 30 天平均值口径、无样本语义和只读展示符合本合同。
- 用户明确取消真实 E2E 合并前门禁；真实页面路径由用户在主代码手动验收，任务文档已记录交接范围和风险。
- `validate_test_report.py` 与 `check_completion.py` 通过。
- 不存在 fallback、mock 成功、默认平均值、吞异常、把未运行真实 E2E 写成已通过或未解释的保留门禁失败。

## Blocking Conditions

- 任何历史参数规则行存在空 `route_process_id` 或空目标值 `default_value`，包括已删除历史行。
- 无法从“工序开始”配置取得当前生产组长正式路线工序授权。
- 参数设备未映射到当前路线工序对应工序，或设备不属于当前组长维护范围。
- 正式 `PRODUCTION_SUBMIT` 事件缺少可验证的路线工序、设备、提交时间或 payload 来源。
- 合并前保留的迁移、目标测试、类型检查、静态合同或 `git diff --check` 任一失败、跳过或缺证据。
- 真实 E2E 已由用户明确移出合并前门禁；不得继续把缺少真实 E2E 前置作为当前任务合并 blocker，也不得把未运行真实 E2E 记录为已通过。
- 发现必须保留旧空上下文、重复接口或自动回填时，必须先取得用户明确批准；不得自行实现兼容 fallback。
