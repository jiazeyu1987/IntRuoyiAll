# backend 原始工作区恢复与 DCC 识别改动保留执行日志

- GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`，本轮只在 clean backend `int_main` worktree 内处理 DCC 识别代码保留，不触碰服务器、发布或原始 backend 工作区里的未提交现场。
- BDD: 保留用户要求的 DCC 识别改动 -> Given 原始 backend 工作区里还有用户要求保留的 DCC 识别本地改动 When 把真正要保留的后端代码并回 int_main Then int_main 必须接住这组 DCC 改动，而不是在恢复目录时把它覆盖丢失。
- BDD: 原始 backend 工作区只有在主线已接住 DCC 改动后才允许切回 -> Given clean int_main worktree 已吸收 DCC 识别改动并验证通过 When 再处理原始 backend 工作区与 holding 分支 Then 后续切回不会丢失 DCC 改动。
- RED: `git diff --no-index --ignore-cr-at-eol --unified=3 <int_main-file> <original-worktree-file>` -> FAIL，`DccControlledFileProjectCodeRecognitionServiceImpl.java` 与 `DccControlledFileProjectCodeRecognitionServiceTest.java` 当前原始 backend 工作区内容不等于 backend 真正的 `int_main`，若直接切回将覆盖用户要求保留的 DCC 改动。
- GREEN: dcc-change-replay -> PASS，已把原始 backend 工作区中的 `DccControlledFileProjectCodeRecognitionServiceImpl.java` 与 `DccControlledFileProjectCodeRecognitionServiceTest.java` 精确重放到 clean backend `int_main` worktree。
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-preview-int-main-clean\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccProjectCodeCodexCliClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`BUILD SUCCESS`，共 `17` 个测试通过，失败 `0`、错误 `0`、跳过 `0`。
