# 生产人员档案真实 E2E 证据

- Task ID: `20260805-production-personnel-management`
- Generated At: `2026-08-05T07:18:35.152Z`
- Status: `PASS`
- Frontend: `http://127.0.0.1:8082`
- Backend: `http://127.0.0.1:48082`
- Tenant: `测试租户`
- User: `aoteman`
- Data Prefix: `PPM-151308`

## BDD

- BDD: 生产人员档案管理真实页面 -> Given 测试生产组长登录真实前端 When 通过生产人员档案 tab 管理正式工和临时工 Then 页面、接口和审计均只作用于当前组长关联员工。
- BDD: 生产填写候选范围 -> Given 临时工已绑定当前组长可切换工序 When 禁用该临时工 Then runtime-config 不再返回该员工候选。

## GREEN

- GREEN: `pnpm e2e:production-personnel-management:real` -> PASS
- Step: 测试生产组长通过真实登录页进入系统
- Step: 只读发现可切换工序 routeProcessId=980006 processId=980002
- Step: 正式工通过远程姓名下拉真实选择并关联当前生产组长
- Step: 临时工通过真实页面录入显示名和电子签名密码
- Step: 同一生产组长重复显示名被拒绝并返回可理解提示
- Step: 临时工档案通过真实页面绑定到当前组长可切换工序
- Step: 可切换工序设备通过真实页面启用并绑定到当前组长负责范围
- Step: 生产填写 runtime-config 返回关联当前组长且未禁用的临时工候选
- Step: 临时工签名密码通过真实页面重置并复用统一密码入口
- Step: 员工禁用后从未禁用人员列表中移除
- Step: 禁用后生产填写 runtime-config 不再返回该临时工候选
- Step: 新增、重置密码、禁用操作均在追溯表中可见
- Screenshot: `D:\IntRuoyiWorktree\20260805-production-personnel-management\IntRuoyiFronted\test-results\production-personnel-management-real\production-personnel-management-pass.png`
- Password handling: login and signature passwords were injected/generated at runtime and are not written to artifacts.
