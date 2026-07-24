# 执行日志

INFO: experience-index -> matched `docs/powershell-memory.md`, `docs/worktree-memory.md`, `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`, frontend/backend/database delivery contracts, and BDD guidance.

GREEN: experience-preflight -> PASS，已确认分支和 worktree 路径唯一，端口 `8094/48094` 未监听，数据库与 Redis 目标显式固定，不操作服务器、正式环境或受保护文件配置。

BDD: 点击边界节点 -> Given 用户打开流转关系图 / When 点击工序开始或工序结束 / Then 对应边界节点显示选中状态和只读关系摘要，普通工序详情不被误用。

BDD: 多开始分支汇合 -> Given 路线包含多个首工序和一个汇合工序 / When 从工序开始分别连接多个首工序并将分支汇合 / Then 草稿允许保存且刷新后边界关系完整恢复。

BDD: 限制非法连接 -> Given 普通工序已有后续或工序结束已有入口 / When 用户尝试增加第二条受限连接 / Then 页面明确拒绝且不替换已有关系。

BDD: 边界关系可删除 -> Given 用户选中开始或结束边界连接线 / When 删除该关系 / Then 草稿关系消失并在保存后持久化。

BLOCKER: none

RED: `node --test tests/e2e/mes-route-flow-boundary-links-static.spec.js` -> FAIL，API 未暴露 boundaryEdges，设计器未维护边界选择与真实边界草稿，符合预期。

GREEN: `node --test tests/e2e/mes-route-flow-graph-static.spec.js tests/e2e/mes-route-flow-boundary-links-static.spec.js` -> PASS，3 tests PASS。

GREEN: 定向 ESLint -> PASS；覆盖 API 类型、设计器、边界静态测试和真实 E2E 脚本。

INFO: `pnpm.cmd ts:check` 在非交互 worktree 中触发 pnpm 模块清理确认而中止；改为执行项目同一 `vue-tsc --noEmit -p tsconfig.relaxed.json` 编译命令，不跳过类型检查。

RED: 直接 TypeScript 检查使用默认 4GB Node 堆内存 -> FAIL，进程 OOM。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。

RED: 官方登录 preflight 首次使用错误的 PowerShell `String.Concat` 参数构造租户名 -> FAIL，浏览器未启动；修正为 UTF-8 安全 char 数组后继续。

RED: 官方登录 preflight 使用页面不存在的目标文本“工艺路线” -> FAIL，真实登录已完成但目标文本等待超时；去除错误文本断言后按目标路由重新执行。

GREEN: 官方 `login-preflight.mjs` -> PASS，测试租户 `aoteman` 真实进入 `http://127.0.0.1:8094/mes/pro/route`。

GREEN: `node tests/e2e/mes-route-flow-boundary-links-real.e2e.js` -> PASS；tenant-id=122、routeId=922074、routeCode=RT000017。

GREEN: 真实 E2E 点击 START/END 后选中样式和只读摘要正确，普通工序关键设置和删除逻辑未出现；真实点击边界连接线后可选中和删除。

GREEN: 真实 E2E 将原线性关系改为 `START -> 922869` 与 `START -> 922870`，两分支汇合到 `922871`，唯一末工序 `922882 -> END`；保存、刷新和 API GET 完整恢复。

GREEN: 真实 E2E 尝试第二条 END 入边和普通工序第二条出边均显示明确提示，关系清单保持不变。

GREEN: E2E 最后通过页面操作恢复路线原始拓扑并再次保存、刷新和 API 断言，未遗留测试数据变更。

REGRESSION: 直接受本任务影响的流转图契约测试全部通过；既有全量静态批次中的旧精确字符串/属性顺序断言另行记录，不以修改无关生产行为方式掩盖。

REGRESSION: 全部 `mes-route-flow-*-static.spec.js` -> 17 PASS / 5 FAIL；将同一 5 个失败测试对任务分支基线 HEAD 的归档源码重新执行后仍为 0 PASS / 5 FAIL，确认均为本任务开始前已存在的脆弱断言，不是本次边界关系改动引入。

GREEN: frontend feature evidence validator -> PASS。

RED: rebase 后真实 E2E 第一次保存多前置汇合 -> FAIL，后端真实库旧目标唯一索引拒绝第二个前置关系；前端未吞异常，保存链路明确暴露后端错误。

RED: 后端索引修复后真实 E2E 第二次保存恢复原拓扑 -> FAIL，路线更新接口因 V10 版本重复返回错误；E2E 明确校验路线更新响应，不将后续图保存等待超时误判为前端成功。

GREEN: 真实 E2E 恢复阶段先通过列表重新打开同一路线，获取最新路线版本状态后再执行删除、连线和保存；最终完整用例两次保存均通过。

GREEN: 最终 Playwright 真实 E2E -> PASS；测试租户 `aoteman`、routeId=922074、routeCode=RT000017，变更拓扑与恢复拓扑均通过页面操作，最终 API 只读断言确认原拓扑已恢复。

GREEN: 前端提交 `8a903a2e0` 已快进融合到 `int_main`，主工作区 HEAD 与任务分支一致。

GREEN: 融合结果静态验证 -> 3 tests PASS；定向 ESLint PASS；8GB `vue-tsc` PASS。

GREEN: 融合结果真实 Playwright E2E -> PASS；双 START 汇合、唯一 END、非法连接拒绝、两次保存刷新均通过，最终页面恢复原拓扑。

INFO: task-closeout-cleanup preview 已确认仅删除 `frontend-feature-evidence.md`；自动 worktree 收尾因主前端工作区存在无关脏改且任务分支已先行快进融合而阻塞，后续仅清理预览列出的任务产物并保留全部无关用户改动。

GREEN: 已删除预览列出的 `frontend-feature-evidence.md`，未删除生产代码、正式测试或其它任务文件。

GREEN: 前端收尾提交已在无重叠任务文档上快进融合到最新 `int_main`；主工作区原有 `docs/request-command-log.md`、MDM 页面及其它未跟踪任务产物保持未提交状态。

GREEN: 隔离前端 `8094` 与后端 `48094` 已停止，任务 worktree、任务根目录和分支 `codex/20260710-route-flow-boundary-links` 已删除。

GREEN: final verification -> COMPLETED，保留正式生产代码、真实 E2E/静态回归测试、`task.md` 和 `execution-log.md`。
