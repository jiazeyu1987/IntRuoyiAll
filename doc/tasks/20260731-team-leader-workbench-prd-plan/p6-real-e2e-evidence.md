# P6 生产组长工作台真实 E2E 证据

- Task ID: `20260731-team-leader-workbench-prd-plan`
- Generated At: `2026-08-01T06:12:51.200Z`
- Status: `PASS`
- Frontend: `http://127.0.0.1:8084`
- Backend: `http://127.0.0.1:48084`
- Tenant: `测试租户`
- User: `aoteman`
- Data Prefix: `TLW-20260731-`

## BDD

- BDD: 生产组长配置驱动员工填报并完成订单工序 -> Given 测试租户有组长、员工、订单、工序、设备和正式批记录绑定 When 组长配置、员工填报、组长确认分配 Then 订单工序完成且批记录回填。
- BDD: FIFO 自动分配且可手动调整 -> Given 员工提交完成数量且活跃订单有剩余 When 组长点击 FIFO 自动分配并必要时手动调整 Then 分配只能保存到活跃订单且总数等于提交数量。

## GREEN

- GREEN: `pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:real` -> PASS
- Step: 真实 UI 登录测试租户成功
- Step: 组长工作台和配置中心可见
- Step: 生产组长通过 UI 加入活跃订单
- Step: 生产组长通过 UI 绑定工序员工
- Step: 生产组长通过 UI 恢复设备为启用
- Step: 生产组长通过 UI 维护工序设备和异常关系
- Step: 生产组长通过 UI 维护设备参数上下限和默认值
- Step: 员工端通过正式 frontlineSubmit 提交报工和设备参数
- Step: 只读发现员工提交事件 eventId=22
- Step: 生产组长通过 UI 生成 FIFO 自动分配行
- Step: 生产组长通过 UI 确认报工并分配到活跃订单
- Step: 分配记录只读 API 核验通过，且分配总数等于员工报工数量
- Step: 订单工序完成只读 API 核验通过，且状态为 COMPLETED / SUCCESS
- Step: 正式批记录回填只读 API 核验通过，且包含字段审计或单元格投影证据
- Screenshot: `D:\IntRuoyiWorktree\20260731_shengchanbanzuzhang\IntRuoyiFronted\test-results\team-leader-workbench-real-flow\team-leader-workbench-pass.png`
- Cleanup: PASS；真实 E2E 后已清理 `TLW-20260731-` 任务自有活跃订单、绑定、参数规则、异常原因、报工、事件、分配、工序完成和记录本条目，复核计数均为 `0`，设备 `980005` 恢复 `REPAIRING` 且 enabled。
