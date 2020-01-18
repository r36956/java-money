/*
 * Copyright (c) 2020, Petr Panteleyev <petr@panteleyev.org>
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

package org.panteleyev.money.filters;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakMapChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import org.panteleyev.fx.PredicateProperty;
import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.CategoryType;
import org.panteleyev.money.persistence.ReadOnlyStringConverter;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import static org.panteleyev.money.Predicates.accountByCategory;
import static org.panteleyev.money.Predicates.accountByCategoryType;
import static org.panteleyev.money.MainWindowController.RB;
import static org.panteleyev.money.persistence.DataCache.cache;

public class CategorySelectionBox extends HBox {
    private static class TypeListItem {
        private final String text;
        private final EnumSet<CategoryType> types;

        TypeListItem(String text, CategoryType type, CategoryType... types) {
            this.text = text;
            this.types = EnumSet.of(type, types);
        }

        String getText() {
            return text;
        }

        EnumSet<CategoryType> getTypes() {
            return types;
        }
    }

    private final ChoiceBox<Object> categoryTypeChoiceBox = new ChoiceBox<>();
    private final ChoiceBox<Object> categoryChoiceBox = new ChoiceBox<>();

    private final PredicateProperty<Account> accountFilterProperty = new PredicateProperty<>();

    @SuppressWarnings("FieldCanBeLocal")
    private final MapChangeListener<UUID, Category> categoryListener = change ->
        Platform.runLater(this::onTypeChanged);

    private final EventHandler<ActionEvent> categoryTypeHandler =
        event -> onTypeChanged();

    private final EventHandler<ActionEvent> categoryHandler =
        event -> accountFilterProperty.set(getAccountFilter());

    public CategorySelectionBox() {
        super(5.0);

        setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(categoryTypeChoiceBox, categoryChoiceBox);

        categoryTypeChoiceBox.setConverter(new ReadOnlyStringConverter<>() {
            @Override
            public String toString(Object object) {
                if (object instanceof TypeListItem) {
                    return ((TypeListItem) object).getText();
                } else {
                    return object != null ? object.toString() : "-";
                }
            }
        });

        categoryChoiceBox.setConverter(new ReadOnlyStringConverter<>() {
            @Override
            public String toString(Object obj) {
                return obj instanceof Category ? ((Category) obj).getName() : obj.toString();
            }
        });

        categoryTypeChoiceBox.setOnAction(categoryTypeHandler);
        categoryChoiceBox.setOnAction(categoryHandler);

        cache().categories().addListener(new WeakMapChangeListener<>(categoryListener));
    }

    public PredicateProperty<Account> accountFilterProperty() {
        return accountFilterProperty;
    }

    public void setupCategoryTypesBox() {
        categoryTypeChoiceBox.setOnAction(event -> {});

        categoryTypeChoiceBox.getItems().setAll(
            new TypeListItem(RB.getString("text.AccountsCashCards"),
                CategoryType.BANKS_AND_CASH, CategoryType.DEBTS),
            new TypeListItem(RB.getString("Incomes_and_Expenses"),
                CategoryType.INCOMES, CategoryType.EXPENSES),
            new Separator()
        );

        for (var t : CategoryType.values()) {
            categoryTypeChoiceBox.getItems().add(new TypeListItem(t.getTypeName(), t));
        }

        categoryTypeChoiceBox.setOnAction(categoryTypeHandler);
        categoryTypeChoiceBox.getSelectionModel().selectFirst();
    }

    private Optional<Category> getSelectedCategory() {
        var obj = categoryChoiceBox.getSelectionModel().getSelectedItem();
        return obj instanceof Category ? Optional.of((Category) obj) : Optional.empty();
    }

    private Set<CategoryType> getSelectedCategoryTypes() {
        var obj = categoryTypeChoiceBox.getSelectionModel().getSelectedItem();
        if (obj instanceof TypeListItem) {
            return ((TypeListItem) obj).getTypes();
        } else {
            return Set.of();
        }
    }

    private Predicate<Account> getAccountFilter() {
        return getSelectedCategory().map(c -> accountByCategory(c.getUuid()))
            .orElseGet(() -> {
                var selectedTypes = getSelectedCategoryTypes();
                return selectedTypes.isEmpty() ? a -> false :
                    accountByCategoryType(selectedTypes);
            });
    }

    private void onTypeChanged() {
        categoryChoiceBox.setOnAction(x -> {});
        var object = categoryTypeChoiceBox.getSelectionModel().getSelectedItem();

        if (!(object instanceof TypeListItem)) {
            return;
        }
        var typeListItem = (TypeListItem) object;

        ObservableList<Object> items =
            FXCollections.observableArrayList(cache().getCategoriesByType(typeListItem.getTypes()));

        if (!items.isEmpty()) {
            items.add(0, new Separator());
        }

        items.add(0, RB.getString("All_Categories"));

        categoryChoiceBox.setItems(items);
        categoryChoiceBox.getSelectionModel().selectFirst();
        categoryChoiceBox.setOnAction(categoryHandler);
        accountFilterProperty.set(getAccountFilter());
    }
}