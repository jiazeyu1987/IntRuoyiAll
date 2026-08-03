# Bug Regression Evidence

## Bug Summary

DCC controlled-file detail displays generic `系统异常` while previewing an active `.docx` controlled file.

## Expected Behavior

The preview path should return a valid viewer configuration for supported `.docx` files, or a precise preview-unavailable response. It must not hide the root cause behind a generic system exception.

## Reproduction

- Screenshot path: DCC `文控中心 > 受控浏览 > 受控文件详情`.
- Visible file: `STM-PM-002（A 0）微粒污染检测操作规程.docx`.
- Visible symptom: preview area shows `系统异常`.

## Root Cause

Pending isolation.

## Regression Test

Pending.

## RED Evidence

Pending.

## GREEN Evidence

Pending.

## Risk And Scope

- Scope is limited to DCC controlled-file preview behavior and the closest affected test(s).
- No fallback, mock success, or swallowed exception is allowed.

## Follow-Up

Pending final verification.
