# 流转关系图进入自动布局与自动生成按钮调整

## 任务目标

- 进入工艺路线“流转关系图”页签时，默认执行一次自动布局，等同用户点击一次“自动布局”。
- 删除顶部工具栏“添加连接线”按钮，保留画布拖拽连线和关系清单删除能力。
- 将“根据序号生成线性关系”按钮文案改为“自动生成”，仍生成线性关系草稿并等待顶部保存写入。

## 里程碑

- [x] M1 创建任务记录，读取 PowerShell、经验索引、前端样式与前端交付门禁。
- [x] M2 补 RED 静态回归，约束进入自动布局、按钮删除与文案变更。
- [x] M3 最小修改流转关系图组件。
- [x] M4 运行目标静态回归、ESLint、证据校验和必要类型检查。
- [x] M5 记录收尾状态并按混合工作区提交边界处理。

## 预期验证

- `node tests/e2e/mes-route-flow-auto-layout-auto-generate-static.spec.js`
- `node tests/e2e/mes-route-flow-graph-static.spec.js`
- `node tests/e2e/mes-route-flow-graph-one-screen-static.spec.js`
- `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/RouteFlowGraphDesigner.vue tests/e2e/mes-route-flow-auto-layout-auto-generate-static.spec.js tests/e2e/mes-route-flow-graph-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-flow-auto-layout-auto-generate/frontend-feature-evidence.md`

## BDD 场景

- BDD: 进入流转关系图默认自动布局 -> Given 用户打开已有工艺路线并进入“流转关系图”页签 / When 流转关系图数据加载完成 / Then 页面自动执行一次布局并适配画布。
- BDD: 工具栏不再显示添加连接线按钮 -> Given 用户查看流转关系图顶部工具栏 / When 页面渲染完成 / Then 不再出现“添加连接线”按钮，仍可通过画布节点手柄拖拽建立连接。
- BDD: 线性关系生成入口改名 -> Given 用户查看流转关系图顶部工具栏 / When 路线至少包含两个工序 / Then 原“根据序号生成线性关系”按钮显示为“自动生成”，点击后仍生成线性关系草稿。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次只调整工具栏与进入行为，不做无关视觉重设计。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；保留现有 API、保存契约和画布拖拽连线能力，不引入 mock 或 fallback。
- 高风险动作：本轮不登录、不写入真实业务数据、不操作测试服/正式服，因此不触发登录和服务器写入门禁。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。将进入页签的自动布局放在图数据加载完成且 loading 结束后执行，避免 pending 布局在加载中被跳过。
- 是否存在临时补丁或绕过：否。

## 2026-07-09 追加需求：全入口自动布局

- BDD: 各入口进入流转关系图都执行一次自动布局 -> Given 用户从编辑页默认进入、URL tab 参数进入、弹框切换页签进入或已加载表单切回“流转关系图” / When 流转关系图组件和图数据可用 / Then 统一请求一次 `autoLayoutOnEntry()`，并复用“自动布局”按钮逻辑完成布局。
- BDD: 延迟挂载不漏触发 -> Given 工艺路线数据加载后才渲染流转关系图组件 / When 表单 loading 状态结束并进入 flow 页签 / Then 自动布局请求等待组件 ref 稳定后执行，若用户已切走页签则不触发旧请求。
- 本轮继续沿用既有任务目录，补充全入口自动布局的 RED/GREEN 证据。

## 2026-07-09 缺陷回归：编辑入口未自动布局

- BDD: 工艺流程列表编辑入口默认自动布局 -> Given 用户在工艺流程/工艺路线列表点击“编辑”进入默认流转关系图 / When 表单数据先加载、流转关系图组件随后挂载 / Then 父表单保留一次待执行自动布局请求，直到图组件 ref 稳定后调用 `autoLayoutOnEntry()`，不得因 ref 暂未挂载而静默丢弃。
- 根因假设：父表单当前通过可选链直接调用 `routeFlowGraphDesignerRef.value?.autoLayoutOnEntry()`；当编辑入口首屏存在表单数据加载与页签内容挂载时序差时，该调用可能早于图组件 ref 稳定并被静默跳过。
- 设计约束：不引入 fallback、不降级、不吞异常；以父表单 pending 请求 + post flush watcher 方式保留正式入口行为，继续复用子组件自动布局逻辑。

## 当前状态

COMPLETED：已修复“通过工艺路线的编辑进入流转关系图后不是默认自动布局”的二次回归。入口自动布局现在会等待 Vue Flow 节点渲染帧完成后再适配画布，真实页面复验中再次点击“自动布局”不再移动节点。

## Current Status

completed

## 2026-07-09 二次缺陷回归：编辑入口自动布局未完成适配

- BDD: 编辑入口进入后布局状态等同手动自动布局 -> Given 用户在工艺路线列表点击“编辑”进入默认流转关系图 / When 图数据加载完成且入口自动布局执行 / Then 再点击“自动布局”不应再次移动节点或改变缩放适配。
- 真实 RED：`tests/output/route-flow-entry-auto-layout-real.cjs` 打开 `http://localhost:8081/mes/pro/route`，点击第一条路线编辑进入 `/mes/pro/route/edit/922111` 后，手动点击“自动布局”仍移动 14/14 个节点。
- 根因修正方向：入口自动布局不能只在下一次 Vue tick 中调用 `fitView`；必须等待 Vue Flow 节点渲染帧完成后再执行画布适配，并让 pending 入口布局等待该完整路径完成。
- 设计约束：不引入 fallback、不降级、不吞异常；继续复用“自动布局”按钮的同一布局逻辑，只修正布局后视口适配时序。

## 最终验证结果

- `node tests/e2e/mes-route-flow-auto-layout-auto-generate-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-entry-auto-layout-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-graph-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-graph-one-screen-static.spec.js` -> PASS。
- `node node_modules\eslint\bin\eslint.js src\views\mes\pro\route\RouteFormContent.vue src\views\mes\pro\route\RouteFlowGraphDesigner.vue tests\e2e\mes-route-flow-entry-auto-layout-static.spec.js tests\e2e\mes-route-flow-auto-layout-auto-generate-static.spec.js` -> PASS。
- `tests/output/route-flow-entry-auto-layout-real.cjs` -> PASS，真实页面从工艺路线列表点击编辑进入 `/mes/pro/route/edit/922111` 后，再点击“自动布局”移动节点数为 0/14。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-flow-auto-layout-auto-generate/frontend-feature-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260709-route-flow-auto-layout-auto-generate/bug-regression-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-flow-auto-layout-auto-generate --mode preview` -> PASS，未发现 blocked。

## 提交状态

- BLOCKER：`yudao-ui-admin-vue3` 子仓存在多项既有脏改，且 `RouteFlowGraphDesigner.vue`、`mes-route-flow-graph-static.spec.js` 同时包含本轮与前置任务改动；为避免混入非本轮改动，本轮未提交。
- BLOCKER：追加需求同样受前端子仓多任务混合脏改影响，且当前行为依赖尚未提交的 `RouteFlowGraphDesigner.vue` 前置改动；为避免提交不完整依赖链，本轮未强行提交。
- BLOCKER：二次回归修复同样落在已有多任务混合脏改文件上，最终仍不创建提交，避免把其他任务 hunk 混入本次提交。
