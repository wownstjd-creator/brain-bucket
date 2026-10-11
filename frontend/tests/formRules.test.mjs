import test from 'node:test';
import assert from 'node:assert/strict';
import { optionalDateTime, optionalUrl } from '../src/capture/formRules.ts';
test('optional dates: absent stays null, date-only events and deadlines use distinct defaults',()=>{
 assert.equal(optionalDateTime('', ''), null);
 assert.equal(optionalDateTime('2026-10-11', ''), '2026-10-11T00:00:00');
 assert.equal(optionalDateTime('2026-10-11', '', true), '2026-10-11T23:59:00');
 assert.equal(optionalDateTime('2026-10-11', '18:30', true), '2026-10-11T18:30:00');
 assert.throws(()=>optionalDateTime('', '18:30'),/날짜/);
});
test('empty and whitespace application links are null, provided links are trimmed',()=>{
 assert.equal(optionalUrl(''), null); assert.equal(optionalUrl('   '), null);
 assert.equal(optionalUrl(' https://example.com/apply '), 'https://example.com/apply');
});
