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
package org.panteleyev.money;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.BorderPane;
import org.panteleyev.money.persistence.Account;
import org.panteleyev.money.persistence.Category;
import org.panteleyev.money.persistence.CategoryType;
import org.panteleyev.money.persistence.MoneyDAO;
import org.panteleyev.money.persistence.Transaction;
import org.panteleyev.money.persistence.TransactionFilter;
import org.panteleyev.utilities.fx.Controller;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AccountTree extends Controller implements Initializable, Styles {

    private static class BalanceCell extends TreeTableCell<AccountTreeItem, Account> {
        private Predicate<Transaction> filter;
        private final boolean total;

        BalanceCell(boolean total, Predicate<Transaction> filter) {
            this.total = total;
            this.filter = filter;
        }

        BalanceCell(Predicate<Transaction> filter) {
            this(false, filter);
        }

        @Override
        protected void updateItem(Account account, boolean empty) {
            super.updateItem(account, empty);
            this.setAlignment(Pos.CENTER_RIGHT);
            if (empty || account == null) {
                setText("");
            } else {
                List<Transaction> transactions = MoneyDAO.getInstance().getTransactions(account);

                BigDecimal sum = transactions.stream()
                        .filter(filter)
                        .map(t -> {
                            BigDecimal amount = t.getAmount();
                            if (account.getId().equals(t.getAccountCreditedId())) {
                                // handle conversion rate
                                BigDecimal rate = t.getRate();
                                if (rate.compareTo(BigDecimal.ZERO) != 0) {
                                    if (t.getRateDirection() == 0) {
                                        amount = amount.divide(rate, BigDecimal.ROUND_HALF_UP);
                                    } else {
                                        amount = amount.multiply(rate);
                                    }
                                }
                            } else {
                                amount = amount.negate();
                            }

                            return amount;
                        })
                        .reduce(total ? account.getOpeningBalance() : BigDecimal.ZERO, BigDecimal::add);

                setText(sum.setScale(2, BigDecimal.ROUND_HALF_UP).toString());

                if (sum.signum() < 0) {
                    getStyleClass().add(RED_TEXT);
                }
            }
        }
    }

    private static final String FXML = "/org/panteleyev/money/AccountTree.fxml";

    @FXML private BorderPane pane;
    @FXML private TreeTableView<AccountTreeItem> tableView;

    // Filters
    @FXML private ChoiceBox<Object> accountFilterBox;
    @FXML private ChoiceBox<Object> transactionFilterBox;

    @FXML private TreeTableColumn<AccountTreeItem, String>  nameColumn;
    @FXML private TreeTableColumn<AccountTreeItem, String>  commentColumn;
    @FXML private TreeTableColumn<AccountTreeItem, Account> approvedColumn;
    @FXML private TreeTableColumn<AccountTreeItem, Account> balanceColumn;
    @FXML private TreeTableColumn<AccountTreeItem, Account> waitingColumn;

    @FXML private CheckMenuItem                             showDeactivatedAccountsMenuItem;

    private final TreeItem<AccountTreeItem> root = new TreeItem<>();
    private TreeItem<AccountTreeItem> balanceRoot;
    private TreeItem<AccountTreeItem> expIncRoot;

    private final TreeItem<AccountTreeItem> banksSubTree;
    private final TreeItem<AccountTreeItem> debtsSubTree;
    private final TreeItem<AccountTreeItem> portfolioSubTree;
    private final TreeItem<AccountTreeItem> assetsSubTree;
    private final TreeItem<AccountTreeItem> incomeSubTree;
    private final TreeItem<AccountTreeItem> expenseSubTree;

    private final Map<CategoryType, TreeItem<AccountTreeItem>> subRoots = new EnumMap<>(CategoryType.class);

    private final SimpleBooleanProperty preloadingProperty = new SimpleBooleanProperty();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final SimpleMapProperty<Integer, Account> accountsProperty =
            new SimpleMapProperty<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final SimpleMapProperty<Integer, Transaction> transactionsProperty =
            new SimpleMapProperty<>();

    // tree update globals
    private TreeItem<AccountTreeItem> categoryTreeItem = null;

    // Listeners
    private Consumer<Account> accountSelectedConsumer = (a) -> {};
    private Consumer<Predicate<Transaction>> transactionFilterConsumer = (f) -> {};

    AccountTree() {
        super(FXML, MainWindowController.UI_BUNDLE_PATH, false);

        MoneyDAO dao = MoneyDAO.getInstance();
        preloadingProperty.bind(dao.preloadingProperty());
        accountsProperty.bind(dao.accountsProperty());
        transactionsProperty.bind(dao.transactionsProperty());

        Arrays.stream(CategoryType.values()).forEachOrdered(type ->
            subRoots.put(type, new TreeItem<>(new AccountTreeItem(type.getName(), type.getComment())))
        );

        banksSubTree = subRoots.get(CategoryType.BANKS_AND_CASH);
        debtsSubTree = subRoots.get(CategoryType.DEBTS);
        portfolioSubTree = subRoots.get(CategoryType.PORTFOLIO);
        assetsSubTree = subRoots.get(CategoryType.ASSETS);
        incomeSubTree = subRoots.get(CategoryType.INCOMES);
        expenseSubTree = subRoots.get(CategoryType.EXPENSES);
    }

    @Override
    public void initialize(URL location, ResourceBundle rb) {
        initAccountFilterBox(rb);

        balanceRoot = new TreeItem<>(new AccountTreeItem(rb.getString("account.Tree.Balance"), ""));
        expIncRoot = new TreeItem<>(new AccountTreeItem(rb.getString("account.Tree.IncomesExpenses"), "Income - Expenses"));

        nameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
        commentColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("comment"));
        approvedColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("account"));
        approvedColumn.setCellFactory(x -> new BalanceCell(true, Transaction::isChecked));
        balanceColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("account"));
        balanceColumn.setCellFactory(x -> new BalanceCell(true, t -> true));
        waitingColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("account"));
        waitingColumn.setCellFactory(x -> new BalanceCell(t -> !t.isChecked()));

        nameColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.3));
        commentColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.4));
        approvedColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.1));
        balanceColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.1));
        waitingColumn.prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(0.1));

        initTreeSkeleton();

        balanceRoot.setExpanded(true);
        banksSubTree.setExpanded(true);
        debtsSubTree.setExpanded(true);
        expIncRoot.setExpanded(true);

        showDeactivatedAccountsMenuItem.setSelected(Options.getShowDeactivatedAccounts());

        tableView.getSelectionModel().selectedItemProperty()
                .addListener((x,y,newItem) -> onTreeItemSelected(newItem));

        accountFilterBox.getSelectionModel().selectedIndexProperty()
                .addListener((x,y,newValue) -> onAccountFilterSelected((Integer)newValue));

        transactionFilterBox.getSelectionModel().selectedItemProperty()
                .addListener((x,y,newValue) -> onTransactionFilterSelected(newValue));

        accountsProperty.addListener(((x, y, z) -> {
            if (!preloadingProperty.get()) {
                Platform.runLater(this::initAccountTree);
            }
        }));

        transactionsProperty.addListener((x,y,z) -> {
            if (!preloadingProperty.get()) {
                Platform.runLater(tableView::refresh);
            }
        });

        preloadingProperty.addListener((x, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                Platform.runLater(this::initTransactionFilterBox);
                Platform.runLater(this::initAccountTree);
            }
        });
    }

    BorderPane getPane() {
        return pane;
    }

    public void clear() {
        banksSubTree.getChildren().clear();
        portfolioSubTree.getChildren().clear();
        assetsSubTree.getChildren().clear();
        debtsSubTree.getChildren().clear();
        incomeSubTree.getChildren().clear();
        expenseSubTree.getChildren().clear();
    }

    private void initAccountFilterBox(ResourceBundle rb) {
        accountFilterBox.getItems().setAll(
                rb.getString("text.All.Accounts"),
                new Separator(),
                rb.getString("account.Tree.Balance"),
                rb.getString("account.Tree.IncomesExpenses"),
                new Separator()
        );

        Arrays.stream(CategoryType.values()).forEach(type -> accountFilterBox.getItems().add(type.getName()));

        accountFilterBox.getSelectionModel().select(0);
    }

    private void initTransactionFilterBox() {
        transactionFilterBox.getItems().setAll(
                TransactionFilter.ALL,
                new Separator(),
                TransactionFilter.CURRENT_YEAR,
                TransactionFilter.CURRENT_MONTH,
                TransactionFilter.CURRENT_WEEK,
                new Separator(),
                TransactionFilter.LAST_YEAR,
                TransactionFilter.LAST_QUARTER,
                TransactionFilter.LAST_MONTH,
                new Separator()
        );

        for (int i = TransactionFilter.JANUARY.ordinal(); i <= TransactionFilter.DECEMBER.ordinal(); i++) {
            transactionFilterBox.getItems().add(TransactionFilter.values()[i]);
        }

        transactionFilterBox.getItems().add(new Separator());

        MoneyDAO.getInstance().getTransactions().stream()
                .map(Transaction::getYear)
                .distinct()
                .sorted()
                .forEach(year -> transactionFilterBox.getItems().add(year));

        transactionFilterBox.getSelectionModel().select(0);
    }

    private void initSubtree(CategoryType categoryType) {
        final TreeItem<AccountTreeItem> rootItem = subRoots.get(categoryType);
        rootItem.getChildren().clear();

        categoryTreeItem = null;

        MoneyDAO dao = MoneyDAO.getInstance();

        dao.getAccountsByType(categoryType)
                .stream()
                .filter(a -> a.isEnabled() || (!a.isEnabled() && Options.getShowDeactivatedAccounts()))
                .sorted(new Account.AccountCategoryNameComparator()).forEach(a -> {
            if (categoryTreeItem == null || !a.getCategoryId().equals(categoryTreeItem.getValue().getId())) {
                Category category = dao.getCategory(a.getCategoryId()).get();
                categoryTreeItem = new TreeItem<>(new AccountTreeItem(category));
                categoryTreeItem.setExpanded(category.isExpanded());

                categoryTreeItem.expandedProperty().addListener((x, y, newValue) ->
                    MoneyDAO.getInstance().updateCategory(category.expand(newValue))
                );

                rootItem.getChildren().add(categoryTreeItem);
            }

            categoryTreeItem.getChildren().add(new TreeItem<>(new AccountTreeItem(a)));
        });
    }

    @SuppressWarnings("unchecked")
    private void initTreeSkeleton() {
        root.getChildren().setAll(
                balanceRoot,
                expIncRoot
        );

        balanceRoot.getChildren().setAll(
                banksSubTree,
                portfolioSubTree,
                assetsSubTree,
                debtsSubTree
        );

        expIncRoot.getChildren().setAll(
                incomeSubTree,
                expenseSubTree
        );

        tableView.setRoot(root);
    }

    private void initAccountTree() {
        Arrays.stream(CategoryType.values())
                .forEachOrdered(this::initSubtree);
    }

    private void onAccountFilterSelected(Integer newValue) {
        switch (newValue) {
            case 0:
                initTreeSkeleton();
                break;
            case 2:
                tableView.setRoot(balanceRoot);
                break;
            case 3:
                tableView.setRoot(expIncRoot);
                break;
            default:
                CategoryType type = CategoryType.values()[newValue - 5];
                tableView.setRoot(subRoots.get(type));
                break;
        }
    }

    private void onTransactionFilterSelected(Object newValue) {
        if (newValue instanceof TransactionFilter) {
            transactionFilterConsumer.accept(((TransactionFilter)newValue).getPredicate());
        } else {
            if (newValue instanceof Integer) {
                transactionFilterConsumer.accept(t -> t.getYear() == (Integer)newValue);
            }
        }
    }


    @SuppressWarnings("unused")
    private void onShowDeactivatedAccounts() {
        Options.setShowDeactivatedAccounts(showDeactivatedAccountsMenuItem.isSelected());
        if (MoneyDAO.isOpen()) {
            initAccountTree();
        }
    }

    public void onNewAccount() {
        Category initialCategory = null;

        final MoneyDAO dao = MoneyDAO.getInstance();

        TreeItem<AccountTreeItem> selectedItem = tableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Account account = selectedItem.getValue().accountProperty().getValue();
            if (account != null) {
                initialCategory = dao.getCategory(account.getCategoryId()).orElse(null);
            } else {
                initialCategory = selectedItem.getValue().categoryProperty().getValue();
            }
        }

        new AccountDialog(initialCategory).load().showAndWait().ifPresent(builder ->
            dao.insertAccount(builder
                    .id(dao.generatePrimaryKey(Account.class))
                    .build())
        );
    }

    void setOnAccountSelected(Consumer<Account> consumer) {
        accountSelectedConsumer = consumer;
    }

    void setOnTransactionFilterSelected(Consumer<Predicate<Transaction>> consumer) {
        transactionFilterConsumer = consumer;
    }

    private void onTreeItemSelected(TreeItem<AccountTreeItem> item) {
        Account account = Optional.ofNullable(item)
                .map(TreeItem::getValue)
                .map(AccountTreeItem::accountProperty)
                .map(ReadOnlyObjectProperty::getValue)
                .orElse(null);

        accountSelectedConsumer.accept(account);
    }
}
