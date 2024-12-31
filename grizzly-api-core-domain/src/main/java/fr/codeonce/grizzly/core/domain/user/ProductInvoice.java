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

public class ProductInvoice {

    private Product productName;
    private String offer;
    private long subamount;
    private long tva;
    private long totalamount;

    public Product getProductName() {
        return productName;
    }

    public void setProductName(Product productName) {
        this.productName = productName;
    }

    public String getOffer() {
        return offer;
    }

    public void setOffer(String offer) {
        this.offer = offer;
    }

    public long getSubamount() {
        return subamount;
    }

    public void setSubamount(long subamount) {
        this.subamount = subamount;
    }

    public long getTva() {
        return tva;
    }

    public void setTva(long tva) {
        this.tva = tva;
    }

    public long getTotalamount() {
        return totalamount;
    }

    public void setTotalamount(long totalamount) {
        this.totalamount = totalamount;
    }


}
