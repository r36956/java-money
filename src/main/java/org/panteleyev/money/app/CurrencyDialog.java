/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.panteleyev.fx.BaseDialog;
import org.panteleyev.fx.Controller;
import org.panteleyev.money.model.Currency;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import static javafx.geometry.Pos.CENTER_LEFT;
import static org.panteleyev.fx.BoxFactory.newHBox;
import static org.panteleyev.fx.FxFactory.newCheckBox;
import static org.panteleyev.fx.GridFactory.EMPTY_NODE;
import static org.panteleyev.fx.GridFactory.addRows;
import static org.panteleyev.fx.GridFactory.newGridPane;
import static org.panteleyev.fx.LabelFactory.newLabel;
import static org.panteleyev.money.app.Constants.COLON;
import static org.panteleyev.money.app.MainWindowController.RB;
import static org.panteleyev.money.persistence.DataCache.cache;

final class CurrencyDialog extends BaseDialog<Currency> {
    private final ValidationSupport validation = new ValidationSupport();

    private final TextField nameEdit = new TextField();
    private final TextField descrEdit = new TextField();
    private final TextField rateEdit = new TextField();
    private final ChoiceBox<String> rateDirectionChoice = new ChoiceBox<>();
    private final CheckBox defaultCheck = newCheckBox(RB, "currency.Dialog.Default");
    private final CheckBox showSymbolCheck = new CheckBox();
    private final ComboBox<String> formatSymbolCombo = new ComboBox<>();
    private final ChoiceBox<String> formatSymbolPositionChoice = new ChoiceBox<>();
    private final CheckBox thousandSeparatorCheck = newCheckBox(RB, "currency.Dialog.ShowSeparator");

    CurrencyDialog(Controller owner, Currency currency) {
        super(owner, MainWindowController.CSS_PATH);

        setTitle(RB.getString("Currency"));

        var gridPane = newGridPane(Styles.GRID_PANE);
        addRows(gridPane,
            List.of(newLabel(RB, "label.Symbol"), nameEdit),
            List.of(newLabel(RB, "Description", COLON), descrEdit),
            List.of(newLabel(RB, "Rate", COLON), rateEdit, rateDirectionChoice),
            List.of(EMPTY_NODE, newHBox(CENTER_LEFT, showSymbolCheck, formatSymbolCombo, formatSymbolPositionChoice)),
            List.of(EMPTY_NODE, thousandSeparatorCheck),
            List.of(EMPTY_NODE, defaultCheck)
        );

        HBox.setMargin(formatSymbolPositionChoice, new Insets(0.0, 0.0, 0.0, 5.0));

        getDialogPane().setContent(gridPane);

        nameEdit.setPrefColumnCount(20);
        formatSymbolCombo.setEditable(true);

        rateDirectionChoice.getItems().setAll("/", "*");

        formatSymbolPositionChoice.getItems().setAll(
            RB.getString("currency.Dialog.Before"),
            RB.getString("currency.Dialog.After"));

        formatSymbolCombo.getItems().setAll(cache().getCurrencies().stream()
            .map(Currency::formatSymbol)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet()));

        if (currency == null) {
            rateDirectionChoice.getSelectionModel().select(0);
            formatSymbolPositionChoice.getSelectionModel().select(0);
            rateEdit.setText("1");
        } else {
            nameEdit.setText(currency.symbol());
            descrEdit.setText(currency.description());
            rateEdit.setText(currency.rate().toString());
            defaultCheck.setSelected(currency.def());
            rateDirectionChoice.getSelectionModel().select(currency.direction());
            showSymbolCheck.setSelected(currency.showFormatSymbol());
            formatSymbolCombo.getSelectionModel().select(currency.formatSymbol());
            formatSymbolPositionChoice.getSelectionModel().select(currency.formatSymbolPosition());
            thousandSeparatorCheck.setSelected(currency.useThousandSeparator());
        }

        setResultConverter((ButtonType b) -> {
            if (b == ButtonType.OK) {
                long now = System.currentTimeMillis();

                var builder = new Currency.Builder(currency)
                    .symbol(nameEdit.getText())
                    .description(descrEdit.getText())
                    .formatSymbol(formatSymbolCombo.getSelectionModel().getSelectedItem())
                    .formatSymbolPosition(formatSymbolPositionChoice.getSelectionModel().getSelectedIndex())
                    .showFormatSymbol(showSymbolCheck.isSelected())
                    .def(defaultCheck.isSelected())
                    .rate(new BigDecimal(rateEdit.getText()))
                    .direction(rateDirectionChoice.getSelectionModel().getSelectedIndex())
                    .useThousandSeparator(thousandSeparatorCheck.isSelected())
                    .modified(now);

                if (currency == null) {
                    builder.guid(UUID.randomUUID())
                        .created(now);
                }

                return builder.build();
            } else {
                return null;
            }
        });

        createDefaultButtons(RB, validation.invalidProperty());

        Platform.runLater(this::createValidationSupport);
    }

    private void createValidationSupport() {
        validation.registerValidator(nameEdit, (Control control, String value) ->
            ValidationResult.fromErrorIf(control, null, value.isEmpty()));

        validation.registerValidator(rateEdit, MainWindowController.BIG_DECIMAL_VALIDATOR);
        validation.initInitialDecoration();
    }

    TextField getNameEdit() {
        return nameEdit;
    }

    TextField getDescrEdit() {
        return descrEdit;
    }

    TextField getRateEdit() {
        return rateEdit;
    }

    CheckBox getDefaultCheck() {
        return defaultCheck;
    }

    CheckBox getThousandSeparatorCheck() {
        return thousandSeparatorCheck;
    }
}