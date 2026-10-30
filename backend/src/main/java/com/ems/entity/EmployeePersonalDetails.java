package com.ems.entity;

import com.ems.security.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * Sensitive identity data, 1:1 with {@link Employee} and sharing its primary key.
 * Aadhaar and PAN are encrypted in the database; the hash columns enforce uniqueness.
 */
@Entity
@Table(name = "employee_personal_details", uniqueConstraints = {
        @UniqueConstraint(name = "uk_personal_aadhaar_hash", columnNames = "aadhaar_hash"),
        @UniqueConstraint(name = "uk_personal_pan_hash", columnNames = "pan_hash")
})
public class EmployeePersonalDetails {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", foreignKey = @ForeignKey(name = "fk_personal_employee"))
    private Employee employee;

    /** Plain 12 digits in Java, AES-GCM ciphertext in the database. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "aadhaar_encrypted", nullable = false, length = 255)
    private String aadhaarNumber;

    @Column(name = "aadhaar_hash", nullable = false, length = 64)
    private String aadhaarHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "pan_encrypted", nullable = false, length = 255)
    private String panNumber;

    @Column(name = "pan_hash", nullable = false, length = 64)
    private String panHash;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    public Long getEmployeeId() { return employeeId; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }
    public String getAadhaarHash() { return aadhaarHash; }
    public void setAadhaarHash(String aadhaarHash) { this.aadhaarHash = aadhaarHash; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
    public String getPanHash() { return panHash; }
    public void setPanHash(String panHash) { this.panHash = panHash; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
}
