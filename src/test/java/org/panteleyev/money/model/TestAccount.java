/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.model;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.panteleyev.money.test.BaseTestUtils.RANDOM;
import static org.panteleyev.money.test.BaseTestUtils.randomBigDecimal;
import static org.panteleyev.money.test.BaseTestUtils.randomBoolean;
import static org.panteleyev.money.test.BaseTestUtils.randomCardType;
import static org.panteleyev.money.test.BaseTestUtils.randomCategoryType;
import static org.panteleyev.money.test.BaseTestUtils.randomString;
import static org.testng.Assert.assertEquals;

public class TestAccount extends ModelTestBase {

    @DataProvider
    @Override
    public Object[][] testBuildDataProvider() {
        var uuid = UUID.randomUUID();
        var name = randomString();
        var comment = randomString();
        var accountNumber = randomString();
        var openingBalance = randomBigDecimal();
        var accountLimit = randomBigDecimal();
        var currencyRate = randomBigDecimal();
        var type = randomCategoryType();
        var categoryUuid = UUID.randomUUID();
        var currencyUuid = UUID.randomUUID();
        var enabled = randomBoolean();
        var interest = randomBigDecimal();
        var closingDate = LocalDate.now();
        var iconUuid = UUID.randomUUID();
        var cardType = randomCardType();
        var cardNumber = randomString();
        var total = randomBigDecimal();
        var totalWaiting = randomBigDecimal();
        var created = System.currentTimeMillis();
        var modified = created + 1000;

        return new Object[][]{
            {
                new Account.Builder()
                    .uuid(uuid)
                    .name(name)
                    .comment(comment)
                    .accountNumber(accountNumber)
                    .openingBalance(openingBalance)
                    .accountLimit(accountLimit)
                    .currencyRate(currencyRate)
                    .type(type)
                    .categoryUuid(categoryUuid)
                    .currencyUuid(currencyUuid)
                    .enabled(enabled)
                    .interest(interest)
                    .closingDate(closingDate)
                    .iconUuid(iconUuid)
                    .cardType(cardType)
                    .cardNumber(cardNumber)
                    .total(total)
                    .totalWaiting(totalWaiting)
                    .created(created)
                    .modified(modified)
                    .build(),
                new Account(
                    uuid, name, comment, accountNumber, openingBalance,
                    accountLimit, currencyRate, type, categoryUuid, currencyUuid,
                    enabled, interest, closingDate, iconUuid, cardType,
                    cardNumber, total, totalWaiting, created, modified
                )
            },
            {
                new Account(
                    uuid, name, null, null, null,
                    null, null, type, categoryUuid, null,
                    enabled, null, null, null, cardType,
                    cardNumber, null, null, created, modified
                ),
                new Account(
                    uuid, name, "", "", BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ONE, type, categoryUuid, null,
                    enabled, BigDecimal.ZERO, null, null, cardType,
                    cardNumber, BigDecimal.ZERO, BigDecimal.ZERO, created, modified
                )
            }
        };
    }


    @Test(dataProvider = "testBuildDataProvider")
    public void testBuild(Account actual, Account expected) {
        assertEquals(actual, expected);
    }

    @Test
    public void testEquals() {
        var name = UUID.randomUUID().toString();
        var comment = UUID.randomUUID().toString();
        var accountNumber = UUID.randomUUID().toString();
        var opening = randomBigDecimal();
        var limit = randomBigDecimal();
        var rate = randomBigDecimal();
        var type = randomCategoryType();
        var categoryUuid = UUID.randomUUID();
        var currencyUuid = UUID.randomUUID();
        var enabled = RANDOM.nextBoolean();
        var interest = randomBigDecimal();
        var closingDate = LocalDate.now();
        var iconUuid = UUID.randomUUID();
        var cardType = CardType.MASTERCARD;
        var cardNumber = UUID.randomUUID().toString();
        var uuid = UUID.randomUUID();
        var created = System.currentTimeMillis();
        var modified = System.currentTimeMillis();

        var a1 = new Account.Builder()
            .name(name)
            .comment(comment)
            .accountNumber(accountNumber)
            .openingBalance(opening)
            .accountLimit(limit)
            .currencyRate(rate)
            .type(type)
            .categoryUuid(categoryUuid)
            .currencyUuid(currencyUuid)
            .enabled(enabled)
            .interest(interest)
            .closingDate(closingDate)
            .iconUuid(iconUuid)
            .cardType(cardType)
            .cardNumber(cardNumber)
            .uuid(uuid)
            .created(created)
            .modified(modified)
            .build();

        var a2 = new Account.Builder()
            .name(name)
            .comment(comment)
            .accountNumber(accountNumber)
            .openingBalance(opening)
            .accountLimit(limit)
            .currencyRate(rate)
            .type(type)
            .categoryUuid(categoryUuid)
            .currencyUuid(currencyUuid)
            .enabled(enabled)
            .interest(interest)
            .closingDate(closingDate)
            .iconUuid(iconUuid)
            .cardType(cardType)
            .cardNumber(cardNumber)
            .uuid(uuid)
            .created(created)
            .modified(modified)
            .build();

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @DataProvider(name = "testAccountNumberDataProvider")
    public Object[][] testAccountNumberDataProvider() {
        return new Object[][]{
            {"   1234  5 6   78 ", "12345678"},
            {"12345678", "12345678"},
            {"123456 78    ", "12345678"},
            {" 12345678", "12345678"},
        };
    }

    @Test(dataProvider = "testAccountNumberDataProvider")
    public void testAccountNumber(String accountNumber, String accountNumberNoSpaces) {
        var a = new Account.Builder()
            .name(randomString())
            .accountNumber(accountNumber)
            .type(CategoryType.DEBTS)
            .categoryUuid(UUID.randomUUID())
            .uuid(UUID.randomUUID())
            .created(System.currentTimeMillis())
            .modified(System.currentTimeMillis())
            .build();

        assertEquals(a.accountNumber(), accountNumber);
        assertEquals(a.getAccountNumberNoSpaces(), accountNumberNoSpaces);
    }

    @Test
    public void testCopy() {
        var original = new Account.Builder()
            .name(UUID.randomUUID().toString())
            .comment(UUID.randomUUID().toString())
            .accountNumber(UUID.randomUUID().toString())
            .openingBalance(randomBigDecimal())
            .accountLimit(randomBigDecimal())
            .currencyRate(randomBigDecimal())
            .type(randomCategoryType())
            .categoryUuid(UUID.randomUUID())
            .currencyUuid(UUID.randomUUID())
            .enabled(RANDOM.nextBoolean())
            .interest(randomBigDecimal())
            .closingDate(LocalDate.now())
            .iconUuid(UUID.randomUUID())
            .cardType(CardType.VISA)
            .cardNumber(UUID.randomUUID().toString())
            .uuid(UUID.randomUUID())
            .created(System.currentTimeMillis())
            .modified(System.currentTimeMillis())
            .build();

        var copy = new Account.Builder(original).build();
        assertEquals(copy, original);

        var manualCopy = new Account.Builder()
            .name(original.name())
            .comment(original.comment())
            .accountNumber(original.accountNumber())
            .openingBalance(original.openingBalance())
            .accountLimit(original.accountLimit())
            .currencyRate(original.currencyRate())
            .type(original.type())
            .categoryUuid(original.categoryUuid())
            .currencyUuid(original.currencyUuid())
            .enabled(original.enabled())
            .interest(original.interest())
            .closingDate(original.closingDate())
            .iconUuid(original.iconUuid())
            .cardType(original.cardType())
            .cardNumber(original.cardNumber())
            .uuid(original.uuid())
            .created(original.created())
            .modified(original.modified())
            .build();
        assertEquals(manualCopy, original);
    }
}
