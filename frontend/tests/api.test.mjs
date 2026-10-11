import test from 'node:test';
import assert from 'node:assert/strict';
import {api,ApiError} from '../src/capture/api.ts';
const originalFetch=globalThis.fetch;
const response=(status,body)=>new Response(body===undefined?null:JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
test('draft save obtains CSRF, uses session cookies, and does not retry writes',async()=>{
  const calls=[];globalThis.fetch=async(url,options)=>{calls.push({url,options});return url.endsWith('/csrf')?response(200,{headerName:'X-CSRF-TOKEN',token:'test-token'}):response(201,{id:7,title:'학술제',status:'DRAFT'});};
  try{const payload={title:'학술제',organizer:'학술팀',category:'행사',description:'소개',location:'로비',applicationUrl:'',eventAt:null,deadlineAt:null,showViewCount:true};const saved=await api.save(payload);assert.equal(saved.status,'DRAFT');assert.equal(calls.length,3);assert.equal(calls[2].url,'/api/posts/mine/7');assert.equal(calls.filter(c=>c.options.method==='POST').length,1);assert.equal(calls[1].url,'/api/posts');assert.equal(calls[1].options.credentials,'include');assert.equal(calls[1].options.headers['X-CSRF-TOKEN'],'test-token');assert.deepEqual(JSON.parse(calls[1].options.body),{...payload,applicationUrl:null,category:'EVENT'});}finally{globalThis.fetch=originalFetch;}
});
test('logout accepts empty 204; next mutation obtains a fresh CSRF token',async()=>{
  const calls=[];globalThis.fetch=async(url)=>{calls.push(url);return url.endsWith('/csrf')?response(200,{headerName:'X-CSRF-TOKEN',token:'fresh-token'}):response(204);};
  try{await api.logout();await api.logout();assert.equal(calls.filter(url=>url.endsWith('/csrf')).length,2);}finally{globalThis.fetch=originalFetch;}
});
test('401 and empty error bodies produce a readable error',async()=>{
  globalThis.fetch=async()=>response(401);
  try{await assert.rejects(api.me(),error=>error instanceof ApiError&&error.status===401);}finally{globalThis.fetch=originalFetch;}
});
test('public collection follows pages and maps backend categories and dates',async()=>{
  const previousLocation=globalThis.location;globalThis.location={origin:'http://localhost:5175'};
  const calls=[];globalThis.fetch=async(url)=>{calls.push(url);const page=String(url).includes('page=1');return response(200,Array.from({length:page?1:20},(_,i)=>({id:page?21:i+1,title:'test',category:'CLUB',displayFrom:'2026-10-01',displayUntil:'2026-11-30',description:'Details',publishedAt:'2026-10-10T10:00:00'})));};
  try{const {request}=await import('../src/capture/api.ts');const list=await request('/api/public/posts?archived=false');assert.equal(list.length,21);assert.equal(list[0].category,'동아리');assert.equal(list[0].postingStart,'2026-10-01');assert.equal(list[0].description,'Details');assert.equal(list[20].id,'21');assert.equal(calls.length,2);}finally{globalThis.fetch=originalFetch;globalThis.location=previousLocation;}
});

test('API error reasons reach the user for both JSON and ProblemDetail responses',async()=>{
 try {
  globalThis.fetch=async()=>response(400,{message:'게시 시작일은 종료일 이후일 수 없습니다.'});
  await assert.rejects(api.me(),/게시 시작일/);
  globalThis.fetch=async()=>response(409,{detail:'다른 변경이 있습니다. 새로고침해주세요.'});
  await assert.rejects(api.me(),/다른 변경/);
 } finally {globalThis.fetch=originalFetch;}
});
