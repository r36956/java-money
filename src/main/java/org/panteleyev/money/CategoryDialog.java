/*
 * Copyright (c) 2015, 2017, Petr Panteleyev <petr@panteleyev.org>
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
package org.panteleyev.money;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.controlsfx.validation.ValidationResult;
import org.panteleyev.money.persistence.Category;
import org.panteleyev.money.persistence.CategoryType;
import org.panteleyev.utilities.fx.BaseDialog;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;

class CategoryDialog extends BaseDialog<Category.Builder> implements Styles {
    private final ResourceBundle           rb = ResourceBundle.getBundle(MainWindowController.UI_BUNDLE_PATH);

    private final ChoiceBox<CategoryType>  typeComboBox = new ChoiceBox<>();
    private final TextField                nameEdit = new TextField();
    private final TextField                commentEdit = new TextField();

    private final Category                 category;

    CategoryDialog(Category category) {
        super(MainWindowController.DIALOGS_CSS);

        this.category = category;

        initialize();
    }

    private void initialize() {
        getDialogPane().getStylesheets().add(MainWindowController.DIALOGS_CSS);

        setTitle(rb.getString("category.Dialog.Title"));

        GridPane pane = new GridPane();
        pane.getStyleClass().add(GRID_PANE);

        int index = 0;
        pane.addRow(index++, new Label(rb.getString("label.Type")), typeComboBox);
        pane.addRow(index++, new Label(rb.getString("label.Name")), nameEdit);
        pane.addRow(index, new Label(rb.getString("label.Comment")), commentEdit);

        nameEdit.setPrefColumnCount(20);

        final Collection<CategoryType> list = Arrays.asList(CategoryType.values());
        typeComboBox.setItems(FXCollections.observableArrayList(list));
        if (!list.isEmpty()) {
            typeComboBox.getSelectionModel().select(0);
        }

        if (category != null) {
            Optional<CategoryType> type = list.stream()
                .filter(x -> x.equals(category.getCatType()))
                .findFirst();
            type.ifPresent(categoryType -> typeComboBox.getSelectionModel().select(categoryType));
            nameEdit.setText(category.getName());
            commentEdit.setText(category.getComment());
        }

        typeComboBox.setConverter(new StringConverter<CategoryType>() {
            @Override
            public String toString(CategoryType object) {
                return object.getName();
            }

            @Override
            public CategoryType fromString(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });

        setResultConverter((ButtonType b) -> {
            if (b == ButtonType.OK) {
                return new Category.Builder(this.category)
                    .name(nameEdit.getText())
                    .comment(commentEdit.getText())
                    .type(typeComboBox.getSelectionModel().getSelectedItem());
            } else {
                return null;
            }
        });

        getDialogPane().setContent(pane);
        createDefaultButtons(rb);

        Platform.runLater(this::createValidationSupport);
    }

    private void createValidationSupport() {
        validation.registerValidator(nameEdit, (Control control, String value) ->
                ValidationResult.fromErrorIf(control, null, value.isEmpty()));
        validation.initInitialDecoration();
    }
}