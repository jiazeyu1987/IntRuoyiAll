# Execution Log

## Intent

- 用户追问全屏之后点击提交的弹框是否会被覆盖；需要把代码判断补成可执行的回归验证。

## BDD

- `BDD: 全屏后打开PQC提交签名弹框 -> Given 一线PQC页面存在正式可填写上下文 When 用户点击最大化并点击提交 Then 浏览器全屏元素必须仍是PQC填写根节点，电子签名弹框必须是该根节点的后代、可见并覆盖填写内容。`
- `BDD: 全屏弹框验证保持只读 -> Given 页面已进入全屏 When 验证提交弹框层级与可见性 Then 不调用PQC正式提交POST、不写入业务数据、不使用mock或默认成功状态。`
- `BDD: 弹框挂载位置回归 -> Given 前端后续调整弹框实现 When 弹框被teleport到body或移出全屏根节点 Then 静态回归合同必须失败。`

## Milestone Status

- completed: 已确认现有PQC签名弹框是自定义DOM，且当前运行态前端8081、后端48081可达。
- completed: 新增 `data-pqc-fullscreen-root` 到 `frontlinePanelRef` 根节点，并新增静态合同锁定签名弹框在 fullscreen root 内部且未 teleport 到 body。
- completed: 同步相邻 `frontline-pqc-formal-submit-static.spec.js` 断言，保留当前 `pqcSubmitResultUncertain` 防重复提交锁定态。
- completed: 真实 Playwright 使用本机 `芋道源码/admin` 打开一线PQC，填写必要只读验证输入，进入浏览器全屏并点击提交，签名弹框可见且未调用正式PQC提交。
- completed: `project-experience-consolidation` 已合并到 `docs/frontend-development.md#Element Plus 全屏弹框挂载门禁` 与 `docs/experience-index.md`。

## Verification Evidence

- RED: `node tests\e2e\frontline-pqc-fullscreen-submit-dialog-static.spec.cjs` -> FAIL，缺少 `data-pqc-fullscreen-root`，无法用静态合同锁定PQC浏览器全屏根节点。
- NOTE: `node tests\e2e\frontline-pqc-formal-submit-static.spec.js` 首次复跑失败在旧断言 `:disabled="payloadLoading || Boolean(pqcSubmitReceipt)"`；当前源码已加入 `pqcSubmitResultUncertain` 不确定态锁定，属于相邻合同滞后，不是本次全屏根节点改动造成。
- GREEN: `node tests\e2e\frontline-pqc-fullscreen-submit-dialog-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\frontline-pqc-formal-submit-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\mes-frontline-pqc-fullscreen-preload-static.spec.js` -> PASS。
- GREEN: `node doc\tasks\20260807-frontline-pqc-fullscreen-submit-dialog\pqc-fullscreen-submit-dialog-real-check.cjs` -> PASS；`document.fullscreenElement` 命中 `data-pqc-fullscreen-root`，`root.contains(dialog)=true`，`elementFromPoint` 命中弹框，`formalSubmitRequestCount=0`。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS。
- CLEANUP: `task_closeout.py --task-id 20260807-frontline-pqc-fullscreen-submit-dialog --mode preview` -> PASS，keep task.md / execution-log.md / verification-report.md / real-check script / JSON / PNG，delete `<none>`。
- CLEANUP: `task_closeout.py --task-id 20260807-frontline-pqc-fullscreen-submit-dialog --mode apply` -> PASS，deleted_paths `<none>`；主工作区非 linked worktree，未执行合并或 worktree 删除。

## Blockers

- 无。
