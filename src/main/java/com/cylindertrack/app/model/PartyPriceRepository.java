package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartyPriceRepository extends JpaRepository<PartyPrice, Long> {
    Optional<PartyPrice> findByPartyNameAndGasType(String partyName, String gasType);
    List<PartyPrice> findByPartyNameOrderByGasType(String partyName);

    @Query("select distinct pp.partyName from PartyPrice pp order by pp.partyName")
    List<String> findDistinctPartyNames();
}
