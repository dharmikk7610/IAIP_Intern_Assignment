package com.ems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Department-wise counter for employee codes. Always read with a row lock before use. */
@Entity
@Table(name = "employee_code_sequences")
public class EmployeeCodeSequence {

    @Id
    @Column(name = "department_id")
    private Long departmentId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", foreignKey = @ForeignKey(name = "fk_code_seq_department"))
    private Department department;

    @Column(name = "next_value", nullable = false)
    private Integer nextValue;

    /** Returns the next code (e.g. "I0007") and advances the counter. Codes are never reused. */
    public String allocateNextCode() {
        String code = department.getCodePrefix() + String.format("%04d", nextValue);
        nextValue++;
        return code;
    }

    public Long getDepartmentId() { return departmentId; }
    public Department getDepartment() { return department; }
    public Integer getNextValue() { return nextValue; }
}
