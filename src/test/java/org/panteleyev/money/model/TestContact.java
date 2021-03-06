/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.model;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.UUID;
import static org.panteleyev.money.test.BaseTestUtils.randomContactType;
import static org.panteleyev.money.test.BaseTestUtils.randomString;
import static org.testng.Assert.assertEquals;

public class TestContact extends ModelTestBase {

    @DataProvider
    @Override
    public Object[][] testBuildDataProvider() {
        var uuid = UUID.randomUUID();
        var name = randomString();
        var type = randomContactType();
        var phone = randomString();
        var mobile = randomString();
        var email = randomString();
        var web = randomString();
        var comment = randomString();
        var street = randomString();
        var city = randomString();
        var country = randomString();
        var zip = randomString();
        var iconUuid = UUID.randomUUID();
        var created = System.currentTimeMillis();
        var modified = System.currentTimeMillis();

        return new Object[][]{
            {
                new Contact.Builder()
                    .uuid(uuid)
                    .name(name)
                    .type(type)
                    .phone(phone)
                    .mobile(mobile)
                    .email(email)
                    .web(web)
                    .comment(comment)
                    .street(street)
                    .city(city)
                    .country(country)
                    .zip(zip)
                    .iconUuid(iconUuid)
                    .created(created)
                    .modified(modified)
                    .build(),
                new Contact(
                    uuid, name, type, phone, mobile,
                    email, web, comment, street, city,
                    country, zip, iconUuid, created, modified
                )
            },
            {
                new Contact(
                    uuid, name, type, null, null,
                    null, null, null, null, null,
                    null, null, iconUuid, created, modified
                ),
                new Contact(
                    uuid, name, type, "", "",
                    "", "", "", "", "",
                    "", "", iconUuid, created, modified
                )
            }
        };
    }

    @Test
    public void testEquals() {
        var name = randomString();
        var type = randomContactType();
        var phone = randomString();
        var mobile = randomString();
        var email = randomString();
        var web = randomString();
        var comment = randomString();
        var street = randomString();
        var city = randomString();
        var country = randomString();
        var zip = randomString();
        var iconUuid = UUID.randomUUID();
        var uuid = UUID.randomUUID();
        var created = System.currentTimeMillis();
        var modified = System.currentTimeMillis();

        var c1 = new Contact.Builder()
            .name(name)
            .type(type)
            .phone(phone)
            .mobile(mobile)
            .email(email)
            .web(web)
            .comment(comment)
            .street(street)
            .city(city)
            .country(country)
            .zip(zip)
            .iconUuid(iconUuid)
            .uuid(uuid)
            .created(created)
            .modified(modified)
            .build();

        var c2 = new Contact.Builder()
            .name(name)
            .type(type)
            .phone(phone)
            .mobile(mobile)
            .email(email)
            .web(web)
            .comment(comment)
            .street(street)
            .city(city)
            .country(country)
            .zip(zip)
            .iconUuid(iconUuid)
            .uuid(uuid)
            .created(created)
            .modified(modified)
            .build();

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testCopy() {
        var original = new Contact.Builder()
            .name(randomString())
            .type(randomContactType())
            .phone(randomString())
            .mobile(randomString())
            .email(randomString())
            .web(randomString())
            .comment(randomString())
            .street(randomString())
            .city(randomString())
            .country(randomString())
            .zip(randomString())
            .iconUuid(UUID.randomUUID())
            .uuid(UUID.randomUUID())
            .created(System.currentTimeMillis())
            .modified(System.currentTimeMillis())
            .build();

        var copy = new Contact.Builder(original).build();
        assertEquals(copy, original);
        assertEquals(copy.hashCode(), original.hashCode());

        var manualCopy = new Contact.Builder()
            .name(original.name())
            .type(original.type())
            .phone(original.phone())
            .mobile(original.mobile())
            .email(original.email())
            .web(original.web())
            .comment(original.comment())
            .street(original.street())
            .city(original.city())
            .country(original.country())
            .zip(original.zip())
            .iconUuid(original.iconUuid())
            .uuid(original.uuid())
            .created(original.created())
            .modified(original.modified())
            .build();
        assertEquals(manualCopy, original);
        assertEquals(manualCopy.hashCode(), original.hashCode());
    }
}
