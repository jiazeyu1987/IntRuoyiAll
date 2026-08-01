# 20260731 PQC 组长列表内容对齐

## Task Goal

让 PQC 组长检查列表中的“提交内容”与 PQC 检验员填写页的逐项检验内容一致展示，覆盖 `长度`、`外观`、`密封`、`压力` 四项正式 PQC 检验内容，避免只展示汇总文案导致组长无法复核逐项提交明细。

## Milestones

- [x] 记录 BDD 场景与 RED/GREEN 验证路径。
- [x] 补充 PQC 组长列表静态契约，先证明现有列表未使用正式逐项明细。
- [x] 实现最小正式链路：列表响应暴露原始 PQC payload，前端按检验员填写项解析并展示。
- [x] 运行定向静态契约、类型检查和必要后端测试。
- [x] 更新验证报告并进入收尾。

## Expected Verification

- `node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `pnpm ts:check`
- 如修改后端响应 VO 或服务映射，运行相关 Maven 定向测试或记录精确阻塞原因。

## Experience Gates

- `前端静态契约隔离门禁`：当前行为先用任务专用静态契约证明 RED，再用同一契约证明 GREEN；不得用无关大契约失败或汇总展示替代当前行为验收。
- `GitHub HTTPS 443 本地代理门禁`：如最终 `git push origin int_main` 仍因本地代理 127.0.0.1 端口连接失败，记录 blocker，不得标记任务完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。PQC 正式逐项明细缺失时必须显式显示缺少正式明细，不用汇总字段冒充逐项明细。
- `是否从根因和长期维护角度解决`：是。通过列表响应携带正式原始 payload，并在组长列表使用与检验员填写页一致的字段口径展示。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout
