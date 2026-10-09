export interface Member {id:number;email:string;displayName:string;role:string;}
export interface DraftInput {title:string;organizer:string;category:string;description:string;location:string;applicationUrl:string;eventAt:string|null;deadlineAt:string|null;showViewCount:boolean;}
export interface DraftResult {id:number;title:string;status:string;}
export class ApiError extends Error { status:number; constructor(status:number,message:string){super(message);this.status=status;} }
async function request<T>(path:string,method='GET',body?:unknown):Promise<T>{
  const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),12000);
  try{
    const headers:Record<string,string>={};
    if(method!=='GET'){
      const csrf=await fetch('/api/auth/csrf',{credentials:'include',signal:controller.signal});
      if(!csrf.ok)throw new ApiError(csrf.status,'백엔드 연결 또는 CSRF 토큰 발급에 실패했어요.');
      const token=await csrf.json();headers[token.headerName]=token.token;if(body!==undefined)headers['Content-Type']='application/json';
    }
    const response=await fetch(path,{method,headers,credentials:'include',signal:controller.signal,body:body===undefined?undefined:JSON.stringify(body)});
    if(!response.ok){const messages:Record<number,string>={400:'입력 형식과 길이를 확인해주세요.',401:'로그인이 필요하거나 이메일·비밀번호가 올바르지 않아요.',403:'접근 권한 또는 로그인 세션을 확인해주세요.',404:'대자보를 찾을 수 없어요.',409:'중복 이메일이거나 현재 상태에서 수정할 수 없어요.',500:'백엔드 서버를 실행했는지 확인해주세요.',502:'백엔드 서버에 연결할 수 없어요.'};throw new ApiError(response.status,messages[response.status]||`요청에 실패했어요 (${response.status}).`);}
    return response.status===204?undefined as T:await response.json();
  }catch(error){if(error instanceof ApiError)throw error;throw new Error('서버에 연결할 수 없어요. 백엔드 실행 상태를 확인해주세요.');}finally{clearTimeout(timeout);}
}
export const api={
  me:()=>request<Member>('/api/auth/me'),
  login:(email:string,password:string)=>request<Member>('/api/auth/login','POST',{email,password}),
  signup:(email:string,password:string,displayName:string)=>request<Member>('/api/auth/signup','POST',{email,password,displayName}),
  logout:()=>request<void>('/api/auth/logout','POST'),
  save:(input:DraftInput,id?:number)=>request<DraftResult>(id?`/api/posts/mine/${id}`:'/api/posts',id?'PUT':'POST',input),
};
