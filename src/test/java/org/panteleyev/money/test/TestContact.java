/*
 * Copyright (c) 2017, Petr Panteleyev <petr@panteleyev.org>
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
package org.panteleyev.money.test;

import org.panteleyev.money.persistence.Contact;
import org.panteleyev.money.persistence.ContactType;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.UUID;

public class TestContact extends BaseTest {
    @Test
    public void testBuilder() throws Exception {
        Contact original = newContact();

        Contact.Builder builder = new Contact.Builder(original);
        Contact newContact = builder.build();
        Assert.assertEquals(newContact, original);
        Assert.assertEquals(newContact.hashCode(), original.hashCode());

        Contact.Builder emptyBuilder = new Contact.Builder()
                .id(original.getId())
                .name(original.getName())
                .comment(original.getComment())
                .type(original.getType())
                .phone(original.getPhone())
                .mobile(original.getMobile())
                .email(original.getEmail())
                .web(original.getWeb())
                .street(original.getStreet())
                .city(original.getCity())
                .country(original.getCountry())
                .zip(original.getZip());

        Assert.assertEquals(emptyBuilder.id().orElse(null), original.getId());

        newContact = emptyBuilder.build();
        Assert.assertEquals(newContact, original);
        Assert.assertEquals(newContact.hashCode(), original.hashCode());
    }

    @Test
    public void testEquals() {
        Integer id = RANDOM.nextInt();
        String name = UUID.randomUUID().toString();
        ContactType type = randomContactType();
        String phone = UUID.randomUUID().toString();
        String mobile = UUID.randomUUID().toString();
        String email = UUID.randomUUID().toString();
        String web = UUID.randomUUID().toString();
        String comment = UUID.randomUUID().toString();
        String street = UUID.randomUUID().toString();
        String city = UUID.randomUUID().toString();
        String country = UUID.randomUUID().toString();
        String zip = UUID.randomUUID().toString();

        Contact c1 = new Contact(id, name, type, phone, mobile, email, web,
                comment, street, city, country, zip);

        Contact c2 = new Contact(id, name, type, phone, mobile, email, web,
                comment, street, city, country, zip);

        Assert.assertEquals(c1, c2);
        Assert.assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testBuilderNullId() {
        Contact.Builder builder = new Contact.Builder(newContact());
        builder.id(null).build();
    }

}
