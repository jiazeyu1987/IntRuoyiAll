# Verification Report

## Scope

- MES 物料产品维护页新增产品专属“工艺路线”页签。
- 产品侧查询、选择、变更和解除工艺路线绑定复用 `mes_pro_route_product`。
- 后端按 `itemId` 提供查询/保存接口，并保持单产品唯一路线绑定语义。

## Results

- `node tests/e2e/mes-md-item-route-selection-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。
- `mvn -pl yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test`：PASS，10 tests，0 failures，0 errors。
- `git diff --check -- <task-owned files>`：PASS，仅有 LF/CRLF 工作区换行提示。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-mes-item-route-selection/frontend-feature-evidence.md`：PASS。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260804-mes-item-route-selection/backend-api-evidence.md`：PASS。
- `node --check doc/tasks/20260804-mes-item-route-selection/mes-md-item-route-selection-readonly-real.e2e.cjs`：PASS。
- `node doc/tasks/20260804-mes-item-route-selection/mes-md-item-route-selection-readonly-real.e2e.cjs`：PASS，真实页面使用 `http://127.0.0.1:8081` 与 `http://127.0.0.1:48081`，身份标签为 `芋道源码/admin`。
- 真实页面证据：打开 `/mes/md/item`，搜索并编辑产品 `A002.09.002.230396`，进入“工艺路线”页签；`item-binding-list` 与 `get-by-item` 业务码均为 `0`，路线选项 4 条，已启用路线以禁用选项显示。
- E2E 网络门禁：`mesWriteRequests=[]`、`simpleListRequests=[]`、`targetNetworkFailures=[]`、`targetHttpErrors=[]`、`pageErrors=[]`、控制台错误数为 `0`。
- Artifact：`output/playwright/20260804-mes-item-route-selection/mes-md-item-route-selection-readonly-real-result.json` 与同目录截图。

## Notes

- 前端未在 `MdItemApi` 增加 `routeId` 或第二套路线关系字段。
- 产品侧解除路线传递 `routeId=null`，由后端删除正式 route-product 绑定和旧路线产品 BOM 关联。
- 产品侧路线下拉使用 `/mes/pro/route/item-binding-list`，避免 `simple-list` 只返回已启用路线而与后端“已启用路线不可维护”校验冲突。
- 已启用路线可作为当前绑定回显，但前端禁用变更入口；后端继续 fail fast，不引入降级。
- 未引入 fallback、默认成功或表单槽位/批记录链路替代关系源。
- 本轮 E2E 按只读范围执行，未点击“保存工艺路线”；保存、变更和解除写链路仍由既有静态契约与后端 JUnit 覆盖。若要执行真实写入 E2E，需要使用已确认的测试租户/账号并创建可追踪、可清理的任务数据，不能在 admin 基线数据上直接写入。

## Closeout

- `task-closeout-cleanup` preview/apply：PASS，删除临时 evidence，保留核心任务记录。
- 经验沉淀：PASS，新增 `docs/backend-development.md#MES 工艺路线产品绑定状态门禁` 并更新 `docs/experience-index.md`。
- 收尾提交：`3f15f0539 docs: close out mes item route selection`。
- 推送：`git -c http.https://github.com.proxy= push origin int_main` PASS，远端更新 `1fd52f05d..3f15f0539`。
- Blocked: 最终完成状态提交 `6107745f0` 本地已生成，本轮 E2E 脚本与文档更新也尚未推送；GitHub 443 连接失败仍阻塞任务完成。
