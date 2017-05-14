/*
 * Copyright (c) 2016, 2017, Petr Panteleyev <petr@panteleyev.org>
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
import javafx.collections.MapChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import org.panteleyev.money.persistence.Account;
import org.panteleyev.money.persistence.MoneyDAO;
import org.panteleyev.money.persistence.Transaction;
import org.panteleyev.money.persistence.TransactionFilter;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class AccountsTab extends BorderPane {
    private static final double DIVIDER_POSITION = 0.85;

    private final TransactionTableView transactionTable = new TransactionTableView(true);

    private Account selectedAccount = null;

    private Predicate<Transaction> transactionFilter = TransactionFilter.ALL.getPredicate();

    private final MapChangeListener<Integer,Transaction> transactionListener =
            l -> Platform.runLater(this::reloadTransactions);

    AccountsTab() {
        AccountTree accountTree = new AccountTree();

        SplitPane split = new SplitPane(accountTree, new BorderPane(transactionTable));
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPosition(0, DIVIDER_POSITION);
        setCenter(split);

        accountTree.setOnAccountSelected(this::onAccountSelected);
        accountTree.setOnTransactionFilterSelected(this::onTransactionFilterSelected);
        transactionTable.setOnCheckTransaction(this::onCheckTransaction);

        final MoneyDAO dao = MoneyDAO.getInstance();
        dao.transactions().addListener(transactionListener);
        dao.preloadingProperty().addListener((x, y, newValue) -> {
            if (!newValue) {
                Platform.runLater(this::reloadTransactions);
            }
        });
    }

    private void onTransactionFilterSelected(Predicate<Transaction> filter) {
        reloadTransactions(filter);
    }

    private void onAccountSelected(Account account) {
        selectedAccount = account;
        reloadTransactions();
    }

    private void onCheckTransaction(List<Transaction> transactions, Boolean check) {
        MoneyDAO dao = MoneyDAO.getInstance();

        transactions.forEach(t -> dao.updateTransaction(new Transaction.Builder(t)
                .checked(check)
                .build()));

        reloadTransactions();
    }

    private void reloadTransactions() {
        reloadTransactions(transactionFilter);
    }

    private void reloadTransactions(Predicate<Transaction> filter) {
        this.transactionFilter = filter;

        int index = transactionTable.getSelectionModel().getSelectedIndex();
        transactionTable.getSelectionModel().clearSelection();
        transactionTable.clear();

        if (selectedAccount != null) {
            List<Transaction> transactions = MoneyDAO.getInstance()
                    .getTransactions(selectedAccount)
                    .stream()
                    .filter(filter)
                    .collect(Collectors.toList());

            transactionTable.addRecords(transactions);
            transactionTable.sort();
        }

        transactionTable.getSelectionModel().select(index);
    }
}