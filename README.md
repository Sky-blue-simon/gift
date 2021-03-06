# gift

# 온라인 수강신청 및 Point 와 Gift 지급

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
    1. 강의 결제가 완료 되어야만 수강 신청 완료 할 수 있음 Sync 호출 (팀 과제)
    2. 포인트 지급이 완료되면 기프도 완료되어야 한다. Sync 호출
1. 장애격리
    1. 수강신청 시스템이 과중되면 사용자를 잠시동안 받지 않고 신청을 잠시 후에 하도록 유도한다  Circuit breaker (팀 과제)
    2. 포인트(point)-->기프트(gift) 연결 시, 요청이 과도할 경우 CB 를 통하여 장애격리.
1. 성능
    1. 학생이 마이페이지에서 등록된 강의와 수강 및 교재 배송 상태를 확인할 수 있어야 한다  CQRS (중복)
    1. 수강신청/배송 상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다  Event driven (팀 과제)
    2. 학생이 마이페이지에서 지급된 포인트 갯수를 확인할 수 있어야 한다. CQRS
    2. 강의 결제가 취소 될 때 포인트의 상태도 바뀌어야 한다. Event driven

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

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언함. 
 (예시는 gift 마이크로 서비스). 가능한 중학교 수준의 영어 사용함. 

```
package lecture;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Gift_table")
public class Gift {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long classId;
    private Long fee;
    private String student;
    private Integer giftstock;

    @PostPersist
    public void onPostPersist(){
        GiftRegistered giftRegistered = new GiftRegistered();
        BeanUtils.copyProperties(this, giftRegistered);
        giftRegistered.publishAfterCommit();
    }

    @PreRemove
    public void onPreRemove(){
        GiftCanceled giftCanceled = new GiftCanceled();
        BeanUtils.copyProperties(this, giftCanceled);
        giftCanceled.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }
    public Long getFee() {
        return fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }
    public String getStudent() {
        return student;
    }

    public void setStudent(String student) {
        this.student = student;
    }
    public Integer getGiftstock() {
        return giftstock;
    }

    public void setGiftstock(Integer giftstock) {
        this.giftstock = giftstock;
    }

}

```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package lecture;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="gifts", path="gifts")
public interface GiftRepository extends PagingAndSortingRepository<Gift, Long>{

    Gift findByClassId(Long classId);

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

![image](https://user-images.githubusercontent.com/80744224/121448679-1c005b80-c9d3-11eb-8577-6023d6dee8cd.png)


- 기프트 등록 확인

![image](https://user-images.githubusercontent.com/80744224/121448415-8ebd0700-c9d2-11eb-9e06-ff97cf9e257d.png)



- 크롬에서 기프트 등록 확인

![image](https://user-images.githubusercontent.com/80744224/121448697-26baf080-c9d3-11eb-9613-09fff88157ea.png)



- My Page

![image](https://user-images.githubusercontent.com/80744224/121448522-cf1c8500-c9d2-11eb-8eb3-90e20c0122e7.png)


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

- 기프트 서비스를 호출하기 위하여 FeignClient 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

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




## 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 포인트(point)-->기프트(gift) 시의 연결을 RESTful Request/Response 로 연동하여 구현하였고, 요청이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 1000 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml

spring:
  profiles: docker
  cloud:
    stream:
      kafka:
        binder:
          brokers: my-kafka.kafka.svc.cluster.local:9092
        streams:
          binder:
            configuration:
              default:
                key:
                  serde: org.apache.kafka.common.serialization.Serdes$StringSerde
                value:
                  serde: org.apache.kafka.common.serialization.Serdes$StringSerde
      bindings:
        event-in:
          group: point
          destination: lecture
          contentType: application/json
        event-out:
          destination: lecture
          contentType: application/json
feign:
  hystrix:
    enabled: true
hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 1000
```



## 오토스케일, Readiness Probe, livenessProbe

* 오토스케일이 가능하도록 HPA 를 설정한다

kubectl autoscale deploy gift --min=1 --max=10 --cpu-percent=15


* 부하테스터 siege 툴 사용

siege -c150 -t30S -r10 -v --content-type "application/json" 'http://a2c1c1b2c20e1474b87e68c5ae666a92-978533572.ap-southeast-2.elb.amazonaws.com:8080/gifts POST {"courseId": 1, "fee": 10000, "student": "gil-dong",}'


- 오토스케일 모니터링

watch kubectl get pod,hpa

![image](https://user-images.githubusercontent.com/80744224/121455331-36d8cd00-c9df-11eb-8bd8-04b010e76735.png)




- Readiness Probe 와 livenessProbe 설정 제거

- 오코스케일 설정 제거 버전으로 배포

kubectl apply -f deployment_no.yml

![image](https://user-images.githubusercontent.com/80744224/121455749-f7f74700-c9df-11eb-9056-67ad839c45c3.png)

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gift
  labels:
    app: gift
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gift
  template:
    metadata:
      labels:
        app: gift
    spec:
      containers:
        - name: gift
          image: 879772956301.dkr.ecr.ap-southeast-2.amazonaws.com/user09-gift:latest
          ports:
            - containerPort: 8080
          resources:
            limits:
              cpu: 1000m
            requests:
              cpu: 1000m
```

- seige 로 부하 생성
 
```
siege -c150 -t30S -r10 -v --content-type "application/json" 'http://a2c1c1b2c20e1474b87e68c5ae666a92-978533572.ap-southeast-2.elb.amazonaws.com:8080/gifts POST {"courseId": 1, "fee": 10000, "student": "gil-dong",}'
```

- Availability 가 100% 미만 확인

![image](https://user-images.githubusercontent.com/80744224/121455490-81f2e000-c9df-11eb-93b4-afff6823916d.png)


- 오토스케일로 gift Pod 다수 생성

![image](https://user-images.githubusercontent.com/80744224/121455836-1a896000-c9e0-11eb-9424-54f0960583d6.png)


- Availability 가 평소 100% 미만으로 떨어지는 것을 확인함
- 이를 막기위해 Readiness Probe 와 livenessProbe 를 설정함:


- deployment.yml 의 readiness probe 의 설정:

(gift) deployment.yml 파일
 
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gift
  labels:
    app: gift
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gift
  template:
    metadata:
      labels:
        app: gift
    spec:
      containers:
        - name: gift
          image: 879772956301.dkr.ecr.ap-southeast-2.amazonaws.com/user09-gift:latest
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5

```

- kubectl apply -f deployment.yml

- 동일한 시나리오로 재배포 한 후 Availability 확인:

![image](https://user-images.githubusercontent.com/80744224/121456248-bd41de80-c9e0-11eb-8243-4ce4ff339f53.png)

- gift Pod 1개 유지

![image](https://user-images.githubusercontent.com/80744224/121456229-b6b36700-c9e0-11eb-8861-29d1e258e8c8.png)



## ConfigMap

```
configmap yaml 파일

apiVersion: v1
kind: ConfigMap
metadata:
  name: kafka-config
data:
  KAFKA_URL: my-kafka.kafka.svc.cluster.local:9092
  LOG_FILE: /tmp/debug.log
```

```
deployment yaml 파일

      containers:
        - name: gift
          image: 879772956301.dkr.ecr.ap-southeast-2.amazonaws.com/user09-gift:latest
          ports:
            - containerPort: 8080
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
프로그램에 환경 변수 적용 

    @PostPersist
    public void onPostPersist(){

        System.out.println("########## Configmap KAFKA_URL => " + System.getenv("KAFKA_URL"));
        this.setStatus(System.getenv("KAFKA_URL"));

        System.out.println("########## Configmap LOG_FILE => " + System.getenv("LOG_FILE"));
        this.setStatus(System.getenv("LOG_FILE"));

```

## 모니터링
* istio 설치, Kiali 설치

![image](https://user-images.githubusercontent.com/80744224/121469486-87f4bb00-c9f7-11eb-98ea-24749d27a23b.png)

![image](https://user-images.githubusercontent.com/80744224/121469528-a0fd6c00-c9f7-11eb-9eba-7cc670ffd4c4.png)



