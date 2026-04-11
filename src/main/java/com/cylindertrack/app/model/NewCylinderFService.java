package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface NewCylinderFService extends JpaRepository<MainCylinderEntry, Long> {

    @Query("select c.fullType from MainCylinderEntry c " +
           "where c.id=(select max(c1.id) from MainCylinderEntry c1 where c1.cylinderNo=?1)")
    String getCylinderStatus(Long cylinderNo);

    @Query("select c.customerName from MainCylinderEntry c " +
           "where c.id=(select max(c1.id) from MainCylinderEntry c1 where c1.cylinderNo=?1)")
    String getCylinderHoldingStatus(Long cylinderNo);

    /** Full history for a cylinder, date DESC */
    @Query("select c.date, c.ctype, c.fullType, c.customerName from MainCylinderEntry c " +
           "where c.cylinderNo=?1 order by c.date desc")
    List<List<?>> findAllByCylinderNo(Long cylinderNo);

    /** Cylinder history by number + date range, date DESC */
    @Query("select c.date, c.ctype, c.fullType, c.customerName from MainCylinderEntry c " +
           "where c.cylinderNo=?1 " +
           "  and DATE(c.date)>=DATE(?2) and DATE(c.date)<=DATE(?3) order by c.date desc")
    List<List<?>> findAllByCylinderNoAndDateRange(Long cylinderNo, Date fromDate, Date toDate);

    @Query("select c.id from MainCylinderEntry c " +
           "where c.cylinderNo=?1 and DATE(c.date)=DATE(?2) and c.customerName=?3 and c.ctype=?4")
    @Transactional
    List<Long> findByDetails(Long cylinderNo, Date date, String customerName, String ctype);

    @Query("select c.cylinderNo, c.date, c.ctype, c.fullType, c.customerName from MainCylinderEntry c " +
           "where DATE(c.date)<=DATE(?1) " +
           "  and c.date=(select max(x.date) from MainCylinderEntry x where x.cylinderNo=c.cylinderNo)")
    List<List<?>> getCylindersNotInRotation(Date date);

    @Query("select c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName=?1 and c.ctype=?2 and c.fullType='FULL' " +
           "  and c.id=(select max(c1.id) from MainCylinderEntry c1 where c1.cylinderNo=c.cylinderNo) " +
           "order by c.cylinderNo asc")
    List<Long> findCylindersFullAtParty(String partyName, String gasType);

    /** Full movement history for a party, date DESC */
    @Query("select c.date, c.ctype, c.fullType, c.customerName, c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName=?1 order by c.date desc")
    List<List<?>> findAllByParty(String partyName);

    /** Party history filtered by date range, date DESC */
    @Query("select c.date, c.ctype, c.fullType, c.customerName, c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName=?1 " +
           "  and DATE(c.date)>=DATE(?2) and DATE(c.date)<=DATE(?3) order by c.date desc")
    List<List<?>> findAllByPartyAndDateRange(String partyName, Date fromDate, Date toDate);

    /** FULL cylinders for billing */
    @Query("select c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName=?1 and c.ctype=?2 and c.fullType='FULL' " +
           "  and DATE(c.date)>=DATE(?3) and DATE(c.date)<=DATE(?4) " +
           "order by c.cylinderNo asc")
    List<Long> findFullCylindersForBill(String partyName, String gasType, Date fromDate, Date toDate);

    @Modifying @Transactional
    @Query("delete from MainCylinderEntry e where e.customerName=?1 and DATE(e.date)=DATE(?2)")
    void deleteByPartyAndDate(String partyName, Date date);

    @Modifying @Transactional
    @Query("delete from MainCylinderEntry e " +
           "where e.customerName=?1 and DATE(e.date)=DATE(?2) and e.cylinderNo=?3")
    void deleteByPartyDateAndCylinder(String partyName, Date date, Long cylinderNo);

    @Query("select c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName=?1 and DATE(c.date)=DATE(?2) order by c.cylinderNo asc")
    List<Long> findCylinderNumbersByPartyAndDate(String partyName, Date date);
}
