package com.suhas.examallocation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "hall")
public class Hall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Min(1)
    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Min(1)
    @Column(name = "total_columns", nullable = false)
    private int totalColumns;

    public Hall() {
    }

    public Hall(String name, int totalRows, int totalColumns) {
        this.name = name;
        this.totalRows = totalRows;
        this.totalColumns = totalColumns;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getTotalColumns() {
        return totalColumns;
    }

    public void setTotalColumns(int totalColumns) {
        this.totalColumns = totalColumns;
    }

    // capacity = rows * columns, not stored in DB — computed on the fly
    public int getCapacity() {
        return totalRows * totalColumns;
    }
}
