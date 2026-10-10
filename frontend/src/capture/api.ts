import type { BoardPost } from '../types';
export interface Member {id:number;email:string;displayName:string;role:string;}
export interface DraftInput {title:string;organizer:string;category:string;description:string;location:string;applicationUrl:string;eventAt:string|null;deadlineAt:string|null;showViewCount:boolean;postingStart?:string|null;postingEnd?:string|null;version?:number;}
export type DraftResult = BoardPost;
export class ApiError extends Error { status:number; constructor(status:number,message:string){super(message);this.status=status;} }
export async function request<T>(path:string,method='GET',body?:unknown):Promise<T>{
  const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),12000);
  try{
    let actual=path;
    let privateView=false;
    if(path.startsWith('/api/public/posts'))actual=path.replace('/api/public/posts','/api/posts/public');
    if(path.includes('/history'))actual=path.replace(/\/api\/posts\/(\d+)\/history/, '/api/posts/mine/$1/history');
    if(path==='/api/admin/posts?status=ALL')actual='/api/admin/posts/managed?status=ALL&size=50';
    if(path.endsWith('/review')) {const r=body as {decision:string;reason:string};actual=path.replace('/review',r.decision==='APPROVED'?'/approve':'/reject');body=r.decision==='APPROVED'?undefined:{reason:r.reason};}
    if(path.endsWith('/schedule')) {const r=body as Record<string,unknown>;body={...r,displayFrom:r.postingStart,displayUntil:r.postingEnd};}
    privateView=actual.startsWith('/api/posts/mine');
    const headers:Record<string,string>={};
    if(method!=='GET'){
      const csrf=await fetch('/api/auth/csrf',{credentials:'include',signal:controller.signal});
      if(!csrf.ok)throw new ApiError(csrf.status,'백엔드 연결 또는 CSRF 토큰 발급에 실패했어요.');
      const token=await csrf.json();headers[token.headerName]=token.token;if(body!==undefined&&!(body instanceof FormData))headers['Content-Type']='application/json';
    }
    const response=await fetch(actual,{method,headers,credentials:'include',signal:controller.signal,body:body===undefined?undefined:body instanceof FormData?body:JSON.stringify(body)});
    if(!response.ok){const messages:Record<number,string>={400:'입력 형식과 길이를 확인해주세요.',401:'로그인이 필요하거나 이메일·비밀번호가 올바르지 않아요.',403:'접근 권한 또는 로그인 세션을 확인해주세요.',404:'대자보를 찾을 수 없어요.',409:'중복 이메일이거나 현재 상태에서 수정할 수 없어요.',500:'백엔드 서버를 실행했는지 확인해주세요.',502:'백엔드 서버에 연결할 수 없어요.'};let detail='';try{detail=(await response.json()).message||'';}catch{}throw new ApiError(response.status,detail||messages[response.status]||`요청에 실패했어요 (${response.status}).`);}
    if(response.status===204)return undefined as T;
    const data=await response.json();
    if(actual.startsWith('/api/posts/public?')||actual.startsWith('/api/admin/posts/managed')) {
      const combined=[...data];
      const base=new URL(actual,location.origin);base.searchParams.set('size',actual.includes('/managed')?'50':'20');
      const size=Number(base.searchParams.get('size'));let page=0;let last=data;
      while(last.length===size){base.searchParams.set('page',String(++page));const next=await fetch(base.pathname+base.search,{credentials:'include',signal:controller.signal});if(!next.ok)throw new ApiError(next.status,'목록을 불러오지 못했어요.');last=await next.json();combined.push(...last);}
      data.splice(0,data.length,...combined);
    }
    if(actual.startsWith('/api/posts/mine')&&/history/.test(actual))return data as T;
    if(actual.startsWith('/api/admin/posts/managed'))return await Promise.all(data.map(async(p:{id:number})=>{const detail=await request<any>(`/api/admin/posts/${p.id}`);return normalize({...detail.post,imageUrl:detail.imageUrl},true);} )) as T;
    if(actual.startsWith('/api/posts/mine')&&Array.isArray(data))return await Promise.all(data.map((p:{id:number})=>request(`/api/posts/mine/${p.id}`))) as T;
    if(data?.post)return data as T;
    if(Array.isArray(data))return data.map(p=>normalize(p,privateView)) as T;
    return data?.id&&data?.displayUntil!==undefined?normalize(data,privateView) as T:data as T;
  }catch(error){if(error instanceof ApiError)throw error;throw new Error('서버에 연결할 수 없어요. 백엔드 실행 상태를 확인해주세요.');}finally{clearTimeout(timeout);}
}
export const api={
  me:()=>request<Member>('/api/auth/me'),
  login:(email:string,password:string)=>request<Member>('/api/auth/login','POST',{email,password}),
  signup:(email:string,password:string,displayName:string)=>request<Member>('/api/auth/signup','POST',{email,password,displayName}),
  logout:()=>request<void>('/api/auth/logout','POST'),
  upload:async(id:string,kind:string,dataUrl:string,version?:number)=>{
    const blob=await (await fetch(dataUrl)).blob();const form=new FormData();form.append('file',blob,`${kind.toLowerCase()}.${blob.type==='image/png'?'png':'jpg'}`);
    await request(`/api/posts/mine/${id}/attachments/${kind.toLowerCase()}${version===undefined?'':`?version=${version}`}`,'PUT',form);
    return request<DraftResult>(`/api/posts/mine/${id}`);
  },
  submit:async(id:string,_version?:number)=>{await request(`/api/posts/mine/${id}/approval-request`,'POST');return request<DraftResult>(`/api/posts/mine/${id}`);},
  save:async(input:DraftInput,id?:string)=>{const result=await request<{id:number}>(id?`/api/posts/mine/${id}`:'/api/posts',id?'PUT':'POST',{...input,category:categoryCodes[input.category]||input.category,displayFrom:input.postingStart,displayUntil:input.postingEnd});return request<DraftResult>(`/api/posts/mine/${result.id}`);},
};
const categoryCodes:Record<string,string>={'동아리':'CLUB','대회':'CONTEST','행사':'EVENT','공지':'NOTICE','기타':'OTHER'};
function normalize(p:any,privateView=false):BoardPost {
 const labels:Record<string,string>={CLUB:'동아리',CONTEST:'대회',EVENT:'행사',NOTICE:'공지',OTHER:'기타'};
 return {...p,id:String(p.id),category:labels[p.category]||p.category,postingStart:p.displayFrom||'',postingEndsAt:p.displayUntil||'',status:p.status||'APPROVED',description:p.description||'',location:p.location||'',
 imageUrl:p.imageUrl||(privateView?`/api/posts/mine/${p.id}/poster`:`/api/posts/public/${p.id}/poster`),
 evidenceUrl:privateView&&p.hasEvidence?`/api/posts/mine/${p.id}/attachments/evidence`:undefined,
 originalUrl:privateView&&p.hasOriginal?`/api/posts/mine/${p.id}/attachments/original`:undefined,
 archiveYear:p.displayUntil&&p.displayUntil<new Intl.DateTimeFormat('sv-SE',{timeZone:'Asia/Seoul'}).format(new Date())?p.displayUntil.slice(0,4):undefined};
}
