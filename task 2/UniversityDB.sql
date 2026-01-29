create database if not exists UniversityDB;
use UniversityDB;

create table Student(
VTU_Number varchar(20) primary key,
Name varchar(100),
email varchar(100),
phone varchar(15),
dept varchar(30)
);
create table Course(
course_code varchar(10),
course_name varchar(30),
faculty_id varchar(100),
student_id varchar(100),
faculty_email varchar(100),
foreign key(student_id) references Student(VTU_Number)
);
insert into Student values('23uess0023','sai','sai@gmail.com','98298032','CSE'),
('23uess0024','pavan','pavan@gmail.com','123654','ECE'),('23uess0025','pandu','pandu@gmail.com','1478523','CSE'),('23uess0026','ram','ram@gmail.com','456789','CSE'),('23uess0027','rithwik','rithwil@gmail.com','123456','ECE');
insert into Course values('2289','dbms','f_12','23uess0023','37@gmail.com'),
('2289','dbms','f_12','23uess0024','37@gmail.com'),
('2289','dbms','f_12','23uess0025','37@gmail.com'),
('2289','dbms','f_12','23uess0026','37@gmail.com'),
('2289','dbms','f_12','23uess0027','37@gmail.com');
select * from Student;
select * from Course;
select count(*) from Student;
select * from Student order by name;
select * from  Student order by VTU_Number;
select * from Student where dept ='CSE';
