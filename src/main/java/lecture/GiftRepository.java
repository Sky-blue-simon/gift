package lecture;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="gifts", path="gifts")
public interface GiftRepository extends PagingAndSortingRepository<Gift, Long>{

    Gift findByClassId(Long classId);

}
