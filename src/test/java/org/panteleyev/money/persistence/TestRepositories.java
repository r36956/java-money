/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.persistence;

import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.Contact;
import org.panteleyev.money.model.Currency;
import org.panteleyev.money.model.MoneyRecord;
import org.panteleyev.money.model.Transaction;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.time.LocalDate;
import java.util.UUID;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;
import static org.panteleyev.money.test.BaseTestUtils.newIcon;
import static org.panteleyev.money.test.BaseTestUtils.randomBigDecimal;
import static org.panteleyev.money.test.BaseTestUtils.randomBoolean;
import static org.panteleyev.money.test.BaseTestUtils.randomCardType;
import static org.panteleyev.money.test.BaseTestUtils.randomCategoryType;
import static org.panteleyev.money.test.BaseTestUtils.randomContactType;
import static org.panteleyev.money.test.BaseTestUtils.randomDay;
import static org.panteleyev.money.test.BaseTestUtils.randomInt;
import static org.panteleyev.money.test.BaseTestUtils.randomMonth;
import static org.panteleyev.money.test.BaseTestUtils.randomString;
import static org.panteleyev.money.test.BaseTestUtils.randomTransactionType;
import static org.panteleyev.money.test.BaseTestUtils.randomYear;
import static org.testng.Assert.assertEquals;

public class TestRepositories extends BaseDaoTest {
    public static final String ICON_DOLLAR = "dollar.png";
    public static final String ICON_EURO = "euro.png";

    private static final UUID ICON_UUID = UUID.randomUUID();
    private static final UUID CATEGORY_UUID = UUID.randomUUID();
    private static final UUID CURRENCY_UUID = UUID.randomUUID();
    private static final UUID CONTACT_UUID = UUID.randomUUID();
    private static final UUID ACCOUNT_UUID = UUID.randomUUID();
    private static final UUID TRANSACTION_UUID = UUID.randomUUID();

    @BeforeClass
    @Override
    public void setupAndSkip() {
        try {
            super.setupAndSkip();
        } catch (Exception ex) {
            throw new SkipException(ex.getMessage());
        }
    }

    @Test
    public void testIcon() {
        var repository = getDao().getIconRepository();
        var insert = newIcon(ICON_UUID, ICON_DOLLAR);
        var update = newIcon(ICON_UUID, ICON_EURO);
        insertAndUpdate(repository, insert, update);
    }

    @Test(dependsOnMethods = "testIcon")
    public void testCategory() {
        var repository = getDao().getCategoryRepository();

        var insert = new Category(
            CATEGORY_UUID,
            randomString(),
            randomString(),
            randomCategoryType(),
            ICON_UUID,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        var update = new Category(
            CATEGORY_UUID,
            randomString(),
            randomString(),
            randomCategoryType(),
            null,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        insertAndUpdate(repository, insert, update);
    }

    @Test
    public void testCurrency() {
        var repository = getDao().getCurrencyRepository();

        var insert = new Currency(
            CURRENCY_UUID,
            randomString(),
            randomString(),
            randomString(),
            randomInt(),
            randomBoolean(),
            randomBoolean(),
            randomBigDecimal(),
            randomInt(),
            randomBoolean(),
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        var update = new Currency(
            CURRENCY_UUID,
            randomString(),
            randomString(),
            randomString(),
            randomInt(),
            randomBoolean(),
            randomBoolean(),
            randomBigDecimal(),
            randomInt(),
            randomBoolean(),
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        insertAndUpdate(repository, insert, update);
    }

    @Test(dependsOnMethods = "testIcon")
    public void testContact() {
        var repository = getDao().getContactRepository();

        var insert = new Contact(
            CONTACT_UUID,
            randomString(),
            randomContactType(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            ICON_UUID,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        var update = new Contact(
            CONTACT_UUID,
            randomString(),
            randomContactType(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            randomString(),
            null,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        insertAndUpdate(repository, insert, update);
    }

    @Test(dependsOnMethods = {"testIcon", "testCategory", "testCurrency"})
    public void testAccount() {
        var repository = getDao().getAccountRepository();

        var insert = new Account(
            ACCOUNT_UUID,
            randomString(),
            randomString(),
            randomString(),
            randomBigDecimal(),
            randomBigDecimal(),
            randomBigDecimal(),
            randomCategoryType(),
            CATEGORY_UUID,
            CURRENCY_UUID,
            randomBoolean(),
            randomBigDecimal(),
            LocalDate.now(),
            ICON_UUID,
            randomCardType(),
            randomString(),
            randomBigDecimal(),
            randomBigDecimal(),
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        var update = new Account(
            ACCOUNT_UUID,
            randomString(),
            randomString(),
            randomString(),
            randomBigDecimal(),
            randomBigDecimal(),
            randomBigDecimal(),
            randomCategoryType(),
            CATEGORY_UUID,
            null,
            randomBoolean(),
            randomBigDecimal(),
            LocalDate.now(),
            null,
            randomCardType(),
            randomString(),
            randomBigDecimal(),
            randomBigDecimal(),
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        insertAndUpdate(repository, insert, update);
    }

    @Test(dependsOnMethods = {"testCategory", "testAccount", "testContact"})
    public void testTransaction() {
        var repository = getDao().getTransactionRepository();

        var insert = new Transaction(
            TRANSACTION_UUID,
            randomBigDecimal(),
            randomDay(),
            randomMonth(),
            randomYear(),
            randomTransactionType(),
            randomString(),
            randomBoolean(),
            ACCOUNT_UUID,
            ACCOUNT_UUID,
            randomCategoryType(),
            randomCategoryType(),
            CATEGORY_UUID,
            CATEGORY_UUID,
            CONTACT_UUID,
            randomBigDecimal(),
            randomInt(),
            randomString(),
            null,
            randomBoolean(),
            LocalDate.now(),
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        var update = new Transaction(
            TRANSACTION_UUID,
            randomBigDecimal(),
            randomDay(),
            randomMonth(),
            randomYear(),
            randomTransactionType(),
            randomString(),
            randomBoolean(),
            ACCOUNT_UUID,
            ACCOUNT_UUID,
            randomCategoryType(),
            randomCategoryType(),
            CATEGORY_UUID,
            CATEGORY_UUID,
            null,
            randomBigDecimal(),
            randomInt(),
            randomString(),
            TRANSACTION_UUID,
            randomBoolean(),
            LocalDate.now(),
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );

        insertAndUpdate(repository, insert, update);
    }

    private static <T extends MoneyRecord> void insertAndUpdate(Repository<T> repository, T insert, T update) {
        var uuid = insert.uuid();

        repository.insert(insert);
        assertEquals(repository.get(uuid).orElseThrow(), insert);

        repository.update(update);
        assertEquals(repository.get(uuid).orElseThrow(), update);
    }
}
