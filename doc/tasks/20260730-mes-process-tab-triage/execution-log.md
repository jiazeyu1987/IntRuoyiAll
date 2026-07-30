# Execution Log

## User Intent

用户提出：在“工序设置”页签和“工艺流程”页签之间增加一个“MES工序”标签。标签中是一个列表，用于表示设备、MES 工序和工序设置中工序的对应关系。用户询问助手是否理解，以及当前方案有什么问题。

用户进一步收窄范围：暂不考虑维护，只显示列表；只有设备和工序执行所使用的工序建立关联，其它数据只列出来，不考虑关联问题。

## BDD Scenarios

BDD: distinguish frontline MES process from formal process -> Given the current process settings object already represents the formal process used by routes and batch-record bindings, When the MES process tab is defined, Then it represents a separate frontline reporting process and maps explicitly to the formal process.

BDD: reuse equipment and formal process masters -> Given machinery and formal process master data already exist, When MES process mappings are maintained, Then the page selects existing machinery and process records rather than duplicating their identities.

BDD: preserve formal batch-record binding -> Given formal batch-record forms are bound per process or route process, When an MES process maps to a formal process, Then the batch-record relationship is read from the formal binding and is not independently reconfigured on the MES process.

BDD: block ambiguous relationship rules -> Given product scope, mapping cardinality, device usage mode, capacity ownership, and historical change behavior are not confirmed, When implementation is requested, Then the change remains accepted but implementation-blocked instead of guessing defaults.

BDD: render a read-only MES process catalog -> Given the user removed maintenance and downstream linkage from the first version, When the MES process tab opens, Then it displays second-generation pressure-pump rows without create, edit, delete, import or business-action controls.

BDD: limit formal associations -> Given only equipment and execution process require formal links, When a row is returned, Then device IDs and execution process ID are structured associations while all remaining columns are plain display snapshots.

## Command And Evidence Log

- GREEN: inspect current menu SQL -> PASS, `工序设置` uses menu sort `2` and `工艺流程` uses sort `3` under the same parent, so a sibling menu can be inserted between them.
- GREEN: inspect current process page -> PASS, current `mes_pro_process` list already shows formal process code/name, route membership, relation summary, workstations and batch-record forms.
- GREEN: inspect current route process API -> PASS, route-process responses already include workstations and machinery lists.
- GREEN: inspect current equipment relationships -> PASS, existing machinery master, workstation-machine binding and machinery-process capacity data can be reused.
- RED: separate MES process model readiness -> FAIL as expected, the current system does not expose a separate frontline MES process identity mapped to the existing formal process.
- GREEN: write `docs/changes/20260730-mes-process-tab.md` -> PASS, recorded request, baseline, impact, decision, downstream routing and blockers.
- GREEN: `validate_change_request.py` -> PASS, change request evidence contains all required sections.
- GREEN: UTF-8 and boundary keyword verification -> PASS.
- GREEN: `git diff --check` -> PASS, no whitespace errors.
- GREEN: `task_closeout.py --mode preview/apply` -> PASS, kept the three core task documents; delete none, blocked none.
- GREEN: staged file boundary review -> PASS, staged list contained only the change request and three task documents.
- GREEN: change assessment commit `ec66b3e2 docs: assess MES process mapping tab` -> PASS.
- GREEN: apply user scope reduction -> PASS, changed the decision to `ACCEPT`, removed maintenance and downstream business linkage, and retained only equipment/execution-process associations.

## Findings

- The UI menu insertion is straightforward.
- The main risk is domain naming: current code already calls `mes_pro_process` a MES production process, while the new user concept is a finer frontline reporting process.
- The new page is a read-only catalog, not a maintenance workspace.
- Only machinery and execution process are structured associations.
- Product, capacity, manpower, price, report flags and batch-record labels remain plain display snapshots.

## Blockers

- No product requirement blocker remains.
- Implementation must fail clearly when a source device or execution process cannot be uniquely matched; it must not guess IDs.

## Milestone Updates

- Classified the request as a product requirement and domain-model extension.
- Initially recommended `ACCEPT_WITH_BLOCKERS`, then updated the decision to `ACCEPT` after the user removed maintenance and nonessential associations.
- Proposed a first-version list and reuse boundaries.
- Recorded seven business blockers that require user confirmation before implementation.
- Completed cleanup preview/apply without deleting task evidence.
- Committed the change assessment as `ec66b3e2`.
- Reduced the first version to a read-only catalog with only equipment and execution-process associations.
