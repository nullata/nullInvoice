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
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author null
 */
@Entity
@Table(name = "invoice_templates")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "InvoiceTemplates.findAll", query = "SELECT i FROM InvoiceTemplates i"),
    @NamedQuery(name = "InvoiceTemplates.findById", query = "SELECT i FROM InvoiceTemplates i WHERE i.id = :id"),
    @NamedQuery(name = "InvoiceTemplates.findByName", query = "SELECT i FROM InvoiceTemplates i WHERE i.name = :name"),
    @NamedQuery(name = "InvoiceTemplates.findByIsDefault", query = "SELECT i FROM InvoiceTemplates i WHERE i.isDefault = :isDefault"),
    @NamedQuery(name = "InvoiceTemplates.findByCreatedAt", query = "SELECT i FROM InvoiceTemplates i WHERE i.createdAt = :createdAt"),
    @NamedQuery(name = "InvoiceTemplates.findByUpdatedAt", query = "SELECT i FROM InvoiceTemplates i WHERE i.updatedAt = :updatedAt")})
public class InvoiceTemplates implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    //@Lob
    @Size(min = 1, max = 16777215)
    @Column(name = "html")
    private String html;
    @Basic(optional = false)
    @NotNull
    @Column(name = "is_default")
    private boolean isDefault;
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
    @OneToMany(mappedBy = "templateId")
    private Collection<Invoices> invoicesCollection;

    public InvoiceTemplates() {
    }

    public InvoiceTemplates(Long id) {
        this.id = id;
    }

    public InvoiceTemplates(Long id, String name, String html, boolean isDefault, Date createdAt, Date updatedAt) {
        this.id = id;
        this.name = name;
        this.html = html;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
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
    public Collection<Invoices> getInvoicesCollection() {
        return invoicesCollection;
    }

    public void setInvoicesCollection(Collection<Invoices> invoicesCollection) {
        this.invoicesCollection = invoicesCollection;
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
        if (!(object instanceof InvoiceTemplates)) {
            return false;
        }
        InvoiceTemplates other = (InvoiceTemplates) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.nullinvoice.entity.InvoiceTemplates[ id=" + id + " ]";
    }

}
