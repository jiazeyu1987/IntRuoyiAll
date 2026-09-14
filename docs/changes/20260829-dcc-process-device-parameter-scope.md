# DCC 报工数据按工序设备读取参数

## Request Summary

- Source: 用户于 2026-08-29 明确补充：不同工序的设备不同，同一工序不同设备需要提交的参数也可能不同。
- Requested behavior: 选择 DCC 项目代码和工序后，报工数据来源必须按“当前产品工序 + 实际设备”读取生产组长工序配置中的设备参数；不同设备即使参数编码相同，也必须分别展示、分别链接和分别回填。

## Current Baseline Reviewed

- 现有设备身份已经按 `selectedDevice.*@device:<deviceId>` 生成真实设备字段。
- 生产组长设备参数规则正式存储在 `mes_pro_process_pool_device_parameter_rule`，包含 `routeProcessId`、`processId`、`deviceId`、参数编码、参数名称和上下限。
- 当前批记录单元格链接来源目录虽然按 `routeProcessId` 查询参数规则，但生成参数来源字段时没有携带 `deviceId` 作用域；同一工序多个设备使用同一参数编码时会发生字段冲突或只能保留一套参数。
- 当前批记录回填读取 `deviceParameterReadings.<parameterCode>.*` 和 `equipmentParameterRules.<parameterCode>.*`，尚不能按设备身份解析设备参数来源。
- 既有生产组长配置和一线提交链路已经按 `deviceId + parameterCode` 校验设备参数，属于本次应复用的正式数据模型。
- 既有任务与验收：`doc/tasks/20260829-dcc-process-pool-real-device-labels/`、`docs/acceptance/production-line-process-pool/`。

## Classification

- requirement change: 已实现真实设备标签的增量需求；同时修正参数来源不能区分设备的行为缺口。

## Impact

- Product: 来源目录显示每台实际设备独有的参数、标准、上下限和报工值。
- Design: 参数字段作用域统一为“路线工序 + 设备”，设备参数不再仅按参数编码去重。
- Data: 复用生产组长工序设备绑定、班组设备资料和设备参数规则，不新增表、不修改历史数据。
- API: 来源字段增加设备身份元数据，并使用带 `@device:<id>` 的参数字段编码；缺失正式参数身份时继续 fail-fast。
- Tests: 增加同一工序不同设备参数编码/名称/上下限差异的后端、回填和前端静态合同测试；真实页面继续使用只读 E2E。
- Release: 不改变数据库 schema，不新增菜单、权限或远程环境操作；仅影响本地代码验证和现有报工数据来源目录。
- Operations: 设备参数绑定缺失、主数据缺失或参数身份不完整时暴露明确错误，不返回通用参数或默认成功。

## Decision

- ACCEPT: 接受并立即实现。
- Rationale: 该请求与原始“按真实设备区分报工来源”目标一致，且现有参数规则已经包含 `deviceId`；不补齐设备作用域会导致多设备参数链接错误。

## Required Approval

- 用户已在 2026-08-29 当前对话中确认该业务规则。

## Downstream Skill Reruns

- `backend-api-delivery`: 修改来源目录与回填读取，执行 RED -> GREEN -> regression。
- `frontend-feature-delivery`: 展示带真实设备身份的参数来源字段并更新静态合同。
- `behavior-driven-development`: 新增设备参数隔离和缺失配置失败场景。
- `playwright`: 继续使用真实页面只读 E2E 验证产品、工序、设备和参数来源。

## Blockers And Next Action

- Blockers: 当前无外部阻塞；正式参数规则和测试租户运行态可用性需在 E2E 前复核。
- Next action: 为设备参数字段增加设备作用域，补齐回填解析和测试证据。
