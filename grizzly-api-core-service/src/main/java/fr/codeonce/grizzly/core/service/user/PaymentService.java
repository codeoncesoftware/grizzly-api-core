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

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import fr.codeonce.grizzly.core.domain.analytics.ProjectUseRepository;
import fr.codeonce.grizzly.core.domain.user.*;
import fr.codeonce.grizzly.core.service.util.EmailService;
import org.joda.time.DateTimeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectUseRepository projectUseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserService user;

    @Autowired
    private EmailService emailService;

    @Value("${stripeKey : 'stripeKeyTest'}")
    private String stripeKey;

    PaymentService() {
        Stripe.apiKey = this.stripeKey;
    }

    public Map<String, String> savePaymentIntent(String email) {
        CustomerCreateParams params = CustomerCreateParams.builder().setEmail(email).build();

        try {
            Customer customer = Customer.create(params);
            Map<String, Object> setupParams = new HashMap<>();
            setupParams.put("customer", customer.getId());
            SetupIntent intent = SetupIntent.create(setupParams);
            Map<String, String> secrets = new HashMap<String, String>();
            secrets.put("clientSecret", intent.getClientSecret());
            secrets.put("customerId", customer.getId());
            return secrets;

        } catch (StripeException e) {
            log.error("error in saving payment intent {}", e);

        }
        return null;
    }

    // @Scheduled(cron = "*/10 * * * * ?")
    public void chargeMembers() {
        Date today = new Date();
        DateTimeComparator dateTimeComparator = DateTimeComparator.getDateOnlyInstance();

        this.userRepository.findAll().forEach(el -> {
            List<ProductInvoice> products = new ArrayList<ProductInvoice>();
            if (el.getNextInvoiceDate() != null && dateTimeComparator.compare(el.getNextInvoiceDate(), today) == 0) {

                if (el.getAccountType().equals(AccountType.LIMITLESS)) {
                    prepareTotalPayment(el, 5000L, 1000L, products, 500000L, "LIMITLESS");
                }
                if (el.getAccountType().equals(AccountType.SCALABILITY)) {
                    long amountToPay = 0;
                    long invoiceAmount = 0;
                    long tax = 0;
                    int count = getNumberOfUsedProjectsWithinLastMonth(el);
                    if (count != 0) {
                        amountToPay += 29000L * count;
                        invoiceAmount += 290L * count;
                        tax += (long) (290L * 0.2) * count;
                    }
                    prepareTotalPayment(el, invoiceAmount, tax, products, amountToPay, "SCALABILITY");
                }
                if (el.getAccountType().equals(AccountType.LIMITLESS)
                        || el.getAccountType().equals(AccountType.SCALABILITY))
                    this.updateUserNextInvoiceDate(el);
            }

        });
    }

    private int getNumberOfUsedProjectsWithinLastMonth(User el) {
        LocalDateTime now = LocalDateTime.now();
        String userEmail = el.getEmail();
        List<String> ids = new ArrayList<String>();
        this.projectUseRepository.findByUserEmail(userEmail).forEach(user -> {
            LocalDateTime lastUpdated = LocalDateTime.ofInstant(user.getUseDate().toInstant(), ZoneId.systemDefault());
            long days = ChronoUnit.DAYS.between(lastUpdated, now);
            if (days < 30) {
                ids.add(user.getId());
            }
        });
        return ids.size();
    }

    private void checkAmountAndPay(User user, long amountToPay, long invoiceAmount, long tax,
                                   List<ProductInvoice> products) {
        if (amountToPay != 0) {
            try {
                pay(user, amountToPay, invoiceAmount, tax, products);
            } catch (IOException | DocumentException e1) {
                log.error("payment error {}", e1);
            }
        }
    }

    private void prepareTotalPayment(User el, long invoiceAmount, long tax, List<ProductInvoice> products,
                                     long amountToPay, String offer) {

        if (invoiceAmount != 0) {
            addProductInvoice(el, invoiceAmount, tax, products, Product.GRIZZLY_API);
        }
    }

    private void getUseForGrizzlyAPI(User userInGrizzlyApi, User userInGrizzlyHub, long amountToPay, long invoiceAmount,
                                     long tax, List<ProductInvoice> products) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeComparator dateTimeComparator = DateTimeComparator.getDateOnlyInstance();
        long invoiceAmountForGrizzlyAPI = 0;
        long taxForGrizzlyAPI = 0;

        try {

            if (userInGrizzlyApi.getAccountType().equals(AccountType.LIMITLESS)) {
                if (dateTimeComparator.compare(userInGrizzlyApi.getNextInvoiceDate(),
                        userInGrizzlyHub.getNextInvoiceDate()) != 0) {
                    LocalDateTime registrationDate = LocalDateTime
                            .ofInstant(userInGrizzlyApi.getRegistrationDate().toInstant(), ZoneId.systemDefault());
                    long days = ChronoUnit.DAYS.between(registrationDate, now);
                    amountToPay += (500000L / 30) * days;
                    invoiceAmountForGrizzlyAPI = (5000L / 30) * days;
                    invoiceAmount += (5000L / 30) * days;
                    taxForGrizzlyAPI = (1000L / 30) * days;
                    tax += (1000L / 30) * days;
                } else {
                    amountToPay += 500000L;
                    invoiceAmountForGrizzlyAPI = 5000L;
                    invoiceAmount += 5000L;
                    taxForGrizzlyAPI = 1000L;
                    tax += 1000L;
                }
            }

            if (userInGrizzlyApi.getAccountType().equals(AccountType.SCALABILITY)) {
                if (dateTimeComparator.compare(userInGrizzlyApi.getNextInvoiceDate(),
                        userInGrizzlyHub.getNextInvoiceDate()) != 0) {
                    List<String> ids = new ArrayList<String>();
                    this.projectUseRepository.findByUserEmail(userInGrizzlyApi.getEmail()).forEach(e -> {
                        ids.add(e.getId());
                    });
                    int count = ids.size();
                    if (count != 0) {
                        amountToPay += 29000L * count;
                        invoiceAmountForGrizzlyAPI = 290L * count;
                        invoiceAmount += 290L * count;
                        taxForGrizzlyAPI = (long) (290L * 0.2) * count;
                        tax += (long) (290L * 0.2) * count;
                    }
                }

            }

            if (amountToPay != 0) {

                addProductInvoice(userInGrizzlyApi, invoiceAmountForGrizzlyAPI, taxForGrizzlyAPI, products,
                        Product.GRIZZLY_API);

            }
        } catch (Exception e2) {
            log.error("payment error {}", e2);
        }

    }

    private void addProductInvoice(User user, long invoiceAmountForGrizzlyAPI, long taxForGrizzlyAPI,
                                   List<ProductInvoice> products, Product product) {
        ProductInvoice productInvoice = new ProductInvoice();
        productInvoice.setOffer(user.getAccountType().name());
        productInvoice.setProductName(product);
        productInvoice.setSubamount(invoiceAmountForGrizzlyAPI);
        productInvoice.setTva(taxForGrizzlyAPI);
        productInvoice.setTotalamount(invoiceAmountForGrizzlyAPI + taxForGrizzlyAPI);
        products.add(productInvoice);
    }

    private void pay(User el, long amount, long invoiceAmount, long tva, List<ProductInvoice> products)
            throws MalformedURLException, IOException, DocumentException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder().setCurrency("eur")
                .setAmount(amount + (tva * 100)).setPaymentMethod(el.getPaymentMethod().getPaymentMethodId())
                .setCustomer(el.getPaymentMethod().getCustomerId()).setConfirm(true).setOffSession(true).build();
        try {
            Invoice invoice = prepareInvoice(el, invoiceAmount, tva, products);
            sendInvoice(invoice, el.getEmail());
            if (!paymentRepository.existsByInvoiceIdAndPaymentStatus(invoice.getId(), "SUCCESS")) {
                PaymentIntent paymentIntent = PaymentIntent.create(params);
                addPaymentLog(el, paymentIntent, invoice, products, "SUCCESS");
            }

        } catch (CardException err) {
            log.error("Credit card error {}", err);

        } catch (StripeException e) {
            String paymentIntentId = e.getStripeError().getPaymentIntent().getId();
            try {
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                log.error("payment error {}", paymentIntent.getId());
                Invoice invoice = prepareInvoice(el, invoiceAmount, tva, products);
                sendInvoice(invoice, el.getEmail());
                addPaymentLog(el, paymentIntent, invoice, products, "FAILED");
            } catch (StripeException e1) {
                log.error("Stripe error {}", e1);
            }
        }
    }

    private void addPaymentLog(User user, PaymentIntent paymentIntent, Invoice invoice, List<ProductInvoice> products,
                               String status) {
        Payment paymentLog = new Payment();
        paymentLog.setUserId(user.getId());
        paymentLog.setPaymentId(paymentIntent.getId());
        paymentLog.setPaymentStatus(status);
        paymentLog.setInvoiceId(invoice.getId());
        paymentLog.setPeriodBigining(invoice.getBillingPeriodBeginningDate());
        paymentLog.setPeriodEnd(invoice.getBillingPeriodEndgDate());
        List<Product> logProducts = new ArrayList<>();
        products.forEach(pr -> {
            if (pr.getProductName().equals(Product.GRIZZLY_API)) {
                logProducts.add(Product.GRIZZLY_API);
            }
            if (pr.getProductName().equals(Product.GRIZZLY_HUB)) {
                logProducts.add(Product.GRIZZLY_HUB);
            }
        });
        paymentLog.setProductsUsed(logProducts);
        paymentRepository.save(paymentLog);
    }

    private Invoice prepareInvoice(User el, long invoiceAmount, long tva, List<ProductInvoice> products) {
        Date today = new Date();
        LocalDateTime now = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        now = now.minusDays(30);
        Date currentDateMinus30Days = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        String username = el.getFirstName() + " " + el.getLastName();
        Invoice invoice = new Invoice(true, el.getId(), username, currentDateMinus30Days, new Date());
        invoice.setNumTva("XXXXXXX");
        invoice.setBillingContact(el.getBillingContact());
        OrganisationAddress address = new OrganisationAddress();
        address.setZipCode("92150");
        address.setName("Codeonce Software");
        address.setAddress("14 BIS RUE DU RATRAIT");
        address.setAddress2("Suresnes");
        address.setCity("Paris");
        address.setCountry("France");
        address.setState("HAUT DE SEINE");
        invoice.setOrganisationAddress(address);
        invoice.setProducts(products);
        long totalAmount = 0;
        for (int i = 0; i < products.size(); i++) {
            totalAmount += products.get(i).getTotalamount();
        }

        invoice.setAmount(totalAmount);
        Invoice result = invoiceRepository.save(invoice);
        invoice.setInvoice_code(
                "COD-" + result.getId().subSequence(result.getId().length() - 9, result.getId().length()));

        Invoice finalInvoice = invoiceRepository.save(invoice);
        return finalInvoice;

    }

    public void sendInvoice(Invoice invoice, String email)
            throws MalformedURLException, IOException, DocumentException {
        Document document = new Document();
        StringBuilder fileName = new StringBuilder();
        fileName.append(invoice.getInvoice_code()).append(".pdf");
        File file = new File(fileName.toString());
        FileOutputStream fi = new FileOutputStream(file);
        PdfWriter.getInstance(document, fi);
        document.open();
        String codeonceLogo = "https://codeonce-grizzly-auth-angular-dev.s3.eu-central-1.amazonaws.com/image.png";
        Image img = Image.getInstance(codeonceLogo);
        img.scaleAbsolute(60f, 60f);
        document.add(img);

        Font f1 = new Font(Font.FontFamily.TIMES_ROMAN, 21.0f, Font.BOLD, BaseColor.BLACK);
        Font f2 = new Font(Font.FontFamily.TIMES_ROMAN, 11.5f, Font.NORMAL, BaseColor.BLACK);
        Font f3 = new Font(Font.FontFamily.TIMES_ROMAN, 11.5f, Font.BOLD, BaseColor.BLACK);
        Font f4 = new Font(Font.FontFamily.TIMES_ROMAN, 11.5f, Font.NORMAL, BaseColor.BLACK);
        Paragraph title = new Paragraph("Facture", f1);
        Paragraph NUM = new Paragraph("Numéro de facture : " + invoice.getInvoice_code(), f2);

        document.add(title);
        document.add(NUM);

        // user Billing Address Preparation
        addUserBillingContactToInvoiceFile(document, invoice, f3, f4);

        // invoice general details
        addInvoiceGeneralDetailsToInvoiceFile(document, invoice, f3, f4);
        invoice.getProducts().forEach(product -> {
            if (product.getProductName().equals(Product.GRIZZLY_API)) {
                // if the user have an account in Grizzly API
                try {
                    addGrizzlyAPIToInvoice(document, product, f3, f4);
                } catch (DocumentException e) {
                    log.error("add product to invoice error {}", e);
                }
            }

            if (product.getProductName().equals(Product.GRIZZLY_HUB)) {
                // if the user have an account in Grizzly HUB
                try {
                    if (invoice.getProducts().stream().map(el -> el.getProductName())
                            .anyMatch(el -> el.equals(Product.GRIZZLY_API))) {
                        addGrizzlyHUBToInvoice(document, product, f3, f4);

                    } else {
                        addGrizzlyHUBWithoutAPIToInvoice(document, product, f3, f4);
                    }
                } catch (DocumentException e) {
                    log.error("add product to invoice error {}", e);
                }
            }
        });

        // add statements
        addInvoiceStatements(document, invoice, f3, f4);

        // add Codeonce details to the invoice
        addCodeonceDetails(document, invoice, f3, f4);

        document.close();
        String last4Digits = user.getUser(email).getPaymentMethod().getLastFourNumber();
        String content = user.getOutputContentWithAtt("invoice.html", "fr", last4Digits, invoice.getAmount());
        emailService.sendWithAttachement(content, "Invoice", email, "api", file);

    }

    private void addUserBillingContactToInvoiceFile(Document document, Invoice invoice, Font f3, Font f4)
            throws DocumentException {
        Paragraph text = new Paragraph("Facturé à : ", f3);
        text.setSpacingBefore(40);
        Paragraph organisation = new Paragraph(invoice.getBillingContact().getOrganisationName(), f4);
        Paragraph name = new Paragraph(invoice.getUsername(), f4);
        Paragraph address = new Paragraph(invoice.getBillingContact().getAddress(), f4);
        Paragraph country = new Paragraph(invoice.getBillingContact().getCountry(), f4);
        document.add(text);
        document.add(name);
        document.add(organisation);
        document.add(address);
        document.add(country);
    }

    private void addInvoiceGeneralDetailsToInvoiceFile(Document document, Invoice invoice, Font f3, Font f4)
            throws DocumentException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-M-yyyy");
        Paragraph details = new Paragraph("Details : ", f3);
        details.setSpacingBefore(20);
        Paragraph numInvoice = new Paragraph("Numéro de la facture ....... " + invoice.getInvoice_code(), f4);
        String startDate = formatter.format(invoice.getBillingPeriodBeginningDate());
        String endDate = formatter.format(invoice.getBillingPeriodEndgDate());
        Paragraph date = new Paragraph("Date de la facture ............" + endDate, f4);
        Paragraph period = new Paragraph("Periode de facturation ...... " + startDate + " - " + endDate, f4);
        Paragraph total = new Paragraph("Montant total ..................... " + invoice.getAmount() + "€", f4);
        document.add(details);
        document.add(numInvoice);
        document.add(date);
        document.add(period);
        document.add(total);
    }

    private void addInvoiceStatements(Document document, Invoice invoice, Font f3, Font f4) throws DocumentException {
        Paragraph lois1 = new Paragraph(
                "Services soumis au mécanisme d'autoliquidation - Conformément à l'article 196 de la directive du Conseil 2006/112/EC, la TVA sur ce service est due par le bénéficiaire du service.",
                f4);
        Paragraph lois2 = new Paragraph("Le montant correspondant à cette facture sera débité automatiquement.", f4);
        Paragraph lois3 = new Paragraph(
                "Vous avez besoin d'aide pour comprendre les frais figurant sur votre facture ? Veuillez-nous contacter sur notre emai support@codeonce.fr",
                f4);

        lois1.setSpacingBefore(40);
        lois2.setSpacingBefore(20);
        lois3.setSpacingBefore(20);
        document.add(lois1);
        document.add(lois2);
        document.add(lois3);
    }

    private void addCodeonceDetails(Document document, Invoice invoice, Font f3, Font f4) throws DocumentException {
        Paragraph codeonceName = new Paragraph(invoice.getOrganisationAddress().getName(), f4);
        Paragraph codeonceAddress = new Paragraph(invoice.getOrganisationAddress().getAddress(), f4);
        Paragraph zipCodeAndCity = new Paragraph(
                invoice.getOrganisationAddress().getZipCode() + " , " + invoice.getOrganisationAddress().getAddress2(),
                f4);
        Paragraph state = new Paragraph(invoice.getOrganisationAddress().getState(), f4);
        Paragraph codeonceCountry = new Paragraph(
                invoice.getOrganisationAddress().getCity() + " , " + invoice.getOrganisationAddress().getCountry(), f4);
        StringBuilder numTva = new StringBuilder();
        numTva.append("Numéro de TVA : ").append(invoice.getNumTva());
        Paragraph codeonceNumTva = new Paragraph(numTva.toString(), f4);

        codeonceName.setSpacingBefore(-650f);
        codeonceAddress.setIndentationLeft(370);
        zipCodeAndCity.setIndentationLeft(370);
        state.setIndentationLeft(370);
        codeonceCountry.setIndentationLeft(370);
        codeonceNumTva.setIndentationLeft(370);
        codeonceName.setIndentationLeft(370);

        document.add(codeonceName);
        document.add(codeonceAddress);
        document.add(zipCodeAndCity);
        document.add(state);
        document.add(codeonceCountry);
        document.add(codeonceNumTva);
    }

    private void addGrizzlyAPIToInvoice(Document document, ProductInvoice product, Font f3, Font f4)
            throws DocumentException {
        Paragraph grizzlyAPI = new Paragraph("Grizzly API", f3);
        Paragraph offer = new Paragraph("Offre", f4);
        Paragraph totalText = new Paragraph("Total en EUR", f4);
        Paragraph grizzlyAPIOffer = new Paragraph(product.getOffer(), f4);
        Paragraph grizzlyAPITotal = new Paragraph(Long.toString(product.getTotalamount()) + "€", f4);
        Paragraph recap = new Paragraph("Récapitulatif de facturation", f4);
        Paragraph sousTotal = new Paragraph("Sous-total en EUR", f4);
        Paragraph tva = new Paragraph("TVA (20%)", f4);
        Paragraph sousTotalValue = new Paragraph(Long.toString(product.getSubamount()) + "€", f4);
        Paragraph tvaValue = new Paragraph(Long.toString(product.getTva()) + "€", f4);
        Paragraph totalValue = new Paragraph(Long.toString(product.getTotalamount()) + "€", f4);
        grizzlyAPI.setSpacingBefore(10);
        grizzlyAPIOffer.setSpacingBefore(-40f);
        recap.setSpacingBefore(20);
        recap.setSpacingAfter(10);
        grizzlyAPIOffer.setIndentationLeft(160);
        grizzlyAPITotal.setIndentationLeft(160);
        sousTotalValue.setSpacingBefore(-55f);
        sousTotalValue.setIndentationLeft(160);
        tvaValue.setIndentationLeft(160);
        totalValue.setIndentationLeft(160);
        document.add(grizzlyAPI);
        document.add(offer);
        document.add(totalText);
        document.add(grizzlyAPIOffer);
        document.add(grizzlyAPITotal);
        document.add(recap);
        document.add(sousTotal);
        document.add(tva);
        document.add(totalText);
        document.add(sousTotalValue);
        document.add(tvaValue);
        document.add(totalValue);
    }

    private void addGrizzlyHUBWithoutAPIToInvoice(Document document, ProductInvoice product, Font f3, Font f4)
            throws DocumentException {
        Paragraph grizzlyHUB = new Paragraph("Grizzly HUB", f3);
        Paragraph grizzlyHUBOffer = new Paragraph(product.getOffer(), f4);
        Paragraph grizzlyHubTotalValue = new Paragraph(Long.toString(product.getTotalamount()) + "€", f4);
        Paragraph offerHUB = new Paragraph("Offre", f4);
        Paragraph recapHUB = new Paragraph("Récapitulatif de facturation", f4);
        Paragraph totalTextHUB = new Paragraph("Total en EUR", f4);
        Paragraph tvaHUB = new Paragraph("TVA (20%)", f4);
        Paragraph secondTotalTextHUB = new Paragraph("Total en EUR", f4);
        Paragraph hubSousTotalValue = new Paragraph(Long.toString(product.getSubamount()) + "€", f4);
        Paragraph hubTvaValue = new Paragraph(Long.toString(product.getTva()) + "€", f4);
        Paragraph secondGrizzlyHubTotalValue = new Paragraph(Long.toString(product.getTotalamount()) + "€", f4);
        grizzlyHUB.setSpacingBefore(10);
        grizzlyHUBOffer.setSpacingBefore(-40f);
        recapHUB.setSpacingBefore(20);
        recapHUB.setSpacingAfter(10);
        grizzlyHUBOffer.setIndentationLeft(160);
        grizzlyHubTotalValue.setIndentationLeft(160);
        hubSousTotalValue.setSpacingBefore(-55f);
        hubSousTotalValue.setIndentationLeft(160);
        hubTvaValue.setIndentationLeft(160);
        secondGrizzlyHubTotalValue.setIndentationLeft(160);
        document.add(grizzlyHUB);
        document.add(offerHUB);
        document.add(totalTextHUB);
        document.add(grizzlyHUBOffer);
        document.add(grizzlyHubTotalValue);
        document.add(recapHUB);
        document.add(secondTotalTextHUB);
        document.add(tvaHUB);
        document.add(totalTextHUB);
        document.add(hubSousTotalValue);
        document.add(hubTvaValue);
        document.add(secondGrizzlyHubTotalValue);
    }

    private void addGrizzlyHUBToInvoice(Document document, ProductInvoice product, Font f3, Font f4)
            throws DocumentException {
        Paragraph grizzlyHUB = new Paragraph("Grizzly HUB", f3);
        Paragraph grizzlyHUBOffer = new Paragraph(product.getOffer(), f4);
        Paragraph grizzlyHubTotalValue = new Paragraph(Long.toString(product.getTotalamount()) + "€", f4);
        Paragraph offerHUB = new Paragraph("Offre", f4);
        Paragraph recapHUB = new Paragraph("Récapitulatif de facturation", f4);
        Paragraph totalTextHUB = new Paragraph("Total en EUR", f4);
        Paragraph sousTotalHUB = new Paragraph("Sous-total en EUR", f4);
        Paragraph tvaHUB = new Paragraph("TVA (20%)", f4);
        Paragraph secondTotalTextHUB = new Paragraph("Total en EUR", f4);
        Paragraph hubSousTotalValue = new Paragraph(Long.toString(product.getSubamount()) + "€", f4);
        Paragraph hubTvaValue = new Paragraph(Long.toString(product.getTva()) + "€", f4);
        Paragraph secondGrizzlyHubTotalValue = new Paragraph(Long.toString(product.getTotalamount()) + "€", f4);
        recapHUB.setSpacingBefore(18);
        recapHUB.setSpacingAfter(8);
        grizzlyHUB.setIndentationLeft(320);
        offerHUB.setIndentationLeft(320);
        totalTextHUB.setIndentationLeft(320);
        grizzlyHUB.setSpacingBefore(-148f);
        grizzlyHubTotalValue.setIndentationLeft(470);
        grizzlyHUBOffer.setIndentationLeft(430);
        sousTotalHUB.setIndentationLeft(320);
        secondTotalTextHUB.setIndentationLeft(320);
        tvaHUB.setIndentationLeft(320);
        recapHUB.setIndentationLeft(320);
        hubSousTotalValue.setIndentationLeft(470);
        hubTvaValue.setIndentationLeft(470);
        secondGrizzlyHubTotalValue.setIndentationLeft(470);
        hubSousTotalValue.setSpacingBefore(-55f);
        grizzlyHUBOffer.setSpacingBefore(-36f);
        document.add(grizzlyHUB);
        document.add(offerHUB);
        document.add(totalTextHUB);
        document.add(grizzlyHUBOffer);
        document.add(grizzlyHubTotalValue);
        document.add(recapHUB);
        document.add(sousTotalHUB);
        document.add(tvaHUB);
        document.add(secondTotalTextHUB);
        document.add(hubSousTotalValue);
        document.add(hubTvaValue);
        document.add(secondGrizzlyHubTotalValue);
    }

    private void updateUserNextInvoiceDate(User userInGrizzlyAPI) {
        Date today = new Date();
        User user = userRepository.findById(userInGrizzlyAPI.getId()).get();
        if (!userInGrizzlyAPI.getAccountType().equals(AccountType.FREE)) {
            LocalDateTime localDateTime = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            localDateTime = localDateTime.plusDays(30);
            Date currentDatePlus30Days = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            user.setNextInvoiceDate(currentDatePlus30Days);
            userRepository.save(user);
        }

    }
}
