package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface BillingRepository extends JpaRepository<MainCylinderEntry, Long> {

    /**
     * All FULL cylinder entries for a party within a date range.
     * These are the chargeable movements.
     */
    @Query("select c.date, c.cylinderNo, c.ctype " +
           "from MainCylinderEntry c " +
           "where c.customerName = ?1 " +
           "  and c.ctype != 'EMPTY' " +
           "  and DATE(c.date) >= DATE(?2) " +
           "  and DATE(c.date) <= DATE(?3) " +
           "order by c.date asc")
    List<Object[]> findBillableEntries(String partyName, Date fromDate, Date toDate);
}
