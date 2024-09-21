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
package fr.codeonce.grizzly.core.service.user;

import fr.codeonce.grizzly.core.domain.user.ProductInvoice;

import java.util.List;

public class InvoiceDto {

    long invAmount;
    long taxe;
    List<ProductInvoice> productList;
    long amountPay;

    public long getInvAmount() {
        return invAmount;
    }

    public void setInvAmount(long invAmount) {
        this.invAmount = invAmount;
    }

    public long getTaxe() {
        return taxe;
    }

    public void setTaxe(long taxe) {
        this.taxe = taxe;
    }

    public List<ProductInvoice> getProductList() {
        return productList;
    }

    public void setProductList(List<ProductInvoice> productList) {
        this.productList = productList;
    }

    public long getAmountPay() {
        return amountPay;
    }

    public void setAmountPay(long amountPay) {
        this.amountPay = amountPay;
    }


}
