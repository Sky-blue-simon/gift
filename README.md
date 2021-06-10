# gift

# 온라인 수강신청 및 Point 와 Gift 지급

본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 예제입니다.
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 예시 답안을 포함합니다.
- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW



# Table of contents

- [온라인 수강신청 시스템](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
    - [개발 운영 환경 분리](#개발-운영-환경-분리)
    - [모니터링](#모니터링)

# 서비스 시나리오


기능적 요구사항
1. 강사가 Online 강의를 등록/수정/삭제 등 강의를 관리한다
1. 수강생은 강사가 등록한 강의를 조회한 후 듣고자 하는 강의를 신청한다
1. 수강생은 신청한 강의에 대한 강의료를 결제한다
1. 강의 수강 신청이 완료되면 강의 교재를 배송한다
1. 강의 수강 신청을 취소하면 강의 교재 배송을 취소한다
1. 강의 수강 신청 내역을 언제든지 수강 신청자가 볼 수 있다
1. 강의 수강 신청 내역 변경 발생 시 카톡으로 알림
2. 강의 수강 결제가 완료되면 포인트를 지급한다.
2. 포인트가 지급되면 기프트를 지급한다.
2. 강의 수강 결제가 취소되면 포인트 지급을 취소한다.
2. 포인트 지급이 취소되면 기프트 지급을 취소한다.

비기능적 요구사항
1. 트랜잭션
    1. 강의 결제가 완료 되어야만 수강 신청 완료 할 수 있음 Sync 호출
    2. 포인트 지급이 완료되면 기프도 완료되어야 한다. Sync 호출
1. 장애격리
    1. 수강신청 시스템이 과중되면 사용자를 잠시동안 받지 않고 신청을 잠시 후에 하도록 유도한다  Circuit breaker
1. 성능
    1. 학생이 마이페이지에서 등록된 강의와 수강 및 교재 배송 상태를 확인할 수 있어야 한다  CQRS
    1. 수강신청/배송 상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다  Event driven
    2. 학생이 마이페이지에서 지급된 포인트와 기프트 갯수를 확인할 수 있어야 한다. CQRS
    2. 강의 결제 상태가 바뀔때마다 포인트와 기프트의 상태도 바뀌어야 한다. Event driven

# 분석/설계

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/Bam9mkNhgBbUzHRuKqYvxJ7sIzG3/mine/0c5f64f61c529fb999652b08bed8a584


### 이벤트 도출
![이벤트 도출](https://user-images.githubusercontent.com/80744224/121274764-299bdf80-c906-11eb-932b-2575e9ffbc59.png)



### 부적격 이벤트 탈락
![부적격 이벤트](https://user-images.githubusercontent.com/80744224/121277157-f871de00-c90a-11eb-8b32-47ec1588cfa8.png)


### 모델링 완료
![모델링 완료](https://user-images.githubusercontent.com/80744224/121277726-302d5580-c90c-11eb-9089-445623fbb708.png)




## 헥사고날 아키텍처 다이어그램 도출
![헥사고날 아키텍처 다이어그램](https://user-images.githubusercontent.com/80744224/121280968-1c84ed80-c912-11eb-89a8-d6901312b9c6.png)



    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트와 파이선으로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd course
mvn spring-boot:run

cd class
mvn spring-boot:run 

cd pay
mvn spring-boot:run  

cd alert
mvn spring-boot:run

cd gateway
mvn spring-boot:run

cd point
mvn spirng-boot:run

cd gift
mvn spring-boot:run
```

- AWS 클라우드의 EKS 서비스 내에 서비스를 모두 배포함.
```
root@labs--1263645818:/home/project# kubectl get all
NAME                           READY   STATUS    RESTARTS   AGE
pod/alert-68fd9f6849-cnj62     1/1     Running   0          78m
pod/class-76f4ffccc5-9nq4d     1/1     Running   0          77m
pod/course-6c84b865bd-2sjsl    1/1     Running   0          55m
pod/gateway-7575d84bdf-6ddcx   1/1     Running   0          73m
pod/gift-864958499f-kzjjs      1/1     Running   0          79m
pod/pay-7658574c6f-vp7x9       1/1     Running   0          74m
pod/point-5fb456d68f-2jc5z     1/1     Running   0          18m

NAME                 TYPE           CLUSTER-IP       EXTERNAL-IP                                                                    PORT(S)          AGE
service/alert        ClusterIP      10.100.34.122    <none>                                                                         8080/TCP         78m
service/class        ClusterIP      10.100.242.45    <none>                                                                         8080/TCP         77m
service/course       ClusterIP      10.100.82.239    <none>                                                                         8080/TCP         75m
service/gateway      LoadBalancer   10.100.138.251   a6e770600b6db4906b16f6cffd71f5b6-1894361895.ap-southeast-2.elb.amazonaws.com   8080:30947/TCP   73m
service/gift         ClusterIP      10.100.115.182   <none>                                                                         8080/TCP         79m
service/kubernetes   ClusterIP      10.100.0.1       <none>                                                                         443/TCP          87m
service/pay          ClusterIP      10.100.251.57    <none>                                                                         8080/TCP         74m
service/point        ClusterIP      10.100.86.12     <none>                                                                         8080/TCP         18m

NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/alert     1/1     1            1           78m
deployment.apps/class     1/1     1            1           77m
deployment.apps/course    1/1     1            1           76m
deployment.apps/gateway   1/1     1            1           73m
deployment.apps/gift      1/1     1            1           79m
deployment.apps/pay       1/1     1            1           75m
deployment.apps/point     1/1     1            1           18m

NAME                                 DESIRED   CURRENT   READY   AGE
replicaset.apps/alert-68fd9f6849     1         1         1       78m
replicaset.apps/class-76f4ffccc5     1         1         1       77m
replicaset.apps/course-6c84b865bd    1         1         1       76m
replicaset.apps/gateway-7575d84bdf   1         1         1       73m
replicaset.apps/gift-864958499f      1         1         1       79m
replicaset.apps/pay-7658574c6f       1         1         1       75m
replicaset.apps/point-5fb456d68f     1         1         1       18m
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: 
 (예시는 course 마이크로 서비스). 이때 가능한 중학교 수준의 영어를 사용하려고 노력했다. 

```
package lecture;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name = "Course_table")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String teacher;
    private Long fee;
    private String textBook;

    @PostPersist
    public void onPostPersist() {
        CourseRegistered courseRegistered = new CourseRegistered();
        BeanUtils.copyProperties(this, courseRegistered);
        courseRegistered.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate() {
        CourseModified courseModified = new CourseModified();
        BeanUtils.copyProperties(this, courseModified);
        courseModified.publishAfterCommit();
    }

    @PreRemove
    public void onPreRemove() {
        CourseDeleted courseDeleted = new CourseDeleted();
        BeanUtils.copyProperties(this, courseDeleted);
        courseDeleted.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getTextBook() {
        return textBook;
    }

    public void setTextBook(String textBook) {
        this.textBook = textBook;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

}
```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package lecture;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface CourseRepository extends PagingAndSortingRepository<Course, Long> {

}
```

- 적용 후 REST API 의 테스트

```
# 신규 강좌 등록
http POST http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/courses name=korean teacher=hong-gil-dong fee=10000 textBook=kor_book

# 등록된 강좌 확인
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/courses

# 수강 신청
http POST http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/classes courseId=1 fee=10000 student=john-doe textBook=kor_book

# 수강 등록 확인
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/classes

# 결제 성공 확인
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/payments

# 수강 교재 배송 시작 확인
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/deliveries

# 포인트 취득 확인
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/points

# 기프트 취득 확인
http GET a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/gifts

# My page에서 수강신청여부/결제성공여부/배송상태 확인
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/inquiryMypages


# 수강 취소
http DELETE http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/classes/1

# 수강 삭제 확인
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/classes

# 결제 취소 확인 (상태값 "CANCEL" 확인)
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/payments

# 배송 취소 확인 (상태값 "DELIVERY_CANCEL" 확인)
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/deliveries

# 포인트 취소 확인
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/points

# 기프트 취소 확인
http GET a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/gifts


# My page에서 수강신청여부/결제성공여부/배송상태 확인
http GET http://a02b3b4c7ed60432eb2724c33b6a12ce-294743840.ap-southeast-2.elb.amazonaws.com:8080/inquiryMypages

```

- 강좌 등록 확인

![image](https://user-images.githubusercontent.com/80744224/121448345-6df4b180-c9d2-11eb-86ae-184bfd1fd344.png)



- 수강 신청

![image](https://user-images.githubusercontent.com/80744224/121448377-7baa3700-c9d2-11eb-993c-723856f1409c.png)



- 포인트 등록 확인

![image](https://user-images.githubusercontent.com/80744224/121448396-84027200-c9d2-11eb-8951-4360b96bf292.png)


- 크롬에서 포인트 등록 확인
![image](https://user-images.githubusercontent.com/80744224/121348828-cb054e80-c963-11eb-920e-f7e8e1ab82ae.png)


- 기프트 등록 확인

![image](https://user-images.githubusercontent.com/80744224/121448415-8ebd0700-c9d2-11eb-9e06-ff97cf9e257d.png)



- 크롬에서 기프트 등록 확인
![image](https://user-images.githubusercontent.com/80744224/121348907-e2dcd280-c963-11eb-8792-7093344b86cc.png)


- My Page
![image](https://user-images.githubusercontent.com/80744224/121400544-af656c80-c992-11eb-90e9-0f9ef1482abd.png)


- 수강 취소
![image](https://user-images.githubusercontent.com/80744224/121349495-8a5a0500-c964-11eb-95cf-54dd66d2a79a.png)

- 수강 취소 확인
![image](https://user-images.githubusercontent.com/80744224/121349554-9e9e0200-c964-11eb-8c4d-ca5160ac8606.png)

- Point 0 로 변경 확인
![image](https://user-images.githubusercontent.com/80744224/121349701-c9885600-c964-11eb-96bb-44d33dd20c2f.png)



## Gateway

gateway 서비스를 통하여 동일 진입점으로 진입하여 각각의 마이크로 서비스를 접근할 수 있다.

![image](https://user-images.githubusercontent.com/80744224/121401305-8691a700-c993-11eb-8826-ce475814c024.png)

외부에서 접근을 위하여 Gateway의 Service는 LoadBalancer Type으로 생성

![image](https://user-images.githubusercontent.com/80744224/121401439-b0e36480-c993-11eb-8cc4-d4fbf7da3090.png)



## 폴리글랏 퍼시스턴스

기프트 (pay) 서비스는 기존 h2 가 아닌 hsqldb로 구성하기 위해, maven dependancy를 추가.

```
# 기프트(gift) 서비스의 pom.xml

    <dependency>
        <groupId>org.hsqldb</groupId>
        <artifactId>hsqldb</artifactId>
        <version>2.5.1</version>
        <scope>runtime</scope>
    </dependency>

```

## 동기식 호출

수강신청(class) 한 후, 포인트(point) -> 기프트(gift) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리함.

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

![image](https://user-images.githubusercontent.com/80744224/121320144-d8144480-c947-11eb-8f3a-685641c1b5cb.png)


- 기프트 (gift) 서비스를 잠시 내려놓음

cd ./gift/kubernetes

kubectl delete -f deployment.yml

- 수강 신청 후, 포인트(point) -> 기프트(gift) 갈 때 Fail

![image](https://user-images.githubusercontent.com/80744224/121329918-95a33580-c950-11eb-8f35-a5bae06645f8.png)

![image](https://user-images.githubusercontent.com/80744224/121330532-1104e700-c951-11eb-8f4a-4cef84da160c.png)


- 기프트(gift) 서비스 재기동

![image](https://user-images.githubusercontent.com/80744224/121323931-32fb6b00-c94b-11eb-8b6c-f92f86713566.png)


- 수강 신청 후, 포인트와 기프트 정상 조회

http GET http://a6e770600b6db4906b16f6cffd71f5b6-1894361895.ap-southeast-2.elb.amazonaws.com:8080/points

http GET http://a6e770600b6db4906b16f6cffd71f5b6-1894361895.ap-southeast-2.elb.amazonaws.com:8080/gifts

![image](https://user-images.githubusercontent.com/80744224/121324229-7a81f700-c94b-11eb-94d4-9ededce6606a.png)

![image](https://user-images.githubusercontent.com/80744224/121323960-3abb0f80-c94b-11eb-8ee7-a5cc13bac923.png)



## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

배송 시스템은 수강신청/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 배송시스템이 유지보수로 인해 잠시 내려간 상태라도 수강신청을 받는데 문제가 없다:

- 배송 서비스 (course) 를 잠시 내려놓음 
cd ./course/kubernetes
kubectl delete -f deployment.yml

- 수강 신청
http POST http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/classes courseId=1 fee=10000 student=KimSoonHee textBook=eng_book #Success
http POST http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/classes courseId=1 fee=12000 student=JohnDoe textBook=kor_book #Success

- 수강 신청 상태 확인
http GET http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/classes   # 수강 신청 완료 
http GET http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/inquiryMypages  # 배송 상태 "deliveryStatus": null

- 배송 서비스 (course) 기동
kubectl apply -f deployment.yml

- 배송 상태 확인
http GET http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/inquiryMypages  # 배송 상태 "deliveryStatus": "DELIVERY_START"



# 운영

## CI/CD 설정

### docker images를 수작업 배포/기동

CodeBuild를 사용하지 않고 docker images를 AWS를 통해 수작업으로 배포/기동하였음.

- package & docker image build/push

mvn package

docker build -t 879772956301.dkr.ecr.ap-southeast-2.amazonaws.com/user09-point:latest .

docker push 879772956301.dkr.ecr.ap-southeast-2.amazonaws.com/user09-point:latest

- docker 이미지로 Deployment 생성

kubectl create deploy point --image=879772956301.dkr.ecr.ap-southeast-2.amazonaws.com/user09-point:latest

- expose

kubectl expose deploy point --type=ClusterIP --port=8080

![image](https://user-images.githubusercontent.com/80744224/121287993-95d60d80-c91d-11eb-8a78-98ffd3fff77a.png)

![image](https://user-images.githubusercontent.com/80744224/121288019-a38b9300-c91d-11eb-973d-5430fbf3e372.png)

![image](https://user-images.githubusercontent.com/80744224/121288036-ad14fb00-c91d-11eb-9863-5f12a0a50929.png)

![image](https://user-images.githubusercontent.com/80744224/121288049-b2724580-c91d-11eb-9e2c-2cd2693e0600.png)

![image](https://user-images.githubusercontent.com/80744224/121288757-cff3df00-c91e-11eb-8872-b91c6a3cd2e4.png)

![image](https://user-images.githubusercontent.com/80744224/121289345-bd2dda00-c91f-11eb-9945-a2237fd7e68c.png)

![image](https://user-images.githubusercontent.com/80744224/121310765-aba7fa80-c93e-11eb-94a9-4b981c96d107.png)




## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 수강신청(class)-->결제(pay) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 1000 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml

feign:
  hystrix:
    enabled: true
hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 1000
```



## 오토스케일 아웃

* replica 를 동적으로 늘려주도록 HPA 를 설정한다

kubectl autoscale deploy gift --min=1 --max=10 --cpu-percent=15

kubectl autoscale deploy course --min=1 --max=10 --cpu-percent=5

kubectl autoscale deploy class --min=1 --max=10 --cpu-percent=1

* 부하테스터 siege 툴 사용

siege -c150 -t30S -v --content-type "application/json" 'http://a2407157de33e4281bce4111697ad1ff-1059344160.ap-southeast-2.elb.amazonaws.com:8080/gifts POST {"classId":"100", "fee":"20000", "student":"young"}'

siege -c255 -t300S -v --content-type "application/json" 'http://a2407157de33e4281bce4111697ad1ff-1059344160.ap-southeast-2.elb.amazonaws.com:8080/courses POST {"name":"english", "teacher":"hong", "fee":"10000", "textBook":"eng_book"}'

siege -c255 -t300S -v --content-type "application/json" 'http://a2407157de33e4281bce4111697ad1ff-1059344160.ap-southeast-2.elb.amazonaws.com:8080/classes POST {"courseId":"3", "fee":"10000", "student":"gil-dong", "textBook":"eng_book"}'

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:

watch kubectl get pod,hpa

![image](https://user-images.githubusercontent.com/80744224/121344497-cab68480-c95e-11eb-8637-c68dc8de5c7f.png)





## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
```
siege -c100 -t120S -r10 -v --content-type "application/json" 'http://gateway:8080/courses POST {"name": "english", "teacher": "hong", "fee": 10000, "textBook": "eng_book"}'


** SIEGE 4.0.5
** Preparing 100 concurrent users for battle.
The server is now under siege...

HTTP/1.1 201     3.43 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     1.28 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     0.20 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     3.44 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     1.18 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     0.28 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     1.41 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     1.22 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     0.21 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     0.13 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     1.41 secs:     251 bytes ==> POST http://gateway:8080/courses
HTTP/1.1 201     1.31 secs:     251 bytes ==> POST http://gateway:8080/courses

```

- 새버전(v0.1)으로의 배포 시작
```
kubectl apply -f kubectl apply -f deployment_v0.1.yml

```

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인
```
Transactions:                    614 hits
Availability:                  35.35 %
Elapsed time:                  34.95 secs
Data transferred:               0.38 MB
Response time:                  3.87 secs
Transaction rate:              17.57 trans/sec
Throughput:                     0.01 MB/sec
Concurrency:                   68.06
Successful transactions:         614
Failed transactions:            1123
Longest transaction:           29.72
Shortest transaction:           0.00
```
배포 중 Availability 가 평소 100%에서 35% 대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:

```
# deployment.yaml 의 readiness probe 의 설정:

# (course) deployment.yaml 파일
 
          readinessProbe:
            httpGet:
              path: '/courses'
              port: 8080
            initialDelaySeconds: 20
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/courses'
              port: 8080
            initialDelaySeconds: 180
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5

/> kubectl apply -f deployment.yml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:
```
Lifting the server siege...
Transactions:                  39737 hits
Availability:                 100.00 %
Elapsed time:                 119.91 secs
Data transferred:               9.66 MB
Response time:                  0.30 secs
Transaction rate:             331.39 trans/sec
Throughput:                     0.08 MB/sec
Concurrency:                   99.71
Successful transactions:       39737
Failed transactions:               0
Longest transaction:            1.89
Shortest transaction:           0.00

```

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.

## 개발 운영 환경 분리
* ConfigMap을 사용하여 운영과 개발 환경 분리

- kafka환경
```
  운영 : kafka-1621824578.kafka.svc.cluster.local:9092
  개발 : localhost:9092
```

```
configmap yaml 파일

apiVersion: v1
kind: ConfigMap
metadata:
  name: kafka-config
data:
  KAFKA_URL: kafka-1621824578.kafka.svc.cluster.local:9092
  LOG_FILE: /tmp/debug.log
```

```
deployment yaml 파일

       - name: consumer
          image: 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/lecture-consumer:latest 
          env:
          - name: KAFKA_URL
            valueFrom:
              configMapKeyRef:
                name: kafka-config
                key: KAFKA_URL
          - name: LOG_FILE
            valueFrom:
              configMapKeyRef:
                name: kafka-config
                key: LOG_FILE
```

```
프로그램(python) 파일

from kafka import KafkaConsumer
from logging.config import dictConfig
import logging
import os

kafka_url = os.getenv('KAFKA_URL')
log_file = os.getenv('LOG_FILE')

consumer = KafkaConsumer('lecture', bootstrap_servers=[
                         kafka_url], auto_offset_reset='earliest', enable_auto_commit=True, group_id='alert')


```
## 모니터링
* istio 설치, Kiali 구성, Jaeger 구성, Prometheus 및 Grafana 구성

```
root@labs-1409824742:/home/project/team# kubectl get all -n istio-system
NAME                                        READY   STATUS    RESTARTS   AGE
pod/grafana-767c5487d6-tccjz                1/1     Running   0          24m
pod/istio-egressgateway-74f9769788-5z25x    1/1     Running   0          10h
pod/istio-ingressgateway-74645cb9df-6t4zk   1/1     Running   0          10h
pod/istiod-756fdd548-rz5fn                  1/1     Running   0          10h
pod/jaeger-566c547fb9-d9g8l                 1/1     Running   0          13s
pod/kiali-89fd7f87b-mjtkl                   1/1     Running   0          10h
pod/prometheus-788c945c9c-ft9wd             2/2     Running   0          10h

NAME                           TYPE           CLUSTER-IP       EXTERNAL-IP                                                                    PORT(S)                                                                      AGE
service/grafana                LoadBalancer   10.100.27.22     a17ce955b36c643dba43634c3958f665-1939868886.ap-northeast-2.elb.amazonaws.com   3000:30186/TCP                                                               24m
service/istio-egressgateway    ClusterIP      10.100.128.222   <none>                                                                         80/TCP,443/TCP,15443/TCP                                                     10h
service/istio-ingressgateway   LoadBalancer   10.100.24.155    aac2dd82b25c4416b973f4e43609696a-1789343097.ap-northeast-2.elb.amazonaws.com   15021:31151/TCP,80:30591/TCP,443:31900/TCP,31400:31273/TCP,15443:32249/TCP   10h
service/istiod                 ClusterIP      10.100.167.39    <none>                                                                         15010/TCP,15012/TCP,443/TCP,15014/TCP,853/TCP                                10h
service/kiali                  LoadBalancer   10.100.5.19      a4aba4808c91d4027949418f3d13b407-827239036.ap-northeast-2.elb.amazonaws.com    20001:32662/TCP,9090:30625/TCP                                               10h
service/prometheus             ClusterIP      10.100.32.199    <none>                                                                         9090/TCP                                                                     10h
service/tracing                LoadBalancer   10.100.15.68     ae3b283c82cb34c0f88f2ca92fc70489-1898513510.ap-northeast-2.elb.amazonaws.com   80:30018/TCP                                                                 13s
service/zipkin                 ClusterIP      10.100.208.86    <none>                                                                         9411/TCP                                                                     13s

NAME                                   READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/grafana                1/1     1            1           24m
deployment.apps/istio-egressgateway    1/1     1            1           10h
deployment.apps/istio-ingressgateway   1/1     1            1           10h
deployment.apps/istiod                 1/1     1            1           10h
deployment.apps/jaeger                 1/1     1            1           14s
deployment.apps/kiali                  1/1     1            1           10h
deployment.apps/prometheus             1/1     1            1           10h

NAME                                              DESIRED   CURRENT   READY   AGE
replicaset.apps/grafana-767c5487d6                1         1         1       24m
replicaset.apps/istio-egressgateway-74f9769788    1         1         1       10h
replicaset.apps/istio-ingressgateway-74645cb9df   1         1         1       10h
replicaset.apps/istiod-756fdd548                  1         1         1       10h
replicaset.apps/jaeger-566c547fb9                 1         1         1       13s
replicaset.apps/kiali-89fd7f87b                   1         1         1       10h
replicaset.apps/prometheus-788c945c9c             1         1         1       10h
```
- Tracing (Kiali) http://a4aba4808c91d4027949418f3d13b407-827239036.ap-northeast-2.elb.amazonaws.com:20001/
![image](https://user-images.githubusercontent.com/80744192/119357389-79619080-bce2-11eb-88b8-41fceafc8568.png)

- Jaeger http://ae3b283c82cb34c0f88f2ca92fc70489-1898513510.ap-northeast-2.elb.amazonaws.com/
![image](https://user-images.githubusercontent.com/80744192/119419756-ed795400-bd35-11eb-9530-6af13f3bfa5d.png)

- 모니터링 (Grafana) http://http://a17ce955b36c643dba43634c3958f665-1939868886.ap-northeast-2.elb.amazonaws.com:3000/
![image](https://user-images.githubusercontent.com/80744192/119419299-f1f13d00-bd34-11eb-88ec-6cfce29ca234.png)

