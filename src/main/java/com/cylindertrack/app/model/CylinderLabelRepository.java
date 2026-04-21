package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CylinderLabelRepository extends JpaRepository<CylinderLabel, Long> {

    /** Party-specific label for a gas type */
    @Query("select l from CylinderLabel l where l.gasType=?1 and l.partyName=?2")
    Optional<CylinderLabel> findByGasTypeAndPartyName(String gasType, String partyName);

    /** Default label (partyName is null) for a gas type */
    @Query("select l from CylinderLabel l where l.gasType=?1 and l.partyName is null")
    Optional<CylinderLabel> findDefaultByGasType(String gasType);

    /** All default labels */
    @Query("select l from CylinderLabel l where l.partyName is null order by l.gasType asc")
    List<CylinderLabel> findAllDefaults();

    /** All party-specific overrides */
    @Query("select l from CylinderLabel l where l.partyName is not null order by l.partyName asc, l.gasType asc")
    List<CylinderLabel> findAllOverrides();
    @org.springframework.data.jpa.repository.Modifying
    @jakarta.transaction.Transactional
    @org.springframework.data.jpa.repository.Query("delete from CylinderLabel l where l.partyName=?1")
    void deleteAllByPartyName(String partyName);

}
