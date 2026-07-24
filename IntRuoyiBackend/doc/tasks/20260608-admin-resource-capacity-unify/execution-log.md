# Execution Log

## Pass 1 - Planning

- task id: admin-resource-capacity-unify
- changed paths: `doc/tasks/20260608-admin-resource-capacity-unify/*`
- implemented behavior: 建立 admin 资源大表设备产能统一任务记录；确认只处理 `tenant_id=1` 中 A03388 两组同设备同工序重复产能，保留较小产能并逻辑删除较大产能。
- validation commands: 文档人工复核。
- validation results: PASS。
- known risks or blockers: 当前 `48081` 是旧 runtime jar，资源大表接口验证使用已确认包含新接口的 `48082` 后端；若需要固定回 48081，需另行解决当前 int_main 后端 `FileService` 编译不一致问题。

BDD: admin 设备工序产能唯一 -> Given admin 租户中 A03388 同设备同工序存在两条不同产能记录 When 执行受保护数据修正 Then 每个设备+工序仅保留一条较小/保守产能记录。

BDD: 资源大表按底层唯一产能展示 -> Given admin 租户设备工序重复产能已清理 When 用户打开资源大表查询 A03388 Then 不再出现同设备同工序因产品不同而产能不同。

BDD: 数据修正边界 -> Given 测试租户已经完成排产演练数据修正 When 本次只处理 admin 租户 Then 测试租户冲突数仍为 0 且不被修改。

## Pass 2 - 前置校验

- task id: admin-resource-capacity-unify
- changed paths: local MySQL tenant `1`
- implemented behavior: 只读确认 admin 租户当前同设备同工序产能冲突和目标记录，未修改数据。
- validation commands: `SELECT same_device_process_conflicts ... FROM mes_dv_machinery_process WHERE tenant_id = 1`
- validation results: PASS。admin 冲突数 `2`；目标活动记录为 `id=129/90/130/91`；route product、route process、workstation process、workstation machine 引用缺失均为 `0`。
- known risks or blockers: 当前无阻塞。

RED: `same_device_process_conflicts tenant_id=1` -> FAIL, 冲突数 `2`；`A03388 + 外管拉伸2` 存在 `25.714286/270.000000` 与 `61.904762/650.000000`；`A03388 + 内管拉伸2` 存在 `40.000000/420.000000` 与 `80.000000/840.000000`。

## Pass 3 - 逻辑删除重复高产能记录

- task id: admin-resource-capacity-unify
- changed paths: local MySQL tenant `1`, table `mes_dv_machinery_process`
- implemented behavior: 在事务内按 `tenant_id=1`、指定 `id`、当前产能值、`deleted=b'0'` 和冲突数守卫，逻辑删除 `id=90` 与 `id=91`；保留 `id=129` 与 `id=130`。
- validation commands: guarded `UPDATE mes_dv_machinery_process SET deleted=b'1', updater='codex', update_time=NOW() ...`
- validation results: PASS，`updated_rows=2`。
- known risks or blockers: 当前无阻塞。

GREEN: `guarded UPDATE ids 90,91` -> PASS, `updated_rows=2`。

## Pass 4 - 后置 SQL 验证

- task id: admin-resource-capacity-unify
- changed paths: local MySQL tenant `1`
- implemented behavior: 校验 admin 冲突清零、目标活动记录唯一、测试租户不受影响。
- validation commands: `SELECT admin_conflicts/test_conflicts ...`, `SELECT active_ids ... A03388`
- validation results: PASS。`admin_conflicts=0`，`test_conflicts=0`；`A03388 + 外管拉伸2` 活动记录仅 `id=129`，产能 `25.714286`；`A03388 + 内管拉伸2` 活动记录仅 `id=130`，产能 `40.000000`。
- known risks or blockers: 当前无阻塞。

GREEN: `post SQL verification` -> PASS, admin 与测试租户冲突数均为 `0`。

## Pass 5 - admin 前端/API 只读验证

- task id: admin-resource-capacity-unify
- changed paths: none
- implemented behavior: 使用 Playwright 登录本机 `芋道源码/admin`，打开 `/mes/pro/route`，切换资源大表，并通过已登录上下文查询 A03388。
- validation commands: inline Playwright read-only check against `http://127.0.0.1:8081`
- validation results: PASS。资源大表 API 返回 `code=0`，`tenantId=1`；A03388 设备行 `rowCount=10`；`PROC-XLSX-00001:外管拉伸2` 产能集合仅 `25.714286`，`PROC-XLSX-00002:内管拉伸2` 产能集合仅 `40`。
- known risks or blockers: 当前 `8081` 已重启为指向 `48082` 后端的本机前端，用于验证新接口；`48081` 旧 runtime jar 仍不是本次修正的验证入口。

GREEN: `Playwright admin route resource read-only check` -> PASS, no `No static resource admin-api/mes/pro/route-resource/page` error.
