# 执行日志：运行控制台全按钮 E2E 融合进 int_main（前端）

- BDD: 前端 int_main 只融合运行控制台 E2E -> Given 全按钮 E2E 分支相对当前 `int_main` 含非本任务差异 / When 融合运行控制台 E2E 成果 / Then 只能引入本任务提交 `310c6e988`，不得带入 DCC 页面、package.json 或其他无关差异。
- BDD: 融合后每个按钮仍通过真实路径验证 -> Given 后端和前端 `int_main` 融合完成且可登录运行控制台 / When Playwright 在测试租户执行全按钮真实 E2E 并在 `芋道源码/admin` 做最终验证 / Then 39 个检查均为 PASS 或明确的 LOGICALLY_BLOCKED。

- FACT: 直接比较 `int_main..codex/20260529-runtime-control-all-buttons-e2e` 发现前端分支会带入 DCC 页面、package.json 和删除旧 DCC E2E 等非本任务差异；整体 merge 不符合本任务范围。
- FACT: 本任务前端融合范围限定为 `310c6e988 任务: 补齐运行控制台全按钮E2E`。
- GREEN: `git cherry-pick 310c6e988` -> PASS，生成 `dfd37d463 任务: 补齐运行控制台全按钮E2E`。
- GREEN: 前端融合范围核查 -> PASS，cherry-pick 仅新增全按钮 E2E 脚本与对应任务记录。
- RED: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` on `http://127.0.0.1:8081` -> FAIL，`8081` 当前 `.env.local` 指向 `48098`，不是本次验证后端 `48081`；影响：不能作为融合结果验证入口。
- GREEN: 启动前端 `int_main` 临时入口 `http://127.0.0.1:8092` -> PASS，显式 `VITE_BASE_URL=http://127.0.0.1:48081`。
- RED: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` on `8092` -> FAIL，后端 `48081` 已退出，前置数据准备无法连接。
- GREEN: 后端 `48081` 恢复健康 -> PASS。
- RED: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` on `8092` -> FAIL，Element Plus 弹窗销毁竞态导致切换高风险按钮时 locator 等待超时。
- GREEN: test-script repair -> PASS，`closeAllDialogs` 改为 DOM 遍历关闭当前可见弹窗。
- RED: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` on `8092` -> FAIL，高风险候选加载后顶部刷新按钮仍处于 loading，日志验证立即点击导致超时。
- GREEN: test-script repair -> PASS，日志验证前等待顶部刷新按钮恢复可用。
- GREEN: `node --check tests\e2e\runtime-control-all-buttons-real.e2e.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` on `http://127.0.0.1:8092` -> PASS，39 checks；测试租户 24 项，`芋道源码/admin` 15 项。
- GREEN: `task-closeout-cleanup --mode preview --worktree-closeout off` -> PASS，仅建议清理 `frontend-8092.err.log` 和 `frontend-8092.out.log`，结果 JSON 保留。
- GREEN: `task-closeout-cleanup --mode apply --worktree-closeout off` -> PASS，已删除本任务 8092 前端临时日志。
- FACT: E2E 结束后已停止临时前端 `8092`。
