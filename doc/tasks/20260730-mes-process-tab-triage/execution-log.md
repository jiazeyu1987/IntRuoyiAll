# Execution Log

## User Intent

用户提出：在“工序设置”页签和“工艺流程”页签之间增加一个“MES工序”标签。标签中是一个列表，用于表示设备、MES 工序和工序设置中工序的对应关系。用户询问助手是否理解，以及当前方案有什么问题。

## BDD Scenarios

BDD: distinguish frontline MES process from formal process -> Given the current process settings object already represents the formal process used by routes and batch-record bindings, When the MES process tab is defined, Then it represents a separate frontline reporting process and maps explicitly to the formal process.

BDD: reuse equipment and formal process masters -> Given machinery and formal process master data already exist, When MES process mappings are maintained, Then the page selects existing machinery and process records rather than duplicating their identities.

BDD: preserve formal batch-record binding -> Given formal batch-record forms are bound per process or route process, When an MES process maps to a formal process, Then the batch-record relationship is read from the formal binding and is not independently reconfigured on the MES process.

BDD: block ambiguous relationship rules -> Given product scope, mapping cardinality, device usage mode, capacity ownership, and historical change behavior are not confirmed, When implementation is requested, Then the change remains accepted but implementation-blocked instead of guessing defaults.

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

## Findings

- The UI menu insertion is straightforward.
- The main risk is domain naming: current code already calls `mes_pro_process` a MES production process, while the new user concept is a finer frontline reporting process.
- The new page should own only MES process identity and mappings; machinery master, formal process master and batch-record form binding remain existing sources of truth.
- Existing equipment relations are reusable as evidence and selection sources, but a finer MES process may require its own explicit device-binding relation when multiple MES processes map to one formal process.

## Blockers

- The scope key for an MES process is not confirmed: global, product, packaging variant, route, or route version.
- Mapping cardinality is not formally confirmed.
- Slash-separated or multiple equipment usage does not yet distinguish simultaneous use from alternative selection.
- Capacity ownership is not confirmed: MES-process total, device-level capacity, or both.
- Historical records need a rule for mapping changes, disablement and deletion.
- It is not confirmed whether the first version needs manual CRUD, Excel import, or both.

## Milestone Updates

- Classified the request as a product requirement and domain-model extension.
- Recommended `ACCEPT_WITH_BLOCKERS`.
- Proposed a first-version list and reuse boundaries.
- Recorded seven business blockers that require user confirmation before implementation.
- Completed cleanup preview/apply without deleting task evidence.
- Committed the change assessment as `ec66b3e2`.
