import test from 'node:test';
import assert from 'node:assert/strict';
import {extractPoster} from '../src/capture/extract.ts';
import {perspectiveMap, validCorners} from '../src/capture/geometry.ts';

test('event, application deadline and posting dates are separate',()=>{
  const result=extractPoster('제목: 교내 사진전\n주최: 사진 동아리\n장소: 학생회관\n행사 일시: 2026.10.20 오후 2시\n접수 마감: 2026.10.15 18:00\n게시 기간: 2026.10.01 ~ 2026.10.21',2026);
  assert.equal(result.fields.title,'교내 사진전');assert.equal(result.fields.eventDate,'2026-10-20');assert.equal(result.fields.eventTime,'14:00');
  assert.equal(result.fields.deadlineDate,'2026-10-15');assert.equal(result.fields.deadlineTime,'18:00');assert.equal(result.fields.postingStart,'2026-10-01');assert.equal(result.fields.postingEnd,'2026-10-21');
});
test('missing year is marked and missing time or posting date is not fabricated',()=>{
  const result=extractPoster('접수 기간: 10/01 ~ 10/15\n행사 일시: 10월 20일',2026);
  assert.equal(result.fields.deadlineDate,'2026-10-15');assert.equal(result.fields.eventDate,'2026-10-20');assert.equal(result.fields.eventTime,undefined);assert.equal(result.fields.postingEnd,undefined);
  assert.ok(result.warnings.some(s=>s.includes('연도가 없는')));
});
test('invalid dates and unrelated dates do not become event dates',()=>{
  const result=extractPoster('행사 일시: 2026.02.30\n문서 작성: 2026.10.01',2026);
  assert.equal(result.fields.eventDate,undefined);assert.equal(result.fields.postingEnd,undefined);
});
test('label on preceding line and noon/midnight',()=>{
  const result=extractPoster('행사 일시\n2026.10.20 오전 12시\n신청 마감: 2026.10.19 오후 12시',2026);
  assert.equal(result.fields.eventTime,'00:00');assert.equal(result.fields.deadlineTime,'12:00');
});
test('quadrilateral corners map accurately and bow ties are rejected',()=>{
  const points=[{x:.1,y:.15},{x:.8,y:.05},{x:.95,y:.9},{x:.2,y:.8}];const map=perspectiveMap(points);
  [[0,0],[1,0],[1,1],[0,1]].forEach(([u,v],i)=>{const p=map(u,v);assert.ok(Math.abs(p.x-points[i].x)<1e-8);assert.ok(Math.abs(p.y-points[i].y)<1e-8);});
  assert.equal(validCorners([points[0],points[2],points[1],points[3]]),false);
  assert.throws(()=>perspectiveMap([{x:0,y:0},{x:0,y:0},{x:0,y:0},{x:0,y:0}]));
});
