/*
 * Copyright (c) 2014, 2017, Petr Panteleyev <petr@panteleyev.org>
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.Validator;
import org.panteleyev.money.persistence.MoneyDAO;
import org.panteleyev.utilities.fx.Controller;
import org.panteleyev.utilities.fx.WindowManager;
import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class MainWindowController extends BaseController {
    public static final String UI_BUNDLE_PATH = "org.panteleyev.money.ui";
    static final String DIALOGS_CSS = "/org/panteleyev/money/dialogs.css";
    private static final String CSS_PATH = "/org/panteleyev/money/main.css";

    private final ResourceBundle rb = ResourceBundle.getBundle(UI_BUNDLE_PATH);

    private final BorderPane    self = new BorderPane();
    private final TabPane       tabPane = new TabPane();

    private final Label         progressLabel = new Label();
    private final ProgressBar   progressBar = new ProgressBar();

    private final Menu          windowMenu = new Menu(rb.getString("menu.Window"));

    private static final Collection<Class<? extends Controller>> WINDOW_CLASSES =
            Arrays.asList(
                    ContactListWindowController.class,
                    AccountListWindowController.class,
                    CategoryWindowController.class,
                    CurrencyWindowController.class
            );

    private final AccountsTab accountsTab = new AccountsTab();
    private final TransactionsTab transactionTab = new TransactionsTab();
    private final RequestTab requestTab = new RequestTab();

    private final BooleanProperty dbOpenProperty = new SimpleBooleanProperty(false);

    static final Validator<String> BIG_DECIMAL_VALIDATOR = (Control control, String value) -> {
        boolean invalid = false;
        try {
            new BigDecimal(value);
        } catch (NumberFormatException ex) {
            invalid = true;
        }
        return ValidationResult.fromErrorIf(control, null, invalid && !control.isDisabled());
    };

    public MainWindowController(Stage stage) {
        super(stage, CSS_PATH);
        initialize();
        setupWindow(self);
    }

    public BooleanProperty dbOpenProperty() {
        return dbOpenProperty;
    }

    @Override
    public String getTitle() {
        return "Money Manager";
    }

    private void initialize() {
        // Main menu
        MenuItem m1 = new MenuItem(rb.getString("menu.File.New"));
        m1.setOnAction((ae) -> onNew());
        MenuItem m2 = new MenuItem(rb.getString("menu.File.Open"));
        m2.setOnAction((ae) -> onOpen());
        MenuItem m3 = new MenuItem(rb.getString("menu.File.Close"));
        m3.setOnAction((ae) -> onClose());
        MenuItem m4 = new MenuItem(rb.getString("menu.File.Exit"));
        m4.setOnAction((ae) -> onExit());

        Menu fileMenu = new Menu(rb.getString("menu.File"), null,
                m1, m2, new SeparatorMenuItem(), m3, new SeparatorMenuItem(), m4);

        MenuItem m5 = new MenuItem(rb.getString("menu.Edit.Delete"));

        MenuItem currenciesMenuItem = new MenuItem(rb.getString("menu.Edit.Currencies"));
        currenciesMenuItem.setOnAction((ae) -> onManageCurrencies());
        MenuItem categoriesMenuItem = new MenuItem(rb.getString("menu.Edit.Categories"));
        categoriesMenuItem.setOnAction((ae) -> onManageCategories());
        MenuItem accountsMenuItem = new MenuItem(rb.getString("menu.Edit.Accounts"));
        accountsMenuItem.setOnAction((ae) -> onManageAccounts());
        MenuItem contactsMenuItem = new MenuItem(rb.getString("menu.Edit.Contacts"));
        contactsMenuItem.setOnAction((ae) -> onManageContacts());

        Menu editMenu = new Menu(rb.getString("menu.Edit"), null,
                m5, new SeparatorMenuItem(),
                currenciesMenuItem, categoriesMenuItem, accountsMenuItem, contactsMenuItem);

        MenuItem m6 = new MenuItem(rb.getString("menu.Tools.Export"));
        m6.setOnAction((ae) -> onExport());
        MenuItem m7 = new MenuItem(rb.getString("menu.Tools.Options"));
        m7.setOnAction((ae) -> onOptions());

        Menu toolsMenu = new Menu(rb.getString("menu.Tools"), null,
                m6, m7);

        /* Dummy menu item is required in order to let onShowing() fire up first time */
        windowMenu.getItems().setAll(new MenuItem("dummy"));

        MenuBar menuBar = new MenuBar(fileMenu, editMenu, toolsMenu,
                windowMenu, createHelpMenu(rb));

        menuBar.setUseSystemMenuBar(true);

        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        self.setTop(menuBar);
        self.setCenter(tabPane);
        self.setBottom(new HBox(progressLabel, progressBar));

        HBox.setMargin(progressLabel, new Insets(0, 0, 0, 5));
        HBox.setMargin(progressBar, new Insets(0, 0, 0, 5));

        progressLabel.setVisible(false);
        progressBar.setVisible(false);

        currenciesMenuItem.disableProperty().bind(dbOpenProperty.not());
        categoriesMenuItem.disableProperty().bind(dbOpenProperty.not());
        accountsMenuItem.disableProperty().bind(dbOpenProperty.not());
        contactsMenuItem.disableProperty().bind(dbOpenProperty.not());

        Tab t2 = new Tab(rb.getString("tab.Transactions"), transactionTab);
        t2.disableProperty().bind(dbOpenProperty.not());
        t2.selectedProperty().addListener((x,y,newValue) -> {
            if (newValue) {
                Platform.runLater(() -> transactionTab.getTransactionEditor().clear());
                Platform.runLater(transactionTab::scrollToEnd);
            }
        });

        Tab t3 = new Tab(rb.getString("tab.Requests"), requestTab);
        t3.disableProperty().bind(dbOpenProperty.not());

        tabPane.getTabs().addAll(
            new Tab(rb.getString("tab.Accouts"), accountsTab),
            t2, t3
        );

        windowMenu.setOnShowing(e -> {
            windowMenu.getItems().clear();

            windowMenu.getItems().add(new MenuItem("Money Manager"));
            windowMenu.getItems().add(new SeparatorMenuItem());

            WindowManager.getFrames().forEach(c -> {
                MenuItem item = new MenuItem(c.getTitle());
                item.setOnAction(a -> c.getStage().toFront());
                windowMenu.getItems().add(item);
            });
        });

        getStage().setOnHiding(e -> onWindowClosing());

        getStage().setWidth(Options.getMainWindowWidth());
        getStage().setHeight(Options.getMainWindowHeight());

        File file;

        Application.Parameters params = MoneyApplication.getApplication().getParameters();
        String fileName = params.getNamed().get("file");
        if (fileName != null && !fileName.isEmpty()) {
            file = new File(fileName);
            if (file.exists()) {
                open(file, false);
            } else {
                newFile(file, false);
            }
        } else {
            file = Options.getDbFile();
            if (file != null) {
                if (file.exists()) {
                    open(file, false);
                } else {
                    Options.setDbFile(null);
                }
            }
        }
    }

    private void onManageCategories() {
        Controller controller = WindowManager.find(CategoryWindowController.class)
                .orElseGet(CategoryWindowController::new);

        Stage stage = controller.getStage();
        stage.show();
        stage.toFront();
    }

    private void onExit() {
        getStage().fireEvent(new WindowEvent(getStage(), WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void onManageCurrencies() {
        Controller controller = WindowManager.find(CurrencyWindowController.class)
                .orElseGet(CurrencyWindowController::new);

        Stage stage = controller.getStage();
        stage.show();
        stage.toFront();
    }

    private void onManageAccounts() {
        Controller controller = WindowManager.find(AccountListWindowController.class)
                .orElseGet(AccountListWindowController::new);

        Stage stage = controller.getStage();
        stage.show();
        stage.toFront();
    }

    private void onManageContacts() {
        Controller controller = WindowManager.find(ContactListWindowController.class)
                .orElseGet(ContactListWindowController::new);

        Stage stage = controller.getStage();
        stage.show();
        stage.toFront();
    }

    private void onNew() {
        FileChooser d = new FileChooser();
        d.setTitle("New Database File");
        d.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("SQLite Database", "*.db")
        );
        File file = d.showSaveDialog(null);
        newFile(file, true);
    }

    private void newFile(File file, boolean overwriteOption) {
        if (overwriteOption) {
            Options.setDbFile(file);
        }

        if (file != null) {
            DataSource ds = new MoneyDAO.Builder()
                .file(file.getAbsolutePath())
                .build();

            CompletableFuture.runAsync(() -> {
                MoneyDAO dao = MoneyDAO.initialize(ds);
                dao.createTables();
                dao.preload();
            }).thenRun(() -> Platform.runLater(() -> {
                setTitle(file);
                dbOpenProperty.set(true);
            }));
        }
    }

    public void onClose() {
        WINDOW_CLASSES.forEach(clazz ->
                WindowManager.find(clazz).ifPresent(c -> ((BaseController)c).onClose()));

        tabPane.getSelectionModel().select(0);

        MoneyDAO.initialize(null);
        Options.setDbFile(null);
        dbOpenProperty.set(false);
    }

    private void onOpen() {
        FileChooser d = new FileChooser();
        d.setTitle("Database File");
        d.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("SQLite Database", "*.db")
        );
        File file = d.showOpenDialog(null);
        open(file, true);
    }

    private void open(File file, boolean overwriteOption) {
        if (overwriteOption) {
            Options.setDbFile(file);
        }

        if (file != null) {
            DataSource ds = new MoneyDAO.Builder()
                    .file(file.getAbsolutePath())
                    .build();
            MoneyDAO.initialize(ds);

            CompletableFuture
                    .runAsync(() -> MoneyDAO.getInstance().preload())
                    .thenRun(() -> Platform.runLater(() -> {
                        setTitle(file);
                        dbOpenProperty.set(true);
                    }));
        }
    }

    private void onExport() {

    }

    private void onOptions() {
        OptionsDialog d = new OptionsDialog();
        d.showAndWait();
    }

    private void setTitle(File file) {
        getStage().setTitle(getTitle() + " - " + file.getAbsolutePath());
    }

    private void onWindowClosing() {
        WINDOW_CLASSES.forEach(clazz ->
                WindowManager.find(clazz).ifPresent(c -> ((BaseController)c).onClose()));

        Options.setMainWindowWidth(getStage().widthProperty().doubleValue());
        Options.setMainWindowHeight(getStage().heightProperty().doubleValue());
    }

    protected Parent getSelf() {
        return self;
    }
}
