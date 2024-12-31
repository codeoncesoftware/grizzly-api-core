/*
 * Copyright © 2020 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.codeonce.grizzly.core.domain.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document("invoice")
public class Invoice {

    @Id
    private String id;
    private Date billingPeriodBeginningDate;
    private Date billingPeriodEndgDate;
    private String invoice_code;
    private long amount;
    private Boolean monthly;
    private String username;
    private Contact billingContact;
    private String numTva;
    private List<ProductInvoice> products;
    private String userId;
    private OrganisationAddress organisationAddress;


    public Invoice(Boolean monthly, String userId, String username, Date billingPeriodBeginningDate
            , Date billingPeriodEndgDate) {
        this.billingPeriodBeginningDate = billingPeriodBeginningDate;
        this.monthly = monthly;
        this.userId = userId;
        this.username = username;
        this.billingPeriodEndgDate = billingPeriodEndgDate;
    }


    public Date getBillingPeriodBeginningDate() {
        return billingPeriodBeginningDate;
    }


    public void setBillingPeriodBeginningDate(Date billingPeriodBeginningDate) {
        this.billingPeriodBeginningDate = billingPeriodBeginningDate;
    }


    public Date getBillingPeriodEndgDate() {
        return billingPeriodEndgDate;
    }


    public void setBillingPeriodEndgDate(Date billingPeriodEndgDate) {
        this.billingPeriodEndgDate = billingPeriodEndgDate;
    }


    public OrganisationAddress getOrganisationAddress() {
        return organisationAddress;
    }


    public void setOrganisationAddress(OrganisationAddress organisationAddress) {
        this.organisationAddress = organisationAddress;
    }


    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }


    public Contact getBillingContact() {
        return billingContact;
    }


    public void setBillingContact(Contact billingContact) {
        this.billingContact = billingContact;
    }


    public String getNumTva() {
        return numTva;
    }


    public void setNumTva(String numTva) {
        this.numTva = numTva;
    }


    public List<ProductInvoice> getProducts() {
        return products;
    }


    public void setProducts(List<ProductInvoice> products) {
        this.products = products;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInvoice_code() {
        return invoice_code;
    }

    public void setInvoice_code(String invoice_code) {
        this.invoice_code = invoice_code;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Boolean getMonthly() {
        return monthly;
    }

    public void setMonthly(Boolean monthly) {
        this.monthly = monthly;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


}
