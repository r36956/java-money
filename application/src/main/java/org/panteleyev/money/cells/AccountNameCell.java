package org.panteleyev.money.cells;

/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */

import javafx.scene.control.TableCell;
import org.panteleyev.money.icons.IconManager;
import org.panteleyev.money.model.Account;

public class AccountNameCell extends TableCell<Account, Account> {
    @Override
    protected void updateItem(Account account, boolean empty) {
        super.updateItem(account, empty);

        if (empty || account == null) {
            setText("");
            setGraphic(null);
        } else {
            setText(account.getName());
            setGraphic(IconManager.getImageView(account.getIconUuid()));
        }
    }
}
