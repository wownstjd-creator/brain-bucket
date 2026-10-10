import type {BoardPost} from './types';
const DAY=86400000;
export function koreaDay(now=new Date()){return new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Seoul',year:'numeric',month:'2-digit',day:'2-digit'}).format(now);}
export function posterBadge(post:BoardPost,now=new Date()){
 const today=koreaDay(now),end=post.postingEndsAt?.slice(0,10);
 if(end&&end<today)return {text:'지난 전단',kind:'past'};
 if(post.deadlineAt&&Date.parse(post.deadlineAt+'+09:00')<now.getTime())return {text:'신청 종료',kind:'past'};
 const deadline=post.deadlineAt?.slice(0,10)||end;
 const days=deadline?Math.round((Date.parse(deadline)-Date.parse(today))/DAY):NaN;
 if(days>=0&&days<=3)return {text:`${post.deadlineAt?'신청':'게시'} ${days===0?'오늘 마감':`D-${days}`}`,kind:'soon'};
 const published=post.publishedAt?Date.parse(post.publishedAt+'+09:00'):NaN;
 if(now.getTime()>=published&&now.getTime()-published<3*DAY)return {text:'NEW',kind:'new'};
 return null;
}
export function readSaved(storage:Pick<Storage,'getItem'>):string[]{
 try{const value=JSON.parse(storage.getItem('han눈에.saved.v1')||'[]');return Array.isArray(value)?[...new Set(value.filter((id):id is string=>typeof id==='string'&&/^\d+$/.test(id)))].slice(0,200):[];}catch{return [];}
}
