package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface NewCylinderFService extends JpaRepository<MainCylinderEntry, Long> {

    /**
     * Returns the fullType (FULL/EMPTY) of the most recent entry for a cylinder.
     * Used to detect consecutive FULL entries (duplicate warning).
     */
    @Query("select c.fullType from MainCylinderEntry c " +
           "where c.id = (select max(c1.id) from MainCylinderEntry c1 " +
           "              where c1.cylinderNo = ?1)")
    String getCylinderStatus(Long cylinderNo);

    /**
     * Returns the customer who last held this cylinder (most recent entry).
     */
    @Query("select c.customerName from MainCylinderEntry c " +
           "where c.id = (select max(c1.id) from MainCylinderEntry c1 " +
           "              where c1.cylinderNo = ?1)")
    String getCylinderHoldingStatus(Long cylinderNo);

    /**
     * Full movement history for a cylinder — shows gas type AND direction (FULL/EMPTY).
     */
    @Query("select c.date, c.ctype, c.fullType, c.customerName from MainCylinderEntry c " +
           "where c.cylinderNo = ?1 order by c.date asc")
    List<List<?>> findAllByCylinderNo(Long cylinderNo);

    @Query("select c.id from MainCylinderEntry c " +
           "where c.cylinderNo = ?1 " +
           "  and DATE(c.date) = DATE(?2) " +
           "  and c.customerName = ?3 " +
           "  and c.ctype = ?4")
    @Transactional
    List<Long> findByDetails(Long cylinderNo, Date date, String customerName, String ctype);

    @Query("select c.cylinderNo, c.date, c.ctype, c.fullType, c.customerName " +
           "from MainCylinderEntry c " +
           "where DATE(c.date) <= DATE(?1) " +
           "  and c.date = (select max(x.date) from MainCylinderEntry x " +
           "                where x.cylinderNo = c.cylinderNo)")
    List<List<?>> getCylindersNotInRotation(Date date);

    /**
     * Cylinders of a given gas type whose LAST entry was FULL at this party.
     * These are the candidates for the EMPTY return dropdown.
     * Excludes cylinders whose last entry was already EMPTY.
     */
    @Query("select c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName = ?1 " +
           "  and c.ctype = ?2 " +
           "  and c.fullType = 'FULL' " +
           "  and c.id = (select max(c1.id) from MainCylinderEntry c1 " +
           "              where c1.cylinderNo = c.cylinderNo) " +
           "order by c.cylinderNo asc")
    List<Long> findCylindersFullAtParty(String partyName, String gasType);

    /**
     * All cylinder-level movements for a party — detail history view.
     * Returns date, gas type, direction (FULL/EMPTY), customer, cylinder number.
     */
    @Query("select c.date, c.ctype, c.fullType, c.customerName, c.cylinderNo " +
           "from MainCylinderEntry c " +
           "where c.customerName = ?1 order by c.date asc")
    List<List<?>> findAllByParty(String partyName);

    /**
     * Cylinders dispatched as FULL to a party for a gas type in a date range.
     * Used by billing to list cylinder numbers on the invoice.
     */
    @Query("select c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName = ?1 " +
           "  and c.ctype = ?2 " +
           "  and c.fullType = 'FULL' " +
           "  and DATE(c.date) >= DATE(?3) " +
           "  and DATE(c.date) <= DATE(?4) " +
           "order by c.cylinderNo asc")
    List<Long> findFullCylindersForBill(String partyName, String gasType,
                                        Date fromDate, Date toDate);
}
