# gift

# 온라인 수강신청 및 Point 와 Gift 지급

본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 예제입니다.
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 예시 답안을 포함합니다.
- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW

![image](https://user-images.githubusercontent.com/80744224/121329802-7c9a8480-c950-11eb-8e1c-d728dcc14a37.png)


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


# 체크포인트

- 분석 설계
  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


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
![강좌 등록](https://user-images.githubusercontent.com/80744224/121299296-287fa800-c930-11eb-984d-5a538905a839.png)

![강좌 등록 확인](https://user-images.githubusercontent.com/80744224/121299250-156cd800-c930-11eb-8b9d-12a1721ed911.png)

- 수강 신청
![수강 신청](https://user-images.githubusercontent.com/80744224/121299431-6250ae80-c930-11eb-96fe-908c401cce3d.png)

![수강 신청 확인](https://user-images.githubusercontent.com/80744224/121299528-8613f480-c930-11eb-96d2-57fe15b6650b.png)

- 포인트 등록 확인
![포인트 등록 확인](https://user-images.githubusercontent.com/80744224/121309393-16f0cd00-c93d-11eb-9468-9a0b5d1b6b6d.png)

- 크롬에서 포인트 등록 확인
![포인트 등록 확인](https://user-images.githubusercontent.com/80744224/121309678-70f19280-c93d-11eb-8cea-37f01e05c579.png)

- 기프트 등록 확인
![기프트 등록 확인](https://user-images.githubusercontent.com/80744224/121309494-3687f580-c93d-11eb-8dff-2bc4ec14ce51.png)

- 크롬에서 기프트 등록 확인
![기프트 등록 확인](https://user-images.githubusercontent.com/80744224/121309886-b01fe380-c93d-11eb-8162-57469f65d65c.png)






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



## 동기식 호출 과 Fallback 처리

수강신청(class) 한 후, 포인트(point) -> 기프트(gift) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리함.

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 


![image](https://user-images.githubusercontent.com/80744224/121320144-d8144480-c947-11eb-8f3a-685641c1b5cb.png)


- FallBack 처리
```
# (class) PaymentServiceFallback.java

package lecture.external;

import org.springframework.stereotype.Component;

@Component
public class PaymentServiceFallback implements PaymentService {
    @Override
    public boolean pay(Payment payment) {
        //do nothing if you want to forgive it

        System.out.println("Circuit breaker has been opened. Fallback returned instead.");
        return false;
    }
}
```

- 주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# Class.java (Entity)
    @PostPersist
    public void onPostPersist() throws Exception {
        Payment payment = new Payment();
        payment.setClassId(this.getId());
        payment.setCourseId(this.getCourseId());
        payment.setFee(this.getFee());
        payment.setStudent(this.getStudent());
        payment.setStatus("PAYMENT_COMPLETED");
        payment.setTextBook(this.getTextBook());

        if (ClassApplication.applicationContext.getBean(PaymentService.class).pay(payment)) {
            ClassRegistered classRegistered = new ClassRegistered();
            BeanUtils.copyProperties(this, classRegistered);
            classRegistered.publishAfterCommit();
        }else {
            throw new RollbackException("Failed during payment");
        }
    }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:


```
# 기프트 (gift) 서비스를 잠시 내려놓음
cd ./gift/kubernetes
kubectl delete -f deployment.yml

# 수강 신청 후, 포인트(point) -> 기프트(gift) 갈 때 Fail

![image](https://user-images.githubusercontent.com/80744224/121329591-4f4dd680-c950-11eb-869d-5a564be35b1a.png)


![image](https://user-images.githubusercontent.com/80744224/121329568-4957f580-c950-11eb-945b-b29297a5e0d5.png)


![image](https://user-images.githubusercontent.com/80744224/121323898-2d9e2080-c94b-11eb-846f-c8314b5ab197.png)


# 결제서비스 재기동
![image](https://user-images.githubusercontent.com/80744224/121323931-32fb6b00-c94b-11eb-8b6c-f92f86713566.png)


# 수강 신청 후, 포인트와 기프트 정상 조회
http GET http://a6e770600b6db4906b16f6cffd71f5b6-1894361895.ap-southeast-2.elb.amazonaws.com:8080/points
http GET http://a6e770600b6db4906b16f6cffd71f5b6-1894361895.ap-southeast-2.elb.amazonaws.com:8080/gifts

![image](https://user-images.githubusercontent.com/80744224/121324229-7a81f700-c94b-11eb-94d4-9ededce6606a.png)

![image](https://user-images.githubusercontent.com/80744224/121323960-3abb0f80-c94b-11eb-8ee7-a5cc13bac923.png)

```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. 


## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


결제가 이루어진 후에 배송시스템으로 이를 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 배송 시스템의 처리를 위하여 결제주문이 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 결제이력에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
package lecture;

@Entity
@Table(name = "Payment_table")
public class Payment {

...
    @PostPersist
    public void onPostPersist() {
        PaymentApproved paymentApproved = new PaymentApproved();
        BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();
    }
```
- 배송 서비스에서는 결제승인 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
package lecture;

...

@Service
public class PolicyHandler {

    @Autowired
    DeliveryRepository deliveryRepository;

    @Autowired
    CourseRepository courseRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_DeliveryTextbook(@Payload PaymentApproved paymentApproved) {

        if (paymentApproved.isMe()) {

            Delivery delivery = new Delivery();
            delivery.setClassId(paymentApproved.getClassId());
            delivery.setCourseId(paymentApproved.getCourseId());
            delivery.setStudent(paymentApproved.getStudent());
            delivery.setPaymentId(paymentApproved.getId());
            delivery.setTextBook(paymentApproved.getTextBook());
            delivery.setStatus("DELIVERY_START");

            Optional<Course> opt = courseRepository.findById(paymentApproved.getClassId());

            Course course;
            if (opt.isPresent()) {
                course = opt.get();
                delivery.setTextBook(course.getTextBook());
            }
            deliveryRepository.save(delivery);
        }
    }
```
실제 구현을 하자면, 학생은 결제완료와 동시에 책 배송 및 수강신청이 완료 되었다는 SMS를 받고, 이후 수강/결제/배송 상태 변경은 Mypage Aggregate 내에서 처리
  
```
    @Autowired

    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentApproved_then_CREATE_1(@Payload PaymentApproved paymentApproved) {
        try {
            if (paymentApproved.isMe()) {
                InquiryMypage inquiryMypage = new InquiryMypage();
                inquiryMypage.setClassId(paymentApproved.getClassId());
                inquiryMypage.setPaymentId(paymentApproved.getId());
                inquiryMypage.setCourseId(paymentApproved.getCourseId());
                inquiryMypage.setFee(paymentApproved.getFee());
                inquiryMypage.setStudent(paymentApproved.getStudent());
                inquiryMypage.setPaymentStatus(paymentApproved.getStatus());
                inquiryMypage.setTextBook(paymentApproved.getTextBook());
                inquiryMypage.setStatus("CLASS_START");
				
                // view 레파지토리에 save
                inquiryMypageRepository.save(inquiryMypage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	@StreamListener(KafkaProcessor.INPUT)
    public void whenTextbookDeliveried_then_UPDATE_2(@Payload TextbookDeliveried textbookDeliveried) {
        try {
            if (textbookDeliveried.isMe()) {
                List<InquiryMypage> inquiryMypageList = inquiryMypageRepository
                        .findByPaymentId(textbookDeliveried.getPaymentId());
                for (InquiryMypage inquiryMypage : inquiryMypageList) {
                    inquiryMypage.setDeliveryStatus(textbookDeliveried.getStatus());

                    // view 레파지 토리에 save
                    inquiryMypageRepository.save(inquiryMypage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

배송 시스템은 수강신청/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 배송시스템이 유지보수로 인해 잠시 내려간 상태라도 수강신청을 받는데 문제가 없다:

```
# 배송 서비스 (course) 를 잠시 내려놓음 
cd ./course/kubernetes
kubectl delete -f deployment.yml

# 수강 신청
http POST http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/classes courseId=1 fee=10000 student=KimSoonHee textBook=eng_book #Success
http POST http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/classes courseId=1 fee=12000 student=JohnDoe textBook=kor_book #Success

# 수강 신청 상태 확인
http GET http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/classes   # 수강 신청 완료 
http GET http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/inquiryMypages  # 배송 상태 "deliveryStatus": null

# 배송 서비스 (course) 기동
kubectl apply -f deployment.yml

# 배송 상태 확인
http GET http://aa8ed367406254fc0b4d73ae65aa61cd-24965970.ap-northeast-2.elb.amazonaws.com:8080/inquiryMypages  # 배송 상태 "deliveryStatus": "DELIVERY_START"
```


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

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 50명
- 30초 동안 실시

```
$ siege -c50 -t30S -r10 -v --content-type "application/json" 'http://gateway:8080/classes POST {"courseId": 1, "fee": 10000, "student": "gil-dong", "textBook": "eng_book"}'

defaulting to time-based testing: 30 seconds
** SIEGE 4.0.4
** Preparing 10 concurrent users for battle.
The server is now under siege...

HTTP/1.1 201     0.68 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.69 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.85 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.80 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.90 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.70 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.20 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.79 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.80 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.71 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.71 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.81 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.10 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.69 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.09 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.80 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     1.38 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.19 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.20 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.80 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.80 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.70 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.70 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.90 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.90 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.71 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.70 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.77 secs:     250 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 201     0.72 secs:     250 bytes ==> POST http://gateway:8080/classes

* 요청이 과도하여 CB를 동작함 요청을 차단

HTTP/1.1 500     1.31 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.51 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.42 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.52 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.51 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.71 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.99 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     2.60 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.70 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.70 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.72 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.91 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.68 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     2.10 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     2.80 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.82 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     2.08 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     0.38 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.60 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     1.90 secs:     221 bytes ==> POST http://gateway:8080/classes
HTTP/1.1 500     0.49 secs:     221 bytes ==> POST http://gateway:8080/classes

* 끝까지 500 에러 발생


Lifting the server siege...
Transactions:                    408 hits
Availability:                  29.04 %
Elapsed time:                  29.92 secs
Data transferred:               0.31 MB
Response time:                  3.57 secs
Transaction rate:              13.64 trans/sec
Throughput:                     0.01 MB/sec
Concurrency:                   48.67
Successful transactions:         408
Failed transactions:             997
Longest transaction:            4.19
Shortest transaction:           0.09

```
- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 하지만, 29% 가 성공하였고, 71%가 실패했다는 것은 고객 사용성에 있어 좋지 않기 때문에 동적 Scale out (replica의 자동적 추가,HPA) 을 통하여 시스템을 확장 해주는 후속처리가 필요.

## 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 


- 수강신청 및 결제서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 30프로를 넘어서면 replica 를 10개까지 늘려준다
```
kubectl autoscale deploy class --min=1 --max=10 --cpu-percent=30
kubectl autoscale deploy pay --min=1 --max=10 --cpu-percent=30
```
- CB 에서 했던 방식대로 워크로드를 30초 동안 걸어준다. 
```
siege -c50 -t30S -r10 -v --content-type "application/json" 'http://gateway:8080/classes POST {"courseId": 1, "fee": 10000, "student": "gil-dong", "textBook": "eng_book"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
watch kubectl get pod,hpa
```
- 어느정도 시간이 흐른 후 (약 30초) 스케일 아웃이 벌어지는 것을 확인할 수 있다:
```
NAME                                        REFERENCE          TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/class   Deployment/class   69%/30%   1         10        5          6m25s
horizontalpodautoscaler.autoscaling/pay     Deployment/pay     27%/30%   1         10        4          6m24s

NAME                           READY   STATUS    RESTARTS   AGE
pod/alert-7cbc74668-clsdv      2/2     Running   0          43m
pod/class-5864b4f7cc-bm88m     0/1     Running   0          19s
pod/class-5864b4f7cc-dbzvz     1/1     Running   0          3m37s
pod/class-5864b4f7cc-fjscn     0/1     Running   0          34s
pod/class-5864b4f7cc-jq2sq     0/1     Running   0          34s
pod/class-5864b4f7cc-rzrz9     1/1     Running   0          13m
pod/course-64978c8dd8-nwlxs    1/1     Running   0          42m
pod/gateway-65d7888594-mqpls   1/1     Running   0          41m
pod/pay-575875fc9-gtkss        1/1     Running   0          2m36s
pod/pay-575875fc9-h28rg        1/1     Running   0          2m36s
pod/pay-575875fc9-kk56d        1/1     Running   2          13m
pod/pay-575875fc9-r2ll2        1/1     Running   0          2m36s
pod/siege                      1/1     Running   0          5h41m
:
```
- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 
```
Lifting the server siege...
Transactions:                   1916 hits
Availability:                  97.21 %
Elapsed time:                  29.15 secs
Data transferred:               0.47 MB
Response time:                  0.74 secs
Transaction rate:              65.73 trans/sec
Throughput:                     0.02 MB/sec
Concurrency:                   48.70
Successful transactions:        1916
Failed transactions:              55
Longest transaction:            8.44
Shortest transaction:           0.00
```


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

