package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface NewCylinderFService extends JpaRepository<MainCylinderEntry, Long> {

    @Query("select c.ctype from MainCylinderEntry c " +
           "where c.id = (select max(c1.id) from MainCylinderEntry c1 where c1.cylinderNo = ?1)")
    String getCylinderStatus(Long cylinderNo);

    @Query("select c.customerName from MainCylinderEntry c " +
           "where c.id = (select max(c1.id) from MainCylinderEntry c1 where c1.cylinderNo = ?1)")
    String getCylinderHoldingStatus(Long cylinderNo);

    @Query("select c.date, c.ctype, c.customerName from MainCylinderEntry c " +
           "where c.cylinderNo = ?1 order by c.date asc")
    List<List<?>> findAllByCylinderNo(Long cylinderNo);

    @Query("select c.id from MainCylinderEntry c " +
           "where c.cylinderNo = ?1 " +
           "  and DATE(c.date) = DATE(?2) " +
           "  and c.customerName = ?3 " +
           "  and c.ctype = ?4")
    @Transactional
    List<Long> findByDetails(Long cylinderNo, Date date, String customerName, String ctype);

    @Query("select c.cylinderNo, c.date, c.ctype, c.customerName from MainCylinderEntry c " +
           "where DATE(c.date) <= DATE(?1) " +
           "  and c.date = (select max(x.date) from MainCylinderEntry x where x.cylinderNo = c.cylinderNo)")
    List<List<?>> getCylindersNotInRotation(Date date);

    /**
     * Cylinder numbers of a given gas type that were last sent FULL to a party —
     * candidates for EMPTY return dropdown.
     */
    @Query("select c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName = ?1 " +
           "  and c.ctype = ?2 " +
           "  and c.id = (select max(c1.id) from MainCylinderEntry c1 where c1.cylinderNo = c.cylinderNo) " +
           "order by c.cylinderNo asc")
    List<Long> findCylindersFullAtParty(String partyName, String gasType);

    /**
     * All entries for a party by cylinder number, for the detail history view.
     */
    @Query("select c.date, c.ctype, c.customerName, c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName = ?1 order by c.date asc")
    List<List<?>> findAllByParty(String partyName);
}
