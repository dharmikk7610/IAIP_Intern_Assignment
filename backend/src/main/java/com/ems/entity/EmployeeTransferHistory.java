package com.ems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One row per department transfer; rows are only ever inserted, never updated. */
@Entity
@Table(name = "employee_transfer_history",
        indexes = @Index(name = "idx_transfer_employee", columnList = "employee_id"))
public class EmployeeTransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transfer_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "old_department_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transfer_old_department"))
    private Department oldDepartment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "new_department_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transfer_new_department"))
    private Department newDepartment;

    @Column(name = "old_employee_code", nullable = false, length = 10)
    private String oldEmployeeCode;

    @Column(name = "new_employee_code", nullable = false, length = 10)
    private String newEmployeeCode;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferred_by_user_id",
            foreignKey = @ForeignKey(name = "fk_transfer_user"))
    private User transferredBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Department getOldDepartment() { return oldDepartment; }
    public void setOldDepartment(Department oldDepartment) { this.oldDepartment = oldDepartment; }
    public Department getNewDepartment() { return newDepartment; }
    public void setNewDepartment(Department newDepartment) { this.newDepartment = newDepartment; }
    public String getOldEmployeeCode() { return oldEmployeeCode; }
    public void setOldEmployeeCode(String oldEmployeeCode) { this.oldEmployeeCode = oldEmployeeCode; }
    public String getNewEmployeeCode() { return newEmployeeCode; }
    public void setNewEmployeeCode(String newEmployeeCode) { this.newEmployeeCode = newEmployeeCode; }
    public LocalDate getTransferDate() { return transferDate; }
    public void setTransferDate(LocalDate transferDate) { this.transferDate = transferDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public User getTransferredBy() { return transferredBy; }
    public void setTransferredBy(User transferredBy) { this.transferredBy = transferredBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
