package com.cylindertrack.app.model;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public interface MainEntryRepository extends JpaRepository<MainEntry, Long> {

    @Query("select sum(e.cfull)-sum(e.cempty) from MainEntry e where e.partyName=?1 and e.ctype=?2 and e.isPurchase=?3")
    Integer getHoldingDetails(String partyName, String type, Boolean isPurchase);

    @Query("select sum(e.cempty)-sum(e.cfull) from MainEntry e where e.partyName=?1 and e.ctype=?2 and e.isPurchase=?3")
    Integer getHoldingDetailsForPurchase(String partyName, String type, Boolean isPurchase);

    @Query("select max(e.date) from MainEntry e where e.partyName=?1 and e.ctype=?2")
    Date getLastDateEntry(String partyName, String type);

    @Query("select max(e.date) from MainEntry e")
    Date getLatestEntryDate();

    @Query("select e from MainEntry e where e.partyName=?1 order by e.date desc")
    List<MainEntry> findByPartyNameOrderByDateDesc(String partyName);

    @Query("select e from MainEntry e where e.partyName=?1 and e.ctype=?2 order by e.date desc")
    List<MainEntry> findByPartyNameAndType(String partyName, String type);

    @Query("select e from MainEntry e where e.partyName=?1 and DATE(e.date)=DATE(?2)")
    List<MainEntry> findByPartyAndDate(String partyName, Date date);

    @Query("select e from MainEntry e where DATE(e.date)=DATE(?1) and e.isPurchase=false order by e.partyName asc")
    List<MainEntry> findSalesEntriesByDate(Date date);

    @Query("select e from MainEntry e where e.id = " +
           "(select max(e2.id) from MainEntry e2 where e2.partyName=e.partyName " +
           " and e2.ctype=e.ctype and e2.isPurchase=false) " +
           "order by e.partyName asc, e.ctype asc")
    List<MainEntry> getCurrentHoldingSummary();

    /** Holding summary filtered by date range */
    @Query("select e from MainEntry e where e.id = " +
           "(select max(e2.id) from MainEntry e2 where e2.partyName=e.partyName " +
           " and e2.ctype=e.ctype and (e2.isPurchase=false or e2.isPurchase is null) " +
           " and DATE(e2.date)>=DATE(?1) and DATE(e2.date)<=DATE(?2)) " +
           "order by e.partyName asc, e.ctype asc")
    List<MainEntry> getCurrentHoldingSummaryForRange(Date fromDate, Date toDate);

    /** Party entries with date range, date DESC */
    @Query("select e from MainEntry e where e.partyName=?1 " +
           "and DATE(e.date)>=DATE(?2) and DATE(e.date)<=DATE(?3) order by e.date desc")
    List<MainEntry> findByPartyNameAndDateRange(String partyName, Date fromDate, Date toDate);

    /** Party+type entries with date range, date DESC */
    @Query("select e from MainEntry e where e.partyName=?1 and e.ctype=?2 " +
           "and DATE(e.date)>=DATE(?3) and DATE(e.date)<=DATE(?4) order by e.date desc")
    List<MainEntry> findByPartyTypeAndDateRange(String partyName, String ctype, Date from, Date to);

    @Modifying @Transactional
    @Query("delete from MainEntry e where e.partyName=?1 and DATE(e.date)=DATE(?2)")
    void deleteByPartyAndDate(String partyName, Date date);

    List<MainEntry> findByPartyName(String partyName, Sort sort);
/*
    @Query("select e.party_name as partyName, "+
    "COALESCE(MAX(CASE WHEN e.ctype = 'OXY' THEN e.cfull END), 0) AS OXY, "+
    "COALESCE(MAX(CASE WHEN e.ctype = 'CO2' THEN e.cfull END), 0) AS CO2, "+
    "COALESCE(MAX(CASE WHEN e.ctype = 'LPG' THEN e.cfull END), 0) AS LPG, "+
    "COALESCE(MAX(CASE WHEN e.ctype = 'DA' THEN e.cfull END), 0) AS DA, "+
    "COALESCE(MAX(CASE WHEN e.ctype = 'ARGON' THEN e.cfull END), 0) AS ARGON, "+
    "COALESCE(MAX(CASE WHEN e.ctype = 'ARGOSHIELD' THEN e.cfull END), 0) AS ARGOSHIELD, "+
    "COALESCE(MAX(CASE WHEN e.ctype = 'NITROGEN' THEN e.cfull END), 0) AS NITROGEN "+
    "from MainEntry e where DATE(e.createdAt)=DATE(?1) and (e.isPurchase=false or e.isPurchase is null) "+ 
    "GROUP BY e.partyName WITH ROLLUP", nativeQuery = true)
    List<Map<String, Object>> findSaleEntriesByCreatedDate(Date date);
*/
        @Query(value = "SELECT e.party_name as partyName, " +
           "COALESCE(MAX(CASE WHEN e.ctype = 'OXY' THEN e.cfull END), 0) AS OXY, " +
           "COALESCE(MAX(CASE WHEN e.ctype = 'CO2' THEN e.cfull END), 0) AS CO2, " +
           "COALESCE(MAX(CASE WHEN e.ctype = 'LPG' THEN e.cfull END), 0) AS LPG, " +
           "COALESCE(MAX(CASE WHEN e.ctype = 'DA' THEN e.cfull END), 0) AS DA, " +
           "COALESCE(MAX(CASE WHEN e.ctype = 'ARGON' THEN e.cfull END), 0) AS ARGON, " +
           "COALESCE(MAX(CASE WHEN e.ctype = 'ARGOSHIELD' THEN e.cfull END), 0) AS ARGOSHIELD, " +
           "COALESCE(MAX(CASE WHEN e.ctype = 'NITROGEN' THEN e.cfull END), 0) AS NITROGEN " +
           "FROM main_entry e WHERE DATE(e.created_at) = DATE(?1) " +
           "AND (e.is_purchase = false OR e.is_purchase IS NULL) " +
           "GROUP BY e.party_name WITH ROLLUP", nativeQuery = true)
    List<Map<String, Object>> findSaleEntriesByCreatedDate(Date date);

}
