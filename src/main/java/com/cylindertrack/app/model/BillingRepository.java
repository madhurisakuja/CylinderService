package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface BillingRepository extends JpaRepository<MainEntry, Long> {

    @Query("select e.ctype, sum(e.cfull) from MainEntry e " +
           "where e.partyName=?1 and (e.isPurchase=false or e.isPurchase is null) " +
           "  and DATE(e.date)>=DATE(?2) and DATE(e.date)<=DATE(?3) " +
           "group by e.ctype order by e.ctype")
    List<Object[]> findBillableEntriesGrouped(String partyName, Date fromDate, Date toDate);

    /**
     * All sales parties with activity in a given month — for bulk generation.
     */
    @Query("select distinct e.partyName from MainEntry e " +
           "where (e.isPurchase=false or e.isPurchase is null) " +
           "  and DATE(e.date)>=DATE(?1) and DATE(e.date)<=DATE(?2) " +
           "order by e.partyName")
    List<String> findPartiesWithActivityInMonth(Date fromDate, Date toDate);

    /**
     * Cylinder numbers dispatched as FULL to a party for a gas type in the month.
     * Only FULL movements — not empty returns.
     */
    @Query("select c.cylinderNo from MainCylinderEntry c " +
           "where c.customerName = ?1 " +
           "  and c.ctype = ?2 " +
           "  and c.fullType = 'FULL' " +
           "  and DATE(c.date) >= DATE(?3) " +
           "  and DATE(c.date) <= DATE(?4) " +
           "order by c.cylinderNo")
    List<Long> findCylinderNumbersForBill(String partyName, String gasType,
                                          Date fromDate, Date toDate);
}
