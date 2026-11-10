# 问题 2 / CR-029 验收结果

结果：PASS。引用存在时准确禁止源文件升版；逐个取消引用后恢复检出、检入和局部变更准备。引用副本取消后通过受控上传产生独立逻辑文件。受控导入来源首次检入建立本次升版合同，自定义类别产生独立申请配置，真实页面保存并重开一致。

## 运行与数据边界

- 仅任务 worktree 前端 `8063`、后端 `48063`，最终运行包 `2026-11-06-21-35-19`。
- 租户“芋道源码”。业务动作全部为 Playwright 页面操作；仅观察页面自然产生的响应，没有 API 造数据、API 业务动作或数据库写入。
- 原源文件 `2054545668044084577` / `WORD1106-REF-DOC`，SOURCE 项目；TARGET 的 F1/F2 两引用由问题 1 先完成正文权限验收后移交。
- 新上传替代文件 `2054545668044084579` / `WORD1106-REPLACEMENT-DOC`，TARGET/F1，master `2054545668044079390`，不同于源 master `2054545668044079388`。
- 原源文件真实检入生成 `2054545668044084581` / A/1-1，其编制人 admin 缺直属主管，准确阻断审批预览；保留这一业务边界，没有修改 admin 主管或绕过验证。
- 为继续完整自定义保存验收，使用本任务自有 Other 类型 `206` / 类别 `908777`（APPLICATION_CUSTOM），UI 明确绑定质量管理，并仅为 admin、Reviewer 增加本类别 EDIT。
- 自有新源 `2054545668044084587` / `WORD1106-CUSTOM-OWN206-REVISION`，SOURCE/F1。任务账号 Reviewer 首次检出、检入产生 `2054545668044084588` / A/1-1，requester `910538`，master `2054545668044079397`。
- Reviewer 经页面补既有“普通员工”岗位，直属主管与项目/类别权限由本任务页面配置。未修改旧类别 `906104` 权限；辅助导入 `4585/4586` 保留且未参与最终自有类别验收。

## 真实页面证据

证据根目录：`output/word1106-e2e/issue2/`。

| 节点 | 页面行为及读回 | 证据 |
| --- | --- | --- |
| 两引用存在 | 局部变更、换版均显示“源文件仍被引用……取消全部引用关系” | `03-two-references-result.json` 及截图 |
| 取消首个 | TARGET/F1 真实取消确认；剩一个引用时两种变更仍禁止 | `04-cancel-first-result.json`、`05-one-reference-result.json` |
| 取消最后一个 | TARGET/F2 真实取消确认；两种变更恢复“前往受控浏览准备工作正文” | `06-cancel-last-result.json`、`07-all-cancelled-result.json` |
| 引用副本需要新版本 | 取消后真实受控上传新文件；目标目录为普通文件行，属性/版本读回证明不同 master | `08-replacement-result.json`、`13-independent-master-result.json` |
| 原源真实正文准备 | 页面检出、修改 PDF 并检入成功，版本历史确认 4577→4581，A/1-1 | `14-source-checkin-confirmation.png`、`15-created-working-body.json`、`15-source-and-working-version-history.png` |
| 真实组织前置条件 | 4581 自定义入口恢复；admin 未配置主管准确拒绝，不称该申请已保存 | `admin-supervisor-boundary/` |
| 自有类别与账号前置 | 页面保存已有岗位、质量管理绑定、本类别两 EDIT 规则 | `23-reviewer-existing-post-saved.png`、`25-owned-other-directory-bound.png`、`27-owned-other-edit-rules-saved.png` |
| 自有首次检出/检入 | Reviewer 实际检出 4587、上传修改后 PDF、检入产生自身 4588 工作正文 | `29-reviewer-source-checked-out.png`、`30-reviewer-checkin-confirmation.png`、`31-reviewer-created-working-body.json` |
| 本次独立 custom 保存 | 页面选择 SIGNOFF `910380`、批准 admin、文控 admin、生效方式“文控审核后立即生效”、培训不需要、下发空名单；真实 POST 保存成功 | `19-custom-revision-draft-confirmation.png`、`20-custom-revision-draft-saved.png` |
| 重开核对 | 原源局部变更面板重新选择 4588；回显一致，`canSubmit=true`、`unavailableReason=null`；独立 tenant/file/round 上下文与摘要 | `21-custom-revision-draft-reopened.png`、`21-custom-revision-draft-result.json` |

4588 的正式配置上下文为 `dcc-custom-v1:1:REVISION:2054545668044084588:1`，摘要 `add4fd7449c5901ede257a9228cbfb6c1feb8f3c355f6ba478ef94c324603dae`。本次验收到可提交的正式草稿保存与重开，未扩大为完整审批、签名、发布。

## 实现与调用链依据

- `DetailApplicationPanel.vue` / `revision-model.ts` / `DccProjectReferences.vue`：引用限制提示、局部变更准备前读取正式锁定事实、取消引用后新上传的规则说明。
- `DccControlledFileQueryServiceImpl`：revision options 公开准确 lockedReason；checkout、checkin 在同 master 锁后检查 tenant 内全部引用。checkin 拒绝时尚未写新 file/version/contract 或消耗上传载荷，覆盖“检出后又新增引用”的竞态。
- `DccControlledFileWorkflowServiceImpl`：读取、准备、预览、保存均经工作申请身份/权限/状态/引用检查；`readFreshCustomApplication` 只对执行合同已证明的 fresh new-action、无已存模式与公共基线读取正式类别，生成只读独立空 custom 上下文，GET 不写回；保存复用原有 reserved token/digest/context 校验与 POST 持久化。
- `DccControlledFileRevisionServiceImpl`：正式变更提交在同 master 锁后再次禁止引用。
- `DccControlledFileMapper` / `ErrorCodeConstants`：tenant+master 引用查询与准确业务错误；引用创建/取消既有 master 排序锁与上述检查一致。
- `DccApplicationExecutionContractService`：CONTROLLED_IMPORT 明确来源分支同时校验正式 tenant/master/current ACTIVE/已生效、导入事实与 artifact ownership；仅合法首次新申请可没有源 BPM 属性轮次。新升版锁定正式 V2 定义，后续复检入继承既有合同。普通历史源仍要求真实 BPM/round，不用 importSource 单标记绕过。
- H2 create/clean 仅补本次引用表和 source ownership 清理，避免 fixture 污染；未执行业务数据库写入。

## 集中验证

以下为各次有针对性的运行，存在重复覆盖，不合计为独立用例总数。

- `agent2-maven-test.log`：RevisionService 70 + Query 聚焦 2，72/72 PASS。
- `agent2-database-test-focused.log`：真实 H2 引用限制与逐个解除 2/2 PASS。
- `agent2-import-regression.log`：新申请合同完整类 83 + 原 checkin 1，84/84 PASS，含合法导入和 8 类异常来源。
- `agent2-checkin-reference-test.log`：checkin 正常及并发新增引用拒绝 2/2 PASS。
- `agent2-custom-projection-final-test.log`：完整新申请基线类 88/88 PASS。
- `agent2-custom-projection-final-focused.log`：最后提示顺序改动后的生产编译及新增 5/5 PASS；覆盖双入口、无写回、own tenant/round/context、真实保存与重开、已有 PUBLIC 不自动转换。
- 前端 `dcc-reference-revision-lock.test.cjs`：1/1 PASS。
- 上述 Maven 日志位于 `C:/Users/D01020/AppData/Local/Temp/word1106-review/`。

最终没有重置、提交、推送，没有改动主运行服务；最后 Reviewer 浏览器已关闭，source Maven 已释放。
