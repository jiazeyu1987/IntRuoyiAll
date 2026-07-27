# Execution Log

## User Intent

- Bug: clicking confirm in the "open or create eDHR batch execution" dialog fails with `批记录附件负责人配置无效：batchRecordAttachmentOwners`.

## BDD / TDD

- BDD: valid batch record attachment owners should not block batch execution confirm -> Given a route BATCH configuration with a valid attachment owner list, When the user confirms opening or creating an eDHR batch execution, Then the backend accepts the owner configuration and creates or opens the execution instead of rejecting the field key.

## Commands And Evidence

- GREEN: experience-preflight -> PASS, read task, backend, PowerShell, bug-regression, backend-api, and experience-index rules before implementation.

## Blockers

- None yet.
