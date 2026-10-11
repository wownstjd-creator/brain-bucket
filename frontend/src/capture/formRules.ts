export function optionalDateTime(date:string,time:string,endOfDay=false):string|null {
  if (!date) {
    if (time) throw new Error('시간을 입력했다면 날짜도 입력해주세요.');
    return null;
  }
  return `${date}T${time || (endOfDay ? '23:59' : '00:00')}:00`;
}
export function optionalUrl(value:string):string|null {
  const trimmed=value.trim();
  return trimmed || null;
}
