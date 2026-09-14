# BDD + TDD 设计：生产组长员工管理

## Scope

本设计覆盖 `AC-D01` 和其衍生联动：生产组长维护可选员工，员工来源包含正式工和临时工，生产填写员工卡片只显示当前生产组长关联且未禁用员工。正式工复用主电子签名流程；临时工不创建登录账号，但必须有独立电子签名密码并接入统一电子签名记录。

## Non-Goals

- 不把临时工录入系统用户管理。
- 不给临时工创建登录账号、角色、菜单权限或后台访问权限。
- 不允许生产组长重置正式工电子签名密码。
- 不用全系统用户列表暴露正式工候选。
- 不用 API-only 替代真实页面 E2E。

## Domain Model

### 生产人员档案

| 字段 | 说明 |
|---|---|
| `id` | 系统内部唯一生产人员 ID。 |
| `tenant_id` | 租户。 |
| `person_type` | `FORMAL_USER` 或 `TEMP_WORKER`。 |
| `display_name` | 生产页面显示名，同一生产组长有效关联范围内必须唯一。 |
| `user_id` | 正式工关联系统用户；临时工为空。 |
| `signature_password_hash` | 临时工签名密码哈希；正式工为空并复用原电子签名密码。 |
| `status` | 启用 / 禁用。 |
| `deleted` | 逻辑删除。 |
| 审计字段 | `creator/create_time/updater/update_time`。 |

### 生产组长员工关联

| 字段 | 说明 |
|---|---|
| `id` | 关联 ID。 |
| `tenant_id` | 租户。 |
| `leader_user_id` | 生产组长用户 ID。 |
| `person_id` | 生产人员档案 ID。 |
| `display_name_snapshot` | 当前关联显示名快照，用于列表和历史追溯。 |
| `status` | 启用 / 禁用。 |
| `disabled_reason` | 禁用原因。 |
| 审计字段 | `creator/create_time/updater/update_time`。 |

### 操作追溯

| 字段 | 说明 |
|---|---|
| `id` | 追溯记录 ID。 |
| `tenant_id` | 租户。 |
| `leader_user_id` | 当前生产组长。 |
| `person_id` | 目标生产人员。 |
| `action_type` | `ADD_TEMP`、`LINK_FORMAL`、`DISABLE`、`ENABLE`、`UPDATE_DISPLAY_NAME`、`RESET_TEMP_SIGNATURE_PASSWORD`。 |
| `before_json` / `after_json` | 操作前后关键值，密码只记录是否重置，不记录明文或哈希。 |
| `reason` | 操作原因，可选但重置和禁用建议必填。 |
| `operator_user_id` / `operation_time` | 操作人和时间。 |

## BDD Scenarios

```gherkin
Feature: 生产组长员工管理

  Scenario: 手工录入临时工并用于后续生产填写
    Given 生产组长打开员工管理 Tab
    When 输入唯一员工姓名和电子签名密码新增临时工
    Then 系统创建临时生产人员档案
    And 自动关联到当前生产组长
    And 生产填写员工卡片可以选择该临时工
    And 系统不创建登录账号

  Scenario: 从受限下拉中搜索并关联正式工
    Given 生产组长打开员工管理 Tab
    When 输入正式工姓名关键字并选择候选正式工
    Then 系统只关联该正式工到当前生产组长
    And 页面不要求设置电子签名密码
    And 正式工后续签名使用主电子签名流程

  Scenario: 不暴露全系统正式工列表
    Given 生产组长打开正式工新增弹窗
    When 未输入搜索关键字或输入不在允许范围内的关键字
    Then 系统不返回全系统用户列表
    And 不返回跨租户或无权限查看的用户

  Scenario: 同一生产组长有效员工显示名不能重复
    Given 当前生产组长已经关联未禁用员工“张三”
    When 再新增或关联显示名为“张三”的员工
    Then 系统拒绝保存
    And 页面提示修改姓名或添加后缀

  Scenario: 员工卡片只显示当前组长关联且未禁用员工
    Given 当前生产组长进入生产填写页面
    When 点击员工卡片选择员工
    Then 员工列表只包含关联当前生产组长且未禁用的员工
    And 不包含未关联、已禁用、跨租户或无权限员工

  Scenario: 禁用员工不影响历史记录
    Given 员工已经产生历史报工或电子签名
    When 生产组长禁用该员工
    Then 新生产填写不能再选择该员工
    And 历史报工、签名和批记录仍显示当时姓名快照

  Scenario: 生产组长可重置临时工电子签名密码并留痕
    Given 当前组长关联了临时工
    When 生产组长重置该临时工签名密码
    Then 系统更新密码哈希
    And 操作追溯记录保存重置动作、操作人、时间和目标人员
    And 追溯记录不保存明文密码

  Scenario: 生产组长不能重置正式工电子签名密码
    Given 当前组长关联了正式工
    When 生产组长尝试重置正式工签名密码
    Then 系统拒绝操作
    And 提示正式工使用主电子签名流程

  Scenario: 无权限和跨租户操作被后端拒绝
    Given 非当前生产组长、无权限用户或跨租户目标员工
    When 调用新增、关联、禁用、启用、搜索或重置接口
    Then 后端拒绝请求
    And 不写入人员档案、组长关联或追溯记录
```

## API Contract

| API | 用途 | 关键约束 |
|---|---|---|
| `GET /mes/pro/process-pool/team-leader/person/page` | 当前组长关联员工列表 | 只返回当前登录生产组长关联员工，不返回全系统人员。 |
| `GET /mes/pro/process-pool/team-leader/person/formal-user-candidates` | 正式工搜索下拉 | 必须有 keyword；只返回允许范围内用户；不返回密码、签名敏感字段。 |
| `POST /mes/pro/process-pool/team-leader/person/temp` | 新增临时工 | `displayName + signaturePassword` 必填；同组长有效显示名唯一。 |
| `POST /mes/pro/process-pool/team-leader/person/formal-link` | 关联正式工 | `userId + displayName`；不设置签名密码。 |
| `PUT /mes/pro/process-pool/team-leader/person/{linkId}/disable` | 禁用关联员工 | 新报工不可选，历史保留。 |
| `PUT /mes/pro/process-pool/team-leader/person/{linkId}/enable` | 启用关联员工 | 恢复前重新校验显示名唯一。 |
| `PUT /mes/pro/process-pool/team-leader/person/{linkId}/display-name` | 修改显示名 | 同组长有效显示名唯一，追溯变更。 |
| `PUT /mes/pro/process-pool/team-leader/person/{linkId}/temp-signature-password` | 重置临时工签名密码 | 仅临时工允许，正式工拒绝。 |
| `GET /mes/pro/feedback/frontline/team-leader/selectable-persons` | 生产填写员工卡片候选 | 只返回当前生产组长关联且未禁用员工。 |
| `GET /mes/pro/process-pool/team-leader/person/{linkId}/audit` | 操作追溯 | 只读追溯记录，敏感字段脱敏。 |

## TDD Sequence

1. `RED-DB`：schema 测试先失败，证明人员档案、组长关联、追溯表和唯一约束尚不存在。
2. `GREEN-DB`：添加迁移和 schema 测试，验证字段、索引、逻辑删除和唯一约束。
3. `RED-BE-01`：服务测试证明临时工新增、签名密码哈希、关联和审计缺失。
4. `GREEN-BE-01`：实现临时工新增最小后端链路。
5. `RED-BE-02`：服务测试证明正式工受限搜索/关联和正式工禁止重置签名密码缺失。
6. `GREEN-BE-02`：实现正式工搜索、关联、签名密码拒绝规则。
7. `RED-BE-03`：服务测试证明重名、禁用、启用、显示名修改、无权限、跨租户隔离缺失。
8. `GREEN-BE-03`：补齐校验、权限和审计。
9. `RED-FE-01`：前端静态合同证明员工管理 Tab、标准列表模板、新增入口和正式工搜索下拉缺失。
10. `GREEN-FE-01`：实现员工管理 Tab 和列表。
11. `RED-FE-02`：前端静态合同证明生产填写员工卡片仍未使用当前组长关联人员。
12. `GREEN-FE-02`：切换员工卡片数据源并处理禁用员工不可选。
13. `REGRESSION`：运行目标 Maven、前端静态合同、`pnpm ts:check` 和相关相邻测试。
14. `E2E`：释放 worktree runtime slot 后，启动 worktree 前后端并用 Playwright 真实页面验收八条用户口径。

## E2E Acceptance Matrix

| 用户验收口径 | E2E 证明 |
|---|---|
| 列表范围 | 登录生产组长后员工管理 Tab 只显示该组长关联员工；检查无全系统用户泄漏。 |
| 员工来源 | 同一页面分别完成正式工搜索关联和临时工手工录入。 |
| 正式工新增 | 输入姓名触发下拉搜索，选择正式工；页面不出现签名密码输入，后端不写临时密码。 |
| 临时工新增 | 输入姓名和签名密码；新增后列表出现，且无系统登录账号。 |
| 生产填写选择 | 进入生产填写页面，点击员工卡片，只看到当前组长关联且未禁用员工。 |
| 禁用员工 | 禁用后员工卡片不再出现；历史只读区域仍能显示姓名快照。 |
| 重名控制 | 添加同名正式工或临时工被拒绝，提示添加后缀。 |
| 追溯记录 | 新增、禁用、启用、显示名修改、临时工密码重置、正式工关联均可在审计记录中看到。 |

## Current E2E Blocker

本 worktree 已创建，但 `int_main` 附加 worktree runtime slot 已满，`reserve-worktree-slot.ps1` 无法分配 1..19 槽位。根据项目规则，不允许随机换端口或绕过登记表启动服务。因此真实 E2E 只能在释放一个槽位或用户明确授权其它合规运行方案后执行。
