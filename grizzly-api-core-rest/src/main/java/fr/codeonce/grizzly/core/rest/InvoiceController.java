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
package fr.codeonce.grizzly.core.rest;

import com.itextpdf.text.DocumentException;
import fr.codeonce.grizzly.core.domain.user.Invoice;
import fr.codeonce.grizzly.core.service.user.InvoiceService;
import fr.codeonce.grizzly.core.service.user.UserService;
import fr.codeonce.grizzly.core.service.util.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = {"*"}, allowedHeaders = {"*"})
@RequestMapping("/api/invoice")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private ResourceLoader resourceloader;

    @Autowired
    private UserService user;

    @GetMapping("/{userId}")
    public List<Invoice> getInvoices(@PathVariable String userId) {
        log.info("Request to fetch invoices with user id: {}", userId);
        return this.invoiceService.getInvoicesByUser(userId);
    }

    @GetMapping("/test")
    public void test() throws DocumentException, IOException {


    }


}
