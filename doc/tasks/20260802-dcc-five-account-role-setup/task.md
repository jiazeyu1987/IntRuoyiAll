# DCC 五账号文档上传 E2E 角色准备

## 任务目标

在本机 `int_main` 数据库中，为文控上传文档完整 E2E 准备 5 个互不相同且不包含 `admin` 的账号，并补齐其文控上传、审核、批准所需的菜单权限、类别权限和 DCC 审批岗位分配。

## 里程碑

- [x] 读取登录、数据库、PowerShell 编码和任务收尾规则。
- [x] 建立任务目录和任务文档。
- [x] 只读核对现有账号、角色、菜单权限、类别权限、DCC 岗位和审批路线。
- [x] 补齐 5 个非 `admin` 账号的角色、类别权限与审批路线分配。
- [x] 复验 5 个账号可分别覆盖上传人、文控审核、会签审核、会签批准、文控批准。

## 预期验证

- [x] 5 个账号均为 `system_users.deleted=0/status=0/tenant_id=1`，且用户名均不是 `admin`。
- [x] 上传人具备 `dcc:controlled-file:submit` 菜单权限和目标类别 `UPLOAD` 权限。
- [x] 文控审核人与文控批准人具备 `dcc:controlled-file:review/approve` 权限，并能解析到 DCC 审批路线节点。
- [x] 会签审核人具备 `dcc:controlled-file:review` 权限，并能解析到目标路线会签审核节点。
- [x] 会签批准人具备 `dcc:controlled-file:approve` 权限，并能解析到目标路线会签批准节点。
- [x] 所有写入仅限本机 `ruoyi-vue-pro`、租户 `1`，不记录密码或 token。
- [x] 5 个账号均通过本机后端登录/登出预检。

## Current Status

ready_for_closeout

## 账号映射

| E2E 角色 | 账号 | 昵称 | 用户 ID | 核验结果 |
| --- | --- | --- | --- | --- |
| 上传人 | `pengyunfeng` | 彭云凤 | 151 | 已启用；具备提交菜单权限和目标类别上传权限 |
| 文控审核 | `zhaohaichen` | 赵海辰 | 376 | 已启用；在目标路线第 1 节点 |
| 会签审核 | `zhaojie` | 赵杰 | 1074 | 已启用；在目标路线第 2 节点 |
| 会签批准 | `zhaomingyu` | 赵明玉 | 424 | 已启用；在目标路线第 3 节点 |
| 文控批准 | `wangsiyu` | 王思雨 | 910250 | 已启用；在目标路线第 4 节点 |

## 验证结论

- 目标类别：`DCC_OTHER_TEMPLATE_900250` / `其他`，类别 ID `906104`。
- 当前唯一启用审批路线：`907390`，版本 `4`。
- 目标路线 4 个审批节点均为直接 `USER` 候选人，未包含 `admin`。
- 5 个账号用户名均不是 `admin`，均可使用本机正式登录接口登录并登出。
- 目标类别仍保留既有 `admin` 上传权限规则；本任务未删除既有基线权限，E2E 使用的 5 个账号不包含 `admin`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按正式角色、类别权限和 DCC 岗位解析链路补齐测试账号。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 数据库写入前必须只读核对真实表结构、目标租户、目标账号、目标角色、目标菜单和 DCC 业务权限链路。
- 中文 SQL 通过容器内 MySQL stdin 执行，禁止在命令或日志中打印数据库密码。
- DCC 上传要同时满足菜单权限 `dcc:controlled-file:submit` 与类别级 `UPLOAD` 权限，不得只补菜单权限。
- DCC 审批任务要同时满足菜单权限和路线快照候选人岗位解析，不得用超管权限替代 DCC 岗位分配。

## Cleanup Keep

- doc/tasks/20260802-dcc-five-account-role-setup/task.md
- doc/tasks/20260802-dcc-five-account-role-setup/execution-log.md
- doc/tasks/20260802-dcc-five-account-role-setup/verification-report.md
