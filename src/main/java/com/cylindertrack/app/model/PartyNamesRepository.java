package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PartyNamesRepository extends JpaRepository<PartyNames, String> {

    /** Sales parties — names NOT starting with '1' */
    @Query("select distinct p.partyName from PartyNames p " +
           "where p.partyName not like '1%' order by p.partyName asc")
    List<String> getAllPartyNames();

    /** Purchase parties — names starting with '1' */
    @Query("select distinct p.partyName from PartyNames p " +
           "where p.partyName like '1%' order by p.partyName asc")
    List<String> getAllPartyNamesPurchaser();

    /** All parties combined */
    @Query("select distinct p.partyName from PartyNames p order by p.partyName asc")
    List<String> getAllPartyNamesAll();
}
