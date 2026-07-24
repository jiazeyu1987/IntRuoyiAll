import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

const source = readFileSync(new URL('../src/layout/components/ToolHeader.vue', import.meta.url), 'utf8');

test('ToolHeader renders tenant visit selector for authorized users', () => {
  assert.match(source, /TenantVisit/);
  assert.match(source, /system:tenant:visit/);
  assert.match(source, /canVisitTenant/);
});

test('ToolHeader gates tenant visit selector behind explicit tenant-visit permission or all permission', () => {
  assert.match(source, /'\*:\*:\*'/);
  assert.match(source, /'system:tenant:visit'/);
  assert.match(source, /'super_admin'/);
  assert.match(source, /canVisitTenant\.value\s*\?/);
});
