# 执行日志

BDD: 同一路线点击不同工序只保留一个编辑标签 -> Given 用户已打开某条工艺路线的流转关系图 When 用户依次点击多个工序且页面把所选路线工序写入 `routeProcessId` 查询参数 Then 标签栏只更新当前“编辑工艺路线”标签，不创建标题重复的新标签。

BDD: 不同路线仍可分别打开编辑标签 -> Given 用户已经打开一条工艺路线编辑页 When 用户打开另一条路线编号不同的编辑页 Then 标签栏保留两个可独立切换的编辑标签。

BDD: 返回后恢复所选工序 -> Given 用户在流转关系图选中工序并点击关联内容离开 When 用户返回原工艺路线编辑页 Then 页面仍按 `routeProcessId` 恢复所选工序。

ROOT_CAUSE: `TagsView` 默认使用 `route.fullPath` 作为标签身份；流转图点击工序后通过 `router.replace` 更新 `routeProcessId`，每个查询参数组合因此被识别为新标签并追加标题后缀。

RED: `node tests/e2e/mes-route-flow-same-route-single-tab-static.spec.js` -> FAIL，工艺路线编辑路由缺少 `tagsViewKeyMode: 'path'`，查询参数变化仍会创建新标签。

GREEN: `node tests/e2e/mes-route-flow-same-route-single-tab-static.spec.js` -> PASS，工艺路线编辑路由按解析后的 path 复用标签。

GREEN: `node tests/e2e/mes-route-flow-link-return-state-static.spec.js` -> PASS

GREEN: `node tests/e2e/mes-route-flow-link-return-no-reload-static.spec.js` -> PASS

GREEN: `node tests/e2e/mes-route-edit-page-static.spec.js` -> PASS

GREEN: `pnpm.cmd exec eslint src/store/modules/tagsView.ts src/router/modules/remaining.ts tests/e2e/mes-route-flow-same-route-single-tab-static.spec.js` -> PASS

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS

REGRESSION: `node tests/e2e/approval-center-root-tab-static.spec.mjs` -> FAIL，既有审批中心测试把 `alwaysShow` 限制在路径后 160 字符内，当前既有路由块超过该距离；失败发生在本任务修改位置之前，且本任务对审批中心路由无改动。

GREEN: official-login-preflight -> PASS，本机 `http://localhost:8081`、测试租户/aoteman 真实登录进入 `/mes/pro/route`。

GREEN: `node tests/e2e/mes-route-flow-same-route-single-tab-real.e2e.js` -> PASS，路线 `RT000017` 依次点击路线工序 `922869`、`922870`、`922871` 后，顶部“编辑工艺路线”标签始终为 1 个且无数字后缀。

GREEN: experience-preflight -> PASS，已读取 PowerShell、登录与 Playwright 经验；真实验证限定本机 `localhost:8081`、测试租户和只读工序点击，不访问远端环境、不修改业务数据。

BLOCKER: task-closeout apply first attempt -> 收尾脚本只识别英文 `Current Status`，中文状态被判定为 unknown；已补充标准状态段后重试。

GREEN: task-closeout preview/apply -> PASS，仅保留 `task.md` 与 `execution-log.md`，删除证据中间文件和本任务 E2E 截图目录。

GREEN: git commit -> PASS，提交信息 `任务: 修复工艺路线重复标签`，仅包含本任务路由、标签身份、类型、回归测试和任务记录。

BLOCKER: none
