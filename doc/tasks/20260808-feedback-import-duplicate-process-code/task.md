# 导入记录重复工序编码分页异常修复

## Task Goal

修复报工管理导入记录区域因 `mes_pro_process.code` 存在多条合法记录而返回系统异常的问题。重复工序编码是合法业务数据：不同人员可提交同一工序编码，同一人员也可在不同时间提交同一工序编码。

## Milestones

- [x] 记录重复工序编码合法性和当前异常证据
- [x] 增加导入记录分页重复工序编码回归测试
- [x] 修改导入记录响应组装逻辑，避免按工序编码唯一查询
- [x] 运行目标后端测试和必要 API 复验
- [x] 更新验证报告和收尾状态

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProFeedbackImportRecordServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 如本地后端运行态可安全刷新，再使用登录态只读请求验证 `/mes/pro/feedback/import-record/page?pageNo=1&pageSize=10` 返回业务码 `0`。

## Current Status

blocked - 本机 48081 运行态已刷新并验证报工列表和导入记录列表恢复；目标 JUnit GREEN 仍被当前 HEAD 中无关 MES 测试编译错误阻塞，未伪造测试通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本次按合法一对多工序编码建模，不吞掉异常、不返回默认成功。
- `是否从根因和长期维护角度解决`：是；修复按工序编码唯一查询的错误假设。
- `是否存在临时补丁或绕过`：否；不删除重复数据、不新增唯一约束、不任取第一条工序。

## Applicable Gates

- `bug-regression-fix-loop`：必须先复现/记录异常，增加失败回归测试，再做最小修复并记录 RED/GREEN。
- `docs/backend-development.md#第三方报工直报正式链路门禁`：第三方报工、导入记录和正式报工链路不得用空数据、前端隐藏或删除数据掩盖真实链路问题。
- `docs/database-rules.md`：重复工序编码属于当前 schema 允许的业务数据，不得未授权修改 schema 或真实业务数据。
- `docs/powershell-memory.md#PowerShell Maven -D 参数引号门禁`：PowerShell 下 Maven `-D` 参数必须整体加双引号。
