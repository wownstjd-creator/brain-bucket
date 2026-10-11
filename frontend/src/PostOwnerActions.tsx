import {useState} from 'react';
import type {BoardPost} from './types';
import {request} from './capture/api';
export default function PostOwnerActions({post,onDone}:{post:BoardPost;onDone:()=>void}){
 const [busy,setBusy]=useState(false),[error,setError]=useState('');
 const act=async(remove:boolean)=>{
  if(post.version===undefined){setError('다시 불러온 뒤 시도해주세요.');return;}
  if(!window.confirm(remove?'게시물을 삭제할까요? 공개 게시판과 내 목록에서 사라지며 직접 복구할 수 없습니다.':'승인 요청을 취소하고 작성 중으로 되돌릴까요?'))return;
  setBusy(true);setError('');
  try{await request(`/api/posts/mine/${post.id}${remove?'':'/approval-cancel'}?version=${post.version}`,remove?'DELETE':'POST');onDone();}
  catch(e){setError((e as Error).message);}finally{setBusy(false);}
 };
 return <section><div className="source-actions">{post.status==='PENDING'&&<button className="secondary" disabled={busy} onClick={()=>void act(false)}>승인 요청 취소</button>}<button className="secondary" disabled={busy} onClick={()=>void act(true)}>{busy?'처리 중…':'게시물 삭제'}</button></div>{error&&<p role="alert" className="error">{error}</p>}</section>;
}
