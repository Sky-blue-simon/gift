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
