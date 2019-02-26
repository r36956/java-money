/*
 * Copyright (c) 2017, 2019, Petr Panteleyev <petr@panteleyev.org>
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

package org.panteleyev.money.persistence.model;

import org.panteleyev.money.BaseTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.UUID;
import static org.panteleyev.money.BaseTestUtils.RANDOM;
import static org.panteleyev.money.BaseTestUtils.randomBigDecimal;
import static org.panteleyev.money.BaseTestUtils.randomCategoryType;
import static org.panteleyev.money.BaseTestUtils.randomId;
import static org.panteleyev.money.BaseTestUtils.randomString;
import static org.testng.Assert.assertEquals;

public class TestAccount extends BaseTest {
    @Test
    public void testEquals() {
        int id = randomId();
        var name = UUID.randomUUID().toString();
        var comment = UUID.randomUUID().toString();
        var accountNumber = UUID.randomUUID().toString();
        var opening = randomBigDecimal();
        var limit = randomBigDecimal();
        var rate = randomBigDecimal();
        var type = randomCategoryType();
        var categoryId = randomId();
        var currencyId = randomId();
        var enabled = RANDOM.nextBoolean();
        var uuid = randomString();
        var modified = System.currentTimeMillis();

        var a1 = new Account.Builder()
            .id(id).name(name)
            .comment(comment)
            .accountNumber(accountNumber)
            .openingBalance(opening)
            .accountLimit(limit)
            .currencyRate(rate)
            .typeId(type.getId())
            .categoryId(categoryId)
            .currencyId(currencyId)
            .enabled(enabled)
            .guid(uuid)
            .modified(modified)
            .build();

        var a2 = new Account.Builder()
            .id(id).name(name)
            .comment(comment)
            .accountNumber(accountNumber)
            .openingBalance(opening)
            .accountLimit(limit)
            .currencyRate(rate)
            .typeId(type.getId())
            .categoryId(categoryId)
            .currencyId(currencyId)
            .enabled(enabled)
            .guid(uuid)
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
            .accountNumber(accountNumber)
            .typeId(CategoryType.DEBTS.getId())
            .categoryId(0)
            .currencyId(0)
            .guid("")
            .modified(0)
            .build();

        assertEquals(a.getAccountNumber(), accountNumber);
        assertEquals(a.getAccountNumberNoSpaces(), accountNumberNoSpaces);
    }

    @Test
    public void testBuilder() {
        var original = new Account.Builder()
            .id(randomId())
            .name(UUID.randomUUID().toString())
            .comment(UUID.randomUUID().toString())
            .accountNumber(UUID.randomUUID().toString())
            .openingBalance(randomBigDecimal())
            .accountLimit(randomBigDecimal())
            .currencyRate(randomBigDecimal())
            .typeId(randomCategoryType().getId())
            .categoryId(randomId())
            .currencyId(randomId())
            .enabled(RANDOM.nextBoolean())
            .guid(randomString())
            .modified(System.currentTimeMillis())
            .build();

        var copy = new Account.Builder(original).build();
        assertEquals(copy, original);

        var manualCopy = new Account.Builder()
            .id(original.getId())
            .name(original.getName())
            .comment(original.getComment())
            .accountNumber(original.getAccountNumber())
            .openingBalance(original.getOpeningBalance())
            .accountLimit(original.getAccountLimit())
            .currencyRate(original.getCurrencyRate())
            .typeId(original.getTypeId())
            .categoryId(original.getCategoryId())
            .currencyId(original.getCurrencyId())
            .enabled(original.getEnabled())
            .guid(original.getGuid())
            .modified(original.getModified())
            .build();
        assertEquals(manualCopy, original);
    }
}