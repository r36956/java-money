/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.app;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.panteleyev.fx.PredicateProperty;
import org.panteleyev.money.app.cells.TransactionCheckCell;
import org.panteleyev.money.app.cells.TransactionContactCell;
import org.panteleyev.money.app.cells.TransactionCreditedAccountCell;
import org.panteleyev.money.app.cells.TransactionDayCell;
import org.panteleyev.money.app.cells.TransactionDebitedAccountCell;
import org.panteleyev.money.app.cells.TransactionRow;
import org.panteleyev.money.app.cells.TransactionSumCell;
import org.panteleyev.money.app.cells.TransactionTypeCell;
import org.panteleyev.money.app.details.TransactionDetailsDialog;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.Contact;
import org.panteleyev.money.model.Transaction;
import org.panteleyev.money.model.TransactionDetail;
import org.panteleyev.money.persistence.MoneyDAO;
import org.panteleyev.money.xml.Export;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static org.panteleyev.fx.FxUtils.fxString;
import static org.panteleyev.fx.MenuFactory.menuItem;
import static org.panteleyev.fx.TableColumnBuilder.tableColumn;
import static org.panteleyev.fx.TableColumnBuilder.tableObjectColumn;
import static org.panteleyev.money.app.Constants.ELLIPSIS;
import static org.panteleyev.money.app.MainWindowController.RB;
import static org.panteleyev.money.persistence.DataCache.cache;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;

class TransactionTableView extends TableView<Transaction> {
    public interface TransactionDetailsCallback {
        void handleTransactionDetails(Transaction transaction, List<TransactionDetail> details);
    }

    public enum Mode {
        ACCOUNT(false),
        STATEMENT(true),
        QUERY(true);

        private final boolean fullDate;

        Mode(boolean fullDate) {
            this.fullDate = fullDate;
        }

        public boolean isFullDate() {
            return fullDate;
        }
    }

    private final Mode mode;
    private BiConsumer<List<Transaction>, Boolean> checkTransactionConsumer = (x, y) -> { };
    private final TransactionDetailsCallback transactionDetailsCallback;

    private final Consumer<Transaction> transactionAddedCallback;
    private final Consumer<Transaction> transactionUpdatedCallback;

    @SuppressWarnings("FieldCanBeLocal")
    private final ListChangeListener<Contact> contactChangeLister = change -> Platform.runLater(this::redraw);

    @SuppressWarnings("FieldCanBeLocal")
    private final ListChangeListener<Category> categoryChangeLister = change -> Platform.runLater(this::redraw);

    // Transaction filter
    private final PredicateProperty<Transaction> transactionPredicateProperty = new PredicateProperty<>(x -> false);

    private final FilteredList<Transaction> filteredList = new FilteredList<>(cache().getTransactions());

    // List size property
    private final SimpleIntegerProperty listSizeProperty = new SimpleIntegerProperty(0);

    TransactionTableView(Mode mode) {
        this(mode, null, x -> {}, x -> {});
    }

    TransactionTableView(Mode mode, TransactionDetailsCallback transactionDetailsCallback, Consumer<Transaction> transactionAddedCallback,
                         Consumer<Transaction> transactionUpdatedCallback)
    {
        this.mode = mode;
        this.transactionDetailsCallback = transactionDetailsCallback;
        this.transactionAddedCallback = transactionAddedCallback;
        this.transactionUpdatedCallback = transactionUpdatedCallback;

        setRowFactory(x -> new TransactionRow());

        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        var w = widthProperty().subtract(20);
        var dayComparator = mode.isFullDate() ? MoneyDAO.COMPARE_TRANSACTION_BY_DATE : MoneyDAO.COMPARE_TRANSACTION_BY_DAY;

        getColumns().setAll(List.of(
            tableObjectColumn(fxString(RB, "Day"), b ->
                b.withCellFactory(x -> new TransactionDayCell(mode.isFullDate()))
                    .withComparator(dayComparator)
                    .withWidthBinding(w.multiply(0.05))),
            tableObjectColumn(fxString(RB, "Type"), b ->
                b.withCellFactory(x -> new TransactionTypeCell())
                    .withComparator(Comparator.comparingInt((Transaction t) -> t.type().ordinal())
                        .thenComparing(dayComparator))
                    .withWidthBinding(w.multiply(0.1))),
            tableObjectColumn(fxString(RB, "column.Account.Debited"), b ->
                b.withCellFactory(x -> new TransactionDebitedAccountCell())
                    .withComparator(Comparator.comparing(Transaction::accountDebitedUuid)
                        .thenComparing(dayComparator))
                    .withWidthBinding(w.multiply(0.1))),
            tableObjectColumn(fxString(RB, "column.Account.Credited"), b ->
                b.withCellFactory(x -> new TransactionCreditedAccountCell()).withWidthBinding(w.multiply(0.1))),
            tableObjectColumn(fxString(RB, "Counterparty"), b ->
                b.withCellFactory(x -> new TransactionContactCell()).withWidthBinding(w.multiply(0.2))),
            tableColumn(fxString(RB, "Comment"), b ->
                b.withPropertyCallback(Transaction::comment).withWidthBinding(w.multiply(0.35))),
            tableObjectColumn(fxString(RB, "Sum"), b ->
                b.withCellFactory(x -> new TransactionSumCell())
                    .withComparator(Comparator.comparing(Transaction::getSignedAmount))
                    .withWidthBinding(w.multiply(0.05))),
            tableObjectColumn("", b ->
                b.withCellFactory(x -> new TransactionCheckCell()).withWidthBinding(w.multiply(0.05)))
        ));

        getSortOrder().add(getColumns().get(0));

        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        createContextMenu();

        cache().getContacts().addListener(new WeakListChangeListener<>(contactChangeLister));
        cache().getCategories().addListener(new WeakListChangeListener<>(categoryChangeLister));

        filteredList.predicateProperty().bind(transactionPredicateProperty);

        var sortedList = filteredList.sorted();
        sortedList.comparatorProperty().bind(comparatorProperty());
        setItems(sortedList);
    }

    ReadOnlyIntegerProperty listSizeProperty() {
        return listSizeProperty;
    }

    private void createContextMenu() {
        var newMenuItem = menuItem(fxString(RB, "Add", ELLIPSIS), event -> onNewTransaction());
        var editMenuItem = menuItem(fxString(RB, "Edit", ELLIPSIS), event -> onEditTransaction());
        var deleteMenuItem = menuItem(fxString(RB, "Delete", ELLIPSIS), event -> onDeleteTransaction());
        var exportMenuItem = menuItem(fxString(RB, "menu.Context.Export"), event -> onExportTransactions());
        var detailsMenuItem = menuItem(fxString(RB, "menu.item.details"), event -> onTransactionDetails());
        var checkMenuItem = menuItem(fxString(RB, "menu.item.check"), event -> onCheckTransactions(true));
        var uncheckMenuItem = menuItem(fxString(RB, "menu.item.uncheck"), event -> onCheckTransactions(false));

        editMenuItem.disableProperty().bind(getSelectionModel().selectedItemProperty().isNull());
        deleteMenuItem.disableProperty().bind(getSelectionModel().selectedItemProperty().isNull());
        detailsMenuItem.disableProperty().bind(getSelectionModel().selectedItemProperty().isNull());

        var ctxMenu = new ContextMenu();

        if (mode == Mode.ACCOUNT) {
            ctxMenu.getItems().addAll(
                newMenuItem
            );
        }

        ctxMenu.getItems().addAll(
            editMenuItem,
            new SeparatorMenuItem()
        );

        if (mode == Mode.ACCOUNT || mode == Mode.QUERY) {
            ctxMenu.getItems().addAll(
                deleteMenuItem,
                new SeparatorMenuItem()
            );
        }

        if (mode != Mode.STATEMENT) {
            ctxMenu.getItems().addAll(
                exportMenuItem,
                new SeparatorMenuItem(),
                detailsMenuItem,
                new SeparatorMenuItem()
            );
        }

        ctxMenu.getItems().addAll(
            checkMenuItem,
            uncheckMenuItem
        );

        setContextMenu(ctxMenu);
    }

    Predicate<Transaction> getTransactionFilter() {
        return transactionPredicateProperty.get();
    }

    void setTransactionFilter(Predicate<Transaction> filter) {
        transactionPredicateProperty.set(filter.and(t -> t.parentUuid() == null));
        listSizeProperty.set(filteredList.size());
    }

    Optional<Transaction> getSelectedTransaction() {
        return Optional.ofNullable(getSelectionModel().getSelectedItem());
    }

    private void onCheckTransaction(List<Transaction> t, boolean checked) {
        checkTransactionConsumer.accept(t, checked);
    }

    void setOnCheckTransaction(BiConsumer<List<Transaction>, Boolean> c) {
        checkTransactionConsumer = c;
    }

    private void onExportTransactions() {
        var toExport = getSelectionModel().getSelectedItems();
        if (toExport.isEmpty()) {
            return;
        }

        var fileChooser = new FileChooser();
        fileChooser.setTitle("Export to file");
        Options.getLastExportDir().ifPresent(fileChooser::setInitialDirectory);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("XML Files", "*.xml"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        var selected = fileChooser.showSaveDialog(null);
        if (selected != null) {
            CompletableFuture.runAsync(() -> {
                try (var out = new FileOutputStream(selected)) {
                    new Export().withTransactions(toExport, true)
                        .doExport(out);
                    Options.setLastExportDir(selected.getParent());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
    }

    private void redraw() {
        getColumns().get(0).setVisible(false);
        getColumns().get(0).setVisible(true);
    }

    void onTransactionDetails() {
        getSelectedTransaction().ifPresent(t -> {
            var childTransactions = cache().getTransactionDetails(t);

            if (mode == Mode.ACCOUNT) {
                if (transactionDetailsCallback == null) {
                    return;
                }
                new TransactionDetailsDialog(childTransactions, t.amount(), false)
                    .showAndWait()
                    .ifPresent(list -> transactionDetailsCallback.handleTransactionDetails(t, list));
            } else {
                new TransactionDetailsDialog(childTransactions, BigDecimal.ZERO, true).showAndWait();
            }
        });
    }

    void onCheckTransactions(boolean check) {
        var process = getSelectionModel().getSelectedItems().stream()
            .filter(t -> t.checked() != check)
            .collect(Collectors.toList());

        onCheckTransaction(process, check);
    }

    void onNewTransaction() {
        new TransactionDialog(cache()).showAndWait().ifPresent(
            builder -> transactionAddedCallback.accept(getDao().insertTransaction(builder))
        );
    }

    void onEditTransaction() {
        getSelectedTransaction()
            .flatMap(selected -> new TransactionDialog(selected, cache()).showAndWait())
            .ifPresent(builder -> transactionUpdatedCallback.accept(getDao().updateTransaction(builder)));
    }

    void onDeleteTransaction() {
        getSelectedTransaction().ifPresent(transaction ->
            new Alert(Alert.AlertType.CONFIRMATION, "Are you sure to delete this transaction?")
                .showAndWait()
                .ifPresent(r -> {
                    if (r == ButtonType.OK) {
                        getDao().deleteTransaction(transaction);
                    }
                }));
    }

    boolean checkFocus() {
        return isFocused();
    }
}
