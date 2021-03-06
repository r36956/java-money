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
import static org.panteleyev.money.test.BaseTestUtils.newTransaction;
import static org.panteleyev.money.test.BaseTestUtils.randomBigDecimal;
import static org.panteleyev.money.test.BaseTestUtils.randomBoolean;
import static org.panteleyev.money.test.BaseTestUtils.randomCategoryType;
import static org.panteleyev.money.test.BaseTestUtils.randomDay;
import static org.panteleyev.money.test.BaseTestUtils.randomInt;
import static org.panteleyev.money.test.BaseTestUtils.randomMonth;
import static org.panteleyev.money.test.BaseTestUtils.randomString;
import static org.panteleyev.money.test.BaseTestUtils.randomTransactionType;
import static org.panteleyev.money.test.BaseTestUtils.randomYear;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestTransaction extends ModelTestBase {

    @DataProvider
    @Override
    public Object[][] testBuildDataProvider() {
        var uuid = UUID.randomUUID();
        var amount = randomBigDecimal();
        var day = randomDay();
        var month = randomMonth();
        var year = randomYear();
        var type = randomTransactionType();
        var comment = randomString();
        var checked = randomBoolean();
        var accountDebitedUuid = UUID.randomUUID();
        var accountCreditedUuid = UUID.randomUUID();
        var accountDebitedType = randomCategoryType();
        var accountCreditedType = randomCategoryType();
        var accountDebitedCategoryUuid = UUID.randomUUID();
        var accountCreditedCategoryUuid = UUID.randomUUID();
        var contactUuid = UUID.randomUUID();
        var rate = randomBigDecimal();
        var rateDirection = randomInt();
        var invoiceNumber = randomString();
        var parentUuid = UUID.randomUUID();
        var detailed = randomBoolean();
        var statementDate = LocalDate.now();
        var created = System.currentTimeMillis();
        var modified = created + 1000;

        return new Object[][]{
            {
                new Transaction.Builder()
                    .uuid(uuid)
                    .amount(amount)
                    .day(day)
                    .month(month)
                    .year(year)
                    .type(type)
                    .comment(comment)
                    .checked(checked)
                    .accountDebitedUuid(accountDebitedUuid)
                    .accountCreditedUuid(accountCreditedUuid)
                    .accountDebitedType(accountDebitedType)
                    .accountCreditedType(accountCreditedType)
                    .accountDebitedCategoryUuid(accountDebitedCategoryUuid)
                    .accountCreditedCategoryUuid(accountCreditedCategoryUuid)
                    .contactUuid(contactUuid)
                    .rate(rate)
                    .rateDirection(rateDirection)
                    .invoiceNumber(invoiceNumber)
                    .parentUuid(parentUuid)
                    .detailed(detailed)
                    .statementDate(statementDate)
                    .created(created)
                    .modified(modified)
                    .build(),
                new Transaction(
                    uuid, amount, day, month, year,
                    type, comment, checked, accountDebitedUuid, accountCreditedUuid,
                    accountDebitedType, accountCreditedType, accountDebitedCategoryUuid, accountCreditedCategoryUuid,
                    contactUuid, rate, rateDirection, invoiceNumber, parentUuid,
                    detailed, statementDate, created, modified
                )
            },
            {
                new Transaction(
                    uuid, amount, day, month, year,
                    type, null, checked, accountDebitedUuid, accountCreditedUuid,
                    accountDebitedType, accountCreditedType, accountDebitedCategoryUuid, accountCreditedCategoryUuid,
                    contactUuid, null, rateDirection, null, null,
                    detailed, null, created, modified
                ),
                new Transaction(
                    uuid, amount, day, month, year,
                    type, "", checked, accountDebitedUuid, accountCreditedUuid,
                    accountDebitedType, accountCreditedType, accountDebitedCategoryUuid, accountCreditedCategoryUuid,
                    contactUuid, BigDecimal.ONE, rateDirection, "", null,
                    detailed, LocalDate.of(year, month, day), created, modified
                )
            }
        };
    }

    @Test
    public void testEquals() {
        var amount = randomBigDecimal();
        var day = randomDay();
        var month = randomMonth();
        var year = randomYear();
        var type = randomTransactionType();
        var comment = UUID.randomUUID().toString();
        var checked = RANDOM.nextBoolean();
        var accountDebitedUuid = UUID.randomUUID();
        var accountCreditedUuid = UUID.randomUUID();
        var accountDebitedType = randomCategoryType();
        var accountCreditedType = randomCategoryType();
        var accountDebitedCategoryUuid = UUID.randomUUID();
        var accountCreditedCategoryUuid = UUID.randomUUID();
        var contactUuid = UUID.randomUUID();
        var rate = randomBigDecimal();
        var rateDirection = RANDOM.nextInt();
        var invoiceNumber = UUID.randomUUID().toString();
        var guid = UUID.randomUUID();
        var created = System.currentTimeMillis();
        var modified = System.currentTimeMillis();
        var parentUuid = UUID.randomUUID();
        var detailed = RANDOM.nextBoolean();
        var statementDate = LocalDate.now();

        var t1 = new Transaction.Builder()
            .amount(amount)
            .day(day)
            .month(month)
            .year(year)
            .type(type)
            .comment(comment)
            .checked(checked)
            .accountDebitedUuid(accountDebitedUuid)
            .accountCreditedUuid(accountCreditedUuid)
            .accountDebitedType(accountDebitedType)
            .accountCreditedType(accountCreditedType)
            .accountDebitedCategoryUuid(accountDebitedCategoryUuid)
            .accountCreditedCategoryUuid(accountCreditedCategoryUuid)
            .contactUuid(contactUuid)
            .rate(rate)
            .rateDirection(rateDirection)
            .invoiceNumber(invoiceNumber)
            .uuid(guid)
            .created(created)
            .modified(modified)
            .parentUuid(parentUuid)
            .detailed(detailed)
            .statementDate(statementDate)
            .build();

        var t2 = new Transaction.Builder()
            .amount(amount)
            .day(day)
            .month(month)
            .year(year)
            .type(type)
            .comment(comment)
            .checked(checked)
            .accountDebitedUuid(accountDebitedUuid)
            .accountCreditedUuid(accountCreditedUuid)
            .accountDebitedType(accountDebitedType)
            .accountCreditedType(accountCreditedType)
            .accountDebitedCategoryUuid(accountDebitedCategoryUuid)
            .accountCreditedCategoryUuid(accountCreditedCategoryUuid)
            .contactUuid(contactUuid)
            .rate(rate)
            .rateDirection(rateDirection)
            .invoiceNumber(invoiceNumber)
            .uuid(guid)
            .created(created)
            .modified(modified)
            .parentUuid(parentUuid)
            .detailed(detailed)
            .statementDate(statementDate)
            .build();

        assertEquals(t2, t1);
        assertEquals(t2.hashCode(), t1.hashCode());
    }

    @Test
    public void testCheck() {
        var t1 = new Transaction.Builder()
            .amount(randomBigDecimal())
            .day(randomDay())
            .month(randomMonth())
            .year(randomYear())
            .type(randomTransactionType())
            .comment(randomString())
            .checked(randomBoolean())
            .accountDebitedUuid(UUID.randomUUID())
            .accountCreditedUuid(UUID.randomUUID())
            .accountDebitedType(randomCategoryType())
            .accountCreditedType(randomCategoryType())
            .accountDebitedCategoryUuid(UUID.randomUUID())
            .accountCreditedCategoryUuid(UUID.randomUUID())
            .contactUuid(UUID.randomUUID())
            .rate(randomBigDecimal())
            .rateDirection(RANDOM.nextInt())
            .invoiceNumber(randomString())
            .uuid(UUID.randomUUID())
            .created(System.currentTimeMillis())
            .modified(System.currentTimeMillis())
            .parentUuid(UUID.randomUUID())
            .detailed(randomBoolean())
            .build();

        var t2 = t1.check(!t1.checked());

        assertEquals(t2.amount(), t1.amount());
        assertEquals(t2.day(), t1.day());
        assertEquals(t2.month(), t1.month());
        assertEquals(t2.year(), t1.year());
        assertEquals(t2.type(), t1.type());
        assertEquals(t2.comment(), t1.comment());
        assertEquals(t2.checked(), !t1.checked());
        assertEquals(t2.accountDebitedUuid(), t1.accountDebitedUuid());
        assertEquals(t2.accountCreditedUuid(), t1.accountCreditedUuid());
        assertEquals(t2.accountDebitedType(), t1.accountDebitedType());
        assertEquals(t2.accountCreditedType(), t1.accountCreditedType());
        assertEquals(t2.contactUuid(), t1.contactUuid());
        assertEquals(t2.rate(), t1.rate());
        assertEquals(t2.rateDirection(), t1.rateDirection());
        assertEquals(t2.invoiceNumber(), t1.invoiceNumber());
        assertEquals(t2.parentUuid(), t1.parentUuid());
        assertEquals(t2.uuid(), t1.uuid());
        assertEquals(t1.created(), t2.created());
        assertTrue(t2.modified() >= t1.modified());
    }

    @Test
    public void testCopy() {
        var original = newTransaction();

        // Builder copy
        var builderCopy = new Transaction.Builder(original).build();
        assertEquals(builderCopy, original);
    }
}
