export type Point = { x: number; y: number };
export const defaultCorners = (): Point[] => [{x:.04,y:.04},{x:.96,y:.04},{x:.96,y:.96},{x:.04,y:.96}];

export function validCorners(points: Point[]) {
  if (points.length !== 4 || points.some(p => !Number.isFinite(p.x) || !Number.isFinite(p.y) || p.x < 0 || p.x > 1 || p.y < 0 || p.y > 1)) return false;
  const crosses = points.map((a,i) => {const b=points[(i+1)%4], c=points[(i+2)%4]; return (b.x-a.x)*(c.y-b.y)-(b.y-a.y)*(c.x-b.x);});
  const area = Math.abs(points.reduce((sum,a,i)=>{const b=points[(i+1)%4];return sum+a.x*b.y-b.x*a.y;},0))/2;
  return crosses.every(v=>v>0.0001) && area > .01;
}

// Map the output unit rectangle back onto the selected quadrilateral.
export function perspectiveMap(p: Point[]) {
  if (!validCorners(p)) throw new Error('네 모서리가 서로 교차하지 않도록 전단 영역을 지정해주세요.');
  const [a,b,c,d]=p;
  const dx1=b.x-c.x, dx2=d.x-c.x, dx3=a.x-b.x+c.x-d.x;
  const dy1=b.y-c.y, dy2=d.y-c.y, dy3=a.y-b.y+c.y-d.y;
  const det=dx1*dy2-dx2*dy1;
  const g=(dx3*dy2-dx2*dy3)/det, h=(dx1*dy3-dx3*dy1)/det;
  const ax=b.x-a.x+g*b.x, bx=d.x-a.x+h*d.x;
  const ay=b.y-a.y+g*b.y, by=d.y-a.y+h*d.y;
  return (u:number,v:number):Point=>({x:(ax*u+bx*v+a.x)/(g*u+h*v+1),y:(ay*u+by*v+a.y)/(g*u+h*v+1)});
}
