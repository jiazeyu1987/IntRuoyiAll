# Feature

优化 DCC 受控文件元数据修改后的联动体验，让上传/升版产生的已有文件在修改 DCC 项目代码和文件分类时，用户能提前看到变更影响，保存后能从详情页进入对应项目代码关联文档位置，并在权限失败时获得可行动诊断。

## Acceptance

- `A1` 元数据弹窗在保存前展示当前/目标 DCC 项目、当前/目标分类路径、当前受控目录和受控浏览落位。
- `A2` 受控文件详情页展示 DCC 项目代码联动区，包含当前项目、当前分类、关联文件 ID、关联文档入口和修正追溯入口。
- `A3` 从详情页进入 DCC 项目代码详情时，关联文档三栏能定位当前文件和文件类型，并高亮当前联动行。
- `A4` 当后端返回文控角色权限失败时，页面提示 `doc_control`、重新登录和 `user_role_ids` 缓存刷新诊断。
- `A5` 不引入 fallback、不吞接口错误、不用 API-only 替代真实页面验证。

## UI Entry Points

- `/dcc/controlled-file/detail/:id`：受控文件详情页和元数据编辑弹窗。
- `/mdm/project-code?projectCodeId=...&associatedFocus=1&associatedFileId=...&fileTypeTaxonomyId=...`：DCC 项目代码详情关联文档定位。
- `/dcc/controlled-file/logs?logType=PROJECT_CODE_CHANGE&controlledFileId=...&projectCodeId=...`：修正追溯入口。

## Owned Files

- `IntRuoyiFronted/src/views/dcc/controlled-file/shared/ControlledFileMetadataDialog.vue`
- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`
- `IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue`
- `IntRuoyiFronted/tests/e2e/dcc-controlled-file-metadata-linkage-ux-static.spec.cjs`
- `IntRuoyiFronted/package.json`

## API Contracts And Data States

- 沿用既有受控文件详情、元数据保存、DCC 文件分类树、DCC 项目代码详情和关联文档 API。
- 本次只做前端展示和路由 query 联动，不改变后端 payload、权限模型、文件分类树来源或项目代码关联文档数据源。
- 权限失败继续由后端 fail-fast，前端只将正式错误转换为更明确的诊断说明。

## BDD:

- `BDD: 元数据变更影响预览 -> Given` 文控账号打开受控文件详情并编辑 DCC 项目代码或文件分类；`When` 弹窗内选择目标项目或目标分类；`Then` 页面在保存前展示当前值、目标值、最终关联文档落位说明和分类完整路径。
- `BDD: 保存后项目代码联动入口 -> Given` 元数据保存成功；`When` 用户回到受控文件详情；`Then` 页面展示当前 DCC 项目代码、文件分类、关联文档入口，并可跳转到项目代码详情关联文档区域。
- `BDD: 权限失败可诊断 -> Given` 当前账号缺少或后端缓存未识别 `doc_control`；`When` 保存元数据被后端拒绝；`Then` 页面显示文控角色、重新登录和权限缓存诊断，而不是通用失败。
- `BDD: 项目代码详情定位 -> Given` 用户从受控文件详情页点击关联文档入口；`When` 项目代码详情加载关联文档；`Then` 页面按目标文件分类展开并高亮当前联动文件。

## RED:

- `node tests\e2e\dcc-controlled-file-metadata-linkage-ux-static.spec.cjs -> FAIL`，预期失败原因是旧 UI 缺少影响预览、联动入口、query 定位和权限诊断。

## GREEN:

- `node tests\e2e\dcc-controlled-file-metadata-linkage-ux-static.spec.cjs -> PASS`。
- `node tests\e2e\dcc-project-code-associated-three-column-static.spec.js -> PASS`。
- `node tests\e2e\dcc-upload-governance-ux-static.spec.js -> PASS`。
- `node tests\e2e\dcc-upload-project-taxonomy-revision-static.spec.js -> PASS`。
- `node tests\e2e\dcc-metadata-file-number-optional-static.spec.js -> PASS`。
- `pnpm ts:check -> PASS`。
- `node doc\tasks\20260802-dcc-project-code-filetype-assignment-e2e\dcc-project-code-filetype-assignment-e2e.cjs -> PASS`。

## Verification

- 静态合同覆盖影响预览、详情页联动卡片、项目代码详情 query 定位、高亮行、追溯入口和权限诊断。
- 相邻 DCC 静态合同证明未破坏已有三栏分类树、上传治理、升版分类项目联动和文件编号可选逻辑。
- 真实 Playwright E2E 使用非 admin 文控账号 `wangsiyu`，完成 5 次文件类型修改和 DCC 项目代码修改，并在项目代码 item 详情三栏验证同步。
- E2E 结束后恢复原项目 `HGGW` 和原分类 `技术文档 / 设计和开发输出阶段 / 来料/过程/成品检验规范`。
- 响应式处理：影响预览和详情页联动卡片在窄屏改为单列/双列 grid，不影响表单保存链路。
- 加载/空状态：未绑定 DCC 项目代码时详情页按钮禁用并展示 `未绑定 DCC 项目代码`。
- 错误/权限状态：保留原后端错误暴露，针对文控权限失败增加明确诊断，不吞异常。

## Blockers

- 无功能验证 blocker。
- 收尾提交/推送未执行：当前工作区存在大量非本任务脏改动，需单独处理提交边界。
