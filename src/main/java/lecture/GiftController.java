package lecture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

 @RestController
 public class GiftController {

        @Autowired
        GiftRepository giftRepository;


        @RequestMapping(value = "/chkAndModifyGiftStock",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")

        public boolean modifyGiftStock(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
            boolean status = false;

            Long classId = Long.valueOf(request.getParameter("classId"));
            String student = request.getParameter("student");
            Long fee = Long.valueOf(request.getParameter("fee"));

            // Gift gift = giftRepository.findByClassId(classId);

            Gift gift = new Gift();
          //  giftRepository.save(gift);
            
            gift.setClassId(classId);
            gift.setStudent(student);
            gift.setFee(fee);
            if (fee <= 10000) {
                gift.setGiftstock(1);
            }
            else if ( fee <= 20000) {
                gift.setGiftstock(2);
            }
            else if (fee <= 30000) {
                gift.setGiftstock(3);
            } else {
                gift.setGiftstock(4);
            }
            giftRepository.save(gift);
            status = true;
      
            return status;
        }
 }
