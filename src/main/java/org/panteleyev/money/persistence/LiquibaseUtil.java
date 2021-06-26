/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */
package org.panteleyev.money.persistence;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSetStatus;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import java.sql.Connection;

public class LiquibaseUtil {
    public enum SchemaStatus {
        UP_TO_DATE,
        UPDATE_REQUIRED,
        INCOMPATIBLE
    }

    private static final String CHANGELOG_XML = "database/masterChangelog.xml";

    private final Liquibase liquibase;

    public LiquibaseUtil(Connection connection) {
        try {
            liquibase = new Liquibase(CHANGELOG_XML,
                new ClassLoaderResourceAccessor(),
                new JdbcConnection(connection));
        } catch (LiquibaseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void update() {
        try {
            liquibase.update(new Contexts());
        } catch (LiquibaseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void dropAndUpdate() {
        try {
            liquibase.dropAll();
            update();
        } catch (LiquibaseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public SchemaStatus checkSchemaUpdateStatus() {
        try {
            var report = liquibase.getChangeSetStatuses(new Contexts(), new LabelExpression(), true);
            return report.stream().anyMatch(ChangeSetStatus::getWillRun) ?
                SchemaStatus.UPDATE_REQUIRED : SchemaStatus.UP_TO_DATE;
        } catch (LiquibaseException ex) {
            return SchemaStatus.INCOMPATIBLE;
        }
    }
}
