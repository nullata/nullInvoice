// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
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
@Table(name = "invoices")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Invoices.findAll", query = "SELECT i FROM Invoices i"),
    @NamedQuery(name = "Invoices.findById", query = "SELECT i FROM Invoices i WHERE i.id = :id"),
    @NamedQuery(name = "Invoices.findByInvoiceNumber", query = "SELECT i FROM Invoices i WHERE i.invoiceNumber = :invoiceNumber"),
    @NamedQuery(name = "Invoices.findByInvoiceNumberInt", query = "SELECT i FROM Invoices i WHERE i.invoiceNumberInt = :invoiceNumberInt"),
    @NamedQuery(name = "Invoices.findByIssueDate", query = "SELECT i FROM Invoices i WHERE i.issueDate = :issueDate"),
    @NamedQuery(name = "Invoices.findByDueDate", query = "SELECT i FROM Invoices i WHERE i.dueDate = :dueDate"),
    @NamedQuery(name = "Invoices.findByLocaleCode", query = "SELECT i FROM Invoices i WHERE i.localeCode = :localeCode"),
    @NamedQuery(name = "Invoices.findByCurrencyCode", query = "SELECT i FROM Invoices i WHERE i.currencyCode = :currencyCode"),
    @NamedQuery(name = "Invoices.findByStatus", query = "SELECT i FROM Invoices i WHERE i.status = :status"),
    @NamedQuery(name = "Invoices.findBySupplierName", query = "SELECT i FROM Invoices i WHERE i.supplierName = :supplierName"),
    @NamedQuery(name = "Invoices.findBySupplierTaxId", query = "SELECT i FROM Invoices i WHERE i.supplierTaxId = :supplierTaxId"),
    @NamedQuery(name = "Invoices.findBySupplierVatId", query = "SELECT i FROM Invoices i WHERE i.supplierVatId = :supplierVatId"),
    @NamedQuery(name = "Invoices.findBySupplierAddressLine1", query = "SELECT i FROM Invoices i WHERE i.supplierAddressLine1 = :supplierAddressLine1"),
    @NamedQuery(name = "Invoices.findBySupplierAddressLine2", query = "SELECT i FROM Invoices i WHERE i.supplierAddressLine2 = :supplierAddressLine2"),
    @NamedQuery(name = "Invoices.findBySupplierCity", query = "SELECT i FROM Invoices i WHERE i.supplierCity = :supplierCity"),
    @NamedQuery(name = "Invoices.findBySupplierRegion", query = "SELECT i FROM Invoices i WHERE i.supplierRegion = :supplierRegion"),
    @NamedQuery(name = "Invoices.findBySupplierPostalCode", query = "SELECT i FROM Invoices i WHERE i.supplierPostalCode = :supplierPostalCode"),
    @NamedQuery(name = "Invoices.findBySupplierCountry", query = "SELECT i FROM Invoices i WHERE i.supplierCountry = :supplierCountry"),
    @NamedQuery(name = "Invoices.findBySupplierEmail", query = "SELECT i FROM Invoices i WHERE i.supplierEmail = :supplierEmail"),
    @NamedQuery(name = "Invoices.findBySupplierPhone", query = "SELECT i FROM Invoices i WHERE i.supplierPhone = :supplierPhone"),
    @NamedQuery(name = "Invoices.findByClientName", query = "SELECT i FROM Invoices i WHERE i.clientName = :clientName"),
    @NamedQuery(name = "Invoices.findByClientTaxId", query = "SELECT i FROM Invoices i WHERE i.clientTaxId = :clientTaxId"),
    @NamedQuery(name = "Invoices.findByClientVatId", query = "SELECT i FROM Invoices i WHERE i.clientVatId = :clientVatId"),
    @NamedQuery(name = "Invoices.findByClientAddressLine1", query = "SELECT i FROM Invoices i WHERE i.clientAddressLine1 = :clientAddressLine1"),
    @NamedQuery(name = "Invoices.findByClientAddressLine2", query = "SELECT i FROM Invoices i WHERE i.clientAddressLine2 = :clientAddressLine2"),
    @NamedQuery(name = "Invoices.findByClientCity", query = "SELECT i FROM Invoices i WHERE i.clientCity = :clientCity"),
    @NamedQuery(name = "Invoices.findByClientRegion", query = "SELECT i FROM Invoices i WHERE i.clientRegion = :clientRegion"),
    @NamedQuery(name = "Invoices.findByClientPostalCode", query = "SELECT i FROM Invoices i WHERE i.clientPostalCode = :clientPostalCode"),
    @NamedQuery(name = "Invoices.findByClientCountry", query = "SELECT i FROM Invoices i WHERE i.clientCountry = :clientCountry"),
    @NamedQuery(name = "Invoices.findByClientEmail", query = "SELECT i FROM Invoices i WHERE i.clientEmail = :clientEmail"),
    @NamedQuery(name = "Invoices.findByClientPhone", query = "SELECT i FROM Invoices i WHERE i.clientPhone = :clientPhone"),
    @NamedQuery(name = "Invoices.findBySubtotal", query = "SELECT i FROM Invoices i WHERE i.subtotal = :subtotal"),
    @NamedQuery(name = "Invoices.findByTaxTotal", query = "SELECT i FROM Invoices i WHERE i.taxTotal = :taxTotal"),
    @NamedQuery(name = "Invoices.findByTotal", query = "SELECT i FROM Invoices i WHERE i.total = :total"),
    @NamedQuery(name = "Invoices.findByCreatedAt", query = "SELECT i FROM Invoices i WHERE i.createdAt = :createdAt"),
    @NamedQuery(name = "Invoices.findByUpdatedAt", query = "SELECT i FROM Invoices i WHERE i.updatedAt = :updatedAt")})
public class Invoices implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "invoice_number")
    private String invoiceNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "invoice_number_int")
    private long invoiceNumberInt;
    @Basic(optional = false)
    @NotNull
    @Column(name = "issue_date")
    @Temporal(TemporalType.DATE)
    private Date issueDate;
    @Column(name = "due_date")
    @Temporal(TemporalType.DATE)
    private Date dueDate;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 8)
    @Column(name = "locale_code")
    private String localeCode;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 3)
    @Column(name = "currency_code")
    private String currencyCode;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 32)
    @Column(name = "status")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "supplier_name")
    private String supplierName;
    @Size(max = 64)
    @Column(name = "supplier_tax_id")
    private String supplierTaxId;
    @Size(max = 64)
    @Column(name = "supplier_vat_id")
    private String supplierVatId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "supplier_address_line1")
    private String supplierAddressLine1;
    @Size(max = 255)
    @Column(name = "supplier_address_line2")
    private String supplierAddressLine2;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "supplier_city")
    private String supplierCity;
    @Size(max = 128)
    @Column(name = "supplier_region")
    private String supplierRegion;
    @Size(max = 32)
    @Column(name = "supplier_postal_code")
    private String supplierPostalCode;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "supplier_country")
    private String supplierCountry;
    @Size(max = 255)
    @Column(name = "supplier_email")
    private String supplierEmail;
    @Size(max = 64)
    @Column(name = "supplier_phone")
    private String supplierPhone;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "client_name")
    private String clientName;
    @Size(max = 64)
    @Column(name = "client_tax_id")
    private String clientTaxId;
    @Size(max = 64)
    @Column(name = "client_vat_id")
    private String clientVatId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "client_address_line1")
    private String clientAddressLine1;
    @Size(max = 255)
    @Column(name = "client_address_line2")
    private String clientAddressLine2;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "client_city")
    private String clientCity;
    @Size(max = 128)
    @Column(name = "client_region")
    private String clientRegion;
    @Size(max = 32)
    @Column(name = "client_postal_code")
    private String clientPostalCode;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "client_country")
    private String clientCountry;
    @Size(max = 255)
    @Column(name = "client_email")
    private String clientEmail;
    @Size(max = 64)
    @Column(name = "client_phone")
    private String clientPhone;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "subtotal")
    private BigDecimal subtotal;
    @Basic(optional = false)
    @NotNull
    @Column(name = "tax_total")
    private BigDecimal taxTotal;
    @Basic(optional = false)
    @NotNull
    @Column(name = "total")
    private BigDecimal total;
    //@Lob
    @Size(max = 65535)
    @Column(name = "notes")
    private String notes;
    //@Lob
    @Size(max = 16777215)
    @Column(name = "invoice_html")
    private String invoiceHtml;
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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "invoiceId")
    private Collection<InvoiceItems> invoiceItemsCollection;
    @JoinColumn(name = "template_id", referencedColumnName = "id")
    @ManyToOne
    private InvoiceTemplates templateId;
    @JoinColumn(name = "client_party_id", referencedColumnName = "id")
    @ManyToOne
    private Parties clientPartyId;
    @JoinColumn(name = "supplier_party_id", referencedColumnName = "id")
    @ManyToOne
    private Parties supplierPartyId;

    public Invoices() {
    }

    public Invoices(Long id) {
        this.id = id;
    }

    public Invoices(Long id, String invoiceNumber, long invoiceNumberInt, Date issueDate, String localeCode, String currencyCode, String status, String supplierName, String supplierAddressLine1, String supplierCity, String supplierCountry, String clientName, String clientAddressLine1, String clientCity, String clientCountry, BigDecimal subtotal, BigDecimal taxTotal, BigDecimal total, Date createdAt, Date updatedAt) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.invoiceNumberInt = invoiceNumberInt;
        this.issueDate = issueDate;
        this.localeCode = localeCode;
        this.currencyCode = currencyCode;
        this.status = status;
        this.supplierName = supplierName;
        this.supplierAddressLine1 = supplierAddressLine1;
        this.supplierCity = supplierCity;
        this.supplierCountry = supplierCountry;
        this.clientName = clientName;
        this.clientAddressLine1 = clientAddressLine1;
        this.clientCity = clientCity;
        this.clientCountry = clientCountry;
        this.subtotal = subtotal;
        this.taxTotal = taxTotal;
        this.total = total;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public long getInvoiceNumberInt() {
        return invoiceNumberInt;
    }

    public void setInvoiceNumberInt(long invoiceNumberInt) {
        this.invoiceNumberInt = invoiceNumberInt;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierTaxId() {
        return supplierTaxId;
    }

    public void setSupplierTaxId(String supplierTaxId) {
        this.supplierTaxId = supplierTaxId;
    }

    public String getSupplierVatId() {
        return supplierVatId;
    }

    public void setSupplierVatId(String supplierVatId) {
        this.supplierVatId = supplierVatId;
    }

    public String getSupplierAddressLine1() {
        return supplierAddressLine1;
    }

    public void setSupplierAddressLine1(String supplierAddressLine1) {
        this.supplierAddressLine1 = supplierAddressLine1;
    }

    public String getSupplierAddressLine2() {
        return supplierAddressLine2;
    }

    public void setSupplierAddressLine2(String supplierAddressLine2) {
        this.supplierAddressLine2 = supplierAddressLine2;
    }

    public String getSupplierCity() {
        return supplierCity;
    }

    public void setSupplierCity(String supplierCity) {
        this.supplierCity = supplierCity;
    }

    public String getSupplierRegion() {
        return supplierRegion;
    }

    public void setSupplierRegion(String supplierRegion) {
        this.supplierRegion = supplierRegion;
    }

    public String getSupplierPostalCode() {
        return supplierPostalCode;
    }

    public void setSupplierPostalCode(String supplierPostalCode) {
        this.supplierPostalCode = supplierPostalCode;
    }

    public String getSupplierCountry() {
        return supplierCountry;
    }

    public void setSupplierCountry(String supplierCountry) {
        this.supplierCountry = supplierCountry;
    }

    public String getSupplierEmail() {
        return supplierEmail;
    }

    public void setSupplierEmail(String supplierEmail) {
        this.supplierEmail = supplierEmail;
    }

    public String getSupplierPhone() {
        return supplierPhone;
    }

    public void setSupplierPhone(String supplierPhone) {
        this.supplierPhone = supplierPhone;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientTaxId() {
        return clientTaxId;
    }

    public void setClientTaxId(String clientTaxId) {
        this.clientTaxId = clientTaxId;
    }

    public String getClientVatId() {
        return clientVatId;
    }

    public void setClientVatId(String clientVatId) {
        this.clientVatId = clientVatId;
    }

    public String getClientAddressLine1() {
        return clientAddressLine1;
    }

    public void setClientAddressLine1(String clientAddressLine1) {
        this.clientAddressLine1 = clientAddressLine1;
    }

    public String getClientAddressLine2() {
        return clientAddressLine2;
    }

    public void setClientAddressLine2(String clientAddressLine2) {
        this.clientAddressLine2 = clientAddressLine2;
    }

    public String getClientCity() {
        return clientCity;
    }

    public void setClientCity(String clientCity) {
        this.clientCity = clientCity;
    }

    public String getClientRegion() {
        return clientRegion;
    }

    public void setClientRegion(String clientRegion) {
        this.clientRegion = clientRegion;
    }

    public String getClientPostalCode() {
        return clientPostalCode;
    }

    public void setClientPostalCode(String clientPostalCode) {
        this.clientPostalCode = clientPostalCode;
    }

    public String getClientCountry() {
        return clientCountry;
    }

    public void setClientCountry(String clientCountry) {
        this.clientCountry = clientCountry;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxTotal() {
        return taxTotal;
    }

    public void setTaxTotal(BigDecimal taxTotal) {
        this.taxTotal = taxTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getInvoiceHtml() {
        return invoiceHtml;
    }

    public void setInvoiceHtml(String invoiceHtml) {
        this.invoiceHtml = invoiceHtml;
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
    public Collection<InvoiceItems> getInvoiceItemsCollection() {
        return invoiceItemsCollection;
    }

    public void setInvoiceItemsCollection(Collection<InvoiceItems> invoiceItemsCollection) {
        this.invoiceItemsCollection = invoiceItemsCollection;
    }

    public InvoiceTemplates getTemplateId() {
        return templateId;
    }

    public void setTemplateId(InvoiceTemplates templateId) {
        this.templateId = templateId;
    }

    public Parties getClientPartyId() {
        return clientPartyId;
    }

    public void setClientPartyId(Parties clientPartyId) {
        this.clientPartyId = clientPartyId;
    }

    public Parties getSupplierPartyId() {
        return supplierPartyId;
    }

    public void setSupplierPartyId(Parties supplierPartyId) {
        this.supplierPartyId = supplierPartyId;
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
        if (!(object instanceof Invoices)) {
            return false;
        }
        Invoices other = (Invoices) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.nullinvoice.entity.Invoices[ id=" + id + " ]";
    }

}
