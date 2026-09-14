# DCC 五账号文档上传 E2E 角色准备验证报告

## Scope 范围

- 本报告验证文控上传文档 E2E 所需的 5 个非 `admin` 账号前置条件。
- 本次范围为账号、权限、类别上传规则、审批路线候选人和登录可用性准备；未执行完整 Playwright 上传文档 E2E。
- 数据范围限定本机 Docker MySQL `ruoyi-vue-pro`、租户 `1`、目标类别 `DCC_OTHER_TEMPLATE_900250`。

## Matrix 需求到验证矩阵

| 需求 | 验证方式 | 结果 |
| --- | --- | --- |
| 5 个账号互不相同且不包含 `admin` | 只读查询 `system_users` 与用户名检查 | PASS |
| 上传人具备上传入口权限 | 菜单权限查询和类别 `UPLOAD` 规则查询 | PASS |
| 文控审核、会签审核、会签批准、文控批准由不同账号承担 | 只读查询启用路线 `907390` 的 4 个节点 | PASS |
| 目标类别只有一个启用审批路线 | 只读查询目标类别启用路线数量 | PASS |
| 5 个账号登录口令前置可用 | 本机后端登录/登出预检，不输出 token 或密码 | PASS |

## Test Data 账号映射

| E2E 角色 | 账号 | 昵称 | 用户 ID | 关键权限或节点 |
| --- | --- | --- | --- | --- |
| 上传人 | `pengyunfeng` | 彭云凤 | 151 | `dcc:controlled-file:query`、`dcc:controlled-file:submit`、类别 `UPLOAD` |
| 文控审核 | `zhaohaichen` | 赵海辰 | 376 | 路线 `907390` 第 1 节点 `DOC_CONTROL_REVIEW` |
| 会签审核 | `zhaojie` | 赵杰 | 1074 | 路线 `907390` 第 2 节点 `MATRIX_REVIEW` |
| 会签批准 | `zhaomingyu` | 赵明玉 | 424 | 路线 `907390` 第 3 节点 `MATRIX_APPROVAL` |
| 文控批准 | `wangsiyu` | 王思雨 | 910250 | 路线 `907390` 第 4 节点 `DOC_CONTROL_APPROVAL` |

## Test Setup 数据变更摘要

- 为 `pengyunfeng` 补齐角色 `910207` / `体系工程师`，满足文控文件查询与提交菜单权限。
- 为 `zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` 补齐角色 `910231` / `文控`，满足文控审核与批准菜单权限。
- 为目标类别 `906104` 增加 `pengyunfeng` 的 `UPLOAD` 类别权限规则。
- 停用旧启用路线 `906306`，新增并启用路线 `907390`，节点候选人均为直接 `USER`。

## RED/GREEN Evidence

- RED: 首次 MySQL 写入事务因字符集 collation mismatch 失败并回滚，未落库。
- GREEN: 使用 `utf8mb4_unicode_ci` 会话字符集后完成正式写入，并通过只读 SQL 复验。
- GREEN: 5 个账号均通过本机后端登录/登出预检，未输出 token 或密码。

## Verification 验证证据

- 用户状态：5 个用户均为 `status=0`、`deleted=0`、`tenant_id=1`。
- 非 admin 检查：目标 5 个用户中 `username='admin'` 的数量为 `0`。
- 菜单权限：上传人具备 `query/submit`；4 个审批账号具备 `query/review/approve`，实际角色还包含 `submit` 不影响本次路径。
- 类别上传规则：目标类别存在 `pengyunfeng` 的启用 `UPLOAD` 规则。
- 路线唯一性：目标类别当前启用路线数量为 `1`，启用路线 ID 为 `907390`。
- 路线节点：第 1-4 节点分别映射 `zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu`，路线节点中 `admin` 数量为 `0`。
- 登录预检：5 个账号均通过本机 `48081` 后端登录并立即登出，未输出 token 或密码。

## Blockers 边界和后续

- 目标类别仍保留既有 `admin` 上传权限规则；本任务没有删除既有基线权限，选定 E2E 五账号和审批路线不包含 `admin`。
- 完整文控上传文档 E2E 仍需通过 Playwright 操作真实前端页面，使用上传人上传任务自有测试文件，再由 4 个审批账号依次处理并清理任务数据。
- 当前工作区存在本任务外的未提交改动和本地 ahead 状态，本报告未执行 Git closeout。
