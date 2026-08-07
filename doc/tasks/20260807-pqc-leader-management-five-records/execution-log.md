# Execution Log

## User Intent

- 用户要求给 PQC 组长的 `PQC管理` 增加 5 条符合从一线 PQC 提交条件的数据。
- 本次范围限定为本机 `int_main` 测试环境，不操作远程测试服、生产服或备用服。

## BDD Scenarios

- BDD: 一线 PQC 提交进入组长管理 -> Given 本机测试租户存在可登录的 PQC 人员、目标 PQC 组长、发布 QA 规程和可提交活跃订单 / When PQC 人员通过真实前端完成 5 次正式检验提交 / Then PQC 组长在 `PQC管理` 中可看到 5 条对应数据，且每条的任务、事件、记录和结构化检验项目完整。
- BDD: 缺正式提交前置时停止 -> Given 缺测试账号、人员范围、发布 QA 规程、活跃订单、工序或正式 schema / When 尝试创建数据 / Then 停止写入并报告精确缺口，不创建孤立数据、mock 成功或前端假行。
- BDD: 任务标识防止重复写入 -> Given `CODX-PQC-20260807` 已存在任一正式提交记录 / When 再次执行本任务 / Then 在新增前停止，不重复创建 5 条数据。
- BDD: 并发写入不覆盖共享状态 -> Given 其它任务可能修改同一工序池或活跃订单 / When 选择本任务提交对象 / Then 必须使用无冲突对象或等待冲突解除，不覆盖其它任务的汇总和测试数据。

## Command And Evidence Log

- 2026-08-07: 已读取数据库、登录、PowerShell 编码、任务收尾规则，以及 `database-schema-delivery` 技能和数据库证据合同。
- 2026-08-07: 已读取 `docs/experience-index.md`，匹配 `docs/backend-development.md#MES-PQC-项目级检验快照门禁`。
- 2026-08-07: 已核对历史任务 `doc/tasks/20260806-pqc-management-list-test-data/`，确认 PQC 管理正式读模型依赖 `mes_pro_process_pool_event`、`mes_pro_process_pool_pqc_record`、`mes_pqc_inspection_task` 和结构化项目/逐件明细；历史对象只作证据，本轮必须重新核对真实运行库。
- 2026-08-07: Git 预检显示根仓库 `int_main` 已领先 `origin/int_main` 2 个提交，并存在其它任务改动；本任务不回滚、清理或覆盖并发任务文件。
- 2026-08-07: 运行态复核通过：前端 `8081`、后端 `48081`、MySQL `8.0.39`、Redis 均正常，后端 `/actuator/health` 返回 `UP`。
- 2026-08-07: 正式身份复核通过：一线 PQC 用户 `659/shangmengying` 在 PQC 组长 `512/huzonggang` 的启用 EMPLOYEE 范围内；生产组长 `1520` 的工单 `980008` 活跃订单为可恢复的 `12`。
- 2026-08-07: 正式业务链路复核通过：路线 `922119/V448`、路线工序 `928609/922985`、发布规程及版本 `16`、PATROL 结构化项目、计划检验数量 `15`、生产来源事件 `131` 的设备账号/设备/工作站上下文完整。
- 2026-08-07: 并发隔离选择工序池 `36`；当前并发的生产反馈任务使用工序池 `37`，本任务不覆盖其事件或汇总状态。

## RED / GREEN / REGRESSION

- RED: marker read-only SQL -> FAIL as expected，`mes_pro_process_pool_event.event_idempotency_key LIKE 'CODX-PQC-20260807%'` 命中 `0`，目标 5 条提交尚不存在。
- RED: formal pending-source query -> FAIL as expected，现有正式待检任务中只有 `task=163 / routeProcess=928611` 具有完整生产来源，不能提供 5 条任务自有提交；因此创建 5 个严格标识、独立轮次的待检 fixture，提交动作仍必须走真实页面。
- RED: 首轮 `run-e2e.ps1` -> FAIL before browser launch，任务目录脚本按自身位置解析不到前端已安装的 `playwright` 模块；账号恢复 `finally` 已执行，正式提交仍为 0。修正为通过前端 `package.json` 的 `createRequire` 加载同一已安装依赖，不安装或降级依赖。
- RED: 第二轮 `run-e2e.ps1` -> FAIL before login write，租户 Unicode 常量首字误写为“花”；临时账号已恢复且提交标识仍为 0。常量已修正为正式租户“芋道源码”。
- RED: 第三轮 `run-e2e.ps1` -> FAIL before active-order write，生产组长真实登录已通过，但脚本误捕获下拉框预加载的空 keyword 候选响应；账号已恢复、提交仍为 0。候选响应条件已收紧为精确业务订单号。
- RED: 第四轮 `run-e2e.ps1` -> FAIL after successful login API and before business write，`waitForURL` 等待 SPA 整页 `load` 超时；改为断言真实页面 `location.pathname` 离开 `/login`，不跳过认证或业务页面。
- RED: 第五轮 `run-e2e.ps1` -> FAIL before active-order write，精确 keyword 候选响应未匹配工单 `980008`；账号已恢复且提交仍为 0。下一轮记录非敏感候选业务体以确认正式不可加入原因，不绕过候选门禁。
- RED: 第六轮候选证据 -> BLOCKED，工单 `980008` 的正式不可加入原因是“QA规程发布版本缺少末检适用性配置”；不修改共享 QA 主数据，已精确回滚未提交任务 `190..194`，改用 ACTIVE 测试订单 `980019`。
- RED: 第七轮 `run-e2e.ps1` -> FAIL before production write，真实生产填写页已打开但提交按钮 60 秒内保持禁用；账号已恢复、正式事件仍为 0。下一轮采集非敏感页面运行态文本定位缺失前置。
- RED: 第八轮运行态证据 -> FAIL before production write，页面已选正式工序“粗洗工序”但员工显示“未选择”，无后端错误；修正为通过生产页面员工卡选择正式员工 `964/刘悦悦` 并校验 switch-employee 响应。
- RED: 第九轮 `run-e2e.ps1` -> FAIL before production write，员工选择器未渲染 `964/刘悦悦`；账号已恢复、正式事件仍为 0。下一轮通过同一真实登录会话只读核对 employee-candidates 正式响应，不直接写候选或扩权。
- RED: 第十轮 employee-candidates -> FAIL with business code `1040760102`，员工 `964` 作为设备账号未授权路线 `922119` / 工序 `922985`。不扩大员工权限，改用正式生产组长设备账号 `1520` 登录并在页面选择实际员工 `964`。
- RED: 第十一轮 employee-candidates -> FAIL with business code `1040760101`，设备账号 `1520` 没有启用的路线绑定。只读根因核对确认路线 `922119` 当前 V24 的工序 `980645` 缺 `workstation_id`，且活跃订单/规程/待检任务仍冻结在旧工序 `928609`。该共享根因已由 `20260807-frontline-route-process-workstation-binding-fix` 和 `20260807-frontline-pqc-latest-active-version` 处理，本任务不跨任务修改路线、规程或权限。
- RED: 方案切换前精确回滚 -> PASS；在正式事件标识命中 `0` 的前提下，仅删除本任务创建且仍为 PENDING/OPEN 的旧前置，复核任务自有 PQC task 和正式事件均为 `0`。
- GREEN-PRECONDITION: 已完成的数据任务 `20260807-production-leader-active-order-five-records` 保留活跃订单 `35..39` / 工单 `980022..980026`；路线 `980091/V622`、当前工序 `980631`、工作站 `980010`、员工账号 `964` 的岗位工作站绑定、发布规程 `36` 及 5 条 FINAL PENDING 任务 `198/202/206/210/214` 完整一致。本任务只新增后续生产来源和 PQC 提交，不改写该任务的已交付数据。
- RED: 新对象首轮 `run-e2e.ps1` -> FAIL before production write；同一真实会话的候选接口已返回员工 `964`，但 Playwright 未在未限定可见面板的全局弹层定位器中命中员工选项。正式事件仍为 `0`，临时账号已恢复；收紧为当前可见生产面板内定位后重试。
- RED: 后续两轮登录运行态 -> FAIL before business write；一轮 90 秒内未捕获登录响应，一轮登录 HTTP/业务响应成功但 SPA 未在 60 秒内自动离开 `/login`。前后端健康均为正常，正式事件 `0`，账号均已恢复。登录门禁改为等待真实登录页写入 `ACCESS_TOKEN`，业务页仍由后续 `page.goto` 真实路由进入，不直接调用登录 API 或写入伪造 token。
- RED: 收紧面板定位后 -> FAIL before production write；弹层真实打开但页面运行时员工选项为空。根据后端正式规则和版本快照确认：路线 `980091/V622` 的 `routeStartProductionLeaders` 只授权 `admin/id=1`；员工 `964` 的精确工序授权不等于路线开始页面账号。改用快照指定的 `admin` 设备账号和其启用生产员工 `1681/陈丽`；不修改路线快照或扩大权限。
- RED: `admin` 路线开始账号 -> FAIL before production write；该账号同时被旧路线 `922119` 授权，正式工序列表在筛选前对全部授权路线执行工作站门禁，因旧路线 V24 缺绑定返回 `1040760104`。不触碰并发修复任务的共享路线数据；改用已有岗位 14 的设备账号 `151/pengyunfeng`，该账号仅通过岗位/工作站获取正式路线 `980091`。为其创建 1 个任务自有、无系统登录账号的 FORMAL 生产员工档案，不扩权现有员工。
- RED: 岗位账号 `151/pengyunfeng` -> FAIL before production write；路线隔离正确，但该账号缺 `mes:pro-feedback:query/create` 正式接口权限，响应业务码 `403`。不扩大该账号角色；改用同样具备岗位 14 且已有正式接口权限的 PQC 账号 `659/shangmengying`。旧路线快照只授权用户 `1`，因此 `659` 的岗位路线集只包含具有正式工作站的路线 `980091`。
- RED: 任务自有无账号生产员工 -> FAIL before production write；当前运行后端的正式候选模型返回岗位用户集合，不返回无系统账号档案。候选响应已包含 `659/商孟莹`，因此删除未使用的任务员工档案，直接使用同一正式候选账号作为生产实际员工和随后 PQC 检验员。
- RED: 工作站正式绑定修复运行包启动后的真实生产页 -> FAIL before production write；只读 `employee-candidates` 返回 `659/商孟莹`，但页面正式数据源是 `runtime-config.employees`，登录账号 `659` 的责任组没有员工档案，员工弹层为空。提交标识仍为 `0`。补充 1 个归属 `659`、无系统登录账号的任务自有 FORMAL 生产员工档案；该档案只作为生产来源实际员工，PQC 检验人仍锁定为登录账号 `659`，不新增权限或正式事件。
- RED: 无账号 FORMAL 档案已在生产页运行态中可见并可选，但提交授权门禁要求实际员工同时命中该工序的正式候选用户，`actualEmployeeId=980033` 被拒绝；事务未留下正式事件。将任务档案的 `system_user_id` 精确绑定为同一测试账号 `659`，使页面运行态、工序候选和提交身份一致，不新增角色或权限。
- RED: 账号绑定后的真实页面执行 -> 前两条生产来源提交成功并生成 `PRODUCTION_SUBMIT` 事件 `166/167`；第三条在页面自动选人完成并关闭弹层时，脚本仍等待弹层选项而停止，尚无 PQC 提交。保留已由真实页面生成的两条事件，按精确工单和幂等键只读恢复其事件 ID；剩余三条继续走页面提交，并在打开弹层前等待页面自动选人，禁止重复写入。
- GREEN-PRECONDITION: 修正页面自动选人竞态后，工单 `980022..980026` 的 5 条生产来源全部由真实生产页生成，事件为 `166..170`。
- RED: 首条 PQC 页面加载 -> FAIL before PQC write，业务码 `1040506106` 明确指出活跃订单 `35` 的第二个当前路线工序 `980632/922986` 缺少已发布 QA 规程。现有 5 个活跃订单只为首个工序配置规程，但正式入口逐一校验路线全部 14 个工序，故不能作为合格的一线 PQC 提交前置。
- PIVOT: 不给 13 个无关工序批量补造规程和待检任务；建立任务自有的单工序正式路线、ACTIVE 版本、产品绑定、发布 QA 规程和 5 个独立订单/活跃订单/FINAL 任务。正式生产和 PQC 事件仍全部由真实页面创建；既有事件 `166..170` 保留为可追踪的已发生生产数据。
- RED: 单工序路线首条生产提交 -> FAIL before write，电子签名号 `99009300` 已被前一组真实事件占用；新前缀正式事件仍为 `0`。切换为只读确认未占用的独立签名号 `99009400..99009404`，不复用已有签名身份。
- GREEN-PRECONDITION: 单工序路线 5 条生产来源均已通过真实生产页创建，事件 `171..175`，工单 `980028..980032`，实际员工 `659`。
- RED: 单工序首条 PQC 结构化填写 -> FAIL before PQC write；QA 项目正式配置为 `equipmentRequired=false`，页面仍保留空的选填设备下拉，脚本误等待非空选项。改为读取页面“无需指定设备”状态，仅在正式标记为设备必填时选择设备。
- RED: 跳过选填设备后的首条 PQC 提交 -> FAIL transactionally，业务门禁明确要求 `itemResults.CODX-PQC-20260807-SP-FINAL.selectedEquipmentId`。任务规程按项目级 PQC 快照门禁改为设备必填，并绑定正式设备 `41/A03190/球囊成型机` 及设备编号 `A03190`；PQC 事件仍为 `0`。
- GREEN: `formal-equipment-amendment.sql` -> PASS；任务规程项目 `166` 已设为设备必填，并精确绑定正式设备 `41/A03190/球囊成型机` 和设备编号 `A03190`，执行前 PQC 事件仍为 `0`。
- GREEN: `run-e2e.ps1` -> PASS；真实一线 PQC 页面提交形成任务 `223..227` 对应事件 `181..185`，真实 PQC 组长页面切换到 `PQC管理` 后 5 个目标工单全部可见，只读分页核验命中 5 条。
- GREEN: 结构化结果 -> PASS；任务 `223..227` 均为 `SUBMITTED`、实际/计划数量 `3/3`，事件实际检验人 `659`，PQC 记录 `104..108` 均为 `SUCCESS`，每个任务 3 条逐件明细，共 15 条；项目方法、标准、设备和设备编号完整。
- REGRESSION: 断点续验 -> PASS；脚本按正式事件类型 `PQC_INSPECTION` 恢复已提交记录，不重复创建生产或 PQC 事件；PQC 事件固定为 5 条。
- REGRESSION: 凭据恢复 -> PASS；每轮 E2E 的 `finally` 均恢复账号 `512/659` 原密码与更新字段，最终 `CODX-PQC-20260807-CREDENTIAL` 标记命中 `0`。
- REGRESSION: 页面证据 -> PASS；截图 `output/playwright/20260807-pqc-leader-management-five-records.png` 显示提交日期 `2026-08-07` 和 5 条 `CODX-PQC-20260807-SP-WO-*` 目标记录。
- REGRESSION: `verify.sql` -> PASS；汇总 `task/submitted/quantity/event/source/record/task-with-details/marker = 5/5/5/5/5/5/5/5`，逐件明细 `15`，凭据标记 `0`。
- REGRESSION: database schema evidence validator -> PASS，数据库证据结构完整。
- EXPERIENCE: 已将“PQC 真实提交前置覆盖活跃路线全部当前工序”合并到 `docs/backend-development.md`，将“Element Plus 页签按 role=tab 点击并断言 aria-selected”合并到 `docs/e2e-rules.md`，并更新既有 `docs/experience-index.md`；未新建长期经验文档。
- CLOSEOUT: `task-closeout-cleanup --mode preview` -> PASS，无 blocked/warnings，删除范围仅为本任务附属文件和临时截图。
- CLOSEOUT: `task-closeout-cleanup --mode apply` -> PASS；保留 `task.md`、`execution-log.md`、`verification-report.md`，已删除一次性 SQL、E2E 脚本、结果 JSON、数据库中间证据和临时截图，不删除正式业务数据。

## Data Safety

- 任务标识：`CODX-PQC-20260807`。
- 写入范围：仅本机测试租户中通过真实一线 PQC 页面产生的 5 条正式提交及系统自动形成的关联数据。
- 禁止范围：远程环境、无关租户、无关业务记录、权限扩大、mock、API-only 写入、直接 SQL 伪造正式提交。
- 并发策略：写入前检查目标工序池/活跃订单是否被其它任务占用；发现共享目标冲突时停止，不强停其它任务。
- 回滚口径：按本任务提交产生的 5 个 PQC task/event/record 主键和 `CODX-PQC-20260807` 标识精确清理关联明细；本任务目标为保留数据，验证通过后不主动回滚。
- Fixture 边界：SQL 仅新增 5 个 `PENDING` 检验任务，不直接写 PQC 事件、PQC 记录或逐件明细；这些正式提交实体必须由一线 PQC 页面事务生成。
