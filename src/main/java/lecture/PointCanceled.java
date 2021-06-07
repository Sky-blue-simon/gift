
package lecture;

public class PointCanceled extends AbstractEvent {

    private Long id;
    private String student;
    private Long point;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getStudent() {
        return student;
    }

    public void setStudent(String student) {
        this.student = student;
    }
    public Long getPoint() {
        return point;
    }

    public void setPoint(Long point) {
        this.point = point;
    }
}

