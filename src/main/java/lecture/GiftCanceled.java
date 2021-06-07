package lecture;

public class GiftCanceled extends AbstractEvent {

    private Long id;
    private Long classId;
    private Long fee;
    private String student;
    private Integer giftstock;

    public GiftCanceled(){
        super();
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
