import { perspectiveMap, type Point } from './geometry';
export async function loadImage(url:string) {
  const image=new Image(); image.src=url; await image.decode(); return image;
}
export async function normalizePhoto(file:File) {
  if (!['image/jpeg','image/png','image/webp'].includes(file.type)) throw new Error('JPG, PNG, WebP 사진을 선택해주세요. PDF·HEIC는 아직 지원하지 않아요.');
  if(file.size>20*1024*1024) throw new Error('20MB 이하의 사진을 선택해주세요.');
  const bitmap=await createImageBitmap(file, {imageOrientation:'from-image'});
  try {
    const scale=Math.min(1,2000/Math.max(bitmap.width,bitmap.height));
    const canvas=document.createElement('canvas'); canvas.width=Math.round(bitmap.width*scale);canvas.height=Math.round(bitmap.height*scale);
    const ctx=canvas.getContext('2d')!;ctx.fillStyle='#fff';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(bitmap,0,0,canvas.width,canvas.height);
    return canvas.toDataURL('image/jpeg',.94);
  } finally {bitmap.close();}
}
export async function correctPhoto(url:string,corners:Point[],enhance:boolean) {
  const map=perspectiveMap(corners),image=await loadImage(url);
  const distance=(a:Point,b:Point)=>Math.hypot((a.x-b.x)*image.width,(a.y-b.y)*image.height);
  const rawW=Math.max(distance(corners[0],corners[1]),distance(corners[3],corners[2]));
  const rawH=Math.max(distance(corners[0],corners[3]),distance(corners[1],corners[2]));
  const scale=Math.min(1,1800/Math.max(rawW,rawH));
  const source=document.createElement('canvas');source.width=image.width;source.height=image.height;
  const sourceCtx=source.getContext('2d',{willReadFrequently:true})!;sourceCtx.drawImage(image,0,0);
  const pixels=sourceCtx.getImageData(0,0,image.width,image.height).data;
  const output=document.createElement('canvas');output.width=Math.max(32,Math.round(rawW*scale));output.height=Math.max(32,Math.round(rawH*scale));
  const ctx=output.getContext('2d')!,result=ctx.createImageData(output.width,output.height);
  const histogram=new Uint32Array(256);
  for(let y=0;y<output.height;y++){
    for(let x=0;x<output.width;x++){
      const p=map(x/(output.width-1),y/(output.height-1));
      const sx=Math.max(0,Math.min(image.width-1,p.x*(image.width-1))),sy=Math.max(0,Math.min(image.height-1,p.y*(image.height-1)));
      const x0=Math.floor(sx),y0=Math.floor(sy),x1=Math.min(x0+1,image.width-1),y1=Math.min(y0+1,image.height-1),fx=sx-x0,fy=sy-y0,i=(y*output.width+x)*4;
      for(let c=0;c<3;c++) result.data[i+c]=pixels[(y0*image.width+x0)*4+c]*(1-fx)*(1-fy)+pixels[(y0*image.width+x1)*4+c]*fx*(1-fy)+pixels[(y1*image.width+x0)*4+c]*(1-fx)*fy+pixels[(y1*image.width+x1)*4+c]*fx*fy;
      result.data[i+3]=255;
      histogram[Math.round(.299*result.data[i]+.587*result.data[i+1]+.114*result.data[i+2])]++;
    }
    if(y%100===0) await new Promise<void>(resolve=>setTimeout(resolve,0));
  }
  if(enhance){
    const percentile=(ratio:number)=>{let total=0;for(let i=0;i<256;i++){total+=histogram[i];if(total>=output.width*output.height*ratio)return i;}return 255;};
    const low=percentile(.01),high=percentile(.99);
    if(high-low>35) for(let i=0;i<result.data.length;i+=4) for(let c=0;c<3;c++){
      const original=result.data[i+c]; const adjusted=Math.max(0,Math.min(255,(original-low)*255/(high-low)));
      result.data[i+c]=original*.4+adjusted*.6;
    }
  }
  ctx.putImageData(result,0,0);return output.toDataURL('image/jpeg',.94);
}
