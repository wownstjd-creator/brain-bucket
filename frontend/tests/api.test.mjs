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
test('public collection fetches only the requested page and maps backend fields',async()=>{
  const previousLocation=globalThis.location;globalThis.location={origin:'http://localhost:5175'};
  const calls=[];globalThis.fetch=async(url)=>{calls.push(url);const page=String(url).includes('page=1');return response(200,Array.from({length:page?1:20},(_,i)=>({id:page?21:i+1,title:'test',category:'CLUB',displayFrom:'2026-10-01',displayUntil:'2026-11-30',description:'Details',publishedAt:'2026-10-10T10:00:00'})));};
  try{const {request}=await import('../src/capture/api.ts');const list=await request('/api/public/posts?archived=false');assert.equal(list.length,20);assert.equal(list[0].category,'동아리');assert.equal(list[0].postingStart,'2026-10-01');assert.equal(list[0].description,'Details');assert.equal(calls.length,1);const next=await request('/api/public/posts?archived=false&page=1&size=20');assert.equal(next[0].id,'21');assert.equal(calls.length,2);}finally{globalThis.fetch=originalFetch;globalThis.location=previousLocation;}
});

test('API error reasons reach the user for both JSON and ProblemDetail responses',async()=>{
 try {
  globalThis.fetch=async()=>response(400,{message:'게시 시작일은 종료일 이후일 수 없습니다.'});
  await assert.rejects(api.me(),/게시 시작일/);
  globalThis.fetch=async()=>response(409,{detail:'다른 변경이 있습니다. 새로고침해주세요.'});
  await assert.rejects(api.me(),/다른 변경/);
 } finally {globalThis.fetch=originalFetch;}
});

test('password changes use a fresh CSRF token and do not repeat the write',async()=>{
 const calls=[];globalThis.fetch=async(url,options)=>{calls.push({url,options});return url.endsWith('/csrf')?response(200,{headerName:'X-CSRF-TOKEN',token:'token'}):response(204);};
 try{await api.changePassword('old','new-password');assert.equal(calls.length,2);assert.equal(calls[1].url,'/api/auth/password');assert.equal(calls[1].options.credentials,'include');assert.deepEqual(JSON.parse(calls[1].options.body),{currentPassword:'old',newPassword:'new-password'});}finally{globalThis.fetch=originalFetch;}
});
test('admin cards do not fan out into per-post detail calls',async()=>{
 const calls=[];globalThis.fetch=async(url)=>{calls.push(url);return response(200,[{id:7,category:'CLUB',status:'PENDING',displayUntil:'2026-11-30',version:3,hasEvidence:true}]);};
 try{const {request}=await import('../src/capture/api.ts');const result=await request('/api/admin/posts/cards?page=0&size=20');assert.equal(calls.length,1);assert.equal(result[0].version,3);assert.equal(result[0].imageUrl,'/api/admin/posts/7/poster');assert.match(result[0].evidenceUrl,/evidence$/);}finally{globalThis.fetch=originalFetch;}
});

test('bookmark IDs and archive years retain numeric values',async()=>{
 globalThis.fetch=async()=>response(200,[2026,2025]);
 try{const {request}=await import('../src/capture/api.ts');assert.deepEqual(await request('/api/posts/public/years'),[2026,2025]);}finally{globalThis.fetch=originalFetch;}
});

test('review approval and rejection preserve the revision actually reviewed',async()=>{
 const calls=[];globalThis.fetch=async(url,options)=>{calls.push({url,options});return url.endsWith('/csrf')?response(200,{headerName:'X-CSRF-TOKEN',token:'token'}):response(200,{id:7,status:'PENDING'});};
 try{const {request}=await import('../src/capture/api.ts');
  await request('/api/admin/posts/7/review','POST',{decision:'APPROVED',reason:'',version:3});
  await request('/api/admin/posts/7/review','POST',{decision:'REJECTED',reason:'fix',version:7});
  const writes=calls.filter(c=>c.options.method==='POST');assert.equal(writes.length,2);
  assert.equal(writes[0].url,'/api/admin/posts/7/approve');assert.deepEqual(JSON.parse(writes[0].options.body),{version:3});
  assert.equal(writes[1].url,'/api/admin/posts/7/reject');assert.deepEqual(JSON.parse(writes[1].options.body),{reason:'fix',version:7});
 }finally{globalThis.fetch=originalFetch;}
});
test('approval requests preserve the saved draft revision',async()=>{
 const calls=[];globalThis.fetch=async(url,options)=>{calls.push({url,options});return url.endsWith('/csrf')?response(200,{headerName:'X-CSRF-TOKEN',token:'token'}):response(200,{id:7,status:'PENDING'});};
 try{await api.submit('7',12);assert.equal(calls[1].url,'/api/posts/mine/7/approval-request?version=12');assert.equal(calls[1].options.method,'POST');}finally{globalThis.fetch=originalFetch;}
});
