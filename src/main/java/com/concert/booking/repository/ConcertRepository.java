package com.concert.booking.repository;

import com.concert.booking.model.Concert;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Concert i WHERE i.id = :id")
    Optional<Concert> findByConcertIdWithLock(@Param("id") String concertId);

    @Query(value = "SELECT * FROM concerts WHERE name ILIKE :name", nativeQuery = true)
    List<Concert> getByName(@Param("name") String name);

    @Query("SELECT c FROM Concert c WHERE c.saleOpen <= :currentTime AND c.saleClose >= :currentTime ORDER BY c.createdAt DESC")
    List<Concert> getConcertAvailable(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT c FROM Concert c WHERE c.saleOpen <= :currentTime AND c.saleClose >= :currentTime AND c.id = :id")
    Concert getConcertAvailableById(@Param("currentTime") LocalDateTime currentTime, @Param("id") String id);

}
