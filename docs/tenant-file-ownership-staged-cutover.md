# 文件归属分阶段切换

当历史文件归属尚未全部核验时，使用 `20261117_tenant_file_ownership_prepare.sql` 完成结构准备和已核验记录的迁入。该迁移依赖 `20261112_tenant_file_configuration.sql`，不依赖全量归属完成。

`infra_file.tenant_id`、`infra_file_content.tenant_id` 在这一阶段允许 NULL。NULL 明确表示待核验，现有 `@TenantIsolated` 查询不会把它作为任一租户的文件返回；不能用默认租户、忽略租户或全局查询恢复其可见性。正常应用写入仍必须携带真实租户上下文。

迁移仅使用 `infra_file_tenant_owner` 中的已核验证据。执行前必须核对每项证据的业务引用、租户、文件配置，以及同一配置和完整对象路径下所有元数据记录的归属。创建人账号或相似文件名不能单独作为归属依据。跨租户共享、配置缺失、证据不完整的整组对象保持待核验。

迁移拒绝未知租户、空证据、缺少文件配置、同一物理键有未核验记录或不同租户、已有归属与证据冲突。只填充原来为 NULL 的归属，保留文件编号、内容字节和原始更新时间，重复执行不覆盖已有归属。MySQL DDL 独立提交；执行失败后应核对实际状态，不能宣称整个操作回滚。

准备迁移不会创建或写入 `infra_file_tenant_migration` 的全量完成标记。全量历史归属核验、必要的对象独立复制和引用分离完成后，仍须满足 `20261112_tenant_file_ownership.sql` 的全部前置，再收紧为 NOT NULL 并标记完成。准备迁移通过不代表历史数据全量迁移或对象复制完成。

本机本次实际结果：40,785 条文件按直接业务引用落实归属（租户 1：40,761；租户 122：24）；13,940 条保留 NULL，其中包含 1,165 个跨租户引用文件和 288 条存储配置为 0 的旧测试文件。没有移动、复制或删除物理对象。原始文件元数据的前后 SHA-256 相同。

验证：独立 MySQL 8.0.40 的 3 项测试通过，覆盖待核验文件不可按租户查询、已有归属冲突与未知租户拒绝、物理键不完整证据拒绝、重复执行与内容保留。目标迁移依赖闭包策略检查通过。真实 Playwright 页面验证通过登录、个人工作台、1,576 条站内信和受控浏览 147 条文件；未验收完整历史、上传、下载或签名放行流程。

本机详细证据和原机器取证 SQL 位于 `.runtime/int_qms/`：`file-ownership-prepare-result.json`、`verified-file-manifest.json`、`pending-missing-file-config.json`、`gxp-disabled-restored-smoke.json`、`original-machine-file-evidence.sql`。原始表备份 `before-ownership-prepare.sql` 含文件元数据，不应公开传播。
