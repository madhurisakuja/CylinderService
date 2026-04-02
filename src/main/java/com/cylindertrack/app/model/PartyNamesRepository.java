package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PartyNamesRepository extends JpaRepository<PartyNames, String> {

    @Query("select distinct p.partyName from PartyNames p order by p.partyName asc")
    List<String> getAllPartyNames();
}
