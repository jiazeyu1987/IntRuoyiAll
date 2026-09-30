# Frontend General Fixes Subtask

范围：URB2-004 至 URB2-009。仅做前端静态分析、静态合同与类型检查，不执行 E2E、页面操作、服务、数据库或 Git 操作。

## BDD

- URB2-004：Given 服务端返回带业务前缀的 scoped session，When 原客户端会话重试同一上传，Then 页面能通过明确的 client/scoped session 对应关系命中原身份；不同载荷仍不放行。
- URB2-005：Given SOURCE、PDF 或附件上传尚未返回，When 删除文件或切换上传上下文，Then 当前 loading、尝试记录和旧回调都不能污染新上下文。
- URB2-006：Given 类别 A 请求慢、类别 B 请求快，When 当前类别已切换为 B，Then A 的目录成功、失败和 loading 均不能覆盖 B。
- URB2-007：Given 培训证据 A 上传未完成，When 关闭/移除并重新打开选择 B，Then A 的响应不能写入 B 或提交载荷；当前弹窗只接受当前 session 的结果。
- URB2-008：Given 首次未生效 NEW 工作稿返工，When 浏览页送审，Then 预检检查 UPLOAD 路线；已生效 NEW 的修订仍检查 REVISION。
- URB2-009：Given 后端版本策略支持多段 Windchill 版本，When 解析 A/1/2 等合法版本，Then 上传校验与浏览最新工作稿判定不因固定两段正则丢失；非法版本仍明确拒绝。

## RED/GREEN/REGRESSION

1. RED：增加只针对上述 handler/解析函数的静态合同，锁定 raw/scoped session 对应、reset/loading/generation、目录代次、培训 session、路线动作选择和多段版本解析。
2. GREEN：实现最小身份/代次保护和策略解析，保持既有 API 合同与错误暴露。
3. REGRESSION：运行任务专用静态合同、相关前端类型检查和 `git diff --check`；不运行 E2E。

## 验证状态

- RED：`node tests/e2e/dcc-upload-revision-browse-general-fixes-static.spec.js` 首次在缺少 `uploadSessionBinding` 的现状失败；修正测试自身正则后仍在首个业务缺口失败。
- GREEN：实现后该专用静态合同 PASS；`pnpm run ts:check` 使用 8192MB Node 堆 PASS。
- REGRESSION：`git diff --check` PASS。未执行 E2E、Playwright、服务、数据库或业务页面操作。
- status: ready_for_closeout
- ready_for_closeout: true
- completed: false

## 未解决风险

- raw/scoped session 的对应关系在页面内存中维护；如果页面刷新导致原响应只剩 scoped session，后端重试能力仍需由正式服务端查询/投影支撑，本子任务未改后端合同。
- 多段版本前端按“首段字母、其余正整数段”接受，并用全部段排序；实际 `majorIdentitySegmentCount` 与运行配置的一致性仍需后续静态接口投影或真实验收确认。
- 详情培训弹窗的迟到响应按原 session + 原 upload ticket 清理，清理接口失败会显式提示；未进行网络故障或真实页面验证。
