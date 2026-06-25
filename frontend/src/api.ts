import type {User} from './types';
const BASE=(import.meta.env.VITE_API_URL??'').replace(/\/+$/,'');
export class ApiError extends Error{constructor(public status:number,message:string,public code?:string){super(message)}}
let refreshPromise:Promise<boolean>|null=null;
async function raw<T>(path:string,init:RequestInit={},retry=true):Promise<T>{
 const response=await fetch(`${BASE}${path}`,{...init,credentials:'include',headers:{'Content-Type':'application/json',...init.headers}});
 if(response.status===401&&retry&&!path.includes('/auth/')){refreshPromise??=fetch(`${BASE}/api/auth/refresh`,{method:'POST',credentials:'include'}).then(r=>r.ok).finally(()=>{refreshPromise=null});if(await refreshPromise)return raw(path,init,false)}
 if(!response.ok){let body:{message?:string;code?:string}={};try{body=await response.json()}catch{}throw new ApiError(response.status,body.message??'Die Anfrage ist fehlgeschlagen.',body.code)}
 return response.status===204?undefined as T:response.json();
}
export const api={get:<T>(p:string)=>raw<T>(p),post:<T>(p:string,b?:unknown)=>raw<T>(p,{method:'POST',body:b===undefined?undefined:JSON.stringify(b)}),patch:<T>(p:string,b:unknown)=>raw<T>(p,{method:'PATCH',body:JSON.stringify(b)}),logout:()=>raw<void>('/api/auth/logout',{method:'POST'},false),me:()=>raw<User>('/api/auth/me',{},false)};
