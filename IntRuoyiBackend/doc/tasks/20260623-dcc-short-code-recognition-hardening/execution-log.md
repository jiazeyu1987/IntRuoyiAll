# DCC 短编码文件名识别硬化执行日志

- BDD: 短编码作为普通长 token 子串时不得直连 -> Given 启用项目编码中存在 IN/OC/EC 等短编码 / When 源文件名只是 INT、OCP、ECR 等普通长 token 子串 / Then 后端不得把该短编码当作唯一直连命中结果。
- BDD: 低置信度短编码不能仅凭文件名直接回写 -> Given 文件名仅命中低置信度短编码 / When 文控执行基础信息识别 / Then 后端必须继续正式识别链路，而不是直接回写项目编码和项目名称。
- BDD: 边界清晰的长编码仍可保留快捷路径 -> Given 源文件名包含边界明确且唯一的长项目编码 / When 文控执行基础信息识别 / Then 后端仍可在读取文件内容前直接锁定该编码。
- GREEN: previous-task-check -> PASS，已核对上一 backend 任务收口状态，本轮允许在独立 clean worktree 中继续新任务。
- GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 与 `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`；本轮仅做本机代码与单测 TDD，不执行服务器写入、发布或 E2E。
- BLOCKER: dirty-main-worktree-compilation -> 主 backend 工作区存在无关脏改动，直接执行模块测试时编译失败点落在其他未收口 DCC 改动而非本任务新增回归；影响：无法在主工作区获得干净 RED 证据。处理：已切换到独立 clean worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening` 继续本任务。
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileProjectCodeRecognitionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，3 个新增回归用例都显示当前实现未调用 `fileService.getFileContent(...)` 与 `codexCliClient.recognizeProjectCode(...)`，证明 `containsIgnoreCase + 最长唯一编码` 仍把短编码/嵌入长 token 的子串误当作文件名直连命中。
- GREEN: shortcut-hardening-implementation -> PASS，已在 `DccControlledFileProjectCodeRecognitionServiceImpl` 引入“高置信度文件名短路”规则：短编码有效字符长度不足 4 时不再文件名直连；其余编码需满足完整 ASCII token 边界后才允许跳过内容识别。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileProjectCodeRecognitionServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccProjectCodeCodexCliClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
