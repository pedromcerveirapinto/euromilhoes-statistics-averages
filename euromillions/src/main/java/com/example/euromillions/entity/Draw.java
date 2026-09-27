package com.example.euromillions.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class Draw {
    @Id
    private LocalDate drawDate;
    private int n1, n2, n3, n4, n5;
    private int s1, s2;

    public Draw() {}

    public Draw(LocalDate drawDate, int n1, int n2, int n3, int n4, int n5, int s1, int s2) {
        this.drawDate = drawDate;
        this.n1 = n1; this.n2 = n2; this.n3 = n3; this.n4 = n4; this.n5 = n5;
        this.s1 = s1; this.s2 = s2;
    }

    public LocalDate getDrawDate() { return drawDate; }
    public int[] getMainNumbers() { return new int[]{n1, n2, n3, n4, n5}; }
    public int[] getStars() { return new int[]{s1, s2}; }
}