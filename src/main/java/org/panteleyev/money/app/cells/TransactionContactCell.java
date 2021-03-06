/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.app.cells;

import javafx.scene.control.TableCell;
import org.panteleyev.money.app.icons.IconManager;
import org.panteleyev.money.model.Transaction;
import static org.panteleyev.money.persistence.DataCache.cache;

public class TransactionContactCell extends TableCell<Transaction, Transaction> {

    @Override
    public void updateItem(Transaction transaction, boolean empty) {
        super.updateItem(transaction, empty);

        setText("");
        setGraphic(null);

        if (empty || transaction == null) {
            return;
        }

        cache().getContact(transaction.contactUuid()).ifPresent(contact -> {
            setText(contact.name());
            setGraphic(IconManager.getImageView(contact.iconUuid()));
        });
    }
}
