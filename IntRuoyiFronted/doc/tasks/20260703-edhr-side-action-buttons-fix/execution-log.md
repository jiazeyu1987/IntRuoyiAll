BDD: 审签归档按钮打开当前工序动作详情 -> Given 用户在批记录详情页选中一道已有执行记录的工序 / When 点击签名记录、审批记录或单表归档 / Then 页面进入当前工序对应详情视图，不跳到泛化列表页。
BDD: 工序执行按钮保持当前工序上下文 -> Given 用户在批记录详情页选中一道工序 / When 点击打开工序、工作任务或执行追踪 / Then 路由或接口携带 batchExecutionId、executionId、workTaskId、routeProcessId 与 reportId 等上下文。
GREEN: experience-preflight -> PASS，已读取 PowerShell、经验索引与前端交付门禁；本轮先做静态契约验证，不执行真实 E2E。RED: node tests/e2e/edhr-side-action-buttons-static.spec.js -> FAIL, 签名记录按钮仍跳到泛化签名列表。
GREEN: node tests/e2e/edhr-side-action-buttons-static.spec.js -> PASS。
GREEN: node tests/e2e/edhr-remove-redundant-review-copy-static.spec.js -> PASS。
BLOCKER: node tests/e2e/edhr-process-form-action-columns-static.spec.js -> FAIL，旧断言要求已删除的冗余标题，不属于本轮按钮逻辑改动。