package org.panteleyev.money.app;

/*
 * Copyright (c) Petr Panteleyev. All rights reserved.
 * Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import org.controlsfx.control.textfield.TextFields;
import org.panteleyev.fx.PredicateProperty;
import org.panteleyev.money.app.filters.AccountSelectionBox;
import org.panteleyev.money.app.filters.ContactFilterBox;
import org.panteleyev.money.app.filters.TransactionFilterBox;
import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Contact;
import org.panteleyev.money.model.Transaction;
import org.panteleyev.money.persistence.MoneyDAO;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static org.panteleyev.fx.ButtonFactory.newButton;
import static org.panteleyev.fx.LabelFactory.newLabel;
import static org.panteleyev.fx.MenuFactory.newMenu;
import static org.panteleyev.fx.MenuFactory.newMenuBar;
import static org.panteleyev.fx.MenuFactory.newMenuItem;
import static org.panteleyev.money.MoneyApplication.generateFileName;
import static org.panteleyev.money.app.Constants.COLON;
import static org.panteleyev.money.app.Constants.ELLIPSIS;
import static org.panteleyev.money.app.Constants.SHORTCUT_DELETE;
import static org.panteleyev.money.app.Constants.SHORTCUT_E;
import static org.panteleyev.money.app.Constants.SHORTCUT_K;
import static org.panteleyev.money.app.Constants.SHORTCUT_U;
import static org.panteleyev.money.app.MainWindowController.RB;
import static org.panteleyev.money.persistence.DataCache.cache;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;

class RequestWindowController extends BaseController {
    private final Account account;

    private final TransactionTableView table = new TransactionTableView(TransactionTableView.Mode.QUERY);

    private final AccountSelectionBox accBox = new AccountSelectionBox();
    private final TransactionFilterBox transactionFilterBox = new TransactionFilterBox(true, true);
    private final ContactFilterBox contactFilterBox = new ContactFilterBox();

    private final PredicateProperty<Transaction> filterProperty =
        PredicateProperty.and(List.of(accBox.predicateProperty(),
            transactionFilterBox.predicateProperty(),
            contactFilterBox.predicateProperty()));

    private final TreeSet<String> contactSuggestions = new TreeSet<>();

    private static class CompletionProvider extends BaseCompletionProvider<String> {
        CompletionProvider(Set<String> set) {
            super(set, Options::getAutoCompleteLength);
        }

        public String getElementString(String element) {
            return element;
        }
    }

    RequestWindowController() {
        this(null);
    }

    RequestWindowController(Account account) {
        this.account = account;

        var fillBox = new Region();

        var filterBox = new HBox(5.0);
        if (account == null) {
            filterBox.getChildren().add(accBox);
        }
        filterBox.getChildren().addAll(
            transactionFilterBox,
            newLabel(RB, "Counterparty", COLON),
            contactFilterBox.getTextField(),
            fillBox,
            newButton(RB, "Reset_Filter", x -> onClearButton())
        );

        HBox.setHgrow(fillBox, Priority.ALWAYS);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(filterBox, new Insets(5.0, 5.0, 5.0, 5.0));

        var centerBox = new BorderPane();
        centerBox.setTop(filterBox);
        centerBox.setCenter(table);

        filterProperty.addListener((x, y, newValue) -> onUpdateFilter());

        transactionFilterBox.setTransactionFilter(TransactionPredicate.CURRENT_MONTH);

        table.setOnCheckTransaction(this::onCheckTransaction);

        var root = new BorderPane();
        root.setTop(createMenuBar());
        root.setCenter(centerBox);

        accBox.setupCategoryTypesBox();
        transactionFilterBox.setFilterYears();

        TextFields.bindAutoCompletion(contactFilterBox.getTextField(), new CompletionProvider(contactSuggestions));
        setupContactMenu();

        setupWindow(root);
        Options.loadStageDimensions(getClass(), getStage());

        if (account != null) {
            accBox.setAccount(account);
            onUpdateFilter();
        }
    }

    Account getAccount() {
        return account;
    }

    @Override
    public String getTitle() {
        return account == null ? RB.getString("Requests") : account.name();
    }

    boolean thisAccount(Account account) {
        return Objects.equals(this.account, account);
    }

    private Predicate<Transaction> getTransactionFilter() {
        return table.getTransactionFilter();
    }

    private MenuBar createMenuBar() {
        return newMenuBar(
            newMenu(RB, "File",
                newMenuItem(RB, "Report", ELLIPSIS, event -> onReport()),
                new SeparatorMenuItem(),
                newMenuItem(RB, "Close", event -> onClose())),
            newMenu(RB, "menu.Edit",
                newMenuItem(RB, "Edit", ELLIPSIS, SHORTCUT_E, event -> table.onEditTransaction()),
                new SeparatorMenuItem(),
                newMenuItem(RB, "Delete", ELLIPSIS, SHORTCUT_DELETE, event -> table.onDeleteTransaction()),
                new SeparatorMenuItem(),
                newMenuItem(RB, "menu.item.details", event -> table.onTransactionDetails()),
                new SeparatorMenuItem(),
                newMenuItem(RB, "menu.item.check", SHORTCUT_K, event -> table.onCheckTransactions(true)),
                newMenuItem(RB, "menu.item.uncheck", SHORTCUT_U, event -> table.onCheckTransactions(false))
            ),
            createWindowMenu(),
            createHelpMenu());
    }

    private void onUpdateFilter() {
        table.setTransactionFilter(filterProperty.get());
    }

    private void onClearButton() {
        accBox.setupCategoryTypesBox();
        contactFilterBox.getTextField().clear();
        transactionFilterBox.setTransactionFilter(TransactionPredicate.CURRENT_MONTH);
    }

    private void onCheckTransaction(List<Transaction> transactions, boolean check) {
        for (Transaction t : transactions) {
            getDao().updateTransaction(t.check(check));
        }
    }

    private void onReport() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle(RB.getString("Report"));
        Options.getLastExportDir().ifPresent(fileChooser::setInitialDirectory);
        fileChooser.setInitialFileName(generateFileName("transactions"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files", "*.html"));

        var selected = fileChooser.showSaveDialog(null);
        if (selected == null) {
            return;
        }

        try (var outputStream = new FileOutputStream(selected)) {
            var transactions = cache().getTransactions(getTransactionFilter())
                .sorted(MoneyDAO.COMPARE_TRANSACTION_BY_DATE)
                .collect(Collectors.toList());
            Reports.reportTransactions(transactions, outputStream);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void setupContactMenu() {
        contactSuggestions.clear();
        cache().getContacts().stream()
            .sorted((c1, c2) -> c1.name().compareToIgnoreCase(c2.name()))
            .map(Contact::name)
            .forEach(contactSuggestions::add);
    }
}