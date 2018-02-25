/*
 * Copyright (c) 2018, Petr Panteleyev <petr@panteleyev.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.panteleyev.money.statements;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.panteleyev.money.Logging;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import static java.util.Map.entry;

class SberbankParser {
    private enum Param {
        TEMPLATE_VALUE,
        TABLE, RECORD, HEADER, DETAIL, CATEGORY,
        NAME, ACTUAL_DATE, DATE_CLASS, DATE_VALUE, SUM, AMOUNT,
        GEO, COUNTRY, CITY, VALUE, EXECUTION_DATE,
        CREDIT_CLASSES
    }

    private final static Map<Param, Object> DEFAULT_CLASSES = new EnumMap<>(Map.ofEntries(
            entry(Param.TABLE, "b-trs"),
            entry(Param.RECORD, "trs_it"),
            entry(Param.HEADER, "trs_head"),
            entry(Param.DETAIL, "trs_detail"),
            entry(Param.NAME, "trs_name"),
            entry(Param.ACTUAL_DATE, "trs_date"),
            entry(Param.DATE_CLASS, "idate"),
            entry(Param.DATE_VALUE, "data-date"),
            entry(Param.SUM, "trs_sum"),
            entry(Param.AMOUNT, "trs_sum-am"),
            entry(Param.GEO, "trs-geo"),
            entry(Param.COUNTRY, "trs_country"),
            entry(Param.CITY, "trs_city"),
            entry(Param.VALUE, "trs_val"),
            entry(Param.CATEGORY, "icat"),
            entry(Param.EXECUTION_DATE, "trs-post"),
            entry(Param.CREDIT_CLASSES, List.of("trs_st-refill"))
    ));

    private final static String TEMPLATE_ATTRIBUTE_NAME = "name";
    private final static String TEMPLATE_ATTRIBUTE_VALUE = "template-details";
    private final static String TEMPLATE_VALUE = "content";

    private enum Format {
        UNKNOWN(null),
        HTML_DEBIT_VERSION_2_1_6("HTML_DEBIT_RUS_REPORT, 07.04.2017, 2.1.6"),
        HTML_CREDIT_VERSION_2_1_6("HTML_CREDIT_RUS_REPORT, 07.04.2017, 2.1.6"),
        HTML_DEBIT_VERSION_2_1_7("HTML_DEBIT_RUS_REPORT, 25.01.2018, 2.1.17");

        private final String formatString;
        private final Map<Param, Object> formatClasses;

        Format(String formatString) {
            this.formatString = formatString;
            this.formatClasses = DEFAULT_CLASSES;
        }

        String getFormatString() {
            return formatString;
        }

        String getString(Param clazz) {
            return (String) formatClasses.get(clazz);
        }

        @SuppressWarnings("unchecked")
        Collection<String> creditClasses() {
            return (Collection<String>) formatClasses.get(Param.CREDIT_CLASSES);
        }

        static Stream<Format> stream() {
            return Arrays.stream(values());
        }

        static Format detectFormat(String formatString) {
            return stream().filter(f -> Objects.equals(f.getFormatString(), formatString))
                    .findFirst()
                    .orElse(Format.UNKNOWN);
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static void checkElement(Element element) {
        Objects.requireNonNull(element, "Malformed statement");
    }

    private static LocalDate parseDate(Element dateElement, Format format) {
        Element iDate = dateElement.getElementsByClass(format.getString(Param.DATE_CLASS)).first();
        if (iDate != null) {
            String dateString = iDate.attributes().get(format.getString(Param.DATE_VALUE));
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } else {
            return null;
        }
    }

    static Statement parseCreditCardHtml(InputStream inputStream) {
        List<StatementRecord> records = new ArrayList<>();

        try {
            Document document = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "");

            // Find template version
            Format format = parseTemplateFormat(document);

            // Transaction table
            Element transactionTable = document.getElementsByClass(format.getString(Param.TABLE)).first();
            if (transactionTable == null) {
                Logging.getLogger().warning("Transactions not found in statement");
                return new Statement(Statement.StatementType.SBERBANK_HTML, records);
            }

            Elements transactionList = transactionTable.getElementsByClass(format.getString(Param.RECORD));
            if (transactionList.isEmpty()) {
                Logging.getLogger().warning("Transactions not found in statement");
                return new Statement(Statement.StatementType.SBERBANK_HTML, records);
            }

            for (Element transaction : transactionList) {
                Element head = transaction.getElementsByClass(format.getString(Param.HEADER)).first();
                if (head == null) {
                    continue;
                }

                StatementRecord.Builder builder = new StatementRecord.Builder();

                Element nameElement = head.getElementsByClass(format.getString(Param.NAME)).first();
                checkElement(nameElement);
                builder = builder.counterParty(nameElement.text());

                // Transaction actual date
                Element dateElement = head.getElementsByClass(format.getString(Param.ACTUAL_DATE)).first();
                checkElement(dateElement);
                LocalDate transactionDate = parseDate(dateElement, format);
                builder = builder.actual(transactionDate);

                // Transaction amount
                builder = builder.amount(parseTransactionAmount(head, format));

                // Transaction category
                Element catElement = head.getElementsByClass(format.getString(Param.CATEGORY)).first();
                String category = catElement != null ? catElement.text() : "";
                builder = builder.description(category);

                // Additional details
                Elements detailsList = transaction.getElementsByClass(format.getString(Param.DETAIL));
                for (Element detail : detailsList) {
                    Set<String> classNames = detail.classNames();

                    if (classNames.contains(format.getString(Param.EXECUTION_DATE))) {
                        builder = builder.execution(parseDate(
                                detail.getElementsByClass(format.getString(Param.VALUE)).first(), format));
                    } else if (classNames.contains(format.getString(Param.GEO))) {
                        Element countryElement =
                                detail.getElementsByClass(format.getString(Param.COUNTRY)).first();
                        if (countryElement != null) {
                            builder = builder.country(countryElement.text());
                        }
                        Element cityElement = detail.getElementsByClass(format.getString(Param.CITY)).first();
                        if (cityElement != null) {
                            builder = builder.place(cityElement.text());
                        }
                    }
                }

                records.add(builder.build());
            }

            return new Statement(Statement.StatementType.SBERBANK_HTML, records);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static Format parseTemplateFormat(Document document) {
        Format format = Format.UNKNOWN;
        String version = "";
        Element versionElement =
                document.getElementsByAttributeValue(TEMPLATE_ATTRIBUTE_NAME, TEMPLATE_ATTRIBUTE_VALUE).first();
        if (versionElement != null) {
            Attributes attributes = versionElement.attributes();
            version = attributes.get(TEMPLATE_VALUE);
            format = Format.detectFormat(version);
        }

        if (format != Format.UNKNOWN) {
            Logging.getLogger().info("Sberbank format recognized: " + format.getFormatString());
        } else {
            Logging.getLogger().info("Sberbank format not recognized: " + version);
        }

        return format;
    }

    private static String parseTransactionAmount(Element head, Format format) {
        Element sumElement = head.getElementsByClass(format.getString(Param.SUM)).first();
        checkElement(sumElement);
        Element amountElement = sumElement.getElementsByClass(format.getString(Param.AMOUNT)).first();
        checkElement(amountElement);
        String sumString = amountElement.text();

        // Check if transaction is credit to the account
        boolean credit = false;
        for (String crClass : format.creditClasses()) {
            if (!sumElement.getElementsByClass(crClass).isEmpty()) {
                credit = true;
                break;
            }
        }

        if (!credit) {
            sumString = '-' + sumString;
        }

        return sumString;
    }
}
