/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.app;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.panteleyev.fx.PredicateProperty;
import org.panteleyev.fx.TableColumnBuilder;
import org.panteleyev.money.app.cells.AccountBalanceCell;
import org.panteleyev.money.app.cells.AccountCardCell;
import org.panteleyev.money.app.cells.AccountCategoryCell;
import org.panteleyev.money.app.cells.AccountClosingDateCell;
import org.panteleyev.money.app.cells.AccountCommentCell;
import org.panteleyev.money.app.cells.AccountInterestCell;
import org.panteleyev.money.app.cells.AccountNameCell;
import org.panteleyev.money.app.filters.AccountNameFilterBox;
import org.panteleyev.money.app.filters.CategorySelectionBox;
import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Category;
import org.panteleyev.money.model.Currency;
import org.panteleyev.money.model.Transaction;
import org.panteleyev.money.persistence.MoneyDAO;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.panteleyev.fx.BoxFactory.hBox;
import static org.panteleyev.fx.FxUtils.ELLIPSIS;
import static org.panteleyev.fx.FxUtils.fxString;
import static org.panteleyev.fx.MenuFactory.checkMenuItem;
import static org.panteleyev.fx.MenuFactory.menuBar;
import static org.panteleyev.fx.MenuFactory.menuItem;
import static org.panteleyev.fx.MenuFactory.newMenu;
import static org.panteleyev.fx.TableColumnBuilder.tableColumn;
import static org.panteleyev.fx.TableColumnBuilder.tableObjectColumn;
import static org.panteleyev.money.MoneyApplication.generateFileName;
import static org.panteleyev.money.app.MainWindowController.UI;
import static org.panteleyev.money.app.Predicates.activeAccount;
import static org.panteleyev.money.app.Shortcuts.SHORTCUT_C;
import static org.panteleyev.money.app.Shortcuts.SHORTCUT_DELETE;
import static org.panteleyev.money.app.Shortcuts.SHORTCUT_E;
import static org.panteleyev.money.app.Shortcuts.SHORTCUT_F;
import static org.panteleyev.money.app.Shortcuts.SHORTCUT_H;
import static org.panteleyev.money.app.Shortcuts.SHORTCUT_N;
import static org.panteleyev.money.app.Shortcuts.SHORTCUT_R;
import static org.panteleyev.money.app.Shortcuts.SHORTCUT_T;
import static org.panteleyev.money.app.options.Options.options;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_EDIT;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_FILE;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_ITEM_ACTIVATE;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_ITEM_ADD;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_ITEM_COPY_NAME;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_ITEM_DEACTIVATE;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_ITEM_DELETE;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_ITEM_EDIT;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_ITEM_REPORT;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_ITEM_SEARCH;
import static org.panteleyev.money.bundles.Internationalization.I18N_MENU_VIEW;
import static org.panteleyev.money.bundles.Internationalization.I18N_MISC_ARE_YOU_SURE;
import static org.panteleyev.money.bundles.Internationalization.I18N_MISC_RECALCULATE_BALANCE;
import static org.panteleyev.money.bundles.Internationalization.I18N_MISC_SHOW_DEACTIVATED_ACCOUNTS;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_ACCOUNTS;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_BALANCE;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_CARD;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_CATEGORY;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_CLOSE;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_COMMENT;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_CURRENCY;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_ENTITY_NAME;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_REPORT;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_TRANSACTIONS;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_UNTIL;
import static org.panteleyev.money.bundles.Internationalization.I18N_WORD_WAITING;
import static org.panteleyev.money.persistence.DataCache.cache;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;

final class AccountWindowController extends BaseController {
    // Filters
    private final CategorySelectionBox categorySelectionBox = new CategorySelectionBox();
    private final AccountNameFilterBox accountNameFilterBox = new AccountNameFilterBox();
    private final PredicateProperty<Account> showDeactivatedAccounts =
        new PredicateProperty<>(options().getShowDeactivatedAccounts() ? a -> true : activeAccount(true));

    private final PredicateProperty<Account> filterProperty =
        PredicateProperty.and(List.of(
            categorySelectionBox.accountFilterProperty(),
            accountNameFilterBox.predicateProperty(),
            showDeactivatedAccounts
        ));

    // Items
    private final FilteredList<Account> filteredAccounts = cache().getAccounts().filtered(filterProperty.get());
    private final TableView<Account> tableView = new TableView<>(
        filteredAccounts.sorted(
            MoneyDAO.COMPARE_ACCOUNT_BY_CATEGORY.thenComparing(MoneyDAO.COMPARE_ACCOUNT_BY_NAME)
        )
    );

    @SuppressWarnings("FieldCanBeLocal")
    private final ListChangeListener<Category> categoryListener = c -> Platform.runLater(() -> {
        tableView.getColumns().get(0).setVisible(false);
        tableView.getColumns().get(0).setVisible(true);
    });

    @SuppressWarnings("FieldCanBeLocal")
    private final ListChangeListener<Transaction> transactionListener = c -> Platform.runLater(tableView::refresh);

    AccountWindowController() {
        setupTableColumns();
        createContextMenu();

        // Tool box
        var hBox = hBox(5.0,
            accountNameFilterBox.getTextField(),
            categorySelectionBox
        );
        hBox.setAlignment(Pos.CENTER_LEFT);

        var self = new BorderPane(
            new BorderPane(tableView, hBox, null, null, null),
            createMainMenu(), null, null, null
        );

        BorderPane.setMargin(hBox, new Insets(5.0, 5.0, 5.0, 5.0));

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        categorySelectionBox.setupCategoryTypesBox();

        filteredAccounts.predicateProperty().bind(filterProperty);

        cache().getCategories().addListener(new WeakListChangeListener<>(categoryListener));
        cache().getTransactions().addListener(new WeakListChangeListener<>(transactionListener));

        setupWindow(self);
        options().loadStageDimensions(this);
    }

    @Override
    public String getTitle() {
        return UI.getString(I18N_WORD_ACCOUNTS);
    }

    private MenuBar createMainMenu() {
        var disableBinding = tableView.getSelectionModel().selectedItemProperty().isNull();

        var activateAccountMenuItem = menuItem(fxString(UI, I18N_MENU_ITEM_DEACTIVATE),
            event -> onActivateDeactivateAccount(),
            disableBinding);

        var editMenu = newMenu(fxString(UI, I18N_MENU_EDIT),
            menuItem(fxString(UI, I18N_MENU_ITEM_ADD, ELLIPSIS), SHORTCUT_N,
                event -> onNewAccount()),
            menuItem(fxString(UI, I18N_MENU_ITEM_EDIT, ELLIPSIS), SHORTCUT_E,
                event -> onEditAccount(), disableBinding),
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_MENU_ITEM_DELETE, ELLIPSIS), SHORTCUT_DELETE,
                event -> onDeleteAccount(), disableBinding),
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_MENU_ITEM_COPY_NAME), SHORTCUT_C,
                event -> onCopyName(), disableBinding),
            menuItem(fxString(UI, I18N_MENU_ITEM_DEACTIVATE),
                event -> onActivateDeactivateAccount(), disableBinding),
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_MENU_ITEM_SEARCH), SHORTCUT_F,
                event -> accountNameFilterBox.getTextField().requestFocus()),
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_WORD_TRANSACTIONS, ELLIPSIS), SHORTCUT_T,
                event -> onShowTransactions(), disableBinding),
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_MISC_RECALCULATE_BALANCE), SHORTCUT_R,
                event -> onUpdateBalance())
        );

        editMenu.setOnShowing(event -> getSelectedAccount()
            .ifPresent(account -> activateAccountMenuItem.setText(UI.getString(
                account.enabled() ? I18N_MENU_ITEM_DEACTIVATE : I18N_MENU_ITEM_ACTIVATE)
            ))
        );

        return menuBar(
            newMenu(fxString(UI, I18N_MENU_FILE),
                menuItem(fxString(UI, I18N_MENU_ITEM_REPORT, ELLIPSIS), event -> onReport()),
                new SeparatorMenuItem(),
                menuItem(fxString(UI, I18N_WORD_CLOSE), event -> onClose())),
            editMenu,
            newMenu(fxString(UI, I18N_MENU_VIEW),
                checkMenuItem(fxString(UI, I18N_MISC_SHOW_DEACTIVATED_ACCOUNTS),
                    options().getShowDeactivatedAccounts(), SHORTCUT_H,
                    event -> {
                        var selected = ((CheckMenuItem) event.getSource()).isSelected();
                        options().setShowDeactivatedAccounts(selected);
                        options().saveSettings();
                        showDeactivatedAccounts.set(selected ? a -> true : activeAccount(true));
                    }
                )
            ),
            createWindowMenu(),
            createHelpMenu());
    }

    private void setupTableColumns() {
        var w = tableView.widthProperty().subtract(20);
        tableView.getColumns().setAll(List.of(
            tableObjectColumn(fxString(UI, I18N_WORD_ENTITY_NAME), b ->
                b.withCellFactory(x -> new AccountNameCell()).withWidthBinding(w.multiply(0.15))),
            tableObjectColumn(fxString(UI, I18N_WORD_CATEGORY), b ->
                b.withCellFactory(x -> new AccountCategoryCell()).withWidthBinding(w.multiply(0.1))),
            tableColumn(fxString(UI, I18N_WORD_CURRENCY),
                b -> b.withPropertyCallback(
                    a -> cache().getCurrency(a.currencyUuid()).map(Currency::symbol).orElse("")
                ).withWidthBinding(w.multiply(0.05))),
            tableObjectColumn(fxString(UI, I18N_WORD_CARD), b ->
                b.withCellFactory(x -> new AccountCardCell()).withWidthBinding(w.multiply(0.1))),
            tableColumn("%%", (TableColumnBuilder<Account, BigDecimal> b) ->
                b.withCellFactory(x -> new AccountInterestCell())
                    .withPropertyCallback(Account::interest)
                    .withWidthBinding(w.multiply(0.03))),
            tableObjectColumn(fxString(UI, I18N_WORD_UNTIL), b ->
                b.withCellFactory(x -> new AccountClosingDateCell(options().getAccountClosingDayDelta()))
                    .withWidthBinding(w.multiply(0.05))),
            tableObjectColumn(fxString(UI, I18N_WORD_COMMENT), b ->
                b.withCellFactory(x -> new AccountCommentCell()).withWidthBinding(w.multiply(0.3))),
            tableObjectColumn(fxString(UI, I18N_WORD_BALANCE), b ->
                b.withCellFactory(x -> new AccountBalanceCell(true)).withWidthBinding(w.multiply(0.1))),
            tableObjectColumn(fxString(UI, I18N_WORD_WAITING), b ->
                b.withCellFactory(x -> new AccountBalanceCell(false)).withWidthBinding(w.multiply(0.1)))
        ));
    }

    private void createContextMenu() {
        var disableBinding = tableView.getSelectionModel().selectedItemProperty().isNull();

        var activateAccountMenuItem = menuItem(fxString(UI, I18N_MENU_ITEM_DEACTIVATE),
            event -> onActivateDeactivateAccount(),
            disableBinding);

        var contextMenu = new ContextMenu(
            menuItem(fxString(UI, I18N_MENU_ITEM_ADD, ELLIPSIS), event -> onNewAccount()),
            menuItem(fxString(UI, I18N_MENU_ITEM_EDIT, ELLIPSIS), event -> onEditAccount(), disableBinding),
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_MENU_ITEM_DELETE, ELLIPSIS), event -> onDeleteAccount(), disableBinding),
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_MENU_ITEM_COPY_NAME), event -> onCopyName(), disableBinding),
            activateAccountMenuItem,
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_MENU_ITEM_SEARCH), actionEvent -> accountNameFilterBox.getTextField().requestFocus()),
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_WORD_TRANSACTIONS, ELLIPSIS), event -> onShowTransactions(), disableBinding),
            new SeparatorMenuItem(),
            menuItem(fxString(UI, I18N_MISC_RECALCULATE_BALANCE), event -> onUpdateBalance())
        );

        contextMenu.setOnShowing(event -> getSelectedAccount()
            .ifPresent(account -> activateAccountMenuItem.setText(UI.getString(
                account.enabled() ? I18N_MENU_ITEM_DEACTIVATE : I18N_MENU_ITEM_ACTIVATE)
            ))
        );

        tableView.setContextMenu(contextMenu);
    }

    private Optional<Account> getSelectedAccount() {
        return Optional.ofNullable(tableView.getSelectionModel().getSelectedItem());
    }

    private void onNewAccount() {
        var initialCategory = getSelectedAccount()
            .flatMap(account -> cache().getCategory(account.categoryUuid()))
            .orElse(null);

        new AccountDialog(this, options().getDialogCssFileUrl(), initialCategory)
            .showAndWait()
            .ifPresent(account -> {
                getDao().insertAccount(account);
                tableView.scrollTo(account);
                tableView.getSelectionModel().select(account);
            });
    }

    private void onEditAccount() {
        getSelectedAccount().flatMap(account -> new AccountDialog(this, options().getDialogCssFileUrl(), account, null)
            .showAndWait())
            .ifPresent(account -> {
                getDao().updateAccount(account);
                tableView.scrollTo(account);
                tableView.getSelectionModel().select(account);
            });
    }

    private void onDeleteAccount() {
        getSelectedAccount().ifPresent(account -> {
            long count = cache().getTransactionCount(account);
            if (count != 0L) {
                new Alert(Alert.AlertType.ERROR,
                    "Unable to delete account\nwith " + count + " associated transactions",
                    ButtonType.CLOSE).showAndWait();
            } else {
                new Alert(Alert.AlertType.CONFIRMATION, fxString(UI, I18N_MISC_ARE_YOU_SURE), ButtonType.OK,
                    ButtonType.CANCEL)
                    .showAndWait()
                    .filter(response -> response == ButtonType.OK)
                    .ifPresent(b -> getDao().deleteAccount(account));
            }
        });
    }

    private void onActivateDeactivateAccount() {
        getSelectedAccount().ifPresent(account -> {
            boolean enabled = account.enabled();
            getDao().updateAccount(account.enable(!enabled));
        });
    }

    private void onCopyName() {
        getSelectedAccount().map(Account::name).ifPresent(name -> {
            Clipboard cb = Clipboard.getSystemClipboard();
            ClipboardContent ct = new ClipboardContent();
            ct.putString(name);
            cb.setContent(ct);
        });
    }

    private void onShowTransactions() {
        getSelectedAccount().ifPresent(BaseController::getRequestController);
    }

    private void onReport() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle(fxString(UI, I18N_WORD_REPORT));
        options().getLastExportDir().ifPresent(fileChooser::setInitialDirectory);
        fileChooser.setInitialFileName(generateFileName("accounts"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files", "*.html"));

        var selected = fileChooser.showSaveDialog(null);
        if (selected == null) {
            return;
        }

        try (var outputStream = new FileOutputStream(selected)) {
            var accounts = cache().getAccounts(filterProperty.get())
                .sorted(MoneyDAO.COMPARE_ACCOUNT_BY_CATEGORY.thenComparing(MoneyDAO.COMPARE_ACCOUNT_BY_NAME))
                .toList();
            Reports.reportAccounts(accounts, outputStream);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void onUpdateBalance() {
        tableView.getItems().forEach(account -> {
            var total = cache().calculateBalance(account, false, t -> true);
            var waiting = cache().calculateBalance(account, false, t -> !t.checked());
            getDao().updateAccount(account.updateBalance(total, waiting));
        });
    }
}
