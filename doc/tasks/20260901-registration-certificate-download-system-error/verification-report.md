# 验证报告：注册证下载系统异常修复

## Result

PASS

- 已从本机正式下载审计定位真实故障：业务文件 `990819112` 的版本批准日期为空，旧运行逻辑直接格式化空日期，触发空指针并向用户显示“系统异常”。
- TR3 当前源码已经包含正式根因修复：批准日期存在时优先使用批准日期；批准日期为空时使用首次获证日期；两者都为空时返回明确中文错误。
- TR3 当前测试已经覆盖该精确场景，因此本任务未重复修改生产代码或测试代码。
- 用户提供已授权本机账号后，真实 E2E 首次复现了第二个根因：下载成功审计使用手工 JSON 拼接，真实请求上下文导致 MySQL JSON 列拒绝写入，最终向页面返回“系统异常”。现已改为标准 JSON 序列化。

## Verification Evidence

- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateFileDeliveryServiceTest" test` -> PASS，14 tests，0 failures，0 errors，0 skipped，`BUILD SUCCESS`。
- REGRESSION: `node IntRuoyiFronted\tests\registration-certificate-attachment-preview-download-static.spec.mjs` -> PASS，注册证附件下载入口继续使用正式业务文件下载 API 和服务端文件名。
- CONTRACT: bug regression evidence validator -> PASS。
- CONTRACT: backend API evidence validator -> PASS。
- STRUCTURE: `git diff --check` -> PASS。
- CLOSEOUT: cleanup preview/apply -> PASS，仅清理本任务临时 evidence，保留三份正式任务记录。
- E2E RUNTIME: TR3 `yudao-server` 全依赖构建成功，并由当前 worktree 启动在 `48084`；`/actuator/health` 返回 `UP`。TR3 前端启动在 `8084`，Playwright 已确认真实登录页可达。
- EARLY E2E PRECHECK: 用户提供凭据前，登录表单和环境均无可用账号信息，因此按登录门禁停止；用户随后提供授权凭据后继续验证。
- EARLY E2E CLEANUP: 首轮无凭据预检启动的 TR3 前后端已正常停止，`8084`、`48084` 当时均已释放。
- TDD RED: 新增审计控制字符回归测试在旧实现上失败，标准 JSON 解析器报告未转义控制字符。
- TDD GREEN: 注册证文件下载服务测试 -> PASS，15 tests，0 failures，0 errors，0 skipped。
- RUNTIME BUILD: TR3 当前修复重新打包进入可执行运行包，30 个 Maven reactor 模块均成功。
- E2E GREEN: 使用真实前端页面登录 `芋道源码/admin`，打开注册证 `990819202` 详情并点击附件下载；附件 `990819112` 返回 HTTP 200，文件名 `20260801_5555555_33333333.png`，内容类型 `image/png`，大小 76,733 字节；页面未出现“系统异常”，页面错误、控制台错误和失败响应均为 0。
- FINAL CLOSEOUT: cleanup preview/apply -> PASS，仅删除本任务临时 E2E 脚本和结果 JSON；服务停止后 `8084/48084` 均未监听。
- IMPLEMENTATION COMMIT: `7905873f8`，提交文件范围仅限本任务四个实现、测试和经验文件。

## Business Impact

- 首证上传形成的注册证即使没有批准日期，也能按正式首次获证日期生成下载文件名，不再因为空日期出现“系统异常”。
- 下载权限、公司范围、真实文件读取和审计链路保持不变；未引入默认成功、绕过授权或文件直链。
- 真实浏览器请求上下文可以被安全写入下载审计 JSON，不再因审计记录格式错误反向中断文件下载。

## Scope Notes

- 用户后续明确要求 E2E、Playwright 和本机运行态验证；正式下载链路新增了一条成功审计，未修改注册证业务数据，未提交、未合并、未推送。
- 本轮仅启动 TR3 隔离端口 `8084/48084`，未占用主工作区 `8081/48081`。

## Remaining Blockers

- 无代码或 E2E 阻塞；等待 `int_main` 快进融合和最终记录提交。
- 默认 worktree 集成预览提示 TR3 不能快进合并且主工作区有其他改动；这不属于本任务完成门禁。按项目 Git 政策未执行提交、合并、推送或 worktree 删除。
