# G68 native withdrawn revision correction

- Status: ready_for_closeout. Actual native B/1 withdrawal4043 ended but public checkout failed; old selected4042 cannot allocate B/1 again. Root owns actual UI/DB/Git/runtime, child sole bounded Java/Maven.
- Current inconsistency: RevisionReworkPolicy and draft initializer accept WITHDRAWN, but Query editable/checkin gates omit it. Legacy resubmit nextMinor would consume B/2 and is not this correction contract.

BDD: Given the actual public native REVISION submission and cancellation, its original requester and exact latest controlled baseline, When that requester checks out the withdrawn formal candidate and checks in corrected content, Then a new WORKING B/1-1 preserves the withdrawn B/1/history/body/attributes; selecting it creates independent B/1 attempt2 with the original failed predecessor and the G67 authoritative current relationship freeze. No original history deletion or legacy version advance.

BDD: Given a nonterminal, wrong-identity/type/tenant/actor, stale allocated target or parallel candidate, When checkout/checkin is attempted, Then normal permission/scope/lock and exact cancelled-history/lineage gates reject with zero writes. INITIAL, obsolete, legacy withdrawal keep their existing contract.

Implementation scope: exact native withdrawn revision predicate/current cancelled process evidence in Query; existing rework allocation/checkin guards remain; disable legacy delete/resubmit actions for this native formal revision and refuse its old direct resubmit rather than incrementing the target. Normal management checkout/checkin remains the real entry. No schema/platform/job/metadata/body permission grant.


## Actual evidence and final scope

- Effective RED: actual WF withdrawal completed with real Flowable history/CANCEL4, then real Query checkout returned CHECKOUT_NOT_ALLOWED;1test1error. No artificial failed-signature history inserted.
- After service predicate was connected, actual checkout CAS still excluded WITHDRAWN; two green attempts surfaced ALREADY_CHECKED_OUT from SQL CAS0. Earlier duplicate-checkout guess was corrected; this was a second production state guard, not fixture success. New dedicated CAS accepts only verified native REVISION/requester/exact original BPM/failed intent/no control/no successor/NULL lock with tenant/id/deletion constraints; old SQL unchanged.
- Native correction requires actual uploaded new bytes. It now proves B/1-1→B/1 attempt2/new BPM and preserves old WITHDRAWN/file identity/body/attributes/process. Official history read demands completed exact tenant/file/starter/key/CANCEL4, latest controlled baseline and allocated-target lineage. Live spoof, wrong actor/kind/key/baseline and older replaced attempt deny.
- Metadata excludes legacy delete/resubmit for native REVISION; the existing service old entry rejects rather than deleting history or consuming nextMinor. Legacy-only behavior retains its old projection/API contract. No new route/node/job/schema, no body permission grant.
- Final source-tree reactor command selected five classes: new3 +current relations5 +rejected retry2 +legacy Query3 +legacy Workflow1 =14/5, all0/CLI0/BUILD SUCCESS2026-10-06T06:17:25+08:00. Existing legacy withdrawn A/1→A/2 test remains green; counts not combined with prior overlapping runs. Five XML copied byte-for-byte before MAVEN_FREE_FROZEN.
- Source3 (Query/WF/Mapper) and one new test;24 affected main classes. This explicitly supersedes earlier Query/WF/Mapper pins only, retaining G64 rejected CAS and all G67 relation assets unchanged. Manifest g68-withdrawn-revision-rework-fingerprints.json SHA4708ce88beb368ca1b16aaefd4a04795ef674285a8ff806b86fd72180f260608; receipt8640bc3159f63916abde6a793b200326e14da0dc6ca482404235a421f9ceec09. Root independent review/package and actual4043 UI correction remain next. No child actual DB/API/UI/services/Git/package.
