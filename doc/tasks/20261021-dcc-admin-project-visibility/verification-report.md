# Verification

- 范围：超级管理员列表和详情只读放宽，限当前租户与启用项目；账号、租户及普通用户授权边界保留，admin 不自动获得 EDIT/OWNER。
- 新上传候选接口返回完整授权范围，按绿色可选优先、代码次序排序。只显示项目代码；红色资料/权限不满足项目禁选，悬停展示原因及维护位置。选择项目之后仍保留类型、目录、审批等正式校验。
- RED：admin 原服务 expected [10] but was []；新候选服务缺失导致 javac 编译失败；前端新显示与加载合同 2 FAIL。
- GREEN/REGRESSION：正式 Maven surefire 运行 `DccAdminProjectVisibilityTest,DccProjectUploadOptionsServiceTest,DccProjectAccessServiceImplTest,DccProjectAttributesBaselineContractTest`，25 tests / 0 failures / 0 errors / 0 skipped，含参数化方法和 MockitoExtension。
- 前端 `node --test` 缓存路由、项目产品身份、长 ID、A32 和新候选测试共 25 PASS / 0 FAIL，含真实 SFC script/template 编译。覆盖 75 条候选不截断、红色缺失名称可展示、搜索竞态以及非法绿色状态拒绝。
- `pnpm ts:check` 主实现和不完整名称/代码只读投影完善后分别执行，最终 exit 0。
- `mvn -q -pl yudao-server -am '-DskipTests' '-Dfile.encoding=UTF-8' package` exit 0，完整 testCompile 保留。此前兄弟模块 testCompile 阻塞已通过 reactor 全量构建解除；未绕过编译。
- 不可变归档 `output/runtime/int_qms/backend-project-options-20261021-211437.jar`；SHA256 `47075e159a3ee8a7d87f7c289a380e8517b12ae98f2a69ca7fe33d883eb30a36`。核对 5 个相关 class 与构建字节相同。
- 本轮用户明确授权后重启：仅停止 int_qms 原 PID1040；新 PID34564 监听 48061，health UP；前端 8061 HTTP200，PID16620 保留。运行归档修改时间早于新进程启动。原安全配置及签名密钥保留，schema auto-update / startup-sync 未启用。
- 用户当轮未授权真实页面 E2E，因此上述为单元/编译/服务健康证据，不宣称已通过真实上传验收；无手工业务数据库写入，未 Git 提交推送。
