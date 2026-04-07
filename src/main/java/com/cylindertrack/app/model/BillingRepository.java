package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface BillingRepository extends JpaRepository<MainCylinderEntry, Long> {

    /**
     * Returns ctype, count(*) for each gas type a party received (FULL only) in a month.
     * Groups by gas type so one line per type on the bill.
     */
    @Query("select c.ctype, count(c) " +
           "from MainCylinderEntry c " +
           "where c.customerName = ?1 " +
           "  and c.ctype != 'EMPTY' " +
           "  and DATE(c.date) >= DATE(?2) " +
           "  and DATE(c.date) <= DATE(?3) " +
           "group by c.ctype " +
           "order by c.ctype")
    List<Object[]> findBillableEntriesGrouped(String partyName, Date fromDate, Date toDate);

    /**
     * All parties who had FULL entries in the given month (for bulk bill generation).
     */
    @Query("select distinct c.customerName from MainCylinderEntry c " +
           "where c.ctype != 'EMPTY' " +
           "  and DATE(c.date) >= DATE(?1) " +
           "  and DATE(c.date) <= DATE(?2) " +
           "order by c.customerName")
    List<String> findPartiesWithActivityInMonth(Date fromDate, Date toDate);
}
