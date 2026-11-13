# 用户批准页面优化：方向 1 交接

## 范围与结论

- 共同基线：`5305869c3590c4266092a6f6efba37df16296453`。
- 分支：`codex/user-approval-01`。
- worktree：`C:/Users/D01020/.codex/worktrees/user-approval-01/IntRuoyiAll-int_main`。
- 页头、九项概览、五项导航及撤回条件在共同基线已实现，本方向保留原实现，没有修改产品代码；新增真实 Chromium 导航组件几何验证。不能把共同基线中的产品实现计为本方向增量。
- 不修改底栏、弹窗、路由、权限或其他任务资产；不新增菜单迁移。

## 业务节点与静态依据

以下行号均对应共同基线，文件路径相对于仓库根目录。

| 节点 | 依据与核对结果 |
| --- | --- |
| 页面入口 | `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue:3631` 由 `handling=approval` 和 `from=approval-center` 选择公共办理布局，无节点专属副本。 |
| 动态页头 | 同文件 `:101` 起，名称、编号、版本和状态读取 `fileDetail`，保留返回；未写入参考图示例值。详情 `:5033` 通过 `getControlledFile` 读取指定文件及申请上下文，`:5053` 校验响应身份后赋值。 |
| 九项概览 | 同文件 `:360` 起固定三列，按项目/存储文件夹/文件分类、产品编号/产品名称/编制人、提交备注/申请生效方式/培训要求排列。项目取项目名称与编码，编制人取归档身份快照，培训要求 `:3715` 取本次申请的正式流转契约。 |
| 页内导航 | 同文件 `:3643` 使用五个真实区块 ref，顺序为文件概览、正文与附件、当前任务、申请与审批、关联文件。`ReviewSectionNavigation.vue` 根据实际滚动容器、吸顶高度计算位置，scroll/resize/ResizeObserver 同步选中，底部选最后可用区块。 |
| 撤回显示 | 同文件 `:3866` 的 `canWithdraw` 同时要求当前有效详情、submit/edit 权限、可撤回状态、当前流程轮次、申请人身份及后端动作投影；按钮 `:123` 位于返回旁。不能由参考图推出所有账号均隐藏撤回。 |
| 撤回请求 | 同文件 `:6266` 的 `handleWithdraw` 要求原因，确认后重新校验路由/文件/权限，提交实际 processInstanceId；`IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts:2517` 请求 `POST /dcc/controlled-files/{id}/withdraw`，失败不显示成功。 |
| 服务端边界 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java:838` 校验 edit/submit 权限。`service/file/DccControlledFileWorkflowServiceImpl.java:2033` 校验分类 UPLOAD 权限、申请人、三种允许状态、非空且一致的流程轮次，锁定当前申请后调用 BPM 撤回。 |
| 动作投影与文控边界 | 同模块 `service/file/DccControlledFileQueryServiceImpl.java:4784` 排除曾进入文控的流程，`:4809` 和 `:4838` 同时按申请人、UPLOAD 权限生成 WITHDRAW 与 canWithdraw。`service/file/DccApplicantCancellationGuard.java:33` 起校验租户、申请人、文件/流程归属并加锁，拒绝曾进入文控的申请。 |
| 下游结果 | `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmProcessInstanceServiceImpl.java:1034` 检查发起人、流程配置和业务 guard 后取消流程；WorkflowService 在同一事务保存 WITHDRAWN、原因并调用 `platformAdapter.recordWithdrawn`，更新失败抛错。未发现本方向需要扩大修改的证据。 |

## 已运行验证

在 worktree 的 `IntRuoyiFronted` 目录执行；只读复用主目录现有工具依赖，通过 `NODE_PATH=C:/IntRuoyiAll-int_main/IntRuoyiFronted/node_modules` 解析，没有向主目录写文件。

1. `node --test tests/unit/dcc-review-navigation-browser.test.cjs`：通过。实际 headless Chromium 加载现有 Vue 组件及其样式，分别覆盖嵌套滚动容器和窗口滚动：五项点击、标题在视口内且不被导航遮挡、鼠标滚轮同步选中、吸顶位置、顶部和底部边界。此为组件验证，不是业务 E2E，不使用业务 API 或业务 mock。
2. `node --test --test-name-pattern='all changed|overview|navigation|anchors|window scrolling|resize observation' tests/unit/dcc-supervisor-review-navigation.test.cjs`：6/6 通过，包括相关 SFC 编译、九项顺序、五个锚点、滚动计算及异步尺寸观察。
3. `node --test tests/unit/dcc-withdraw-entry.test.cjs`：9/9 通过，包括多页面模式显示、非申请人/无权限/缺动作投影/历史轮次/完成状态不显示，当前申请人合法阶段显示，取消/上下文变化/服务器失败不报成功。
4. `python scripts/preflight/check-menu-migrations.py` 及暂存后的 `--staged`：均通过（累计基线 173 项信号，本方向未新增菜单/权限/页面）。
5. `scripts/preflight/branch-runtime-port-guard.ps1`：通过，识别 `codex/user-approval-01/int_qms`，8077/48077。

浏览器运行日志和截图保留在本 worktree 的 `.runtime/user-approval-01-browser.log` 及 `.runtime/user-approval-01/`（nested/window 的 top/bottom 四张组件截图），不作为业务页面截图。

## 真实页面验收限制

- 尚未完成真实前端业务 E2E；不能声明 E2E PASS。
- 当前 worktree 没有 `IntRuoyiFronted/node_modules`，也没有 `IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar`；预约后 8077/48077 均无监听，尚无本任务独立前后端服务。
- 未提供本方向自有文件及申请轮次，未操作参考图中的业务样本，未直接调用业务 API、写数据库或批准/驳回任何申请。组件验证不替代真实账号与实际文件权限验证。
- 集成者需在真实页面以“芋道源码”租户、admin/admin123 及任务自有申请验证动态数据、实际滚动布局、返回和撤回显示；若需验证非申请人/无权限账号，需提供对应实际身份。撤回可用时的业务执行亦需任务自有可撤回申请。

## 运行资源与集成注意

- 已读取磁盘项目 AGENTS.md、worktree 限制和 ponytail 技能；已检查用户目录及上级目录，全局 AGENTS.md 不存在，前端没有更近的 AGENTS.md。
- 应用工具返回的工作目录叶名为 `IntRuoyiAll-int_main`，预约脚本要求 Name 匹配叶名，因此登记名使用该叶名，实际路径和分支唯一指向本任务。
- int_qms slot 16，前端 8077，后端 48077。worktree 与预约保留，没有启动、停止或重启任何业务服务，没有占用 48081。
- 本方向增量仅测试与此交接文件；没有 `index.vue` 或公共导航组件冲突。集成后若其他方向改变导航或区块尺寸，请在最终真实页面重新核对，不把本次组件通过视作合并后的综合验收。
- 未合并、未推送、未发布。提交 SHA 以本分支最终 HEAD 为准。
