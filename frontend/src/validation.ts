import {z} from 'zod';
export const passwordSchema=z.string().min(8,'Das Passwort muss mindestens 8 Zeichen lang sein.').max(128);
export const loginSchema=z.object({email:z.string().email(),password:passwordSchema});
export const registerSchema=loginSchema.extend({displayName:z.string().min(2).max(80)});
export const phoneSchema=z.string().regex(/^\+[1-9][0-9]{7,14}$/);
