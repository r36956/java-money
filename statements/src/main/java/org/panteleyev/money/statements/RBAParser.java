package org.panteleyev.money.statements;

/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */

import org.panteleyev.ofx.BankTransactionList;
import org.panteleyev.ofx.OFXParser;
import org.panteleyev.ofx.StatementTransaction;
import java.io.InputStream;
import java.util.ArrayList;

class RBAParser {
    static Statement parseOfx(InputStream inStream) {
        var records = new ArrayList<StatementRecord>();

        var parser = new OFXParser();
        var ofxStatement = parser.parse(inStream);

        var transactionList = new BankTransactionList();
        var accountNumber = "";

        if (!ofxStatement.getAccountStatements().isEmpty()) {
            var statement = ofxStatement.getAccountStatements().get(0);
            transactionList = statement.getBankTransactionList();
            accountNumber = statement.getAccountInfo().getAccountNumber();
        } else if (!ofxStatement.getCreditCardStatements().isEmpty()) {
            var statement = ofxStatement.getCreditCardStatements().get(0);
            transactionList = ofxStatement.getCreditCardStatements().get(0).getBankTransactionList();
            accountNumber = statement.getAccountInfo().getAccountNumber();
        }

        for (StatementTransaction tr : transactionList.getTransactions()) {
            var builder = new StatementRecord.Builder()
                .amount(tr.getAmount().toString())
                .counterParty(tr.getName())
                .description(tr.getMemo())
                .actual(tr.getDatePosted().toLocalDate())
                .execution(tr.getDateAvailable().toLocalDate());
            records.add(builder.build());
        }

        return new Statement(Statement.StatementType.RAIFFEISEN_OFX, accountNumber, records);
    }
}
