# 一线 PQC 正式提交验证报告

## Result

实现与可执行的定向验证通过。正式提交现在由当前登录人触发电子签名，绑定明确的生产提交事件，按发布态 QA 规程在服务端判定结果，并在事务内形成 PQC 任务、逐件明细、工序池事件、正式记录和可追溯回执。

成功写入型真实 E2E 未放行：当前真实页面可见订单不同时具备待执行 PQC 任务、发布态 QA 规程和正式生产提交三个前置，项目规则禁止通过 mock、API-only、默认项目或伪造数据绕过。

## Automated Verification

- `mvn -pl yudao-module-mes -Pmes-frontline-pqc-formal-submit-targeted-tests test`：PASS，35 tests，0 failures，0 errors，0 skipped。
- `mvn -pl yudao-module-mes -am -DskipTests compile`：PASS。
- `pnpm ts:check`：PASS。
- 前端受影响静态合同：6/6 PASS。
- `git diff --check -- <task-owned paths>`：PASS，无空白错误。
- `validate_backend_api.py --evidence .../backend-api-evidence.md`：PASS，`Backend API evidence is valid.`
- `validate_frontend_feature.py --evidence .../frontend-feature-evidence.md`：PASS，`Frontend feature evidence is valid.`

## Real Page Verification

- 本地后端健康检查：`UP`。
- Playwright 通过真实登录入口打开一线 PQC 页面，1920x1080 工作台布局可操作。
- 选择缺任务/规程的真实订单后点击提交，页面明确显示正式前置缺失原因；没有正式写入，也没有新的未处理事件异常。
- `CODX-PQC-20260807-SP-WO-*`：缺少待执行 PQC 检验任务。
- `PQC-E2E-FS-20260804`：缺少已发布 QA 检验规程。

## Contract Evidence

- 前端不再从路由读取 PQC 生产事件或签名编号。
- 多个生产提交候选必须显式选择，单候选才自动选中，零候选明确阻塞。
- 客户端不再提交权威签名编号、签名快照或最终检验结果。
- 服务端按当前登录人生成本次 `PQC_SUBMIT` 电子签名和幂等键。
- 数值项目按上下限、选择项目按合格/不合格由服务端判定；损耗大于零或任一逐件失败时最终结果为不合格且必须填写不良说明。
- 重复提交返回已有正式回执，不新增第二条签名、逐件明细或 PQC 事件。

## Residual Limits

- 全量 `testCompile` 存在与本任务无关的缺失测试类型；本次通过正式定向 Maven profile 隔离并执行受影响 JUnit。
- 成功写入型 E2E 需要补齐任务自有的待执行 PQC 任务、发布态 QA 规程、正式生产提交和签名授权测试账号后再执行。
- 现有订单选择器在约 1280px 宽窗口存在布局拥挤；1920x1080 目标工作台尺寸验证通过，本任务未扩展响应式重构。

## Closeout

- 可复用经验已合并到现有 `docs/frontend-development.md`，并在 `docs/experience-index.md` 增加检索入口。
- cleanup preview/apply 均成功，核心任务记录保留，设计草稿、Playwright 快照/截图和临时定向测试产物已删除。
- backend/frontend 技能证据 validator 均通过；结果已归档到本报告，最终 cleanup 清理中间 evidence 文件。
- 用户未要求 Git 操作，未执行提交、合并或推送。
