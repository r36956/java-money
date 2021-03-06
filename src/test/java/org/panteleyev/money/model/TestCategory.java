/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.model;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.UUID;
import static org.panteleyev.money.test.BaseTestUtils.randomCategoryType;
import static org.panteleyev.money.test.BaseTestUtils.randomString;
import static org.testng.Assert.assertEquals;

public class TestCategory extends ModelTestBase {

    @DataProvider
    @Override
    public Object[][] testBuildDataProvider() {
        var uuid = UUID.randomUUID();
        var name = randomString();
        var comment = randomString();
        var type = randomCategoryType();
        var iconUuid = UUID.randomUUID();
        var created = System.currentTimeMillis();
        var modified = created + 1000;

        return new Object[][]{
            {
                new Category.Builder()
                    .uuid(uuid)
                    .name(name)
                    .comment(comment)
                    .type(type)
                    .iconUuid(iconUuid)
                    .created(created)
                    .modified(modified)
                    .build(),
                new Category(uuid, name, comment, type, iconUuid, created, modified)
            },
            {
                new Category(uuid, name, null, type, iconUuid, created, modified),
                new Category(uuid, name, "", type, iconUuid, created, modified)
            }
        };
    }

    @Test
    public void testEquals() {
        var name = UUID.randomUUID().toString();
        var comment = UUID.randomUUID().toString();
        var type = randomCategoryType();
        var iconUuid = UUID.randomUUID();
        var uuid = UUID.randomUUID();
        var created = System.currentTimeMillis();
        var modified = System.currentTimeMillis();

        var c1 = new Category.Builder()
            .name(name)
            .comment(comment)
            .type(type)
            .iconUuid(iconUuid)
            .uuid(uuid)
            .created(created)
            .modified(modified)
            .build();
        var c2 = new Category.Builder()
            .name(name)
            .comment(comment)
            .type(type)
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
        var original = new Category.Builder()
            .name(randomString())
            .comment(randomString())
            .type(randomCategoryType())
            .iconUuid(UUID.randomUUID())
            .uuid(UUID.randomUUID())
            .created(System.currentTimeMillis())
            .modified(System.currentTimeMillis())
            .build();

        var copy = new Category.Builder(original).build();
        assertEquals(copy, original);

        var manualCopy = new Category.Builder()
            .name(original.name())
            .comment(original.comment())
            .type(original.type())
            .iconUuid(original.iconUuid())
            .uuid(original.uuid())
            .created(original.created())
            .modified(original.modified())
            .build();
        assertEquals(manualCopy, original);
    }

    @Test(expectedExceptions = {IllegalStateException.class})
    public void testNegativeBuilder() {
        new Category.Builder().build();
    }
}
