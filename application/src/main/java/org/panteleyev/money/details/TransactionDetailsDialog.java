/*
 * Copyright (c) 2019, 2020, Petr Panteleyev <petr@panteleyev.org>
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

package org.panteleyev.money.details;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.panteleyev.fx.BaseDialog;
import org.panteleyev.money.RecordEditorCallback;
import org.panteleyev.money.Styles;
import org.panteleyev.money.cells.TransactionDetailSumCell;
import org.panteleyev.money.model.Account;
import org.panteleyev.money.model.Transaction;
import org.panteleyev.money.model.TransactionDetail;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import static org.panteleyev.fx.LabelFactory.newLabel;
import static org.panteleyev.fx.TableFactory.newTableColumn;
import static org.panteleyev.money.Constants.COLON;
import static org.panteleyev.money.MainWindowController.RB;
import static org.panteleyev.money.persistence.DataCache.cache;

public final class TransactionDetailsDialog extends BaseDialog<List<TransactionDetail>> implements RecordEditorCallback<TransactionDetail> {
    private final ObservableList<TransactionDetail> details = FXCollections.observableArrayList();
    private final DetailEditorPane detailEditor;
    private final Label deltaLabel = new Label();

    private final BigDecimal totalAmount;
    private final TableView<TransactionDetail> detailsTable = new TableView<>();

    public TransactionDetailsDialog(List<Transaction> transactions, BigDecimal totalAmount, boolean readOnly) {
        setTitle(RB.getString("DetailsDialog_title"));

        this.totalAmount = totalAmount;
        detailEditor = new DetailEditorPane(this, cache());

        details.addAll(transactions.stream()
            .map(TransactionDetail::new)
            .collect(Collectors.toList()));

        calculateDelta();

        detailsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        var w = detailsTable.widthProperty().subtract(20);
        detailsTable.getColumns().setAll(List.of(
            newTableColumn(RB, "Credited_Account", null,
                d -> cache().getAccount(d.getAccountCreditedUuid()).map(Account::getName)
                    .orElse(""), w.multiply(0.3)),
            newTableColumn(RB, "Comment", null, TransactionDetail::getComment, w.multiply(0.6)),
            newTableColumn(RB, "Sum", x -> new TransactionDetailSumCell(), w.multiply(0.1))
        ));

        detailsTable.setItems(details);

        var hBox = new HBox(Styles.BIG_SPACING, newLabel(RB, "Delta", COLON), deltaLabel);
        var vBox = new VBox(Styles.BIG_SPACING, hBox, detailEditor);
        VBox.setMargin(hBox, new Insets(Styles.BIG_SPACING, 0, 0, 0));

        var content = new BorderPane();
        content.setCenter(detailsTable);
        if (!readOnly) {
            content.setBottom(vBox);
        }
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        detailsTable.getSelectionModel().selectedItemProperty().addListener((x, oldValue, newValue) -> {
            if (oldValue != newValue) {
                detailEditor.setTransactionDetail(newValue);
            }
        });

        setResultConverter((ButtonType b) -> {
            if (b == ButtonType.OK) {
                return details;
            } else {
                return null;
            }
        });

        setResizable(true);
        getDialogPane().setPrefWidth(800);
        getDialogPane().setPrefHeight(400);
    }

    private void calculateDelta() {
        deltaLabel.setText(
            details.stream()
                .map(TransactionDetail::getAmount)
                .reduce(totalAmount, BigDecimal::subtract).toString()
        );
    }

    @Override
    public void addRecord(TransactionDetail detail) {
        details.add(detail);
        calculateDelta();
    }

    @Override
    public void updateRecord(TransactionDetail detail) {
        for (int index = 0; index < details.size(); index++) {
            if (Objects.equals(details.get(index).getUuid(), detail.getUuid())) {
                details.set(index, detail);
                calculateDelta();
                return;
            }
        }
    }

    @Override
    public void deleteRecord(TransactionDetail detail) {
        for (var index = 0; index < details.size(); index++) {
            if (Objects.equals(details.get(index).getUuid(), detail.getUuid())) {
                details.remove(index);
                detailsTable.getSelectionModel().clearSelection();
                calculateDelta();
                return;
            }
        }
    }
}
