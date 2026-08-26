# P6 生产组长工作台真实 E2E 证据

- Task ID: `20260731-team-leader-workbench-prd-plan`
- Generated At: `2026-08-26T08:06:33.981Z`
- Status: `BLOCKED`
- Frontend: `--`
- Backend: `--`
- Tenant: `--`
- User: `--`
- Data Prefix: `TLW-20260731-`

## BDD

- BDD: 生产组长配置驱动员工填报并完成订单工序 -> Given 测试租户有组长、员工、订单、工序、设备和正式批记录绑定 When 组长配置、员工填报、组长确认分配 Then 订单工序完成且批记录回填。
- BDD: FIFO 自动分配且可手动调整 -> Given 员工提交完成数量且活跃订单有剩余 When 组长点击 FIFO 自动分配并必要时手动调整 Then 分配只能保存到活跃订单且总数等于提交数量。

## BLOCKED

- E2E: `pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real` -> BLOCKED, 缺少真实写入型 E2E 前置条件。
- Missing: `TLW_FRONTEND_URL` - 真实前端入口，例如 http://127.0.0.1:8084 或 http://127.0.0.1:8081。
- Missing: `TLW_BACKEND_URL` - 真实后端入口，例如 http://127.0.0.1:48084 或 http://127.0.0.1:48081。
- Missing: `TLW_TENANT` - 可写测试租户，禁止使用生产或 admin 基线租户。
- Missing: `TLW_USERNAME` - 拥有生产组长页签和员工填报路径权限的测试账号。
- Missing: `TLW_PASSWORD` - 测试账号密码，只能通过进程环境注入。
- Missing: `TLW_WORK_ORDER_ID` - 任务自有生产订单 ID。
- Missing: `TLW_WORK_ORDER_CODE` - 任务自有生产订单编码。
- Missing: `TLW_TASK_ID` - 任务自有生产任务 ID。
- Missing: `TLW_ROUTE_ID` - 正式工艺路线 ID。
- Missing: `TLW_ROUTE_PROCESS_ID` - 正式路线工序 ID。
- Missing: `TLW_PROCESS_ID` - 正式工序 ID。
- Missing: `TLW_ITEM_ID` - 生产订单对应产品物料 ID。
- Missing: `TLW_EMPLOYEE_PROFILE_ID` - 组长配置的员工档案 ID，可为临时工档案。
- Missing: `TLW_DEVICE_ID` - 组长配置的设备 ID。
- Missing: `TLW_RECORDBOOK_ID` - 正式记录本 ID。
- Missing: `TLW_SIGNATURE_ID` - 真实电子签名 ID。
- Missing: `TLW_SIGNATURE_EMPLOYEE_ID` - 签名员工 ID，必须等于实际填报员工。
- Missing: `TLW_APPROVE_USER_ID` - 生产组长审批人 ID。
- Missing: `TLW_FEEDBACK_CODE` - 本次一线报工单号，建议带 TLW-20260731- 前缀。
- Missing: `TLW_FEEDBACK_TYPE` - 正式报工类型。
- Missing: `TLW_WORK_ORDER_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_TASK_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_ROUTE_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_ROUTE_PROCESS_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_PROCESS_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_ITEM_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_EMPLOYEE_PROFILE_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_DEVICE_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_RECORDBOOK_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_SIGNATURE_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_SIGNATURE_EMPLOYEE_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_APPROVE_USER_ID` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_FEEDBACK_TYPE` - 必须是大于 0 的真实数字 ID，不能使用占位值。
- Missing: `TLW_FRONTEND_URL/TLW_BACKEND_URL` - 前后端 URL 必须成对使用：8084/48084 用于当前 worktree，或 8081/48081 用于 int_main 融合后验证。
- Impact: 未执行写入型真实 E2E；没有使用 mock、静态合同或 API-only 冒充成功。
