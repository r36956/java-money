/*
 * Copyright (c) 2017, 2019, Petr Panteleyev <petr@panteleyev.org>
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

import com.mysql.cj.jdbc.MysqlDataSource;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.Validator;
import org.panteleyev.commons.database.ConnectDialog;
import org.panteleyev.commons.database.ConnectionProfile;
import org.panteleyev.commons.database.ConnectionProfileManager;
import org.panteleyev.commons.fx.Controller;
import org.panteleyev.commons.fx.WindowManager;
import org.panteleyev.commons.ssh.SshManager;
import org.panteleyev.money.charts.ChartsTab;
import org.panteleyev.money.icons.IconWindowController;
import org.panteleyev.money.persistence.MoneyDAO;
import org.panteleyev.money.persistence.model.Account;
import org.panteleyev.money.persistence.model.Transaction;
import org.panteleyev.money.statements.StatementTab;
import org.panteleyev.money.xml.Export;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import static org.panteleyev.money.FXFactory.newMenuItem;
import static org.panteleyev.money.MoneyApplication.generateFileName;
import static org.panteleyev.money.persistence.MoneyDAO.getDao;

public class MainWindowController extends BaseController {
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(MainWindowController.class);

    private static final String UI_BUNDLE_PATH = "org.panteleyev.money.res.ui";
    public static final URL CSS_PATH = MainWindowController.class.getResource("/org/panteleyev/money/res/main.css");

    public static final ResourceBundle RB = ResourceBundle.getBundle(UI_BUNDLE_PATH);

    private final BorderPane self = new BorderPane();
    private final TabPane tabPane = new TabPane();

    private final Label progressLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar();

    private final Menu windowMenu = new Menu(RB.getString("menu.Window"));

    private final AccountsTab accountsTab = new AccountsTab();
    private final TransactionsTab transactionTab = new TransactionsTab();
    private final RequestTab requestTab = new RequestTab();
    private final StatementTab statementsTab = new StatementTab();
    private final ChartsTab chartsTab = new ChartsTab();

    private final Tab tabAccounts = new Tab(RB.getString("tab.Accouts"), accountsTab);
    private final Tab tabTransactions = new Tab(RB.getString("tab.Transactions"), transactionTab);
    private final Tab tabRequests = new Tab(RB.getString("tab.Requests"), requestTab);
    private final Tab tabStatements = new Tab(RB.getString("Statement"), statementsTab);
    private final Tab tabCharts = new Tab(RB.getString("tab.Charts"), chartsTab);

    private final SimpleBooleanProperty dbOpenProperty = new SimpleBooleanProperty(false);

    private final SshManager sshManager = new SshManager(PREFERENCES);
    private final ConnectionProfileManager profileManager =
        new ConnectionProfileManager(this::onInitDatabase, this::onBuildDatasource,
            PREFERENCES, sshManager);

    static final Validator<String> BIG_DECIMAL_VALIDATOR = (Control control, String value) -> {
        boolean invalid = false;
        try {
            new BigDecimal(value);
        } catch (NumberFormatException ex) {
            invalid = true;
        }

        return ValidationResult.fromErrorIf(control, null, invalid && !control.isDisabled());
    };

    private static final List<Class<? extends Controller>> WINDOW_CLASSES = List.of(
        ContactListWindowController.class,
        CategoryWindowController.class,
        CurrencyWindowController.class
    );

    public MainWindowController(Stage stage) {
        super(stage, CSS_PATH.toString());

        sshManager.loadSessions();
        profileManager.loadProfiles();

        stage.setOnCloseRequest(event -> sshManager.closeAllSessions());

        stage.getIcons().add(Images.APP_ICON);
        initialize();
        setupWindow(self);
    }

    @Override
    public String getTitle() {
        return "Money Manager";
    }

    private MenuBar createMainMenu() {
        // Main menu
        var fileConnectMenuItem = newMenuItem(RB, "menu.File.Connect",
            new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), event -> onOpenConnection());
        var fileCloseMenuItem = newMenuItem(RB, "menu.File.Close", event -> onClose());
        var fileExitMenuItem = newMenuItem(RB, "menu.File.Exit", event -> onExit());
        var exportMenuItem = newMenuItem(RB, "menu.Tools.Export", event -> xmlDump());
        var importMenuItem = new MenuItem(RB.getString("word.Import") + "...");
        importMenuItem.setOnAction(event -> onImport());
        var reportMenuItem = newMenuItem(RB, "menu.File.Report", event -> onReport());

        var fileMenu = new Menu(RB.getString("menu.File"), null,
            fileConnectMenuItem,
            new SeparatorMenuItem(),
            importMenuItem,
            exportMenuItem,
            new SeparatorMenuItem(),
            reportMenuItem,
            new SeparatorMenuItem(),
            fileCloseMenuItem,
            new SeparatorMenuItem(),
            fileExitMenuItem);

        var editDeleteMenuItem = new MenuItem(RB.getString("menu.Edit.Delete"));

        var currenciesMenuItem = newMenuItem(RB, "menu.Edit.Currencies",
            new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN), event -> onManageCurrencies());
        var categoriesMenuItem = newMenuItem(RB, "menu.Edit.Categories",
            new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN), event -> onManageCategories());
        var contactsMenuItem = newMenuItem(RB, "menu.Edit.Contacts",
            new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN), event -> onManageContacts());

        var editMenu = new Menu(RB.getString("menu.Edit"), null,
            editDeleteMenuItem,
            new SeparatorMenuItem(),
            currenciesMenuItem,
            categoriesMenuItem,
            contactsMenuItem);

        var sshMenuItem = new MenuItem("SSH...");
        sshMenuItem.setOnAction(a -> sshManager.getEditor().showAndWait());
        var profilesMenuItem = new MenuItem(RB.getString("menu.Tools.Profiles"));
        profilesMenuItem.setOnAction(a -> profileManager.getEditor(false).showAndWait());

        var optionsMenuItem = newMenuItem(RB, "menu.Tools.Options", event -> onOptions());
        var importSettingsMenuItem = newMenuItem(RB, "menu.tools.import.settings", event -> onImportSettings());
        var exportSettingsMenuItem = newMenuItem(RB, "menu.tool.export.settings", event -> onExportSettings());
        var iconWindowMenuItem = new MenuItem(RB.getString("string.icons") + "...");
        iconWindowMenuItem.setOnAction(a -> onIconWindow());

        var toolsMenu = new Menu(RB.getString("menu.Tools"), null,
            sshMenuItem,
            profilesMenuItem,
            new SeparatorMenuItem(),
            iconWindowMenuItem,
            new SeparatorMenuItem(),
            optionsMenuItem,
            importSettingsMenuItem,
            exportSettingsMenuItem
        );

        /* Dummy menu item is required in order to let onShowing() fire up first time */
        windowMenu.getItems().setAll(new MenuItem("dummy"));

        var menuBar = new MenuBar(fileMenu, editMenu, toolsMenu,
            windowMenu, createHelpMenu(RB));

        menuBar.setUseSystemMenuBar(true);

        currenciesMenuItem.disableProperty().bind(dbOpenProperty.not());
        categoriesMenuItem.disableProperty().bind(dbOpenProperty.not());
        contactsMenuItem.disableProperty().bind(dbOpenProperty.not());
        iconWindowMenuItem.disableProperty().bind(dbOpenProperty.not());

        exportMenuItem.disableProperty().bind(dbOpenProperty.not());
        importMenuItem.disableProperty().bind(dbOpenProperty.not());
        reportMenuItem.disableProperty().bind(dbOpenProperty.not());

        return menuBar;
    }

    private void initialize() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        self.setTop(createMainMenu());
        self.setCenter(tabPane);
        self.setBottom(new HBox(progressLabel, progressBar));

        HBox.setMargin(progressLabel, new Insets(0.0, 0.0, 0.0, 5.0));
        HBox.setMargin(progressBar, new Insets(0.0, 0.0, 0.0, 5.0));

        progressLabel.setVisible(false);
        progressBar.setVisible(false);

        tabTransactions.selectedProperty().addListener((x, y, newValue) -> {
            if (newValue) {
                Platform.runLater(transactionTab::scrollToEnd);
            }
        });

        tabPane.getTabs().addAll(
            tabTransactions,
            tabAccounts,
            tabRequests,
            tabStatements,
            tabCharts
        );

        accountsTab.setAccountTransactionsConsumer(account -> {
            requestTab.showTransactionsForAccount(account);
            tabPane.getSelectionModel().select(tabRequests);
        });

        statementsTab.setNewTransactionCallback((record, account) -> {
            tabPane.getSelectionModel().select(tabTransactions);
            transactionTab.handleStatementRecord(record, account);
        });

        windowMenu.setOnShowing(event -> {
            windowMenu.getItems().clear();

            windowMenu.getItems().add(new MenuItem("Money Manager"));
            windowMenu.getItems().add(new SeparatorMenuItem());

            WindowManager.getFrames().forEach(frame -> {
                if (frame != this) {
                    var item = new MenuItem(frame.getTitle());
                    item.setOnAction(action -> frame.getStage().toFront());
                    windowMenu.getItems().add(item);
                }
            });
        });

        getStage().setOnHiding(event -> onWindowClosing());

        getStage().setWidth(Options.getMainWindowWidth());
        getStage().setHeight(Options.getMainWindowHeight());

        profileManager.getProfileToOpen(MoneyApplication.application).ifPresent(this::open);
    }

    private void onManageCategories() {
        var controller = WindowManager.find(CategoryWindowController.class)
            .orElseGet(CategoryWindowController::new);

        var stage = controller.getStage();
        stage.show();
        stage.toFront();
    }

    private void onIconWindow() {
        var controller = WindowManager.find(IconWindowController.class)
            .orElseGet(IconWindowController::new);

        var stage = controller.getStage();
        stage.show();
        stage.toFront();
    }

    private void onExit() {
        getStage().fireEvent(new WindowEvent(getStage(), WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void onManageCurrencies() {
        var controller = WindowManager.find(CurrencyWindowController.class)
            .orElseGet(CurrencyWindowController::new);

        var stage = controller.getStage();
        stage.show();
        stage.toFront();
    }

    private void onManageContacts() {
        var controller = WindowManager.find(ContactListWindowController.class)
            .orElseGet(ContactListWindowController::new);

        var stage = controller.getStage();
        stage.show();
        stage.toFront();
    }

    @Override
    public void onClose() {
        for (var clazz : WINDOW_CLASSES) {
            WindowManager.find(clazz).ifPresent(c -> ((BaseController) c).onClose());
        }

        tabPane.getSelectionModel().select(0);

        setTitle(AboutDialog.APP_TITLE);
        getDao().initialize(null);
        dbOpenProperty.set(false);
    }

    private void onOpenConnection() {
        new ConnectDialog(profileManager).showAndWait()
            .ifPresent(this::open);
    }

    private void open(ConnectionProfile profile) {
        sshManager.setupTunnel(profile.getSshSession());
        var ds = onBuildDatasource(profile);

        getDao().initialize(ds);

        var loadResult = CompletableFuture
            .runAsync(() -> getDao().preload())
            .thenRun(() -> Platform.runLater(() -> {
                setTitle(AboutDialog.APP_TITLE + " - " + profile.getName() + " - " + profile.getConnectionString());
                dbOpenProperty.set(true);
            }));

        checkFutureException(loadResult);
    }

    private void checkFutureException(Future<?> f) {
        try {
            f.get();
        } catch (ExecutionException | InterruptedException ex) {
            MoneyApplication.uncaughtException(ex.getCause());
        }
    }

    private void onOptions() {
        new OptionsDialog().showAndWait();
    }

    private void setTitle(String title) {
        getStage().setTitle(title);
    }

    private void onWindowClosing() {
        WINDOW_CLASSES.forEach(clazz -> WindowManager.find(clazz).ifPresent(c -> ((BaseController) c).onClose()));

        Options.setMainWindowWidth(getStage().widthProperty().doubleValue());
        Options.setMainWindowHeight(getStage().heightProperty().doubleValue());
    }

    private void xmlDump() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Export to file");
        Options.getLastExportDir().ifPresent(fileChooser::setInitialDirectory);
        fileChooser.setInitialFileName(generateFileName());
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("XML Files", "*.xml"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));

        var selected = fileChooser.showSaveDialog(null);
        if (selected == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try (var outputStream = new FileOutputStream(selected)) {
                new Export()
                    .withIcons(getDao().getIcons())
                    .withCategories(getDao().getCategories(), false)
                    .withAccounts(getDao().getAccounts(), false)
                    .withCurrencies(getDao().getCurrencies())
                    .withContacts(getDao().getContacts(), false)
                    .withTransactions(getDao().getTransactions(), false)
                    .doExport(outputStream);
                Options.setLastExportDir(selected.getParent());
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    private void onImport() {
        new ImportWizard().showAndWait();
    }

    private void onReport() {
        var tab = tabPane.getSelectionModel().getSelectedItem().getContent();
        String prefix;
        if (tab instanceof TransactionListTab) {
            prefix = "transactions";
        } else if (tab instanceof AccountsTab) {
            prefix = "accounts";
        } else if (tab instanceof StatementTab) {
            prefix = "statement";
        } else {
            return;
        }

        var fileChooser = new FileChooser();
        fileChooser.setTitle("Report");
        Options.getLastExportDir().ifPresent(fileChooser::setInitialDirectory);
        fileChooser.setInitialFileName(generateFileName(prefix));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files", "*.html"));

        var selected = fileChooser.showSaveDialog(null);
        if (selected == null) {
            return;
        }

        try (var outputStream = new FileOutputStream(selected)) {
            if (tab instanceof TransactionListTab) {
                var filter = ((TransactionListTab) tab).getTransactionFilter();
                var transactions = getDao().getTransactions(filter)
                    .sorted(Transaction.BY_DATE)
                    .collect(Collectors.toList());
                Reports.reportTransactions(transactions, outputStream);
            } else if (tab instanceof AccountsTab) {
                var filter = ((AccountsTab) tab).getAccountFilter();
                var accounts = getDao().getAccounts(filter)
                    .sorted(Account.COMPARE_BY_CATEGORY.thenComparing(Account.COMPARE_BY_NAME))
                    .collect(Collectors.toList());
                Reports.reportAccounts(accounts, outputStream);
            } else {
                ((StatementTab) tab).getStatement()
                    .ifPresent(statement -> Reports.reportStatement(statement, outputStream));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Exception onInitDatabase(org.panteleyev.commons.database.ConnectionProfile profile) {
        var ds = onBuildDatasource(profile);
        return MoneyDAO.initDatabase(ds, profile.getSchema());
    }

    private MysqlDataSource onBuildDatasource(org.panteleyev.commons.database.ConnectionProfile profile) {
        try {
            var ds = new MysqlDataSource();

            ds.setCharacterEncoding("utf8");
            ds.setUseSSL(false);
            ds.setServerTimezone(TimeZone.getDefault().getID());
            ds.setPort(profileManager.getDatabasePort(profile));
            ds.setServerName(profileManager.getDatabaseHost(profile));
            ds.setUser(profile.getDataBaseUser());
            ds.setPassword(profile.getDataBasePassword());
            ds.setDatabaseName(profile.getSchema());
            ds.setAllowPublicKeyRetrieval(true);

            return ds;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void onImportSettings() {
        var d = new FileChooser();
        d.setTitle("Import Settings");
        d.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Settings", "*.xml")
        );
        var file = d.showOpenDialog(null);
        if (file != null) {
            try (var in = new FileInputStream(file)) {
                Preferences.importPreferences(in);
                sshManager.loadSessions();
                profileManager.loadProfiles();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void onExportSettings() {
        var d = new FileChooser();
        d.setTitle("Export Settings");
        d.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Settings", "*.xml")
        );
        var file = d.showSaveDialog(null);
        if (file != null) {
            try (var out = new FileOutputStream(file)) {
                PREFERENCES.exportSubtree(out);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
