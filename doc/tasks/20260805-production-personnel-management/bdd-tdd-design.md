# BDD / TDD Design

## Scope

生产组长在生产工作台单独 tab 中管理“生产人员档案”。档案分为正式工与临时工，二者都必须与当前生产组长关联后才能在生产填写页员工卡片中选择。

## Non-Goals

- 不把临时工创建为系统登录账号。
- 不允许生产组长浏览全系统用户列表。
- 不允许生产组长重置正式工电子签名密码。
- 不用前端本地过滤替代后端权限与范围控制。

## Domain Model

- Production Person：生产人员档案，保存显示名、来源类型、正式用户 ID 或临时工签名密码哈希、启用状态、租户信息。
- Leader Person Link：生产组长与生产人员关联，保存关联状态、显示名快照、禁用信息。
- Audit Log：记录新增、关联、禁用、启用、改名、临时工签名密码重置等动作。
- Fill Candidate：生产填写页员工卡片候选，只返回当前组长关联且未禁用人员。

## BDD Scenarios

```gherkin
Feature: 生产组长生产人员档案管理

  Scenario: 组长只查看自己的关联员工
    Given 当前登录用户是生产组长
    When 打开生产工作台员工管理 tab
    Then 列表只显示已关联当前生产组长的员工
    And 页面不展示全系统用户列表

  Scenario: 搜索选择正式工并关联当前组长
    Given 当前登录用户是生产组长
    When 在正式工输入下拉框输入姓名关键字
    Then 后端只返回允许范围内的正式用户候选
    When 组长选择候选并确认关联
    Then 该正式工关联当前生产组长
    And 组长不能设置该正式工签名密码

  Scenario: 手动录入临时工
    Given 当前登录用户是生产组长
    When 录入临时工姓名和电子签名密码
    Then 系统创建临时生产人员档案
    And 不创建系统登录账号
    And 该临时工关联当前生产组长

  Scenario: 同一组长有效员工显示名不能重复
    Given 当前生产组长已有有效员工显示名“张三”
    When 组长新增或改名为“张三”
    Then 系统拒绝操作
    And 提示用户增加后缀区分重名员工

  Scenario: 生产填写只选择当前组长可用员工
    Given 当前生产组长有关联员工 A 和已禁用员工 B
    When 组长进入生产填写页面并点击员工卡片
    Then 候选列表包含 A
    And 候选列表不包含 B
    And 候选列表不包含未关联当前组长的员工

  Scenario: 禁用员工不影响历史快照
    Given 员工已参与历史报工和电子签名
    When 组长禁用该员工
    Then 该员工不再进入新报工选择
    And 历史报工、签名、批记录继续显示当时姓名快照

  Scenario: 人员管理操作全部留痕
    Given 当前生产组长执行人员管理操作
    When 新增、禁用、启用、修改显示名、重置临时工签名密码或关联正式工
    Then 审计日志记录动作、操作人、目标人员、结果、变更摘要和时间
```

## API Contract

- `GET /admin-api/mes/production-personnel/page`：分页查询当前组长关联员工。
- `GET /admin-api/mes/production-personnel/formal-user-candidates?keyword=`：按姓名关键字搜索允许范围内正式工候选；keyword 为空必须拒绝或返回空，不得返回全量。
- `POST /admin-api/mes/production-personnel/temp`：新增临时工，字段为显示名和签名密码。
- `POST /admin-api/mes/production-personnel/formal-link`：关联正式工，字段为正式用户 ID 和显示名。
- `PUT /admin-api/mes/production-personnel/{id}/display-name`：修改当前组长关联显示名。
- `PUT /admin-api/mes/production-personnel/{id}/disable`：禁用当前组长关联员工。
- `PUT /admin-api/mes/production-personnel/{id}/enable`：启用当前组长关联员工。
- `PUT /admin-api/mes/production-personnel/{id}/temp-sign-password`：仅允许重置临时工签名密码。
- `GET /admin-api/mes/production-personnel/fill-candidates`：生产填写员工卡片候选，只返回当前组长关联且未禁用员工。
- `GET /admin-api/mes/production-personnel/audit/page`：当前组长人员管理操作追溯。

## TDD Sequence

1. Schema RED：测试缺少生产人员、组长关联、审计表与唯一约束。
2. Schema GREEN：添加迁移、DO、Mapper、测试 schema。
3. Backend RED：新增服务与 Controller 目标 JUnit，先覆盖临时工新增、正式工搜索、重名、禁用、候选和审计。
4. Backend GREEN：实现最小正式服务，使用后端范围控制和 fail-fast 错误。
5. Frontend RED：新增静态合同，断言员工管理 tab 使用 `UnifiedListTemplate`、正式工远程搜索、临时工密码表单、生产填写候选 endpoint。
6. Frontend GREEN：实现 API wrapper、tab、列表、弹窗、操作按钮、员工卡片候选改造。
7. E2E RED/GREEN：若 runtime slot 可用，执行真实 Playwright 页面路径；若仍无槽位，记录阻塞，不做 API-only 降级。

## E2E Acceptance Matrix

| Requirement | Real Path Evidence |
| --- | --- |
| 列表只显示当前组长关联员工 | 组长登录后打开员工管理 tab，表格无未关联人员 |
| 正式工输入下拉搜索 | 输入姓名关键字后只出现允许候选，选择后新增关联 |
| 临时工姓名 + 签名密码 | 新增临时工成功且系统用户列表无新登录账号 |
| 重名提示 | 同组长重复有效显示名提交失败并显示加后缀提示 |
| 员工卡片候选 | 生产填写页候选只含当前组长关联且未禁用人员 |
| 禁用历史快照 | 禁用后新候选消失，历史记录仍显示原姓名快照 |
| 追溯记录 | 审计列表可见新增、禁用、启用、改名、密码重置等动作 |

## Current Blocker

真实 E2E 需要 worktree 登记端口并启动成对前后端服务。当前 `int_main` profile 的 `slot 1..19` 已无可用槽位，必须释放合规槽位或由用户授权符合项目规则的替代路径后才能执行真实 E2E。
