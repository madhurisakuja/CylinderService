package com.cylindertrack.app.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounter, Integer> {

    @Modifying
    @Transactional
    @Query("update InvoiceCounter c set c.currentNumber = c.currentNumber + 1 where c.id = 1")
    void increment();

    @Modifying
    @Transactional
    @Query("update InvoiceCounter c set c.currentNumber = c.currentNumber - 1 where c.id = 1 and c.currentNumber > 1")
    void decrement();

    @Modifying
    @Transactional
    @Query("update InvoiceCounter c set c.currentNumber = 1, c.fiscalStartYear = ?1 where c.id = 1")
    void resetForNewFiscalYear(Integer fiscalStartYear);
}
