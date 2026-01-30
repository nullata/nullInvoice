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
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author null
 */
@Entity
@Table(name = "invoice_items")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "InvoiceItems.findAll", query = "SELECT i FROM InvoiceItems i"),
    @NamedQuery(name = "InvoiceItems.findById", query = "SELECT i FROM InvoiceItems i WHERE i.id = :id"),
    @NamedQuery(name = "InvoiceItems.findByLineNumber", query = "SELECT i FROM InvoiceItems i WHERE i.lineNumber = :lineNumber"),
    @NamedQuery(name = "InvoiceItems.findByDescription", query = "SELECT i FROM InvoiceItems i WHERE i.description = :description"),
    @NamedQuery(name = "InvoiceItems.findByQuantity", query = "SELECT i FROM InvoiceItems i WHERE i.quantity = :quantity"),
    @NamedQuery(name = "InvoiceItems.findByUnitPrice", query = "SELECT i FROM InvoiceItems i WHERE i.unitPrice = :unitPrice"),
    @NamedQuery(name = "InvoiceItems.findByTaxRate", query = "SELECT i FROM InvoiceItems i WHERE i.taxRate = :taxRate"),
    @NamedQuery(name = "InvoiceItems.findByLineSubtotal", query = "SELECT i FROM InvoiceItems i WHERE i.lineSubtotal = :lineSubtotal"),
    @NamedQuery(name = "InvoiceItems.findByLineTax", query = "SELECT i FROM InvoiceItems i WHERE i.lineTax = :lineTax"),
    @NamedQuery(name = "InvoiceItems.findByLineTotal", query = "SELECT i FROM InvoiceItems i WHERE i.lineTotal = :lineTotal"),
    @NamedQuery(name = "InvoiceItems.findByCreatedAt", query = "SELECT i FROM InvoiceItems i WHERE i.createdAt = :createdAt")})
public class InvoiceItems implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "line_number")
    private int lineNumber;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 512)
    @Column(name = "description")
    private String description;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "quantity")
    private BigDecimal quantity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "unit_price")
    private BigDecimal unitPrice;
    @Basic(optional = false)
    @NotNull
    @Column(name = "discount")
    private BigDecimal discount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "tax_rate")
    private BigDecimal taxRate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "line_subtotal")
    private BigDecimal lineSubtotal;
    @Basic(optional = false)
    @NotNull
    @Column(name = "line_tax")
    private BigDecimal lineTax;
    @Basic(optional = false)
    @NotNull
    @Column(name = "line_total")
    private BigDecimal lineTotal;
    @Basic(optional = false)
    @NotNull
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "invoice_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Invoices invoiceId;

    public InvoiceItems() {
    }

    public InvoiceItems(Long id) {
        this.id = id;
    }

    public InvoiceItems(Long id, int lineNumber, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal taxRate, BigDecimal lineSubtotal, BigDecimal lineTax, BigDecimal lineTotal, Date createdAt) {
        this.id = id;
        this.lineNumber = lineNumber;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = discount;
        this.taxRate = taxRate;
        this.lineSubtotal = lineSubtotal;
        this.lineTax = lineTax;
        this.lineTotal = lineTotal;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public void setLineSubtotal(BigDecimal lineSubtotal) {
        this.lineSubtotal = lineSubtotal;
    }

    public BigDecimal getLineTax() {
        return lineTax;
    }

    public void setLineTax(BigDecimal lineTax) {
        this.lineTax = lineTax;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Invoices getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Invoices invoiceId) {
        this.invoiceId = invoiceId;
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
        if (!(object instanceof InvoiceItems)) {
            return false;
        }
        InvoiceItems other = (InvoiceItems) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.nullinvoice.entity.InvoiceItems[ id=" + id + " ]";
    }

}
