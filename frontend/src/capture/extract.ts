export interface PosterFields {title:string;organizer:string;category:string;description:string;location:string;applicationUrl:string;eventDate:string;eventTime:string;deadlineDate:string;deadlineTime:string;postingStart:string;postingEnd:string;}
export const emptyFields=():PosterFields=>({title:'',organizer:'',category:'기타',description:'',location:'',applicationUrl:'',eventDate:'',eventTime:'',deadlineDate:'',deadlineTime:'',postingStart:'',postingEnd:''});
export interface Extraction {fields:Partial<PosterFields>;evidence:Partial<Record<keyof PosterFields,string>>;warnings:string[];}
export function extractPoster(text:string,referenceYear:number):Extraction {
  const lines=text.split(/\r?\n/).map(s=>s.trim()).filter(Boolean), fields:Partial<PosterFields>={},evidence:Extraction['evidence']={},warnings=new Set<string>();
  const assign=(key:keyof PosterFields,value:string,line:string)=>{if(value){fields[key]=value;evidence[key]=line;}};
  const datePattern=/(?:(20\d{2})\s*[년.\/-]\s*)?(\d{1,2})\s*[월.\/-]\s*(\d{1,2})(?:\s*일)?/g;
  const validDate=(year:number,month:number,day:number)=>{const value=new Date(Date.UTC(year,month-1,day));return value.getUTCFullYear()===year&&value.getUTCMonth()===month-1&&value.getUTCDate()===day;};
  lines.forEach((line,index)=>{
    const context=/^(?:게시\s*기간|접수\s*기간|모집\s*기간|행사\s*일시|일시|신청\s*마감)\s*[:：]?\s*$/.test(line)?`${line} ${lines[index+1]||''}`:line;
    const dates=[...context.matchAll(datePattern)].map(m=>{const year=Number(m[1]||referenceYear),month=Number(m[2]),day=Number(m[3]);if(!validDate(year,month,day)){warnings.add('올바르지 않은 날짜를 발견했어요. 원문을 확인해주세요.');return null;} if(!m[1]) warnings.add(`연도가 없는 날짜는 ${referenceYear}년으로 제안했어요. 반드시 확인해주세요.`);return `${year}-${String(month).padStart(2,'0')}-${String(day).padStart(2,'0')}`;}).filter((s):s is string=>!!s);
    const timeMatch=context.match(/(오전|오후)?\s*(\d{1,2})\s*(?:시(?:\s*(\d{1,2})\s*분)?|:(\d{2}))/);
    let time='';if(timeMatch){let h=Number(timeMatch[2]);const m=Number(timeMatch[3]||timeMatch[4]||0);if(timeMatch[1]==='오후'&&h<12)h+=12;if(timeMatch[1]==='오전'&&h===12)h=0;if(h<24&&m<60)time=`${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;}
    if(dates.length){
      if(/게시|게시종료|게시 종료/.test(context)){if(dates.length>1){assign('postingStart',dates[0],context);assign('postingEnd',dates.at(-1)!,context);}else if(/종료|까지/.test(context))assign('postingEnd',dates[0],context);else if(/시작/.test(context))assign('postingStart',dates[0],context);}
      else if(/접수|신청|모집|마감/.test(context)){assign('deadlineDate',dates.at(-1)!,context);if(time&&dates.length===1)assign('deadlineTime',time,context);}
      else if(/행사|일시|개최|일정|공연|전시/.test(context)){assign('eventDate',dates[0],context);if(time&&dates.length===1)assign('eventTime',time,context);}
    }
    const organizer=line.match(/^(?:주최|주관)\s*[:：]\s*(.+)$/);if(organizer)assign('organizer',organizer[1].slice(0,100),line);
    const place=line.match(/^(?:장소|행사 장소)\s*[:：]\s*(.+)$/);if(place)assign('location',place[1].slice(0,150),line);
    const title=line.match(/^(?:제목|행사명)\s*[:：]\s*(.+)$/);if(title)assign('title',title[1].slice(0,150),line);
    const url=line.match(/https?:\/\/[^\s<>]+/);if(url)assign('applicationUrl',url[0].slice(0,2048),line);
  });
  if(!fields.title){const line=lines.find(s=>s.length>=3&&!/^(주최|주관|장소|게시|접수|신청|일시|\d)/.test(s));if(line)assign('title',line.slice(0,150),line);}
  if(text.trim())assign('description',text.trim().slice(0,10000),'인식한 전체 글자');
  if(fields.eventDate&&!fields.eventTime)warnings.add('행사 시간은 확인되지 않았어요. 저장하려면 시간을 직접 입력해주세요.');
  if(fields.deadlineDate&&!fields.deadlineTime)warnings.add('신청 마감 시간은 확인되지 않았어요. 저장하려면 시간을 직접 입력해주세요.');
  if(!fields.postingEnd)warnings.add('명시된 게시 종료일을 찾지 못했어요. 행사일을 게시 종료일로 대신 입력하지 않았습니다.');
  return {fields,evidence,warnings:[...warnings]};
}
