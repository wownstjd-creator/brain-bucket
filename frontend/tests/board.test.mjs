import test from 'node:test';
import assert from 'node:assert/strict';
import {posterBadge,readSaved,koreaDay} from '../src/boardLogic.ts';
test('Korea midnight moves expired posters into archive even in another time zone',()=>{
 const now=new Date('2026-10-09T15:00:00Z');
 assert.equal(koreaDay(now),'2026-10-10');
 assert.equal(posterBadge({postingEndsAt:'2026-10-09'},now).text,'지난 전단');
});
test('application deadline takes precedence over NEW and posting expiry',()=>{
 const now=new Date('2026-10-09T03:00:00Z');
 assert.equal(posterBadge({postingEndsAt:'2026-10-30',deadlineAt:'2026-10-11T18:00:00',publishedAt:'2026-10-09T10:00:00'},now).text,'신청 D-2');
 assert.equal(posterBadge({postingEndsAt:'2026-10-30',deadlineAt:'2026-10-09T10:00:00'},now).text,'신청 종료');
 assert.equal(posterBadge({postingEndsAt:'2026-10-09'},now).text,'게시 오늘 마감');
});
test('NEW uses actual publishing time, never fabricated from start date',()=>{
 const now=new Date('2026-10-09T03:00:00Z');
 assert.equal(posterBadge({postingEndsAt:'2026-10-30',publishedAt:'2026-10-08T18:00:00'},now).text,'NEW');
 assert.equal(posterBadge({postingEndsAt:'2026-10-30',postingStart:'2026-10-09'},now),null);
 assert.equal(posterBadge({postingEndsAt:'2026-10-30',publishedAt:'2026-10-10T18:00:00'},now),null);
});
test('saved IDs tolerate corrupt storage and exclude demo or arbitrary paths',()=>{
 assert.deepEqual(readSaved({getItem:()=>'{bad'}),[]);
 assert.deepEqual(readSaved({getItem:()=>JSON.stringify(['12','12','demo-0','../admin',3,'7'])}),['12','7']);
 assert.deepEqual(readSaved({getItem:()=>{throw new Error('blocked');}}),[]);
});
