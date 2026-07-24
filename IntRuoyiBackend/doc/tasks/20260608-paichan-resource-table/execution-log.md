# Execution Log

## Pass 1 - Planning

- task id: T1
- changed paths: `doc/tasks/20260608-paichan-resource-table/*`
- implemented behavior: 建立任务文档、需求、开发计划、测试计划和状态文件；确认大表仅为聚合操作视图，底层数据模型保持现有路线/工序/设备/工位关系。
- validation commands: 文档结构人工复核。
- validation results: PASS。
- covered acceptance ids: AC-01
- known risks or blockers: 当前无阻塞。

BDD: 产品工艺资源大表展示 -> Given 测试租户已经覆盖 admin 的 MES 资源数据 When 用户打开大表 Then 用户按产品路线看到工序资源类型、设备或人数与产能来源。

BDD: 有设备工序产能来源 -> Given 同一设备绑定同一工序 When 大表在不同产品行展示该设备工序 Then 单台产能保持一致，不因产品不同而不同。

BDD: 无设备工序人数来源 -> Given 工序没有设备绑定 When 用户查看或维护该工序 Then 系统按人工人数计算预算产能，缺失时建立默认 5 人配置。

BDD: 大表保存到底层表 -> Given 用户在大表修改设备数量或人工人数 When 用户保存 Then 系统更新现有工位设备或工位人员表，刷新后显示一致。

## Pass 2 - 覆盖测试租户 MES 数据

- task id: T2
- changed paths: `doc/tasks/20260608-paichan-resource-table/copy-admin-mes-to-test.sql`, local MySQL tenant `122`
- implemented behavior: 使用事务将 tenant `1` 的 MES 路线、路线产品、路线工序、路线 BOM、工序、设备、设备工序、工位、工位设备、工位人员、工作中心、单位和实际引用物料覆盖到 tenant `122`。复制时为目标租户重建自增主键映射，不复用 admin 主键。路线/BOM 实际引用物料复制 12 条，未整库复制 admin 的 16316 条物料。
- validation commands: `Get-Content -Encoding utf8 ...\copy-admin-mes-to-test.sql | docker exec -i int-ruoyi-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 -D ruoyi-vue-pro`
- validation results: PASS。关键数量：route `4=4`，route_product `4=4`，route_process `101=101`，route_product_bom `50=50`，process `93=93`，machinery `31=31`，machinery_process `83=83`，workstation `93=93`，workstation_machine `54=54`，workstation_worker `3=3`。
- covered acceptance ids: AC-01
- known risks or blockers: 工位原 `production_line_id/warehouse_id/location_id/area_id` 未复制，因为本次大表与排程产能不依赖这些仓储/产线引用；目标工位中这些字段置空并记录为覆盖差异。

RED: `same_device_process_conflicts` SQL -> FAIL, `A03388 + 外管拉伸2` 与 `A03388 + 内管拉伸2` 各存在两条产品来源产能。

GREEN: `same_device_process_conflicts` SQL -> PASS, 冲突数 `0`；`A03388 + 外管拉伸2` 统一为 `25.714286`，`A03388 + 内管拉伸2` 统一为 `40.000000`。

GREEN: `route/workstation/machinery reference checks` SQL -> PASS, route product、route process、machinery process、workstation machine 引用缺失均为 `0`。

GREEN: `manual workstation defaults` SQL -> PASS, `穿显影环`、`包套装管`、`全检导丝` 三个人工工序均为 `5` 人。

## Pass 3 - 后端产品工艺资源大表 API

- task id: T3
- changed paths: `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/*`, `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/*`, `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/dv/machinery/MesDvMachineryProcessMapper.java`, `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteResourceServiceImplTest.java`
- implemented behavior: 新增 `/admin-api/mes/pro/route-resource/page` 与 `/admin-api/mes/pro/route-resource/save`。查询按产品、路线、工序、工位、设备和人员聚合成前端大表行；保存仍写入现有工位设备、设备工序产能或工位人员底层表，不新增替代表。
- validation commands: `mvn -pl yudao-module-mes -Dtest=MesProRouteResourceServiceImplTest test`
- validation results: PASS，4 个资源大表服务测试通过。
- covered acceptance ids: AC-02, AC-03, AC-04, AC-05
- known risks or blockers: 当前无阻塞。

BDD: 同设备同工序产能一致 -> Given A03388 绑定同一工序并出现在多个产品路线中 When 查询资源大表 Then 每条设备行展示同一设备工序产能。

BDD: 设备产能缺失不降级 -> Given 工位绑定了设备但设备工序产能没有配置 When 查询资源大表 Then 系统显示设备工序产能缺失，不使用设备主档产能替代。

BDD: 人工工序按人数预算 -> Given 工序无设备绑定且工位缺少人员绑定 When 保存人工人数 Then 系统写入现有工位人员表，刷新后按人数计算预算产能。

GREEN: `mvn -pl yudao-module-mes -Dtest=MesProRouteResourceServiceImplTest test` -> PASS。

GREEN: `mvn -pl yudao-module-mes '-Dtest=MesProRouteResourceServiceImplTest,MesMdWorkstationCapacityServiceTest,MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest' test` -> PASS，39 个相关回归测试通过。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS。

## Pass 4 - 前端资源大表页面

- task id: T4
- changed paths: `src/api/mes/pro/route/resource.ts`, `src/views/mes/pro/route/index.vue`, `src/views/mes/pro/route/RouteResourceTable.vue`, `vite.config.ts`, `build/vite/optimize.ts`
- implemented behavior: 在 MES 工艺路线页增加 `工艺路线/资源大表` 视图切换；资源大表支持按关键词和资源类型筛选，展示产品、路线、工序、资源类型、设备/人数、产能来源和预算产能，并支持行内保存。`jsbarcode` 纳入 Vite 预优化以确保现有动态路由加载成功。
- validation commands: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- validation results: PASS。
- covered acceptance ids: AC-06
- known risks or blockers: 当前无阻塞。

GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。

GREEN: `node --check tests\e2e\mes-route-resource-table-real-flow.e2e.js` -> PASS。

## Pass 5 - 真实 E2E 验证

- task id: T5
- changed paths: `tests/e2e/mes-route-resource-table-real-flow.e2e.js`, `doc/tasks/20260608-paichan-resource-table/test-report.md`
- implemented behavior: 使用 Playwright 打开 `http://127.0.0.1:8081`，登录 `测试租户/aoteman`，进入 MES 工艺路线页切换到资源大表，编辑并恢复 `穿显影环` 人工人数，最后通过已登录页面上下文读取大表 API 校验 `A03388` 同设备同工序产能一致。
- validation commands: `node tests\e2e\mes-route-resource-table-real-flow.e2e.js`
- validation results: PASS，输出 `PASS: MES route resource table real UI E2E`。
- covered acceptance ids: AC-07
- known risks or blockers: 当前无阻塞。

GREEN: `node tests\e2e\mes-route-resource-table-real-flow.e2e.js` -> PASS。

## Pass 6 - 收尾与提交

- task id: T6
- changed paths: `current-task-files`
- implemented behavior: 按收尾基线运行 task-closeout-cleanup 预览；清理前端仓库内临时 Vite 日志目录；保留正式任务文档、测试证据与数据脚本本地记录；分别提交前后端本任务改动。
- validation commands: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-paichan-resource-table --mode preview`
- validation results: BLOCKED。脚本识别当前后端为 linked worktree，但找不到 `master-jdk17` 的 checked-out main worktree；预览同时会默认删除 PRD、测试计划、测试报告和数据脚本，因此未执行 apply。
- covered acceptance ids: AC-01, AC-02, AC-03, AC-04, AC-05, AC-06, AC-07
- known risks or blockers: 后端 `runtime/runtime-control/runtime-ops/*.json` 为运行状态文件，未纳入提交。

GREEN: `git commit -m "任务: 增加MES产品工艺资源大表"` -> PASS，前端提交 `4ed68521b`。

GREEN: `TDD_TASK_DIR=...\doc\tasks\20260608-paichan-resource-table git commit -m "任务: 增加MES产品工艺资源大表接口"` -> PASS，后端提交已通过 TDD compliance。
