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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;


@Document("user")
public class User {

    @Id
//	@Indexed(unique=true)
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String organization;
    private String phone;
    private String password;
    private boolean enabled;
    private boolean firstTime;
    @CreatedDate
    private Date registrationDate;
    private Date lastConnection;
    private Boolean checkNewsletter;
    private String customerId;
    private AccountType accountType = AccountType.FREE;
    private String apiKey;
    private Boolean hasPaymentMethod;
    private Contact techContact;
    private Contact billingContact;
    private PaymentMethod paymentMethod;
    private Date buyOfferDate;
    private Date nextInvoiceDate;
    private int welcomeEmailDay;


    public Date getBuyOfferDate() {
        return buyOfferDate;
    }

    public void setBuyOfferDate(Date buyOfferDate) {
        this.buyOfferDate = buyOfferDate;
    }

    public Date getNextInvoiceDate() {
        return nextInvoiceDate;
    }

    public void setNextInvoiceDate(Date nextInvoiceDate) {
        this.nextInvoiceDate = nextInvoiceDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Contact getTechContact() {
        return techContact;
    }

    public void setTechContact(Contact techContact) {
        this.techContact = techContact;
    }

    public Contact getBillingContact() {
        return billingContact;
    }

    public void setBillingContact(Contact billingContact) {
        this.billingContact = billingContact;
    }

    public Boolean getHasPaymentMethod() {
        return hasPaymentMethod;
    }

    public void setHasPaymentMethod(Boolean hasPaymentMethod) {
        this.hasPaymentMethod = hasPaymentMethod;
    }

    public Boolean getCheckNewsletter() {
        return checkNewsletter;
    }

    public void setCheckNewsletter(Boolean checkNewsletter) {
        this.checkNewsletter = checkNewsletter;
    }

    public Date getLastConnection() {
        return lastConnection;
    }

    public void setLastConnection(Date lastConnection) {
        this.lastConnection = lastConnection;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getId() {
        return id;
    }


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isFirstTime() {
        return firstTime;
    }

    public void setFirstTime(boolean firstTime) {
        this.firstTime = firstTime;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public int getWelcomeEmailDay() {
        return welcomeEmailDay;
    }

    public void setWelcomeEmailDay(int welcomeEmailDay) {
        this.welcomeEmailDay = welcomeEmailDay;
    }

}
