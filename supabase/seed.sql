-- DEVELOPMENT ONLY. Run after creating a dev staff/admin account in Supabase Auth.
-- No passwords, real identities, or auth.users fixtures are stored in the seed.
begin;
do $$
declare actor uuid; program uuid;
begin
 select id into actor from public.profiles where role in ('staff','admin') and active order by created_at limit 1;
 if actor is null then raise exception 'Create a development Auth account and promote its profile with a trusted SQL operator first'; end if;
 insert into public.programs(name,description,pic,pic_id,start_date,end_date,status,progress,budget,created_by)
 values('Demo Sports Program','Development fixture','Development Staff',actor,current_date,current_date+30,'Planning',0,1000000,actor) returning id into program;
 insert into public.tasks(title,program_id,assignee_id,status,priority,due_date,created_by) values('Prepare development proposal',program,actor,'To Do','Medium',current_date+7,actor);
 insert into public.finances(description,program_id,type,amount,category,transaction_date,status,created_by) values('Development equipment fixture',program,'expense',100000,'Equipment',current_date,'approved',actor);
 insert into public.inventory(name,category,quantity,status,condition,emoji,location,created_by) values('Development Futsal Ball','Equipment',3,'Available','Baik','⚽','Development Storage',actor);
end $$;
commit;
