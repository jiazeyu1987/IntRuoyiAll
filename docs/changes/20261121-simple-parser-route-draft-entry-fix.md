# 生产编制草稿进入流程编辑及正式发布验证

## 范围与实际结果

- 测试服务：`http://172.30.30.58:8081`，租户“测试A”，业务操作账号 `panxin`。
- 按用户要求未部署新包，服务器页面版本仍为 `2026-11-20-23-36-11`。
- 前端修复通过本机 `http://127.0.0.1:18191` 连接真实测试后端验证。所有配置与发布动作均由 Playwright 操作真实前端完成；未直接调用 API 或写数据库执行业务动作。
- 未重新导入、重新解析或创建重复流程；发布的是用户指定的五条现有 V2。

| 流程码 | 流程 ID | 已发布 V2 ID | 工序数 | 最终版本状态 | 待发布版本 |
| --- | ---: | ---: | ---: | --- | --- |
| IDI-GYLC-0001 | 922029 | 14 | 15 | ACTIVE，active=true | 无 |
| ID-GYLC-0001 | 922030 | 16 | 14 | ACTIVE，active=true | 无 |
| IDE-GYLC-0001 | 922031 | 18 | 15 | ACTIVE，active=true | 无 |
| IDML-GYLC-0001 | 922032 | 20 | 15 | ACTIVE，active=true | 无 |
| IDPR-GYLC-0001 | 922033 | 22 | 15 | ACTIVE，active=true | 无 |

五条生产组长均按用户指定配置为张飞红（`zhangfeihong`，用户 ID 910915）。最终列表接口只读核验：五条 `activeRouteVersionNo=V2`，`pendingRouteVersionCount=0`，`scheduleRouteEnabled=true`、`batchRouteEnabled=true`。流程主数据仍为 `status=1`（停用），关联产品数量均为 0；版本发布不等同于流程主数据启用或产品绑定完成，本次未另行启用主数据或绑定产品。

## 调用链与原因区分

1. **创建草稿后“配置并发布”入口**：`SimpleParserPage.vue` 的 `openRoute()` 已传候选版本 ID、版本号和 DRAFT 状态，但缺少 `routeEditMode=edit`，导致未明确进入现有候选版本编辑模式。修复为仅在存在候选版本时传入该参数；生效版本仍按原浏览模式进入。
2. **编辑页递归溢出**：`tagsView.ts` 的 `replaceActiveMenuView()` 对完整路由及旧页签执行 `cloneDeep`，遍历 `matched` 中的组件实例、计算属性和 Pinia 运行时对象。现场堆栈包含 Pinia getter、Vue computed 与深拷贝递归。修复为先用已有 `getRawRoute()` 提取路由元数据，再深拷贝；保留查询参数和页签恢复行为。
3. **返回列表缺失**：编辑详情路由已在 `src/router/modules/remaining.ts` 注册，`activeMenu=/mes/pro/route` 正确；列表为动态菜单页面。`panxin` 的工艺工程师角色缺少流程目录、列表菜单和版本权限，返回目标无法注册。属于账号菜单/权限配置问题，未改返回地址或新增路由。
4. **复用工艺查询失败**：`MesProCraftController` 的查询允许 `mes:pro-craft:query` 或 `mes:pro-route:version-query`。补齐版本查询后可通过；该 403 与路由深拷贝递归是独立问题。
5. **正式发布策略缺失**：`submit-publish` 进入 `MesProRouteVersionBusinessApprovalSubmitServiceImpl`，需要 `MES/MES/ROUTE_VERSION/PUBLISH/DRAFT` 的已发布策略。测试A原有两条策略分别面向树形工艺统一变更和工艺路线批量变更，不匹配单条版本；首次提交返回 1040271057，事务未发布版本。
6. **生产组长前置校验**：用户授权关闭审批后，提交被 `MesProRouteVersionSnapshotValidator.requireProductionLeaderAssignments()` 拦截，提示未配置有效生产组长。按用户随后指定的 `zhangfeihong` 保存到原 V2 后，发布成功。

## 实际权限和策略操作

- 通过测试A管理员页面为工艺工程师角色（991358，`process_engineer`）补齐：工艺路线目录、工艺流程菜单、路线查询/更新、排产配置查询、批记录配置查询、版本查询/编辑/提交。
- 配置生产组长时，管理员账号被对象级 `ROUTE:922033:ROUTE_EDIT` 校验拒绝，未绕过对象权限。改用已有项目对象权限的 `panxin`，补齐批记录配置更新以及现有快捷配置入口要求的版本创建权限；实际操作始终复用原候选版本，未创建新版本。
- 未授予路线创建/删除、版本独立发布/撤回等额外权限。角色原有授权保留。
- 用户明确授权测试A关闭审批并直接发布五条版本后，经业务审批策略页面新建、发布 `MES/MES/ROUTE_VERSION/PUBLISH/DRAFT`、执行器 `MES_ROUTE_VERSION_PUBLISH` 的临时“关闭审批”策略。
- 五条版本依次由 `panxin` 点击“发布”，在工序一致性弹窗选择“保留差异并提交”。单条发布响应均为 code=0、原候选 ID、ACTIVE、active=true，并记录 submittedBy/publishedBy=910351。
- 五条完成后已通过页面停用该临时策略，页面核验为“已停用”；原有两条策略未改动。后续单条版本发布需要重新配置并发布显式策略。

## 验证与限制

- 生产旧包曾复现递归溢出、查询 403、返回列表缺失。
- 本地修复前端接真实测试后端：五条原候选版本均成功加载完整工序图，点击“返回”进入列表，无递归异常；IDPR 原候选版本保存成功。
- 真实页面完成五条生产组长配置和正式发布，最终列表五行均显示当前生效版本 V2、待发布版本“无”。
- `node --test tests/unit/parser-route-candidate-entry.test.cjs tests/unit/mes-route-edit-form-remount.test.cjs tests/unit/route-edit-unsaved-exit.test.cjs`：15 项通过。
- `mes-route-version-list-draft-visible-static.spec.js`：通过。
- `python scripts/preflight/check-menu-migrations.py`：通过。未新增权限标识、页面或修改 router 配置。
- 本次源码 `git diff --check`：通过。
- 两项旧静态检查失败：`mes-route-flow-link-return-no-reload-static.spec.js`、`process-route-edit-dcc-lazy-load-static.spec.cjs`。已对照 HEAD，分别为原有断言与 `AppView` 当前实现不一致、期待的 `loadDccProjectCodeOptions` 在 HEAD 中已不存在；不属于本次改动，未改测试基线。
- 新包未部署，因此测试服旧前端中的深拷贝和入口参数缺陷仍需后续按授权发布修复包；不能将本机修复验证视为服务器代码已更新。

## 本次源码资产

- `IntRuoyiFronted/src/store/modules/tagsView.ts`
- `IntRuoyiFronted/src/views/form-center/parser/SimpleParserPage.vue`
- `IntRuoyiFronted/tests/unit/parser-route-candidate-entry.test.cjs`

未提交或推送 Git；未触碰并行任务改动。
