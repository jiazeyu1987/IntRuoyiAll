# DCC 截图需求实现执行日志（前端）

BDD: 前端按后端契约实现截图需求 -> Given 用户授权按合理默认规则实现 / When 后端契约逐步落地 / Then 前端按现有 DCC/BPM/System 页面增量对接，不独立重建页面体系。

GREEN: 前端实现 worktree 创建 -> PASS，分支 `task/20260525-dcc-screenshot-implementation` 已从文档分支创建。

GREEN: 前端风格基线读取 -> PASS，已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。

## T2 - 上传、列表和下载入口

BDD: 上传受控文件补充截图元数据 -> Given 申请人进入现有 DCC 上传页 / When 填写产品编号、是否培训并为图纸源文件补 PDF / Then 提交 payload 必须携带后端新增字段且客户端先拦截缺 PDF 或非法产品编号。

BDD: 用户下载受控文件前确认 -> Given 用户在详情、我的文件或目录浏览点击下载 / When 用户确认下载提示 / Then 前端调用下载接口时必须携带 `nonControlledWarningConfirmed=true`。

BDD: 现行版本存在待审批新版本 -> Given 文件当前为现行版且后端返回 `modifying=true` / When 用户查看列表或详情 / Then 页面必须显示“修改中”标识，不能掩盖现行版状态。

RED: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> FAIL，预期失败：缺少截图元数据字段、产品编号和图纸 PDF 校验、下载确认参数、“修改中”标识。

GREEN: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS，4 tests passed。

REGRESSION: `node scripts/dcc-controlled-file-download-auth.test.mjs` -> PASS，3 tests passed。

REGRESSION: `node scripts/dcc-controlled-browser-version-selector.test.mjs` -> PASS，3 tests passed。

REGRESSION: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` -> PASS。验证时临时复用主仓库 `node_modules` 联接并复制已生成的 `src/types/auto-imports.d.ts`；验证后已删除临时文件。

## M6 - E2E review 修复

BDD: 下载确认必须提示非受控 -> Given 用户在真实 DCC 详情或受控浏览点击下载 / When 弹出下载确认框 / Then 文案必须明确 `下载后的文件为非受控文件`，并继续携带 `nonControlledWarningConfirmed=true` 写下载留痕。

RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_upload_download_e2e.py -q` in `ruoyi-vue-pro` -> FAIL，真实前端 8089 下载确认框仅提示 `确认下载该受控文件？系统将记录本次下载留痕。`，缺少 `非受控` 提醒。

GREEN: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS，4 tests passed；前端下载确认契约已要求 `下载后的文件为非受控文件` 且继续保留 `nonControlledWarningConfirmed=true`。

BDD: 申请人处理回退 -> Given DCC 文件被审批人回退且当前用户是申请人 / When 申请人直接从我的文件进入详情页 / Then 详情页必须复用现有 DCC 审批签名接口显示“处理回退”入口，不能要求用户必须从 BPM 待办携带 `taskId` 进入。

RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_workflow_actions_e2e.py -q` in `ruoyi-vue-pro` -> FAIL，E2E-06/08/09/10 通过，但 E2E-07 申请人详情页只显示 `有流程回退，需处理`，未显示继续重提入口。

GREEN: `node scripts/dcc-screenshot-t4-frontend.test.mjs` -> PASS，4 tests passed；详情页复用现有 DCC 审批签名面板，能够在无 `taskId` 的直接详情路径中从 BPM 任务列表选中当前用户待办并显示“处理回退”。

BDD: E2E-14 进入个人中心重置密码页签 -> Given 用户已登录并访问 `/user/profile` / When 后端菜单也返回 `/user/profile` 重复隐藏子路由 / Then 前端必须保留本地真实 Profile 页面组件并显示 `resetPwd` 重置密码页签，不能只显示系统壳。

RED: `node scripts/dcc-screenshot-t4-frontend.test.mjs` -> FAIL，新增断言失败：隐藏壳路由合并未保留静态 Profile 子路由组件，后端重复 `/user/profile` 子路由可覆盖真实个人中心页面。

GREEN: `node scripts/dcc-screenshot-t4-frontend.test.mjs` -> PASS，5 tests passed；`/user/profile` 静态路由继续指向 `src/views/Profile/Index.vue`，Profile 页面保留 `resetPwd` 页签，隐藏壳路由合并时重复后端子路由会合并元数据但保留静态 Profile 组件。

REGRESSION: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` -> PASS。

Bug: E2E-14 访问 `http://127.0.0.1:8089/user/profile` 后只显示系统壳，无法找到 `修改密码|密码设置|重置密码` 页签。
Expected: 登录用户进入 `/user/profile` 时加载真实个人中心页面，并可打开 `resetPwd` 重置密码页签。
Reproduction: `script/tests/test_dcc_screenshot_admin_policy_e2e.py` 的 E2E-14 真实前端路径访问 `/user/profile` 后等待页签失败；本轮用静态回归断言复现重复后端菜单子路由覆盖本地 Profile 组件的风险。
Root Cause: `mergeHiddenStaticShellRoute` 在后端菜单返回同名或同 path 的隐藏子路由时，会把本地静态隐藏子路由过滤掉，导致 `/user/profile` 可能失去 `src/views/Profile/Index.vue` 组件。
Verification: `node scripts/dcc-screenshot-t4-frontend.test.mjs` 和 `pnpm ts:check` 均已通过。
Blockers: 本 worker 切片未重跑后端 Playwright E2E-14；需要主任务在 8089 真实前端服务上复验。

BDD: T4 前端补齐发放/打印/外来评审入口 -> Given T4 真实 E2E 不接受 xfail / When 用户访问 DCC 详情、BPM 模型、外来文件评审和个人中心 / Then 前端必须复用现有 DCC 上传、DCC 详情、BPM 打印模板和 Profile 路由能力，暴露可真实操作的入口。

RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py -q` in `ruoyi-vue-pro` -> FAIL，硬门槛暴露接收人加签入口、流程打印/Word 导出入口、BPM 模型页、外来文件评审入口和 Profile 改密页签问题。

GREEN: `node scripts/dcc-screenshot-t4-frontend.test.mjs` -> PASS，7 tests passed；新增覆盖 `createDistributionRecipientSignTask`、外来文件评审静态路由复用 upload、DCC 详情 `流程打印/流程导出 Word`、BPM `Word 打印模板`、Profile 重复隐藏路由合并。

GREEN: 真实前端路径复验 -> PASS；8089 重启时显式设置 `VITE_BASE_URL=http://127.0.0.1:48089`、`VITE_PROXY_TARGET=http://127.0.0.1:48089` 后，`/user/profile` 显示 `密码设置`，`/bpm/manager/model` 显示 `流程模型 / Word 打印模板`，DCC 详情显示 `确认签收 / 接收人加签 / 流程打印 / 流程导出 Word`。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py -q` in `ruoyi-vue-pro` -> PASS，7 passed in 83.78s。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_e2e_suite.py script/tests/test_dcc_screenshot_navigation_e2e.py script/tests/test_dcc_screenshot_upload_download_e2e.py script/tests/test_dcc_screenshot_workflow_actions_e2e.py script/tests/test_dcc_screenshot_admin_policy_e2e.py -q` in `ruoyi-vue-pro` -> PASS，11 passed in 240.13s。

REGRESSION: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS，4 tests passed。

REGRESSION: `node scripts/dcc-screenshot-t4-frontend.test.mjs` -> PASS，7 tests passed。

REGRESSION: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` -> PASS。

Changed paths:

- `src/api/dcc/controlledFile/workflow.ts`
- `src/views/dcc/controlled-file/upload/submitter.ts`
- `src/views/dcc/controlled-file/upload/index.vue`
- `src/views/dcc/controlled-file/mine/index.vue`
- `src/views/dcc/controlled-file/browser/index.vue`
- `src/views/dcc/controlled-file/detail/index.vue`
- `scripts/dcc-screenshot-t2-frontend.test.mjs`
- `doc/tasks/20260525-dcc-screenshot-implementation/frontend-feature-evidence.md`

## T4 - 流程动作与第四节点页面

BDD: 申请人选择会签人 -> Given 申请人在现有 DCC 上传页提交受控文件 / When 需要指定电子发放或会签处理人 / Then 前端必须把选择的用户 ID 放入 `selectedSignoffUserIds`，并继续复用现有提交接口。

BDD: DCC 审批任务流程动作必须电子签名 -> Given 审批人在 DCC 详情页打开自己的待办任务 / When 执行回退、转办或加签 / Then 前端必须调用 DCC 专用任务接口并提交登录密码，不得通过 BPM 通用接口绕过 DCC 签名留痕。

BDD: 第四节点发布前资料门禁 -> Given 文控在第四节点批准文件 / When 文件进入发布前处理 / Then 前端必须要求上传盖章 PDF；若文件要求培训，还必须上传培训记录后才能提交批准。

RED: `node scripts/dcc-screenshot-t4-frontend.test.mjs` -> FAIL，预期失败：缺少 DCC 流程动作接口、申请人会签人选择、第四节点文件上传和 BPM 通用动作绕过防护。

GREEN: `node scripts/dcc-screenshot-t4-frontend.test.mjs` -> PASS，4 tests passed。

REGRESSION: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS，4 tests passed。

REGRESSION: `node scripts/dcc-controlled-file-download-auth.test.mjs` -> PASS，3 tests passed。

REGRESSION: `node scripts/dcc-controlled-browser-version-selector.test.mjs` -> PASS，3 tests passed。

REGRESSION: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` -> PASS。验证时临时复用主仓库 `node_modules` 联接并复制已生成的 `src/types/auto-imports.d.ts`；验证后已删除临时文件。
