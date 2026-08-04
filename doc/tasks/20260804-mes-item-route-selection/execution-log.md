# Execution Log

## User Intent

- 用户要求按已确认方案继续设计、开发、验证：工艺路线可以选择产品，产品也可以选择工艺路线。
- 设计决策：产品侧入口放在 MES 物料产品维护，而不是 MDM 产品主数据；复用 `mes_pro_route_product.route_id + item_id` 关系。

## Baseline And Workspace State

- `git log --oneline -- doc/tasks/20260804-mes-item-route-selection/*` 显示任务初始文档进入并发基线提交：`0cb7335da chore: baseline residual before dcc approval detail tab removal`。
- 当前工作区存在大量无关并发改动；本任务只拥有 MES 产品侧工艺路线绑定相关后端、前端、测试和 `doc/tasks/20260804-mes-item-route-selection/` 文件。
- `git status --short --branch` 于验证后显示 `int_main...origin/int_main [ahead 16]`，并包含 DCC、PQC、排产、多维筛选等无关未暂存文件；提交必须显式选择本任务路径。

## BDD Scenarios

- BDD: 产品侧查看当前工艺路线 -> Given MES 物料产品已通过 `mes_pro_route_product` 绑定一条工艺路线 / When 用户打开该物料产品编辑表单 / Then 表单展示当前工艺路线，数据来源是 route-product 绑定关系。
- BDD: 产品侧选择工艺路线 -> Given MES 物料产品未绑定工艺路线且存在未启用工艺路线 / When 用户在物料产品表单选择工艺路线并保存 / Then 系统创建 `mes_pro_route_product` 绑定，路线侧关联产品列表同步可见该产品。
- BDD: 产品侧更换工艺路线 -> Given MES 物料产品已绑定工艺路线 A / When 用户改选工艺路线 B 并保存 / Then 系统更新同一产品的 route-product 绑定为路线 B，不新增第二条关系。
- BDD: 产品侧解除工艺路线 -> Given MES 物料产品已绑定工艺路线 / When 用户清空工艺路线并保存 / Then 系统删除该产品的 route-product 绑定，产品侧和路线侧均不再显示关联。
- BDD: 单产品唯一路线约束 -> Given MES 物料产品已绑定路线 A / When 另一请求尝试再绑定路线 B / Then 后端 fail fast 返回现有唯一性错误，不静默覆盖、不创建重复绑定。
- BDD: 已启用路线只回显不可维护 -> Given MES 物料产品当前绑定的工艺路线已启用 / When 用户打开产品侧工艺路线页签 / Then 页面回显当前路线但禁用变更入口，后端仍禁止修改已启用路线。

## RED/GREEN Evidence

- `RED: node tests\e2e\mes-md-item-route-selection-static.spec.js -> FAIL, expected reason: MES 物料产品表单必须提供独立 MdItemRouteForm 维护产品侧工艺路线绑定。`
- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> INTERRUPTED, expected reason not confirmed because command stayed in long MES javac before target surefire report was produced.`
- `GREEN: node tests\e2e\mes-md-item-route-selection-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`
- `GREEN: mvn -pl yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test -> PASS, Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`
- `GREEN: git diff --check -- <task-owned files> -> PASS, only LF will be replaced by CRLF warnings.`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-mes-item-route-selection/frontend-feature-evidence.md -> PASS`
- `GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260804-mes-item-route-selection/backend-api-evidence.md -> PASS`

## Command And Verification Notes

- 2026-08-04：读取 `backend-api-delivery`、`frontend-feature-delivery`、`behavior-driven-development` 技能和前后端契约说明。
- 2026-08-04：读取任务收尾、后端开发、前端开发、PowerShell 编码、PowerShell/Git 经验和项目经验索引；适用门禁已摘入 `task.md`。
- 2026-08-04：实现 `MesProRouteProductService.saveRouteProductByItem`、`/mes/pro/route-product/get-by-item`、`/mes/pro/route-product/save-by-item`，产品侧保存复用 `mes_pro_route_product` 正式关系。
- 2026-08-04：实现 `MdItemRouteForm.vue` 与 `MdItemForm.vue` 产品专属“工艺路线”页签，前端调用 route-product API，不向 `MdItemApi` 增加第二套路线字段。
- 2026-08-04：发现 `ProRouteApi.getRouteSimpleList()` 只返回已启用路线，但后端 route-product 保存要求路线未启用；已新增 `/mes/pro/route/item-binding-list` 产品维护权限下的选择接口，并在前端禁用已启用路线选项。
- 2026-08-04：Maven 首轮和第二轮验证会话未产生 MES 目标 surefire 报告；随后观察到同目标 `-DforkCount=0` Maven 进程并等待收敛，最终 surefire 明确 PASS。
- 2026-08-04：项目经验已沉淀到 `docs/backend-development.md#MES 工艺路线产品绑定状态门禁`，并在 `docs/experience-index.md` 增加关键词路由。
- 2026-08-04：`task-closeout-cleanup` preview/apply 通过；删除临时 `backend-api-evidence.md` 与 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- 2026-08-04：并发基线提交 `af1bfb191 chore: baseline current frontend backend updates before push` 已包含本任务实现、验证报告和临时 evidence；后续收尾提交 `3f15f0539 docs: close out mes item route selection` 删除临时 evidence 并提交经验沉淀。
- 2026-08-04：首次 `git push origin int_main` 因全局 GitHub 代理 `http://127.0.0.1:7890` 未监听失败；`Test-NetConnection github.com -Port 443` 直连成功，`git -c http.https://github.com.proxy= ls-remote origin HEAD` 成功。
- 2026-08-04：使用一次性代理覆盖执行 `git -c http.https://github.com.proxy= push origin int_main` 成功，远端 `int_main` 更新 `1fd52f05d..3f15f0539`。

## Blockers

- 当前无实现阻塞。
