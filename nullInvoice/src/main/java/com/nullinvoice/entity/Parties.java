// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author null
 */
@Entity
@Table(name = "parties")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Parties.findAll", query = "SELECT p FROM Parties p"),
    @NamedQuery(name = "Parties.findById", query = "SELECT p FROM Parties p WHERE p.id = :id"),
    @NamedQuery(name = "Parties.findByRole", query = "SELECT p FROM Parties p WHERE p.role = :role"),
    @NamedQuery(name = "Parties.findByName", query = "SELECT p FROM Parties p WHERE p.name = :name"),
    @NamedQuery(name = "Parties.findByTaxId", query = "SELECT p FROM Parties p WHERE p.taxId = :taxId"),
    @NamedQuery(name = "Parties.findByVatId", query = "SELECT p FROM Parties p WHERE p.vatId = :vatId"),
    @NamedQuery(name = "Parties.findByAddressLine1", query = "SELECT p FROM Parties p WHERE p.addressLine1 = :addressLine1"),
    @NamedQuery(name = "Parties.findByAddressLine2", query = "SELECT p FROM Parties p WHERE p.addressLine2 = :addressLine2"),
    @NamedQuery(name = "Parties.findByCity", query = "SELECT p FROM Parties p WHERE p.city = :city"),
    @NamedQuery(name = "Parties.findByRegion", query = "SELECT p FROM Parties p WHERE p.region = :region"),
    @NamedQuery(name = "Parties.findByPostalCode", query = "SELECT p FROM Parties p WHERE p.postalCode = :postalCode"),
    @NamedQuery(name = "Parties.findByCountry", query = "SELECT p FROM Parties p WHERE p.country = :country"),
    @NamedQuery(name = "Parties.findByEmail", query = "SELECT p FROM Parties p WHERE p.email = :email"),
    @NamedQuery(name = "Parties.findByPhone", query = "SELECT p FROM Parties p WHERE p.phone = :phone"),
    @NamedQuery(name = "Parties.findByCreatedAt", query = "SELECT p FROM Parties p WHERE p.createdAt = :createdAt"),
    @NamedQuery(name = "Parties.findByUpdatedAt", query = "SELECT p FROM Parties p WHERE p.updatedAt = :updatedAt")})
public class Parties implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 16)
    @Column(name = "role")
    private String role;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name")
    private String name;
    @Size(max = 64)
    @Column(name = "tax_id")
    private String taxId;
    @Size(max = 64)
    @Column(name = "vat_id")
    private String vatId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "address_line1")
    private String addressLine1;
    @Size(max = 255)
    @Column(name = "address_line2")
    private String addressLine2;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "city")
    private String city;
    @Size(max = 128)
    @Column(name = "region")
    private String region;
    @Size(max = 32)
    @Column(name = "postal_code")
    private String postalCode;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "country")
    private String country;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Size(max = 255)
    @Column(name = "email")
    private String email;
    // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax number consider using this annotation to enforce field validation
    @Size(max = 64)
    @Column(name = "phone")
    private String phone;

    // Localization fields (for suppliers)
    @Size(max = 8)
    @Column(name = "locale_code")
    private String localeCode;
    @Size(max = 8)
    @Column(name = "locale_language")
    private String localeLanguage;
    @Size(max = 8)
    @Column(name = "locale_country")
    private String localeCountry;
    @Size(max = 3)
    @Column(name = "currency_code")
    private String currencyCode;
    @Column(name = "default_tax_rate")
    private BigDecimal defaultTaxRate;
    @Size(max = 32)
    @Column(name = "date_pattern")
    private String datePattern;
    @Column(name = "invoice_start_number")
    private Long invoiceStartNumber;
    @Size(max = 16)
    @Column(name = "invoice_prefix")
    private String invoicePrefix;
    @Column(name = "invoice_number_digits")
    private Integer invoiceNumberDigits;
    @JoinColumn(name = "default_template_id", referencedColumnName = "id")
    @ManyToOne
    private InvoiceTemplates defaultTemplateId;
    @Column(name = "last_invoice_number")
    private Long lastInvoiceNumber;

    // Soft delete
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Basic(optional = false)
    @NotNull
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Basic(optional = false)
    @NotNull
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    @OneToMany(mappedBy = "clientPartyId")
    private Collection<Invoices> invoicesAsClient;
    @OneToMany(mappedBy = "supplierPartyId")
    private Collection<Invoices> invoicesAsSupplier;

    public Parties() {
    }

    public Parties(Long id) {
        this.id = id;
    }

    public Parties(Long id, String role, String name, String addressLine1, String city, String country, Date createdAt, Date updatedAt) {
        this.id = id;
        this.role = role;
        this.name = name;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.country = country;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getVatId() {
        return vatId;
    }

    public void setVatId(String vatId) {
        this.vatId = vatId;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getLocaleLanguage() {
        return localeLanguage;
    }

    public void setLocaleLanguage(String localeLanguage) {
        this.localeLanguage = localeLanguage;
    }

    public String getLocaleCountry() {
        return localeCountry;
    }

    public void setLocaleCountry(String localeCountry) {
        this.localeCountry = localeCountry;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getDefaultTaxRate() {
        return defaultTaxRate;
    }

    public void setDefaultTaxRate(BigDecimal defaultTaxRate) {
        this.defaultTaxRate = defaultTaxRate;
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    public Long getInvoiceStartNumber() {
        return invoiceStartNumber;
    }

    public void setInvoiceStartNumber(Long invoiceStartNumber) {
        this.invoiceStartNumber = invoiceStartNumber;
    }

    public String getInvoicePrefix() {
        return invoicePrefix;
    }

    public void setInvoicePrefix(String invoicePrefix) {
        this.invoicePrefix = invoicePrefix;
    }

    public Integer getInvoiceNumberDigits() {
        return invoiceNumberDigits;
    }

    public void setInvoiceNumberDigits(Integer invoiceNumberDigits) {
        this.invoiceNumberDigits = invoiceNumberDigits;
    }

    public InvoiceTemplates getDefaultTemplateId() {
        return defaultTemplateId;
    }

    public void setDefaultTemplateId(InvoiceTemplates defaultTemplateId) {
        this.defaultTemplateId = defaultTemplateId;
    }

    public Long getLastInvoiceNumber() {
        return lastInvoiceNumber;
    }

    public void setLastInvoiceNumber(Long lastInvoiceNumber) {
        this.lastInvoiceNumber = lastInvoiceNumber;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @XmlTransient
    public Collection<Invoices> getInvoicesAsClient() {
        return invoicesAsClient;
    }

    public void setInvoicesAsClient(Collection<Invoices> invoicesAsClient) {
        this.invoicesAsClient = invoicesAsClient;
    }

    @XmlTransient
    public Collection<Invoices> getInvoicesAsSupplier() {
        return invoicesAsSupplier;
    }

    public void setInvoicesAsSupplier(Collection<Invoices> invoicesAsSupplier) {
        this.invoicesAsSupplier = invoicesAsSupplier;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Parties)) {
            return false;
        }
        Parties other = (Parties) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.nullinvoice.entity.Parties[ id=" + id + " ]";
    }

}
