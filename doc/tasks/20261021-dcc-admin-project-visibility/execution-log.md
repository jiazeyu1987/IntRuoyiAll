# Execution

- 分支 int_qms；已读取后端、数据库、登录访问及任务收尾规则；沿用 ponytail 最小修改原则。
- 确认 listReadableProjectIds / assertProjectViewerOrAbove 为公共只读范围；hasProjectEditorOrOwner / assertProjectOwner 属于写入边界，不能一起放宽。
- 正式角色代码 super_admin，PermissionApi.hasAnyRoles 精确匹配启用角色。
- RED：实际 service + 新测试 javac 编译通过，执行测试失败 expected [10] but was []。
- GREEN：相同编译及直接执行 JUnit 测试方法 PASS；覆盖 admin 全量读取、外租户/停用项目拒绝、普通账号无授权拒绝、停用或跨租户账号拒绝、admin 不新增 EDIT/OWNER。
- REGRESSION：编译现有 DccProjectAccessServiceImplTest，MockitoAnnotations 初始化、逐方法 BeforeEach/AfterEach，9 个 @Test 全部 PASS；未执行参数化方法或 MockitoExtension 严格 stub 检查，不等同完整 Maven 测试。
- 未重复执行此前已确认被兄弟模块缺失 AdminUserDirectSupervisorRespDTO 阻塞的 Maven 全量 testCompile；使用现有 classpath 编译实际改动 service / 测试并执行，未伪造全模块构建通过。
- git diff --check 针对修改 service PASS。仅变更读取路径，未修改数据库或写入授权。
- cleanup preview/apply PASS，无删除项；最终 blocked 仅对应未授权 Git 提交推送和未更新运行态，不表示单元验证失败。

## 追加：项目代码颜色、全量候选及重启

- 用户追加明确授权：仅显示项目代码、可选绿色/不完整红色、绿色在上，修改完重启后端。复用本任务，不另建重复目录。
- RED：前端新合同 2 项失败（原名称+代码展示、原分页请求无新可选状态）；后端新 options service 测试编译失败（新服务未实现）。
- GREEN：只读 `/dcc/project-codes/upload-options` 返回当前账号完整授权范围，不限前 50；通过正式属性验证及正式产品预览判定可选状态，EDIT/OWNER 不足明确列入阻断原因。已知业务校验错误作为可见红色原因，依赖/数据库等其他异常原样抛出。
- 前端只显示 projectCode；green/red、红色 disabled、title 给出缺项与维护入口；服务端在全集按可选优先排序；请求序号阻止旧搜索结果覆盖新结果。
- 前端 24 项定向测试 PASS（含真实 SFC 编译）；`pnpm ts:check` exit 0。
- Maven 首次命令 PowerShell 拆分 `-Dfile.encoding` 导致参数错误，完整引用后 `mvn -q -pl yudao-server -am '-DskipTests' '-Dfile.encoding=UTF-8' package` exit 0，保留 testCompile，无手工改写 Jar。
- 完整构建后正式 `mvn -q -pl yudao-module-dcc '-Dtest=DccAdminProjectVisibilityTest,DccProjectUploadOptionsServiceTest,DccProjectAccessServiceImplTest,DccProjectAttributesBaselineContractTest' '-DfailIfNoTests=true' surefire:test` exit 0。4 类合计 25 tests / 0 failures / 0 errors / 0 skipped，替代此前直接执行普通方法的局部回归证据，现包含 MockitoExtension 和参数化场景。
- 运行归档 `output/runtime/int_qms/backend-project-options-20261021-211437.jar`，SHA256 `47075e159a3ee8a7d87f7c289a380e8517b12ae98f2a69ca7fe33d883eb30a36`；核对 access/options/record/controller/默认属性提示共 5 个 class 与构建 target 字节一致，admin role 和 upload-options 映射均存在。
- 重启前确认原 PID1040 / 48061 / int_qms / 旧不可变 Jar；安全配置、原签名密钥及冻结工作流转换注册核验 PASS。仅停止该 PID，复用已核验的现有启动脚本保留配置，新 PID34564；前端 PID16620 保留。无手工数据库写入，无 E2E 或 Git 提交推送。
- 21:16:38 健康核验：48061 listener PID34564、health UP；8061 HTTP200、前端 PID16620；运行 Jar 未在启动后被修改。
- 末次前端完善：允许服务端明确标红的缺失名称/代码保持红色展示，不让一条历史缺值阻断整批候选；缺值不会被判为绿色。新增 API 合同覆盖 75 条完整保留及错误可选状态，前端总 25 PASS。
- 最终 pnpm ts:check exit 0；末次 diff --check PASS。运行态更新与健康验证已完成，真实页面 E2E 未获当轮授权未执行。
- 本轮 cleanup preview/apply PASS，保留 3 份正式记录，无删除项。blocked 仅对应尚未授权 Git 提交推送；实现、定向测试、构建与本机后端重启均已完成。
