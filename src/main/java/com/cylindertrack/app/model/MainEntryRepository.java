package com.cylindertrack.app.model;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface MainEntryRepository extends JpaRepository<MainEntry, Long> {

    // ── Holding calculations ──────────────────────────────────────────────────

    /** Running holding for a sales party: sum(full) - sum(empty) */
    @Query("select sum(e.cfull) - sum(e.cempty) " +
           "from MainEntry e where e.partyName=?1 and e.ctype=?2 and e.isPurchase=?3")
    Integer getHoldingDetails(String partyName, String type, Boolean isPurchase);

    /** Running holding for a purchase party: sum(empty) - sum(full) */
    @Query("select sum(e.cempty) - sum(e.cfull) " +
           "from MainEntry e where e.partyName=?1 and e.ctype=?2 and e.isPurchase=?3")
    Integer getHoldingDetailsForPurchase(String partyName, String type, Boolean isPurchase);

    /** Latest entry date for a party+type (used for back-date prevention) */
    @Query("select max(e.date) from MainEntry e where e.partyName=?1 and e.ctype=?2")
    Date getLastDateEntry(String partyName, String type);

    /** Latest entry date across all entries */
    @Query("select max(e.date) from MainEntry e")
    Date getLatestEntryDate();

    // ── Search / history ──────────────────────────────────────────────────────

    @Query("select e from MainEntry e where e.partyName=?1 order by e.date asc")
    List<MainEntry> findByPartyNameOrderByDate(String partyName);

    @Query("select e from MainEntry e where e.partyName=?1 and e.ctype=?2 order by e.date asc")
    List<MainEntry> findByPartyNameAndType(String partyName, String type);

    /** All entries for a party on a specific date */
    @Query("select e from MainEntry e where e.partyName=?1 and DATE(e.date) = DATE(?2)")
    List<MainEntry> findByPartyAndDate(String partyName, Date date);

    /** All entries on a specific date (for cylinder number form pre-load) */
    @Query("select e from MainEntry e where DATE(e.date) = DATE(?1) and e.isPurchase = false order by e.partyName asc")
    List<MainEntry> findSalesEntriesByDate(Date date);

    /** Holding summary: latest cholding per party per type */
    @Query("select e from MainEntry e where e.id = " +
           "(select max(e2.id) from MainEntry e2 where e2.partyName = e.partyName " +
           " and e2.ctype = e.ctype and e2.isPurchase = false) " +
           "order by e.partyName asc, e.ctype asc")
    List<MainEntry> getCurrentHoldingSummary();

    // ── Delete ────────────────────────────────────────────────────────────────

    @Modifying
    @Transactional
    @Query("delete from MainEntry e where e.partyName=?1 and DATE(e.date) = DATE(?2)")
    void deleteByPartyAndDate(String partyName, Date date);

    // ── All entries for CSV export, sorted ────────────────────────────────────
    List<MainEntry> findByPartyName(String partyName, Sort sort);
}
