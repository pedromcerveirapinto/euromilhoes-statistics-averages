package com.example.euromillions.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
public interface DrawRepository extends JpaRepository<Draw, LocalDate> {
    @Query("SELECT MAX(d.drawDate) FROM Draw d")
    LocalDate findLatestDrawDate();
}