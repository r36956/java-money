package org.panteleyev.money;

/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ButtonType;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.CategoryType;
import org.panteleyev.money.test.BaseTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import static org.panteleyev.money.test.BaseTestUtils.randomCategoryType;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class CategoryDialogTest extends BaseTest {
    private static final String CATEGORY_NAME = UUID.randomUUID().toString();
    private static final String CATEGORY_COMMENT = UUID.randomUUID().toString();
    private static final CategoryType CATEGORY_TYPE = randomCategoryType();
    private static final CategoryType CATEGORY_TYPE_NEW = randomCategoryType();

    private final static Category CATEGORY = new Category.Builder()
        .name(UUID.randomUUID().toString())
        .comment(UUID.randomUUID().toString())
        .catTypeId(CategoryType.BANKS_AND_CASH.getId())
        .guid(UUID.randomUUID())
        .modified(System.currentTimeMillis())
        .build();

    @BeforeClass
    public void setupAndSkip() {
        new JFXPanel();
    }

    private void setupDialog(CategoryDialog dialog) {
        dialog.getNameEdit().setText(CATEGORY_NAME);
        dialog.getCommentEdit().setText(CATEGORY_COMMENT);
        dialog.getTypeComboBox().getSelectionModel().select(CATEGORY_TYPE);
    }

    private void setupDialogUpdate(CategoryDialog dialog) {
        dialog.getTypeComboBox().getSelectionModel().select(CATEGORY_TYPE_NEW);
    }

    @Test
    public void testNewCategory() throws Exception {
        BlockingQueue<Category> queue = new ArrayBlockingQueue<>(1);

        Platform.runLater(() -> {
            var dialog = new CategoryDialog(null, null);
            setupDialog(dialog);
            var category = dialog.getResultConverter().call(ButtonType.OK);
            queue.add(category);
        });

        var category = queue.take();

        assertNotNull(category.getUuid());
        assertEquals(category.getName(), CATEGORY_NAME);
        assertEquals(category.getComment(), CATEGORY_COMMENT);
        assertEquals(category.getType(), CATEGORY_TYPE);
        assertEquals(category.getCreated(), category.getModified());
    }

    @Test
    public void testExistingCategory() throws Exception {
        BlockingQueue<Category> queue = new ArrayBlockingQueue<>(1);

        Platform.runLater(() -> {
            var dialog = new CategoryDialog(null, CATEGORY);
            setupDialogUpdate(dialog);
            var category = dialog.getResultConverter().call(ButtonType.OK);
            queue.add(category);
        });

        var category = queue.take();

        assertEquals(category.getUuid(), CATEGORY.getUuid());
        assertEquals(category.getName(), CATEGORY.getName());
        assertEquals(category.getComment(), CATEGORY.getComment());
        assertEquals(category.getType(), CATEGORY_TYPE_NEW);
        assertTrue(category.getModified() > CATEGORY.getModified());
        assertTrue(category.getModified() > category.getCreated());
    }
}
