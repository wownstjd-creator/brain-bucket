import test from 'node:test';
import assert from 'node:assert/strict';
import {api,ApiError} from '../src/capture/api.ts';
const originalFetch=globalThis.fetch;
const response=(status,body)=>new Response(body===undefined?null:JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
test('draft save obtains CSRF, uses session cookies, and does not retry writes',async()=>{
  const calls=[];globalThis.fetch=async(url,options)=>{calls.push({url,options});return url.endsWith('/csrf')?response(200,{headerName:'X-CSRF-TOKEN',token:'test-token'}):response(201,{id:7,title:'학술제',status:'DRAFT'});};
  try{const payload={title:'학술제',organizer:'학술팀',category:'행사',description:'소개',location:'로비',applicationUrl:'',eventAt:null,deadlineAt:null,showViewCount:true};const saved=await api.save(payload);assert.equal(saved.status,'DRAFT');assert.equal(calls.length,2);assert.equal(calls[1].url,'/api/posts');assert.equal(calls[1].options.credentials,'include');assert.equal(calls[1].options.headers['X-CSRF-TOKEN'],'test-token');assert.deepEqual(JSON.parse(calls[1].options.body),payload);}finally{globalThis.fetch=originalFetch;}
});
test('logout accepts empty 204; next mutation obtains a fresh CSRF token',async()=>{
  const calls=[];globalThis.fetch=async(url)=>{calls.push(url);return url.endsWith('/csrf')?response(200,{headerName:'X-CSRF-TOKEN',token:'fresh-token'}):response(204);};
  try{await api.logout();await api.logout();assert.equal(calls.filter(url=>url.endsWith('/csrf')).length,2);}finally{globalThis.fetch=originalFetch;}
});
test('401 and empty error bodies produce a readable error',async()=>{
  globalThis.fetch=async()=>response(401);
  try{await assert.rejects(api.me(),error=>error instanceof ApiError&&error.status===401);}finally{globalThis.fetch=originalFetch;}
});
