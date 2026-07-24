# Execution Log：产品主数据引用按钮缺少 id 参数回归修复

- `BDD: 点击引用按钮时前端必须提交当前行 id -> Given 用户在产品主数据列表点击某一行“引用”按钮 / When 前端请求 /mdm/product/references / Then 查询参数必须包含该行 id，后端返回引用统计后页面打开展示弹框。`
- `INFO: prior-task-check -> 已复核 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-audio-modal\task.md，状态为 COMPLETED，不阻塞本次任务。`
- `INFO: experience-gate -> 命中 D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md；本轮仅做静态契约和类型验证，不执行真实登录或写入型 E2E。`
- `RED: node tests/e2e/mdm-product-reference-id-static.spec.js -> FAIL，断言“产品主数据引用接口必须按后端契约提交 id 查询参数”失败；当前源码实际发送 params { productId }，可稳定复现后端缺少 id 报错。`
- `GREEN: apply_patch -> 创建任务台账与缺陷证据，新增 mdm-product-reference-id 静态回归，并将 src/api/mdm/product/index.ts 中 /mdm/product/references 的查询参数从 { productId } 修正为 { id: productId }。`
- `GREEN: node tests/e2e/mdm-product-reference-id-static.spec.js -> PASS`
- `BLOCKER: node scripts/mdm-product-master-contract.test.mjs -> FAIL，当前脚本命中仓库现有 DCC 受控文件 API 契约断言 productMasterId:number 不匹配，与本次产品主数据引用参数修复无关。`
- `BLOCKER: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> FAIL，本地 node_modules 缺少 @volar/typescript/lib/quickstart/runTsc，未进入业务类型检查阶段。`
- `GREEN: python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mdm-product-reference-id-missing\bug-regression-evidence.md -> PASS`
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-mdm-product-reference-id-missing --mode preview -> PASS，status=ready。`
