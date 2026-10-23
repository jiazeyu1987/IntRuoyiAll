# G44 upload submit-directory readiness review

Readonly source review for Root's task-owned upload fixture. No directory/DB/API/browser/service/Maven/Git action was performed by this reviewer.

## Exact directory-management path

1. Open DCC **目录管理** (`controlled-file/directories`). `DirectoryForm.vue` fields are 上级目录, 目录编码, 目录名称, 启用状态, 排序, 备注. The page's “新建目录” button is permission-gated by `dcc:controlled-file:directory:manage`; access-rule maintenance is separately gated by `dcc:controlled-file:access-rule:manage`.
2. Select the appropriate existing `qualityroot` parent (Root must use the actual visible tree identity). For the task folder choose a unique task marker in code/name, active=true, and save. The form checks parent-cycle/unknown-parent in the current tree before POST.
3. The POST is `/dcc/directories`; `DccDirectoryController.createDirectory` requires `dcc:controlled-file:directory:manage` and `DccDirectoryAdminServiceImpl.createDirectory:152–159` validates hierarchy, inserts one `dcc_file_directory` row and sets `accessRuleManuallyBound=false`. Directory creation is a database directory-tree record only. It does **not** call a NAS client, `Files.createDirectories`, `FileService`, ACL writer or physical storage mkdir. Do not describe this UI action as proof that a physical NAS path or OS permission exists.
4. Grant/read the actual directory access rule through the separate visible access-rule button/page only if this is required by the task actor. `DccDirectoryAccessPermissionServiceImpl` matches USER/DEPT/POSITION/ROLE active rules for QUERY/PREVIEW/DOWNLOAD. A management permission alone returns the full directory tree to the manager, but a normal uploader needs an active visible QUERY/PREVIEW rule or the project-assignment path. Capture actual account, subject type/id, directory id, access flags and tenant from the UI/approved read-only evidence; never invent a role or copy admin permission.

## Category leaf and upload binding

The upload page loads `getControlledFileUploadDirectoryTree(categoryId)` after the selected file category. It displays “最终提交目录”; if `leafBinding` exists it preselects the binding directory, otherwise it requires the user to choose a final leaf below the bound directory. Source anchors: `upload/index.vue:206–230, 1699–1775, 1929–1935, 2543–2551`.

Backend `DccControlledFileWorkflowServiceImpl.validateSelectedDirectory:2124–2144` loads enabled directories, requires the category binding directory to exist, requires the selected ID inside its subtree, and when `requireLeaf=true` rejects a directory with children. Therefore Root must choose the **last leaf**, not `qualityroot` or a non-leaf category binding. `directoryId` is a database directory identity; the service does not independently prove a real physical NAS folder at this point.

The category mapping must point to the final task leaf through the formal category-directory configuration used by `getControlledFileUploadDirectoryTree`. The current category research says the quality root has candidate leaf categories `1 QMS`, `2 DHF`, `3 DMR`, `4 Other`; those labels are not enough. Root must read the actual enabled category/leaf mapping and choose the permitted category whose binding subtree contains the new task leaf. A 2DHF/in-progress category with only another project's leaf must be rejected; never submit into another project's directory to make a test pass.

## Physical storage fact and accepted evidence

The upload submit validation proves DB directory binding/subtree/leaf identity and upload ticket/session; finalization later uses the infrastructure `FileService`/storage path for the source and stamped copy. Directory creation itself does not mkdir a NAS path. Root should therefore record two separate facts:

- `directoryRecord`: actual tenant/category binding/leaf ID/path and access rule visible through approved UI/read-only evidence;
- `physicalStorage`: actual upload/finalization result showing the task file's storage object/path was created by the normal upload/finalization flow, with no direct NAS/API/SQL seed.

If a physical submit directory is a hard business prerequisite beyond the current code contract, it is a product/runtime gap to report. Do not add a filesystem creation workaround in this acceptance run.

## Exact task-owned setup order

1. Root verifies current slot6 frontend/backend/Jar ownership and the actual enabled directory-management account.
2. Via DCC 目录管理 UI, create one unique task parent/leaf under the permitted quality root; capture resulting directory IDs and names. If the existing mapping forbids the selected category, stop and use a formally enabled category/leaf or report the missing mapping.
3. Via access-rule UI, grant only the task actor the minimum actual QUERY/PREVIEW (and upload/category permission through its formal category/page configuration). Re-read the tree as that actor; a visible but non-selectable/non-leaf path is not accepted.
4. Via formal category-directory mapping/type UI, bind the selected file category to the new leaf if the current mapping is absent; capture category/leaf identity and active state. Do not alter another project's existing leaf.
5. Proceed through normal project/product creation and upload page. Select the category, verify the prefilled binding/path, select the final leaf, and ensure the confirmation summary contains the actual directory path. Upload a real task-owned file; only then observe the normal storage/ticket result.

## Current conclusion

No source blocker prevents a task-owned **DB directory leaf** from being created and selected when the category binding and access rules are configured. The source does not prove a physical NAS directory is created by DirectoryForm; physical storage is established only by the normal upload/finalization flow. Root must keep this distinction in the main acceptance matrix. This review does not claim any UI/runtime setup or business PASS.
