package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


import org.springframework.data.jpa.repository.Modifying;
import jakarta.transaction.Transactional;

@Repository
public interface PartyPriceRepository extends JpaRepository<PartyPrice, Long> {
    Optional<PartyPrice> findByPartyNameAndGasType(String partyName, String gasType);
    List<PartyPrice> findByPartyNameOrderByGasType(String partyName);

    @Query("select distinct pp.partyName from PartyPrice pp order by pp.partyName")
    List<String> findDistinctPartyNames();
    
    @Modifying
    @Transactional
    @org.springframework.data.jpa.repository.Query("delete from PartyPrice p where p.partyName=?1")
    void deleteAllByPartyName(String partyName);

}
