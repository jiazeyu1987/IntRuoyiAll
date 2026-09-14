# 执行日志

## 用户意图

- 用户要求核对测试服务器智能排产模块中九类疑似问题是否真实存在。
- 本轮为独立只读验证，不授权修复、发布、远端服务变更、数据写入或权限变更。
- 用户补充：测试服务器使用 `zhaojie` 登录，账号属于排产员，但工艺路线列表没有“编辑”按钮；提供了真实页面截图。

BDD: 排产员工艺路线编辑权限可见 -> Given 测试服务器账号 `zhaojie` 真实绑定启用的排产员角色且该角色有效拥有 `mes:pro-route:update` / When 登录后打开工艺路线列表 / Then 每行操作列显示“编辑”，登录权限响应、角色菜单绑定和前端 `v-hasPermi` 判断保持一致。

## 命令与操作意图

- 读取项目规则：确认测试服务器、登录、E2E、编码、任务记录和收尾边界。
- 建立任务档案：在任何浏览器验证或测试操作前记录目标、范围、里程碑与预期证据。
- 后续浏览器操作：仅导航、筛选、查看行状态、查看提示和捕获请求，不触发业务写接口。

## Milestone Updates

### M1 规则与前置核对

- 状态：完成。
- 已确认测试服务器前端入口：`http://172.30.30.58:8081/`。
- 已确认远端操作范围：用户当前请求授权测试服务器只读验证。
- 已确认账号来源要求：只使用现有浏览器会话，核对账号/租户，不记录凭据。
- 已确认数据清理方式：不执行写操作，预期无需清理。

### M2 入池状态筛选

- 状态：阻塞（真实页面）；代码与静态合同诊断已完成。
- 测试服务器浏览器跳转到 `/login?redirect=/user/profile`，不存在可用的 `zhaojie` 会话。
- 前端三个状态值与后端常量一致，未发现枚举整体错位。
- 同步工单首屏不带 `READY_TO_ADMIT` 符合 `20260805-standard-list-empty-tabs` 已验收的标准列表空条件契约。
- 多条件筛选 Tab 标签读取草稿状态，普通下拉变更只更新草稿且不会触发查询；因此新标签可以和旧列表结果同时存在。
- 现有 `mes-schedule-order-sync-tab-static.spec.js` 仍要求 8 月 4 日旧版默认可入池条件，与 8 月 5 日任务记录和真实 E2E 证据冲突，判定为过期静态合同，不能用其失败证明默认条件回归。

### M3 列表可用性和风险提示

- 状态：代码核对完成，真实页面待登录补证。
- “完成筛选”实际表达完成状态，命名不清晰。
- 非冻结行仍可能因已完成或已取消而禁选；页面未在复选框附近直接展示禁用原因。
- 交期风险已有红色/橙色文字颜色，但没有文本、图标、逾期天数或行级标识。
- 空日期、物料缺失、当前工序缺失均不在手动重排可选性和人工完成的正式前置校验中；是否全部阻断需业务规则确认。

### M4 工艺维护权限闭环

- 状态：代码核对完成，当前账号页面表现待登录补证。
- 工艺路线阻断动作的责任角色为“工艺维护”，所需权限固定为 `mes:pro-route:update`。
- 前端缺权限时仅显示缺失权限，不提供转办、申请权限或责任人联系入口。

### M5 报告

- 状态：部分完成。
- 已生成 `verification-report.md`，总体结论为 `PARTIAL / BLOCKED`。
- 按 `project-experience-consolidation` 将“筛选草稿标签不得冒充已执行结果口径”的通用门禁合并到现有 `docs/frontend-development.md#统一列表复合工具栏布局门禁`，未新建长期经验文档。
- 未标记 completed：真实测试服务器复验仍缺少 `zhaojie` 登录前置。

### M6 `zhaojie` 工艺路线编辑权限链路

- 状态：根因定位完成，修复等待远端写入授权。
- 用户截图来源：`C:\Users\BJB110\AppData\Local\Temp\codex-clipboard-9bd811de-a8ab-4292-992a-dae0575fe6c8.png`。
- 页面可见证据：工艺路线共 4 条，状态开关可见，“操作”列为空；新增、导入、导出按钮也未显示。
- 初步判断：前端只有在登录权限集合包含 `mes:pro-route:update` 时才渲染“编辑”；截图证明该权限当前未在页面生效，但截图本身不能证明 `zhaojie` 的实际角色绑定、角色菜单行、租户套餐菜单集合或权限缓存状态。
- 操作边界：仅做浏览器和只读服务端核对，不写 `system_role_menu`、`system_user_role` 或缓存。
- 只读数据库核对：`zhaojie` 对应 `system_users.id=1074`、`tenant_id=1`、用户启用且未删除；有效绑定 `system_role.id=910216`、`code=mes_scheduler`、角色启用且未删除。
- 角色菜单核对：`5720` 父菜单和 `5721/mes:pro-route:query` 均有效；`5723/mes:pro-route:update` 行存在但 `deleted=1`，最后由 `codex-scheduler-smart-only-20260618` 于 `2026-06-18 10:03:26` 软删除；`5730/mes:pro-route:version-query` 菜单在测试库不存在。
- 迁移状态核对：`infra_release_migration` 中没有 `20260716_mes_route_version_permission_menu` 和 `20260728_mes_scheduler_route_flow_list_permission` 的测试环境记录。`20260629_mes_smart_scheduling_role_scope` 已按旧 SHA `dc1ad592...` 应用，当前仓库同 ID 文件 SHA 为 `b078b3e1...`；新增的 `20260728` 迁移就是正式的已应用环境补齐路径，但测试服尚未执行。
- 首次组合 SQL 因远端 shell 引号被提前解析而失败，MySQL 未收到 SQL、无数据影响；随后改用 UTF-8 Base64 stdin 传递只读 SQL，查询成功。未输出或记录数据库密码、token 或连接密钥。
- RED: `zhaojie` 真实页面截图 + 测试库只读角色菜单查询 -> FAIL，排产员角色的 `5723/mes:pro-route:update` 为 `deleted=1`，页面操作列为空，符合失败原因。
- GREEN: BLOCKED，当前请求未授权执行测试服务器迁移、权限写入或缓存失效操作。
- 结论修正：先前“职责分离，不应给排产员扩权”的判断不成立。正式任务 `20260728-scheduler-route-flow-list-permission` 明确要求排产员拥有 `5723/update` 和 `5730/version-query`，只排除删除权限 `5724`。

## Verification Evidence

- `docs/server-access.md`：测试服务器为 `172.30.30.58`，访问远端需当前任务授权。
- `docs/login-access.md`：远端真实路径验证必须记录目标主机、租户、账号来源和清理方式。
- `docs/e2e-rules.md`：E2E 使用真实前端；只读验证说明数据来源与只读范围；API 仅作只读辅助核验。
- `docs/frontend-development.md#统一列表复合工具栏布局门禁`：筛选条件必须验证正式请求参数和真实页面状态，不能只看条件 Tab 标签。
- 测试前端 HTTP 200；测试后端健康检查 HTTP 200、`{"status":"UP"}`。
- 发布元数据：`release-20260806-intmain-head-test-r260806c-r1`，源码提交 `19173d2b41442e6b2df3c16c424c11713b3dbbcc`。
- `node tests\e2e\mes-schedule-order-sync-tab-static.spec.js`：FAIL；失败原因是遗留断言仍要求默认 `READY_TO_ADMIT`，与 8 月 5 日已验收的首屏空条件正式契约冲突。
- 后端聚焦 Maven 测试未进入测试阶段：javac 编译 MES 2531 个源文件时长时间停留在类文件写入；停止本任务进程后无 surefire 报告，不记为 PASS/FAIL。
- 远端写请求：0。

## Blockers

- 浏览器没有测试服务器登录会话，无法核对 `zhaojie` 当前租户、三组实时数量、指定工单 UI 和账号权限表现。
- 后端聚焦测试受本机 Maven/javac 编译卡住影响，未获得测试方法级结果。
